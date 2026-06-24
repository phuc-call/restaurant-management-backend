package com.example.shop.service.impl;

import com.example.shop.entity.Cart;
import com.example.shop.entity.CartItem;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.CartDTO;
import com.example.shop.repository.CartItemRepo;
import com.example.shop.repository.CartRepo;
import com.example.shop.service.CartItemService;
import com.example.shop.service.CartService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;

@Transactional
@Service
public class CartServiceImpl implements CartService{
    @Autowired
    CartItemRepo cartItemRepo;
    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    CartRepo cartRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    CartItemService cartItemService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Override
    public CartDTO addToCart(Long tableId, Long menuItemId, int quantity) {

        // Lấy cart theo bàn
        Cart cartDB = cartRepo.findByRestaurantTable_tableId(tableId)
                .orElseThrow(() -> new APIException("Table has not cart!!"));

        // Add hoặc update quantity
        CartItem savedItem = cartItemService.addOrUpdateItem(cartDB, menuItemId, 1);

        // Tính tổng tiền giỏ hàng
        BigDecimal total = cartDB.getCartItems()
                .stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cartDB.setTotalPrice(total);
        cartRepo.save(cartDB);

        // Map DTO để gửi realtime
        CartDTO cartDTO = modelMapper.map(cartDB, CartDTO.class);

        // === Gửi realtime đến FE (theo tableId) ===
        Long tableIdRealtime = cartDB.getRestaurantTable().getTableId();
        messagingTemplate.convertAndSend("/topic/cart/" + tableIdRealtime, cartDTO);
        // trả về DTO
        return cartDTO;
    }
    @Override
    public CartDTO getCartById(Long cartId){
        Cart cart=cartRepo.findById(cartId).orElseThrow(()->
                new APIException("Not found cart!"));
        return modelMapper.map(cart,CartDTO.class);
    }
}
