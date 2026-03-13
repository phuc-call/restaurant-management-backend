package com.example.shop.controller;

import com.example.shop.payloads.CartDTO;
import com.example.shop.payloads.CartNoteRequest;
import com.example.shop.service.CartService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    @PutMapping("/employee/staff/carts/{cartId}/note")
    public ResponseEntity<CartNoteRequest>noteCartRequest(@PathVariable Long cartId, @RequestParam String note){
        CartNoteRequest cartNoteRequest=cartService.noteCart(note,cartId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/employee/staff/cart/{tableId}")
    public ResponseEntity<CartDTO>addToCartStaff(
            @PathVariable Long tableId,
            @RequestParam Long menuItemId){
        CartDTO cartDTO= cartService.addToCart(tableId,menuItemId,1);
        return ResponseEntity.ok(cartDTO);
    }

    @PostMapping("/employee/staff/provInvoice/{cartId}")
    public ResponseEntity<byte[]> provisionalInvoice(@PathVariable Long cartId) {

        byte[] pdf = cartService.generateBillPdf(cartId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=bill-" + cartId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

}

