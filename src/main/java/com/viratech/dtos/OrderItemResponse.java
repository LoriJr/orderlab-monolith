package com.viratech.dtos;

import java.math.BigDecimal;

public record OrderItemResponse(

        Long id,
        String description,
        Integer quantity,
        BigDecimal unitPrice
) {
}
