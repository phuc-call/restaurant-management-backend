package com.example.shop.controller;

import com.example.shop.payloads.CartDTO;
import com.example.shop.service.CartService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
@RequiredArgsConstructor
public class CartController {
    @Autowired
    CartService cartService;
    @PostMapping("/public/cart/{tableId}")
    public ResponseEntity<CartDTO>addToCart(
            @PathVariable Long tableId,
            @RequestParam Long menuItemId){
        CartDTO cartDTO= cartService.addToCart(tableId,menuItemId,1);
        return ResponseEntity.ok(cartDTO);
    }
    @GetMapping("/public/cart/{cartId}")
    public ResponseEntity<CartDTO>getCartById(@PathVariable Long cartId){
        CartDTO cartDTO=cartService.getCartById(cartId);
        return new ResponseEntity<>(cartDTO,HttpStatus.OK);
    }
}

