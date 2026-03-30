package com.foodapp.order_service.controller;

import com.foodapp.order_service.dto.*;
import com.foodapp.order_service.service.OrderHistoryService;
import com.foodapp.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Controller", description = "Endpoints for managing customer orders and history")
public class OrderController {

    // Injecting the OrderService to handle business logic
    private final OrderService orderService;
    private final OrderHistoryService orderHistoryService;

    /**
     * Create a new order.
     *
     * @param request - DTO containing userId, order items, and payment method
     * @return created OrderResponseDTO with order details
     */
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    /**
     * Get a specific order by its ID.
     *
     * @param id - the order ID
     * @return OrderResponseDTO for the requested order
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * Get all orders for a specific user.
     *
     * @param userId - the ID of the user
     * @return list of OrderResponseDTOs for the user's orders
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    /**
     * Update the status of an order
     *
     * @param id - order ID
     * @return updated OrderResponseDTO
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    /**
     * Remove an item from an existing order
     *
     * @param id     - order ID
     * @return updated OrderResponseDTO
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);

        ApiResponse response = new ApiResponse(
                "Order deleted successfully",
                id,
                "SUCCESS"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get order item history with optional search and status filter.
     *
     * @param itemName optional item name to search
     * @param status   optional status filter (CANCELLED, DELIVERED)
     * @param page     page number, default 0
     * @param size     page size, default 10
     */
        @GetMapping("/history")
        public ResponseEntity<PageResponse<OrderItemHistoryDTO>> getHistory(
                @RequestParam Long userId,
                @RequestParam(required = false) String itemName,
                @RequestParam(required = false) String status,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size
        ) {
            PageResponse<OrderItemHistoryDTO> response = orderHistoryService
                    .getOrderItemHistory(userId, itemName, status, page, size);
            return ResponseEntity.ok(response);
        }

    /**
     * Delete an order item from history
     *
     * @param historyId ID of the order item history record
     * @return JSON response indicating success
     */
    @DeleteMapping("/history/{historyId}")
    public ResponseEntity<ApiResponse> deleteHistory(@PathVariable Long historyId) {
        orderHistoryService.deleteOrderItemHistory(historyId);
        return ResponseEntity.ok(new ApiResponse("Order item history deleted successfully", historyId,"status"));
    }


}