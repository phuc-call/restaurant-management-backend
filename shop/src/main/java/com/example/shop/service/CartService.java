package com.example.shop.service;

import com.example.shop.entity.Cart;
import com.example.shop.entity.CartItem;
import com.example.shop.payloads.CartDTO;
import com.example.shop.payloads.CartItemDTO;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {
//    void removeItem(Long cartItemId);
//    void clearCart(Long cartId);
//    List<CartItemDTO> getItemsByCart(Long cartId);
//    BigDecimal calculateCartTotal(Long cartId);
    CartDTO addToCart(Long tableId, Long menuItemId, int quantity);
    CartDTO getCartById(Long cartId);
}
