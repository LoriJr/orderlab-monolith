package com.viratech.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateOrderRequest(

        @NotBlank
        @Size(max = 50)
        String customerName,

        @NotBlank
        @Size(max = 254)
        @Email
        String customerEmail,

        @NotEmpty
        @Valid
        @NotNull
        List<OrderItemRequest> items
) {}
