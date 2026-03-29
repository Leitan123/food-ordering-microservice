package com.foodapp.order_service.service;

import com.foodapp.order_service.dto.CreateOrderRequest;
import com.foodapp.order_service.dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {
    OrderResponseDTO createOrder(CreateOrderRequest request);
    OrderResponseDTO getOrderById(Long id);
    List<OrderResponseDTO> getOrdersByUserId(Long userId);
}
