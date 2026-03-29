package com.foodapp.order_service.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long menuId;
    private Integer quantity;
    private Double price;
}
