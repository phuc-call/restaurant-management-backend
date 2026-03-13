package com.example.shop.controller;


import com.example.shop.payloads.CartItemDTO;
import com.example.shop.payloads.CartItemNoteDTO;
import com.example.shop.payloads.CartItemNoteViewDTO;

import com.example.shop.service.CartItemService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")

public class CartItemController {
    @Autowired
    CartItemService cartItemService;

    @DeleteMapping("/public/carts/{cartId}/items")
    public ResponseEntity<Void> deleteAllItems(@PathVariable Long cartId) {
        cartItemService.clearCartAll(cartId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/public/carts/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        cartItemService.deleteCartItem(itemId);
        return ResponseEntity.noContent().build();
    }



    @PutMapping("/public/cart/cartItem/{cartItemId}/update")
    public ResponseEntity<CartItemDTO> updateQuantity(@PathVariable Long cartItemId) {
        CartItemDTO cartItemDTO = cartItemService.updateQuantity(cartItemId);
        if (cartItemDTO == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(cartItemDTO, HttpStatus.OK);
    }

    //staff
    @PutMapping("/employee/staff/carts/cartItem/{cartItemId}/update")
    public ResponseEntity<CartItemDTO> updateQuantityForStaff(@PathVariable Long cartItemId) {
        CartItemDTO cartItemDTO = cartItemService.updateQuantity(cartItemId);
        if (cartItemDTO == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(cartItemDTO, HttpStatus.OK);
    }

    @DeleteMapping("/employee/staff/carts/items/{itemId}")
    public ResponseEntity<Void> deleteItemForStaff(@PathVariable Long itemId) {
        cartItemService.deleteCartItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("public/cart-items/{cartItemId}/note")
    public ResponseEntity<CartItemNoteDTO> noteCartItem(@PathVariable Long cartItemId,@RequestBody CartItemNoteDTO dto){
        CartItemNoteDTO cartItemNoteDTO=cartItemService.noteCartItem(cartItemId,dto.getNote());
        return ResponseEntity.ok(cartItemNoteDTO);
    }

    @GetMapping("/public/carts/{cartId}/items/note")
    public ResponseEntity<List<CartItemNoteViewDTO>> getCartItemNotes(
            @PathVariable Long cartId
    ) {
        List<CartItemNoteViewDTO> result =
                cartItemService.getCartItemView(cartId);

        return ResponseEntity.ok(result);
    }
    @GetMapping("/public/carts/{cartId}/items")
    public ResponseEntity<List<CartItemDTO>> getAllCartItem(@PathVariable Long cartId) {
        List<CartItemDTO> cartItemDTO = cartItemService.getAllCartItem(cartId);
        return new ResponseEntity<List<CartItemDTO>>(cartItemDTO, HttpStatus.OK);
    }

}
