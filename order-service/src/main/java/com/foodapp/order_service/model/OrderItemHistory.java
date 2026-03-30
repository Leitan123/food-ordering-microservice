package com.foodapp.order_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_item_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long menuId;
    private Long userId;
    private String itemName;
    private Integer quantity;
    private Double price;

    private String status;         // CANCELLED or DELIVERED
    private String reason;         // optional: why cancelled

    @CreationTimestamp
    private LocalDateTime createdAt;  // automatically set when record created

    private LocalDateTime deletedAt;   // set when item is cancelled/deleted
}