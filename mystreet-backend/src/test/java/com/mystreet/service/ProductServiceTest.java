package com.mystreet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mystreet.domain.Product;
import com.mystreet.dto.ProductRequest;
import com.mystreet.dto.ProductResponse;
import com.mystreet.exception.ResourceNotFoundException;
import com.mystreet.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository repository;

    @InjectMocks
    ProductService service;

    private ProductRequest request() {
        return new ProductRequest("Pegasus", "Nike", "Running shoe",
                new BigDecimal("129.99"), null, Set.of("9", "10"), 5);
    }

    @Test
    void get_unknownId_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_persistsAndReturnsMappedProduct() {
        when(repository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse created = service.create(request());

        assertThat(created.name()).isEqualTo("Pegasus");
        assertThat(created.sizes()).containsExactly("10", "9");
        assertThat(created.stockQty()).isEqualTo(5);
    }

    @Test
    void update_existingProduct_overwritesFields() {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setName("Old");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse updated = service.update(id, request());

        assertThat(updated.name()).isEqualTo("Pegasus");
        assertThat(updated.brand()).isEqualTo("Nike");
    }

    @Test
    void delete_unknownId_throwsAndDoesNotDelete() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any(Product.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_withFilters_queriesRepository() {
        Product p = new Product();
        p.setName("Pegasus");
        p.setBrand("Nike");
        p.setPrice(new BigDecimal("129.99"));
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(p));

        List<ProductResponse> result = service.list("Nike", "9");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).brand()).isEqualTo("Nike");
    }
}
