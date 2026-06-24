package com.example.shop.service;

import com.example.shop.payloads.OrderDTO;

public interface OrderService {
    OrderDTO createOrder(Long tableId);
    OrderDTO getOrderById(Long orderId);
    String checkout(Long orderId);
}
