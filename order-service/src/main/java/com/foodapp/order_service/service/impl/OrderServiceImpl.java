package com.foodapp.order_service.service.impl;

import com.foodapp.order_service.dto.CreateOrderRequest;
import com.foodapp.order_service.dto.OrderItemDTO;
import com.foodapp.order_service.dto.OrderResponseDTO;
import com.foodapp.order_service.model.Order;
import com.foodapp.order_service.model.OrderItem;
import com.foodapp.order_service.model.OrderStatus; // import enum
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

    /**
     * Create a new order from the request
     */
    @Override
    public OrderResponseDTO createOrder(CreateOrderRequest request) {
        // Convert DTO items to entity items
        List<OrderItem> items = request.getItems().stream()
                .map(dto -> OrderItem.builder()
                        .menuId(dto.getMenuId())
                        .quantity(dto.getQuantity())
                        .price(dto.getPrice())
                        .build())
                .collect(Collectors.toList());

        // Calculate total price of the order
        double totalPrice = items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

        // Build order entity
        Order order = Order.builder()
                .userId(request.getUserId())
                .items(items)
                .status(OrderStatus.PENDING) // Use enum instead of string
                .totalPrice(totalPrice)
                .paymentMethod(request.getPaymentMethod())
                .build();

        // Save order to DB
        Order savedOrder = orderRepository.save(order);

        // Convert saved order to response DTO
        return mapToDTO(savedOrder);
    }

    /**
     * Get order by order ID
     */
    @Override
    public OrderResponseDTO getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    /**
     * Get all orders for a specific user
     */
    @Override
    public List<OrderResponseDTO> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert Order entity to OrderResponseDTO
     */
    private OrderResponseDTO mapToDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> {
                    OrderItemDTO dto = new OrderItemDTO();
                    dto.setMenuId(item.getMenuId());
                    dto.setQuantity(item.getQuantity());
                    dto.setPrice(item.getPrice());
                    return dto;
                })
                .collect(Collectors.toList());

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(order.getId());
        response.setUserId(order.getUserId());
        response.setStatus(String.valueOf(order.getStatus())); // returns enum
        response.setPaymentMethod(order.getPaymentMethod());
        response.setTotalPrice(order.getTotalPrice());
        response.setItems(itemDTOs);
        return response;
    }
}