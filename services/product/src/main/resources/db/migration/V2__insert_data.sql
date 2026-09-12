-- Categories
INSERT INTO category (id, name, description)
VALUES
    (nextval('category_seq'), 'Electronics', 'Electronic devices and accessories'),
    (nextval('category_seq'), 'Books', 'Printed and digital books'),
    (nextval('category_seq'), 'Clothing', 'Clothes and fashion products');

-- Products
INSERT INTO product (
    id,
    name,
    description,
    available_quantity,
    price,
    category_id
)
VALUES
    (
        nextval('product_seq'),
        'Laptop',
        'Professional laptop',
        10,
        1299.99,
        (SELECT id FROM category WHERE name = 'Electronics' LIMIT 1)
    ),
    (
        nextval('product_seq'),
        'Wireless Mouse',
        'Ergonomic wireless mouse',
        30,
        29.99,
        (SELECT id FROM category WHERE name = 'Electronics' LIMIT 1)
    ),
    (
        nextval('product_seq'),
        'Clean Code',
        'Software development book',
        20,
        39.90,
        (SELECT id FROM category WHERE name = 'Books' LIMIT 1)
    ),
    (
        nextval('product_seq'),
        'T-Shirt',
        'Classic cotton T-shirt',
        50,
        19.99,
        (SELECT id FROM category WHERE name = 'Clothing' LIMIT 1)
    );