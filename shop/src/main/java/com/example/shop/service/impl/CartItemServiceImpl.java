package com.example.shop.service.impl;

import com.example.shop.entity.Cart;
import com.example.shop.entity.CartItem;
import com.example.shop.entity.MenuItem;
import com.example.shop.entity.enums.ECartStatus;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.CartDTO;
import com.example.shop.payloads.CartItemDTO;
import com.example.shop.repository.CartItemRepo;
import com.example.shop.repository.CartRepo;
import com.example.shop.repository.MenuItemRepo;
import com.example.shop.service.CartItemService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class CartItemServiceImpl implements CartItemService {
    @Autowired
    CartRepo cartRepo;
    @Autowired
     MenuItemRepo menuItemRepo;
    @Autowired
    CartItemRepo cartItemRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Override
    //do not make controller
    public CartItem addOrUpdateItem(Cart cart, Long menuItem, int quantity){
        MenuItem menuItemDB=menuItemRepo.findById(menuItem).orElseThrow(()->
                new APIException("Not food in cart"));
        Optional<CartItem>existMenuItem=cartItemRepo.findByCartIdAndMenuItemId(cart.getId(),menuItem);
        CartItem cartItem;
        // update quantity
        if(existMenuItem.isPresent()){
            cartItem=existMenuItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        }
        //not exist
        else {
            cartItem=new CartItem();
            cartItem.setMenuItem(menuItemDB);
            cartItem.setUnitPrice(menuItemDB.getPrice());
            cartItem.setDiscount(BigDecimal.ZERO);
            cartItem.setQuantity(quantity);
            cartItem.setCart(cart);
            //add to cart list
            cart.getCartItems().add(cartItem);
        }
        //save cartItem
        BigDecimal total=cart.getCartItems()
                .stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        cart.setTotalPrice(total);
        cart.setStatus(ECartStatus.ORDERING);
        return cartItemRepo.save(cartItem);

    }
    @Override
    public String clearCartAll(Long cartId) {
        List<CartItem> cartItem=cartItemRepo.findByCartId(cartId);
        cartItemRepo.deleteAll(cartItem);
        return "delete foods success!!";
    }
    @Override
    public String deleteCartItem(Long cartItemId) {

        CartItem cartItem = cartItemRepo.findById(cartItemId)
                .orElseThrow(() -> new APIException("Foods not found"));

        Cart cart = cartItem.getCart();
        // delete
        cart.getCartItems().remove(cartItem);
        cartItemRepo.delete(cartItem);

        // update sum money for each other cart
        BigDecimal total = cart.getCartItems()
                .stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalPrice(total);
        if (cart.getCartItems().isEmpty()) {
            cart.setStatus(ECartStatus.ACTIVE);
        }
        cartRepo.save(cart);
        //RealTime
        CartDTO cartDTO=modelMapper.map(cart,CartDTO.class);
        Long tableId=cart.getRestaurantTable().getTableId();
        messagingTemplate.convertAndSend("/topic/cart/" + tableId, cartDTO);
        return "Item removed.";
    }
    public CartItemDTO updateQuantity(Long cartItemId) {

        CartItem cartItem = cartItemRepo.findById(cartItemId)
                .orElseThrow(() -> new APIException("CartItem not found"));
        Cart cart = cartItem.getCart();
        if (cart == null) {
            throw new APIException("Cart not found");
        }
        int newQuantity = cartItem.getQuantity() - 1;
        if (newQuantity <= 0) {
            cart.getCartItems().remove(cartItem);//remove out member
            cartItemRepo.delete(cartItem);
            BigDecimal total = cart.getCartItems()
                    .stream()
                    .map(CartItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            cart.setTotalPrice(total);
            cartRepo.save(cart);
            return null;
        }

        // Giảm số lượng
        cartItem.setQuantity(newQuantity);
        cartItemRepo.save(cartItem);

        // Cập nhật tổng tiền
        BigDecimal total = cart.getCartItems()
                .stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalPrice(total);
        cartRepo.save(cart);
        // Trả về item DTO
        return modelMapper.map(cartItem, CartItemDTO.class);
    }

    @Override
    public List<CartItemDTO> getAllCartItem(Long cartId){
       Cart cartDB=cartRepo.findById(cartId).orElseThrow(()->
               new APIException("Not fount any cart!!"));
       List<CartItem>cartItems=cartItemRepo.findByCartId(cartDB.getId());
       return cartItems.stream().map(items->modelMapper.map(items,CartItemDTO.class)).toList();
    }

}
