package com.foodapp.order_service.service;

import com.foodapp.order_service.dto.OrderItemHistoryDTO;
import com.foodapp.order_service.dto.PageResponse;

public interface OrderHistoryService {

    /**
     * Get paginated order item history
     *
     * @param itemName keyword to search item name (can be partial)
     * @param status   filter by status (CANCELLED / DELIVERED), optional
     * @param page     page number (1-based)
     * @param size     number of items per page
     * @return Page of OrderItemHistory
     */
    PageResponse<OrderItemHistoryDTO> getOrderItemHistory(
            Long userId, String itemName, String status, int page, int size
    );

    void deleteOrderItemHistory(Long historyId);
}