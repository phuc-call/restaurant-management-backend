package com.example.shop.service.impl;

import com.example.shop.entity.CartItem;
import com.example.shop.entity.MenuItem;
import com.example.shop.entity.Order;
import com.example.shop.entity.OrderItem;
import com.example.shop.payloads.CartItemSnapshot;
import com.example.shop.payloads.OrderItemDTO;
import com.example.shop.repository.OrderItemRepo;
import com.example.shop.service.OrderItemService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderItemServiceImpl implements OrderItemService {
    @Autowired
    OrderItemRepo orderItemRepo;
    @Autowired
    ModelMapper modelMapper;
    @Override
    public List<OrderItem> CreateOrderItemFromCart(Order order, List<CartItem>cartItems){
        List<OrderItem> orderItems=new ArrayList<>();
        for(CartItem ci:cartItems){
            OrderItem oi=new OrderItem();
            oi.setOrder(order);
            oi.setMenuItem(ci.getMenuItem());
            oi.setPrice(ci.getUnitPrice());
            oi.setQuantity(ci.getQuantity());
            oi.setDiscount(ci.getDiscount());
            oi.setTotalPrice(ci.getTotalPrice());
            orderItems.add(oi);
        }
        return orderItemRepo.saveAll(orderItems);
    }
    @Override
    public List<OrderItem> createFromSnapshot(
            Order order,
            List<CartItemSnapshot> snapshots
    ) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemSnapshot s : snapshots) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);

            // ⚠️ không fetch MenuItem lại → snapshot là nguồn sự thật
            oi.setMenuItem(null); // hoặc optional
            oi.setPrice(s.getUnitPrice());
            oi.setQuantity(s.getQuantity());
            oi.setDiscount(s.getDiscount());
            oi.setTotalPrice(s.getTotalPrice());

            orderItems.add(oi);
        }

        return orderItemRepo.saveAll(orderItems);
    }


}
