package com.spring.cloud.practice.service.order.controller;

import com.spring.cloud.practice.user.api.client.UserClient;
import com.spring.cloud.practice.user.api.dto.UserDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final UserClient userClient;

    public OrderController(UserClient userClient) {
        this.userClient = userClient;
    }

    @GetMapping("/getUserByOrderId")
    public UserDTO getUserByOrderId() {
        return userClient.getUserById(1L);
    }
}