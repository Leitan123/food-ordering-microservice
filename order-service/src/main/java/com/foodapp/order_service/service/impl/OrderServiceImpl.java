package com.foodapp.order_service.service.impl;

import com.foodapp.order_service.dto.CreateOrderRequest;
import com.foodapp.order_service.dto.OrderItemDTO;
import com.foodapp.order_service.dto.OrderResponseDTO;
import com.foodapp.order_service.model.Order;
import com.foodapp.order_service.model.OrderItem;
import com.foodapp.order_service.model.OrderItemHistory;
import com.foodapp.order_service.model.OrderStatus;
import com.foodapp.order_service.repository.OrderItemHistoryRepository;
import com.foodapp.order_service.repository.OrderRepository;
import com.foodapp.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemHistoryRepository orderItemHistoryRepository;


    /**
     * Create a new order from the request
     * Automatically sets status to PENDING initially
     * Can also trigger auto-confirm after payment (simulate here)
     */
    @Override
    public OrderResponseDTO createOrder(CreateOrderRequest request) {
        // Build order without items yet
        Order order = Order.builder()
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .build();

        // Convert DTO items to entity items and link them to the order
        List<OrderItem> items = request.getItems().stream()
                .map(dto -> OrderItem.builder()
                        .menuId(dto.getMenuId())
                        .itemName(dto.getItemName())
                        .quantity(dto.getQuantity())
                        .price(dto.getPrice())
                        .order(order) // link to parent
                        .build())
                .collect(Collectors.toList());

        order.setItems(items); // set items in order

        // Calculate total price
        double totalPrice = items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();
        order.setTotalPrice(totalPrice);

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Optional: auto-confirm status
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        savedOrder = orderRepository.save(savedOrder);

        // Map to DTO
        return mapToDTO(savedOrder);
    }

    /**
     * Get order by ID
     */
    @Override
    public OrderResponseDTO getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    /**
     * Get all orders for a user
     */
    @Override
    public List<OrderResponseDTO> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update order status manually
     */
    @Override
    public OrderResponseDTO updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        try {
            // Update status using enum
            order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status");
        }

        Order updatedOrder = orderRepository.save(order);
        return mapToDTO(updatedOrder);
    }

    /**
     * Remove an item from an existing order
     */
    @Override
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Only allow deletion if order is not yet delivered or cancelled
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot delete order. Already delivered.");
        }

        // Save all items to history
        for (OrderItem item : order.getItems()) {
            OrderItemHistory history = OrderItemHistory.builder()
                    .orderId(order.getId())
                    .menuId(item.getMenuId())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .status(order.getStatus().toString()) // preserve current status
                    .build();
            orderItemHistoryRepository.save(history);
        }

        // Delete the order
        orderRepository.delete(order);
    }

//    /**
//     * Get all orders (admin or history)
//     */
//    @Override
//    public List<OrderResponseDTO> getAllOrders() {
//        return orderRepository.findAll().stream()
//                .map(this::mapToDTO)
//                .collect(Collectors.toList());
//    }

    /**
     * Convert Order entity to DTO
     */
    private OrderResponseDTO mapToDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus().name());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setCreatedAt(order.getCreatedAt()); // map the timestamp
        dto.setItems(order.getItems().stream().map(item -> {
            OrderItemDTO itemDTO = new OrderItemDTO();
            itemDTO.setMenuId(item.getMenuId());
            itemDTO.setItemName(item.getItemName());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setPrice(item.getPrice());
            return itemDTO;
        }).toList());
        return dto;
    }
}