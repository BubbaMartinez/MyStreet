package com.mystreet.service;

import com.mystreet.domain.Product;
import com.mystreet.dto.ProductRequest;
import com.mystreet.dto.ProductResponse;
import com.mystreet.exception.ResourceNotFoundException;
import com.mystreet.repository.ProductRepository;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(String brand, String size) {
        Specification<Product> spec = Specification.where(null);
        if (brand != null && !brand.isBlank()) {
            spec = spec.and(ProductSpecs.brandEquals(brand));
        }
        if (size != null && !size.isBlank()) {
            spec = spec.and(ProductSpecs.hasSize(size));
        }
        return repository.findAll(spec).stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        return ProductResponse.from(find(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return ProductResponse.from(repository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = find(id);
        apply(product, request);
        return ProductResponse.from(repository.save(product));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(find(id));
    }

    private Product find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private void apply(Product product, ProductRequest r) {
        product.setName(r.name());
        product.setBrand(r.brand());
        product.setDescription(r.description());
        product.setPrice(r.price());
        product.setImageUrl(r.imageUrl());
        product.setSizes(new HashSet<>(r.sizes()));
        product.setStockQty(r.stockQty());
    }
}
