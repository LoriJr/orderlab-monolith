package com.viratech.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Order {

    private UUID id;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private Instant createdAt;
    private List<OrderItem> itens;
}
