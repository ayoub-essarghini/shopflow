package com.ayoub.ecommerce.product;

import com.ayoub.ecommerce.exception.ProductPurchaseException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public Integer createProduct(ProductRequest productRequest) {

        Product product = productMapper.toProduct(productRequest);
        productRepository.save(product);
        return product.getId();
    }

    public List<PurchaseResponse> purchaseProducts(List<PurchaseRequest> request) {

        var productIds = request.stream().map(PurchaseRequest::productId).toList();
        var storedProducts = productRepository.findAllByIdInOrderById(productIds);
        if (productIds.size() != storedProducts.size()) {
            throw new ProductPurchaseException("One or more products does not exists");
        }

        var storedRequest = request.stream().sorted(Comparator.comparing(PurchaseRequest::productId))
                .toList();

        var purchasedProducts = new ArrayList<PurchaseResponse>();
        for (int i = 0 ; i < storedProducts.size(); i++)
        {
            var product = storedProducts.get(i);
            var purchaseRequest = storedRequest.get(i);
            if (product.getAvailableQuantity() < purchaseRequest.quantity()) {
                throw new ProductPurchaseException("Insufficient quantity for product: " + product.getName());
            }
            var newAvailableQuantity = product.getAvailableQuantity() - purchaseRequest.quantity();
            product.setAvailableQuantity(newAvailableQuantity);
            productRepository.save(product);
            purchasedProducts.add(productMapper.toProductPurchaseResponse(product,purchaseRequest.quantity()));

        }


        return purchasedProducts;
    }

    public ProductResponse getProductById(Integer productId) {

        return productRepository.findById(productId)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }


}
