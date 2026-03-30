package com.foodapp.order_service.service.impl;

import com.foodapp.order_service.dto.OrderItemHistoryDTO;
import com.foodapp.order_service.dto.PageResponse;
import com.foodapp.order_service.model.OrderItemHistory;
import com.foodapp.order_service.repository.OrderItemHistoryRepository;
import com.foodapp.order_service.service.OrderHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderHistoryServiceImpl implements OrderHistoryService {

    private final OrderItemHistoryRepository historyRepository;

    @Override
    public PageResponse<OrderItemHistoryDTO> getOrderItemHistory(
            Long userId, String itemName, String status, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Use empty string if null
        String statusFilter = (status != null) ? status : "";
        String itemFilter = (itemName != null) ? itemName : "";

        Page<OrderItemHistory> historyPage = historyRepository
                .findByUserIdAndStatusContainingIgnoreCaseAndItemNameContainingIgnoreCase(
                        userId, statusFilter, itemFilter, pageable
                );

        List<OrderItemHistoryDTO> content = historyPage.getContent().stream()
                .map(h -> OrderItemHistoryDTO.builder()
                        .id(h.getId())
                        .orderId(h.getOrderId())
                        .menuId(h.getMenuId())
                        .itemName(h.getItemName())
                        .quantity(h.getQuantity())
                        .price(h.getPrice())
                        .status(h.getStatus())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return new PageResponse<>(content, page, size, historyPage.getTotalElements());
    }

    @Override
    public void deleteOrderItemHistory(Long historyId) {
        // Optional: check if exists
        OrderItemHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Order item history not found with id " + historyId));

        // Delete the record
        historyRepository.delete(history);
    }
}