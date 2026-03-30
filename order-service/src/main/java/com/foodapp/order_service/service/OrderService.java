package com.foodapp.order_service.service;

import com.foodapp.order_service.dto.CreateOrderRequest;
import com.foodapp.order_service.dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {

    //Create a new order
    OrderResponseDTO createOrder(CreateOrderRequest request);

    //Get order by ID
    OrderResponseDTO getOrderById(Long id);

    //Get all orders for a user
    List<OrderResponseDTO> getOrdersByUserId(Long userId);

    // Update order status manually
    OrderResponseDTO updateOrderStatus(Long orderId, String status);

    // remove an item
    void deleteOrder(Long orderId);

    // admin or history
    //List<OrderResponseDTO> getAllOrders();
}
