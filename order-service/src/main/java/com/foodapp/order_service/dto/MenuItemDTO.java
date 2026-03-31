package com.foodapp.order_service.dto;

import lombok.Data;

@Data
public class MenuItemDTO {
    private Long id;
    private String name;
    private Double price;
    private Boolean available;
}
