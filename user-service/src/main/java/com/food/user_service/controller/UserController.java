package com.food.user_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class UserController {

    @GetMapping
    public String root() {
        return "User Service is up and running";
    }

    @GetMapping("health")
    public String health() {
        return "ok";
    }
}
