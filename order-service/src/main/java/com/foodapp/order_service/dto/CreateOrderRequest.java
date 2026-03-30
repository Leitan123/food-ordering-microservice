package com.foodapp.order_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateOrderRequest {
    private Long userId;
    private List<OrderItemDTO> items;
    private String paymentMethod;
}
