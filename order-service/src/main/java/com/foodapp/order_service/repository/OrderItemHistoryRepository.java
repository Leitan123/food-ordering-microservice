package com.foodapp.order_service.repository;

import com.foodapp.order_service.model.OrderItemHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemHistoryRepository extends JpaRepository<OrderItemHistory, Long> {

//    // Search by item name containing keyword and filter by status
//    Page<OrderItemHistory> findByItemNameContainingIgnoreCaseAndStatus(
//            String itemName,
//            String status,
//            Pageable pageable
//    );
//
//    // If no status filter, just search by name
//    Page<OrderItemHistory> findByItemNameContainingIgnoreCase(
//            String itemName,
//            Pageable pageable
//    );

    // Filter by userId, status (optional), and itemName (optional)
    Page<OrderItemHistory> findByUserIdAndStatusContainingIgnoreCaseAndItemNameContainingIgnoreCase(
            Long userId,
            String status,
            String itemName,
            Pageable pageable
    );

}