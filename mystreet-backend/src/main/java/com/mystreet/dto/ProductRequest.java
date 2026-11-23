package com.mystreet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

public record ProductRequest(
        @NotBlank String name,
        @NotBlank String brand,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        String imageUrl,
        @NotEmpty Set<@NotBlank String> sizes,
        @NotNull @Min(0) Integer stockQty) {
}
