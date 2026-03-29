package com.foodapp.order_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderResponseDTO {
    private Long orderId;
    private Long userId;
    private String status;
    private Double totalPrice;
    private String paymentMethod;
    private List<OrderItemDTO> items;
}
