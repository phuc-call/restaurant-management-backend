package com.example.shop.service;

import com.example.shop.entity.CartItem;
import com.example.shop.entity.Order;
import com.example.shop.entity.OrderItem;
import com.example.shop.payloads.CartItemSnapshot;
import com.example.shop.payloads.OrderItemDTO;

import java.util.List;

public interface OrderItemService {
    List<OrderItem>CreateOrderItemFromCart(Order order, List<CartItem>cartItems);
    public List<OrderItem> createFromSnapshot(
            Order order,
            List<CartItemSnapshot> snapshots
    );
}
