package com.foodapp.order_service.controller;

import com.foodapp.order_service.dto.CreateOrderRequest;
import com.foodapp.order_service.dto.OrderResponseDTO;
import com.foodapp.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    // Injecting the OrderService to handle business logic
    private final OrderService orderService;

    /**
     * Create a new order.
     * @param request - DTO containing userId, order items, and payment method
     * @return created OrderResponseDTO with order details
     */
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    /**
     * Get a specific order by its ID.
     * @param id - the order ID
     * @return OrderResponseDTO for the requested order
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * Get all orders for a specific user.
     * @param userId - the ID of the user
     * @return list of OrderResponseDTOs for the user's orders
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }
}