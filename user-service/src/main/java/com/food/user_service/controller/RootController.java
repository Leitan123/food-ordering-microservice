package com.food.user_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public String welcome() {
        return "User Service is up and running. Use /api/users for user management.";
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
