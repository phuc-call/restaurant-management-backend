package com.example.shop.service;

import com.example.shop.entity.Cart;
import com.example.shop.entity.CartItem;
import com.example.shop.entity.MenuItem;
import com.example.shop.payloads.CartItemDTO;

import java.util.List;

public interface CartItemService {
    CartItem addOrUpdateItem(Cart cart, Long menuItemId, int quantity);
    String clearCartAll(Long cartId);
    String deleteCartItem(Long cartItemId);
    List<CartItemDTO>getAllCartItem(Long cartId);
    CartItemDTO updateQuantity(Long cartItemId);
}
