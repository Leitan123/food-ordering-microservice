package com.foodapp.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse {

    private String message;
    private Long orderId;
    private String status;
}
