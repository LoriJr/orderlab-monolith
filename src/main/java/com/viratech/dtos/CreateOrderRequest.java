package com.viratech.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

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
        List<OrderItemRequest> items
) {}
