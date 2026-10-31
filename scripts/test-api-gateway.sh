#!/usr/bin/env bash

set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8222}"
AUTH_TOKEN="${AUTH_TOKEN:-}"
REQUEST_TIMEOUT="${REQUEST_TIMEOUT:-15}"
READINESS_TIMEOUT="${READINESS_TIMEOUT:-60}"
NOTIFICATION_TIMEOUT="${NOTIFICATION_TIMEOUT:-20}"
KEEP_DATA="${KEEP_DATA:-false}"
VERIFY_NOTIFICATIONS="${VERIFY_NOTIFICATIONS:-auto}"
VERBOSE="${VERBOSE:-false}"

PASSED=0
FAILED=0
SKIPPED=0
START_TIME=$(date +%s)
RUN_ID="e2e-$(date +%Y%m%d%H%M%S)-$$"
TEMP_DIR=""
CUSTOMER_ID=""
PRODUCT_ID=""
ORDER_ID=""
DIRECT_PAYMENT_ORDER_ID=$((900000000 + ($$ % 90000000)))
ORDER_REFERENCE="ORDER-$RUN_ID"
DIRECT_PAYMENT_REFERENCE="PAYMENT-$RUN_ID"
LAST_BODY=""
LAST_CODE=""
LAST_TIME=""
CLEANUP_COMPLETE=false

if [[ -t 1 ]]; then
    GREEN='\033[0;32m'
    RED='\033[0;31m'
    YELLOW='\033[0;33m'
    BLUE='\033[0;34m'
    BOLD='\033[1m'
    RESET='\033[0m'
else
    GREEN=''
    RED=''
    YELLOW=''
    BLUE=''
    BOLD=''
    RESET=''
fi

usage() {
    cat <<'EOF'
Usage: scripts/test-api-gateway.sh [options]

Runs an end-to-end test against every business endpoint exposed by the API gateway.

Options:
  --base-url URL              Gateway URL (default: http://localhost:8222)
  --token TOKEN               Bearer token for a secured gateway
  --timeout SECONDS           Per-request timeout (default: 15)
  --readiness-timeout SECONDS Gateway readiness timeout (default: 60)
  --notification-timeout SEC  Kafka notification wait time (default: 20)
  --keep-data                 Keep records created by this run
  --skip-notifications        Skip MongoDB notification assertions
  --verbose                   Print every response body
  -h, --help                  Show this help

Environment variables:
  BASE_URL, AUTH_TOKEN, REQUEST_TIMEOUT, READINESS_TIMEOUT,
  NOTIFICATION_TIMEOUT, KEEP_DATA, VERIFY_NOTIFICATIONS, VERBOSE

Requirements: bash, curl, jq. Docker is optional but required for notification
assertions and complete cleanup of product, order, and payment test records.
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --base-url)
            [[ $# -ge 2 ]] || { echo "Missing value for --base-url" >&2; exit 2; }
            BASE_URL="$2"
            shift 2
            ;;
        --token)
            [[ $# -ge 2 ]] || { echo "Missing value for --token" >&2; exit 2; }
            AUTH_TOKEN="$2"
            shift 2
            ;;
        --timeout)
            [[ $# -ge 2 ]] || { echo "Missing value for --timeout" >&2; exit 2; }
            REQUEST_TIMEOUT="$2"
            shift 2
            ;;
        --readiness-timeout)
            [[ $# -ge 2 ]] || { echo "Missing value for --readiness-timeout" >&2; exit 2; }
            READINESS_TIMEOUT="$2"
            shift 2
            ;;
        --notification-timeout)
            [[ $# -ge 2 ]] || { echo "Missing value for --notification-timeout" >&2; exit 2; }
            NOTIFICATION_TIMEOUT="$2"
            shift 2
            ;;
        --keep-data)
            KEEP_DATA=true
            shift
            ;;
        --skip-notifications)
            VERIFY_NOTIFICATIONS=false
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

BASE_URL="${BASE_URL%/}"

section() {
    printf '\n%b%s%b\n' "$BOLD$BLUE" "$1" "$RESET"
}

pass() {
    PASSED=$((PASSED + 1))
    printf '%bPASS%b %s\n' "$GREEN" "$RESET" "$1"
}

fail() {
    FAILED=$((FAILED + 1))
    printf '%bFAIL%b %s\n' "$RED" "$RESET" "$1" >&2
}

skip() {
    SKIPPED=$((SKIPPED + 1))
    printf '%bSKIP%b %s\n' "$YELLOW" "$RESET" "$1"
}

fatal() {
    fail "$1"
    cleanup
    print_summary
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "Required command not found: $1" >&2
        exit 2
    }
}

pretty_body() {
    if jq -e . >/dev/null 2>&1 <<<"$1"; then
        jq . <<<"$1"
    else
        printf '%s\n' "$1"
    fi
}

extract_scalar() {
    local value="$1"

    if jq -e . >/dev/null 2>&1 <<<"$value"; then
        jq -r '.' <<<"$value"
    else
        printf '%s' "$value" | tr -d '\r\n'
    fi
}

status_matches() {
    local expected_codes=",$1,"
    [[ "$expected_codes" == *",$2,"* ]]
}

request() {
    local label="$1"
    local method="$2"
    local endpoint_path="$3"
    local expected_codes="$4"
    local payload="${5-}"
    local response_file="$TEMP_DIR/response.json"
    local curl_meta
    local curl_exit
    local curl_args=(
        --silent
        --show-error
        --connect-timeout "$REQUEST_TIMEOUT"
        --max-time "$REQUEST_TIMEOUT"
        --output "$response_file"
        --write-out '%{http_code} %{time_total}'
        --request "$method"
        --header 'Accept: application/json'
    )

    if [[ -n "$AUTH_TOKEN" ]]; then
        curl_args+=(--header "Authorization: Bearer $AUTH_TOKEN")
    fi

    if [[ -n "$payload" ]]; then
        curl_args+=(--header 'Content-Type: application/json' --data "$payload")
    fi

    curl_meta=$(curl "${curl_args[@]}" "$BASE_URL$endpoint_path" 2>"$TEMP_DIR/curl-error.log")
    curl_exit=$?
    LAST_BODY=$(cat "$response_file" 2>/dev/null || true)
    LAST_CODE="${curl_meta%% *}"
    LAST_TIME="${curl_meta#* }"

    if [[ $curl_exit -ne 0 ]]; then
        fail "$label: curl failed: $(cat "$TEMP_DIR/curl-error.log")"
        return 1
    fi

    if status_matches "$expected_codes" "$LAST_CODE"; then
        pass "$label [$method $endpoint_path -> $LAST_CODE in ${LAST_TIME}s]"
        if [[ "$VERBOSE" == true && -n "$LAST_BODY" ]]; then
            pretty_body "$LAST_BODY"
        fi
        return 0
    fi

    fail "$label: expected HTTP $expected_codes but received $LAST_CODE"
    pretty_body "$LAST_BODY" >&2
    return 1
}

assert_json() {
    local label="$1"
    shift

    if jq -e "$@" >/dev/null 2>&1 <<<"$LAST_BODY"; then
        pass "$label"
        return 0
    fi

    fail "$label: response did not satisfy the expected JSON expression"
    pretty_body "$LAST_BODY" >&2
    return 1
}

wait_for_gateway() {
    local deadline=$((SECONDS + READINESS_TIMEOUT))
    local code

    while (( SECONDS < deadline )); do
        code=$(curl --silent --output /dev/null --write-out '%{http_code}' \
            --connect-timeout 2 --max-time 3 "$BASE_URL/api/v1/products" 2>/dev/null || true)
        if [[ "$code" == 200 || "$code" == 401 || "$code" == 403 ]]; then
            pass "API gateway is reachable [$code]"
            return 0
        fi
        sleep 2
    done

    fatal "API gateway did not become ready at $BASE_URL within ${READINESS_TIMEOUT}s"
}

docker_available() {
    command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1
}

postgres_exec() {
    local database="$1"
    local statement="$2"
    docker exec ms_postgres psql -v ON_ERROR_STOP=1 -U ayoub -d "$database" -qAtc "$statement"
}

mongo_exec() {
    local expression="$1"
    docker exec ms_mongo mongo --quiet \
        --username root \
        --password ayoub \
        --authenticationDatabase admin \
        customer \
        --eval "$expression"
}

notification_count() {
    local filter="$1"
    mongo_exec "print(db.notification.countDocuments($filter))" 2>/dev/null | tail -1
}

wait_for_notification() {
    local label="$1"
    local filter="$2"
    local deadline=$((SECONDS + NOTIFICATION_TIMEOUT))
    local count

    while (( SECONDS < deadline )); do
        count=$(notification_count "$filter" || true)
        if [[ "$count" =~ ^[0-9]+$ ]] && (( count > 0 )); then
            pass "$label"
            return 0
        fi
        sleep 1
    done

    fail "$label: no matching MongoDB notification within ${NOTIFICATION_TIMEOUT}s"
    return 1
}

cleanup() {
    local cleanup_status=0
    local order_ids=""

    [[ "$CLEANUP_COMPLETE" == true ]] && return
    CLEANUP_COMPLETE=true

    if [[ "$KEEP_DATA" == true ]]; then
        skip "Cleanup disabled; run id is $RUN_ID"
        return
    fi

    section "Cleanup"

    if [[ -n "$CUSTOMER_ID" ]]; then
        local cleanup_args=(--silent --output /dev/null --request DELETE --max-time "$REQUEST_TIMEOUT")
        if [[ -n "$AUTH_TOKEN" ]]; then
            cleanup_args+=(--header "Authorization: Bearer $AUTH_TOKEN")
        fi
        if curl "${cleanup_args[@]}" "$BASE_URL/api/v1/customers/$CUSTOMER_ID"; then
            pass "Removed test customer"
        else
            fail "Could not remove test customer through the gateway"
            cleanup_status=1
        fi
    fi

    if docker_available; then
        if ! order_ids=$(postgres_exec order "SELECT COALESCE(string_agg(id::text, ','), '') FROM customer_order WHERE reference = '$ORDER_REFERENCE';"); then
            cleanup_status=1
        fi
        if [[ -z "$order_ids" && -n "$ORDER_ID" ]]; then
            order_ids="$ORDER_ID"
        fi
        if [[ -n "$order_ids" ]]; then
            if [[ "$order_ids" =~ ^[0-9]+(,[0-9]+)*$ ]]; then
                postgres_exec payment "DELETE FROM payment WHERE order_id IN ($order_ids);" >/dev/null || cleanup_status=1
            else
                cleanup_status=1
            fi
        fi
        postgres_exec order "DELETE FROM order_line WHERE order_id IN (SELECT id FROM customer_order WHERE reference = '$ORDER_REFERENCE'); DELETE FROM customer_order WHERE reference = '$ORDER_REFERENCE';" >/dev/null || cleanup_status=1
        postgres_exec payment "DELETE FROM payment WHERE order_id = $DIRECT_PAYMENT_ORDER_ID;" >/dev/null || cleanup_status=1
        if [[ -n "$PRODUCT_ID" ]]; then
            postgres_exec product "DELETE FROM product WHERE id = $PRODUCT_ID;" >/dev/null || cleanup_status=1
        fi
        mongo_exec "db.notification.deleteMany({\$or: [{\"paymentConfirmation.orderReference\": {\$in: [\"$ORDER_REFERENCE\", \"$DIRECT_PAYMENT_REFERENCE\"]}}, {\"orderConfirmation.orderReference\": \"$ORDER_REFERENCE\"}]})" >/dev/null || cleanup_status=1

        if [[ $cleanup_status -eq 0 ]]; then
            pass "Removed test product, order, payments, order lines, and notifications"
        else
            fail "One or more local database cleanup operations failed"
        fi
    else
        skip "Docker is unavailable; product, order, payment, and notification records were kept with run id $RUN_ID"
    fi
}

print_summary() {
    local duration=$(( $(date +%s) - START_TIME ))

    printf '\n%bTest summary%b\n' "$BOLD" "$RESET"
    printf 'Run ID:   %s\n' "$RUN_ID"
    printf 'Gateway:  %s\n' "$BASE_URL"
    printf 'Passed:   %s\n' "$PASSED"
    printf 'Failed:   %s\n' "$FAILED"
    printf 'Skipped:  %s\n' "$SKIPPED"
    printf 'Duration: %ss\n' "$duration"
}

require_command curl
require_command jq
TEMP_DIR=$(mktemp -d)
trap 'cleanup; rm -rf "$TEMP_DIR"' EXIT
trap 'exit 130' INT TERM

section "Readiness"
wait_for_gateway

section "Customer endpoints"
invalid_customer=$(jq -n '{firstName:"", lastName:"", email:"", address:null}')
request "Reject blank customer" POST /api/v1/customers 400 "$invalid_customer" || true

customer_payload=$(jq -n --arg run "$RUN_ID" '{
    firstName:"Gateway",
    lastName:"Tester",
    email:("gateway+" + $run + "@example.com"),
    address:{street:"Test Street", houseNumber:"42", zipCode:"10000"}
}')
request "Create customer" POST /api/v1/customers 200 "$customer_payload" || fatal "Customer creation is required for the remaining flow"
CUSTOMER_ID=$(extract_scalar "$LAST_BODY")
[[ -n "$CUSTOMER_ID" ]] || fatal "Customer creation did not return a string id"

request "Find customer by id" GET "/api/v1/customers/$CUSTOMER_ID" 200 || true
assert_json "Created customer has the expected email" --arg run "$RUN_ID" '.email == ("gateway+" + $run + "@example.com")' || true

request "Check customer exists" GET "/api/v1/customers/exists/$CUSTOMER_ID" 200 || true
assert_json "Customer existence is true" '. == true' || true

updated_customer=$(jq -n --arg id "$CUSTOMER_ID" --arg run "$RUN_ID" '{
    id:$id,
    firstName:"Updated",
    lastName:"Tester",
    email:("updated+" + $run + "@example.com"),
    address:{street:"Updated Street", houseNumber:"84", zipCode:"20000"}
}')
request "Update customer" PUT /api/v1/customers 202 "$updated_customer" || true
request "Read updated customer" GET "/api/v1/customers/$CUSTOMER_ID" 200 || true
assert_json "Customer update is persisted" --arg run "$RUN_ID" '.firstName == "Updated" and .email == ("updated+" + $run + "@example.com") and .address.houseNumber == "84"' || true

request "List customers" GET /api/v1/customers 200 || true
assert_json "Customer list contains the test customer" --arg id "$CUSTOMER_ID" 'any(.[]; .id == $id)' || true

section "Product endpoints"
request "List products" GET /api/v1/products 200 || fatal "Products are required for the remaining flow"
assert_json "Product response is a non-empty array" 'type == "array" and length > 0' || fatal "No product category is available for product creation"
CATEGORY_ID=$(jq -r '.[0].categoryId' <<<"$LAST_BODY")

invalid_product=$(jq -n --argjson category "$CATEGORY_ID" '{name:"Missing values", description:"Missing values", categoryId:$category}')
request "Reject product with missing quantity and price" POST /api/v1/products 400 "$invalid_product" || true

product_payload=$(jq -n --arg run "$RUN_ID" --argjson category "$CATEGORY_ID" '{
    name:("Gateway product " + $run),
    description:"Created by the API gateway end-to-end test",
    availableQuantity:10,
    price:12.50,
    categoryId:$category
}')
request "Create product" POST /api/v1/products 200 "$product_payload" || fatal "Product creation is required for the order flow"
PRODUCT_ID=$(jq -r 'if type == "number" then . else empty end' <<<"$LAST_BODY")
[[ "$PRODUCT_ID" =~ ^[0-9]+$ ]] || fatal "Product creation did not return a numeric id"

request "Find product by id" GET "/api/v1/products/$PRODUCT_ID" 200 || true
assert_json "Created product has quantity 10" --argjson id "$PRODUCT_ID" '.id == $id and .availableQuantity == 10' || true

negative_purchase=$(jq -n --argjson id "$PRODUCT_ID" '[{productId:$id, quantity:-1}]')
request "Reject negative purchase quantity" POST /api/v1/products/purchase 400 "$negative_purchase" || true

purchase_payload=$(jq -n --argjson id "$PRODUCT_ID" '[{productId:$id, quantity:1}]')
request "Purchase product" POST /api/v1/products/purchase 200 "$purchase_payload" || true
assert_json "Purchase response contains requested product" --argjson id "$PRODUCT_ID" 'length == 1 and .[0].productId == $id and .[0].quantity == 1' || true

request "Read product after purchase" GET "/api/v1/products/$PRODUCT_ID" 200 || true
assert_json "Product stock was reduced to 9" '.availableQuantity == 9' || true

insufficient_purchase=$(jq -n --argjson id "$PRODUCT_ID" '[{productId:$id, quantity:999999}]')
request "Reject purchase above available stock" POST /api/v1/products/purchase 404 "$insufficient_purchase" || true

request "Return not found for missing product" GET /api/v1/products/2147483647 404 || true

section "Payment endpoint"
request "Reject empty payment" POST /api/v1/payments 400 '{}' || true

direct_payment=$(jq -n \
    --arg reference "$DIRECT_PAYMENT_REFERENCE" \
    --arg run "$RUN_ID" \
    --argjson orderId "$DIRECT_PAYMENT_ORDER_ID" '{
        amount:7.50,
        paymentMethod:"VISA",
        orderId:$orderId,
        orderReference:$reference,
        customer:{firstName:"Direct", lastName:"Payment", email:("payment+" + $run + "@example.com")}
    }')
request "Create direct payment" POST /api/v1/payments 200 "$direct_payment" || true
assert_json "Payment creation returns an id" 'type == "number" and . > 0' || true

section "Order and order-line endpoints"
request "List orders" GET /api/v1/orders 200 || true
assert_json "Order list response is an array" 'type == "array"' || true

request "Reject empty order" POST /api/v1/orders 400 '{}' || true

order_payload=$(jq -n \
    --arg reference "$ORDER_REFERENCE" \
    --arg customerId "$CUSTOMER_ID" \
    --argjson productId "$PRODUCT_ID" '{
        reference:$reference,
        amount:25.00,
        paymentMethod:"VISA",
        customerId:$customerId,
        products:[{productId:$productId, quantity:2}]
    }')
request "Create order through the full service chain" POST /api/v1/orders 200 "$order_payload" || fatal "Order creation did not complete"
ORDER_ID=$(jq -r 'if type == "number" then . else empty end' <<<"$LAST_BODY")
[[ "$ORDER_ID" =~ ^[0-9]+$ ]] || fatal "Order creation did not return a numeric id"

request "Find order by id" GET "/api/v1/orders/$ORDER_ID" 200 || true
assert_json "Order response matches the created order" --arg reference "$ORDER_REFERENCE" --arg customerId "$CUSTOMER_ID" '.reference == $reference and .customerId == $customerId and .totalAmount == 25' || true

request "List orders after creation" GET /api/v1/orders 200 || true
assert_json "Order list contains the created order" --argjson id "$ORDER_ID" 'any(.[]; .id == $id)' || true

request "List lines for the created order" GET "/api/v1/order-lines/order/$ORDER_ID" 200 || true
assert_json "Order has one line with quantity 2" 'length == 1 and .[0].quantity == 2' || true

request "Return not found for missing order" GET /api/v1/orders/2147483647 404 || true

request "Read final product stock" GET "/api/v1/products/$PRODUCT_ID" 200 || true
assert_json "Order purchase reduced stock from 9 to 7" '.availableQuantity == 7' || true

section "Notification side effects"
if [[ "$VERIFY_NOTIFICATIONS" == auto ]]; then
    if docker_available; then
        VERIFY_NOTIFICATIONS=true
    else
        VERIFY_NOTIFICATIONS=false
    fi
fi

if [[ "$VERIFY_NOTIFICATIONS" == true ]]; then
    wait_for_notification "Direct payment notification has the expected MongoDB payload" "{\"paymentConfirmation.orderReference\": \"$DIRECT_PAYMENT_REFERENCE\", \"paymentConfirmation.amount\": 7.5, \"paymentConfirmation.customerEmail\": \"payment+$RUN_ID@example.com\"}" || true
    wait_for_notification "Order payment notification has the expected MongoDB payload" "{\"paymentConfirmation.orderReference\": \"$ORDER_REFERENCE\", \"paymentConfirmation.amount\": 25, \"paymentConfirmation.customerEmail\": \"updated+$RUN_ID@example.com\"}" || true
    wait_for_notification "Order confirmation has the expected MongoDB payload" "{\"orderConfirmation.orderReference\": \"$ORDER_REFERENCE\", \"orderConfirmation.amount\": 25, \"orderConfirmation.customer._id\": ObjectId(\"$CUSTOMER_ID\"), \"orderConfirmation.products\": {\$elemMatch: {productId: $PRODUCT_ID, quantity: 2}}}" || true
else
    skip "Notification assertions disabled or Docker unavailable"
fi

section "Customer deletion"
DELETED_CUSTOMER_ID="$CUSTOMER_ID"
if request "Delete customer" DELETE "/api/v1/customers/$DELETED_CUSTOMER_ID" 202; then
    CUSTOMER_ID=""
fi
request "Check deleted customer no longer exists" GET "/api/v1/customers/exists/$DELETED_CUSTOMER_ID" 200 || true
assert_json "Deleted customer existence is false" '. == false' || true

cleanup
print_summary

if (( FAILED > 0 )); then
    exit 1
fi

exit 0
