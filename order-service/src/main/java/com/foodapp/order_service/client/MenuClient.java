package com.foodapp.order_service.client;

import com.foodapp.order_service.dto.MenuItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "menu-service", url = "${menu-service.url:http://localhost:8002}")
public interface MenuClient {
    
    @GetMapping("/api/menu/{id}")
    MenuItemDTO getMenuItemById(@PathVariable("id") Long id);
}
