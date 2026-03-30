package com.foodapp.order_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderItemHistoryDTO {

    private Long id;
    private Long orderId;
    private Long menuId;
    private String itemName;
    private int quantity;
    private double price;
    private String status; // CANCELLED, DELIVERED
    private LocalDateTime createdAt;
}