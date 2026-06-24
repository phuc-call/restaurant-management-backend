package com.example.shop.service.impl;

import com.example.shop.entity.*;
import com.example.shop.entity.enums.ECartStatus;
import com.example.shop.entity.enums.OrderStatus;
import com.example.shop.entity.enums.TableStatus;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.OrderDTO;
import com.example.shop.repository.*;
import com.example.shop.service.OrderItemService;
import com.example.shop.service.OrderService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderItemRepo orderItemRepo;
    @Autowired
    OrderRepo orderRepo;
    @Autowired
    CartRepo cartRepo;
    @Autowired
    RestaurantRepo restaurantRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    CartItemRepo cartItemRepo;
    @Autowired
    OrderItemService orderItemService;

    @Override
    public OrderDTO createOrder(Long tableId) {
        Cart cart = cartRepo.findByRestaurantTable_tableId(tableId).orElseThrow(() ->
                new APIException("Cart not found for this table"));
        if (cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty!");
        }
        // Create new order
        Order order = new Order();
        order.setTable(cart.getRestaurantTable());
        order.setTable(cart.getRestaurantTable());
        order.setTotalPrice(cart.getTotalPrice());
        order.setStatus(OrderStatus.PENDING);
        Order saved = orderRepo.save(order);

        //main create Order
        orderItemService.CreateOrderItemFromCart(saved, cart.getCartItems());

        // update status of tables
        RestaurantTable table = cart.getRestaurantTable();
            table.setStatus(TableStatus.OCCUPIED);
        restaurantRepo.save(table);
        //reset cart
        cart.setStatus(ECartStatus.ORDERED);
        cartItemRepo.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepo.save(cart);
        return modelMapper.map(saved, OrderDTO.class);
    }

    @Override
    public String checkout(Long orderId) {
        Order order = orderRepo.findById(orderId) .orElseThrow(() ->
                new APIException("Order not found"));
        //waiting kitchen
        order.setStatus(OrderStatus.WAITING); orderRepo.save(order);

        RestaurantTable table = order.getTable();
        //after customer hangout need clear table
        if(table.getStatus()==TableStatus.AVAILABLE){
            table.setStatus(TableStatus.OCCUPIED);
        }
        restaurantRepo.save(table);
        return "Checkout successful.";
    }
    @Override
    public OrderDTO getOrderById(Long orderId){
        Order order = orderRepo.findById(orderId) .orElseThrow(() ->
                new APIException("Order not found"));
        return modelMapper.map(order,OrderDTO.class);
    }
}
