package com.foodapp.order_service.model;

public enum OrderStatus {
    PENDING,       // Order is created but not yet confirmed
    CONFIRMED,     // Order is confirmed by the restaurant
    PREPARING,     // Restaurant is preparing the order
    OUT_FOR_DELIVERY, // Delivery person picked up the order
    DELIVERED,     // Order delivered successfully
    CANCELLED      // Order cancelled by user or restaurant
}