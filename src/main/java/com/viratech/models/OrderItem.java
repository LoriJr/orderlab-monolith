package com.viratech.models;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {

    private UUID id;
    private Order order;
    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
}
