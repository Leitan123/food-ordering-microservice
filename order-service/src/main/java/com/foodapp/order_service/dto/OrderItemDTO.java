package com.foodapp.order_service.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long menuId;
    private String itemName;
    private Integer quantity;
    private Double price;
}
