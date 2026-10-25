package com.viratech.dtos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record OrderItemRequest(

        @NotBlank
        @Size(max = 255)
        String description,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 19, fraction = 2)
        BigDecimal unitPrice
) {}
