package com.mystreet.service;

import com.mystreet.domain.Product;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

final class ProductSpecs {

    private ProductSpecs() {}

    static Specification<Product> brandEquals(String brand) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase());
    }

    static Specification<Product> hasSize(String size) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.isMember(size.trim(), root.<Set<String>>get("sizes"));
        };
    }
}
