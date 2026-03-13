package com.example.shop.controller;

import com.example.shop.payloads.*;
import com.example.shop.service.CartItemService;
import com.example.shop.service.CartService;
import com.example.shop.service.RestaurantTableService;
import com.example.shop.service.impl.PaymentServiceImpl;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
@RequiredArgsConstructor
public class StaffOrderController {
    @Autowired
    RestaurantTableService restaurantTableService;
    @Autowired
    CartService cartService;
    @Autowired
    CartItemService cartItemService;
    @Autowired
    PaymentServiceImpl paymentService;

    @PostMapping("/employee/staff/tables/{tableId}/ordering")
    public ResponseEntity<RestaurantTableDTO> openTable(
            @PathVariable Long tableId
    ) {
        return ResponseEntity.ok(
                restaurantTableService.ORDERING(tableId)
        );
    }

//    @PostMapping("/employee/staff/carts/{cartId}/items")
//    public ResponseEntity<CartDTO>addToCart(
//            @PathVariable Long tableId,
//            @RequestParam Long menuItemId){
//        CartDTO cartDTO= cartService.addToCart(tableId,menuItemId,1);
//        return ResponseEntity.ok(cartDTO);
//    }

//    @PatchMapping("/employee/staff/cart-items/{cartItemId}/decrease")
//    public ResponseEntity<CartItemDTO> decreaseQuantity(
//            @PathVariable Long cartItemId
//    ) {
//        return ResponseEntity.ok(
//                cartItemService.updateQuantity(cartItemId)
//        );
//    }

//    @DeleteMapping("/employee/staff/cart-items/{cartItemId}")
//    public ResponseEntity<String> deleteItem(
//            @PathVariable Long cartItemId
//    ) {
//        return ResponseEntity.ok(
//                cartItemService.deleteCartItem(cartItemId)
//        );
//    }

//    @PostMapping("/employee/staff/carts/{cartId}/request-payment")
//    public ResponseEntity<String> requestPay(
//            @PathVariable Long cartId
//    ) {
//        return ResponseEntity.ok(
//                paymentService.customerRequestREADY_TO_PAY(cartId)
//        );
//    }
}
