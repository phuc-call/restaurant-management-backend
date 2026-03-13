package com.example.shop.service.impl;

import com.example.shop.entity.Cart;
import com.example.shop.entity.CartItem;
import com.example.shop.entity.MenuItem;
import com.example.shop.entity.enums.ECartStatus;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.CartDTO;
import com.example.shop.payloads.CartItemDTO;
import com.example.shop.payloads.CartItemNoteDTO;
import com.example.shop.payloads.CartItemNoteViewDTO;
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
import java.util.Map;
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

    private CartItemDTO toCartItemDTO(CartItem ci) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(ci.getId());
        dto.setMenuItemId(ci.getMenuItem().getId());
        dto.setMenuItemName(ci.getMenuItem().getName());
        dto.setQuantity(ci.getQuantity());
        dto.setUnitPrice(ci.getUnitPrice());
        dto.setSubTotal(
                ci.getUnitPrice()
                        .multiply(BigDecimal.valueOf(ci.getQuantity()))
                        .subtract(
                                Optional.ofNullable(ci.getDiscount())
                                        .orElse(BigDecimal.ZERO)
                        )
        );
        return dto;
    }

    @Override
    public CartItem addOrUpdateItem(Cart cart, Long menuItem, int quantity) {
        MenuItem menuItemDB = menuItemRepo.findById(menuItem).orElseThrow(() ->
                new APIException("Món ăn không tồn tại"));
        Optional<CartItem> existMenuItem = cartItemRepo.findByCartIdAndMenuItemId(cart.getId(), menuItem);
        CartItem cartItem;
        // update quantity
        if (quantity <= 0) {
            throw new APIException("Số lượng phải lớn hơn 0");
        }

        if (existMenuItem.isPresent()) {
            cartItem = existMenuItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setSubTotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        //not exist
        else {
            cartItem = new CartItem();
            cartItem.setMenuItem(menuItemDB);
            cartItem.setUnitPrice(menuItemDB.getPrice());
            cartItem.setDiscount(BigDecimal.ZERO);
            cartItem.setQuantity(quantity);
            cartItem.setSubTotal(menuItemDB.getPrice());
            cartItem.setCart(cart);
            //add to cart list
            cart.getCartItems().add(cartItem);
        }

        cartItemRepo.save(cartItem);

        //save cartItem
        BigDecimal total = cart.getCartItems()
                .stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalPrice(total);
        cart.setStatus(ECartStatus.ORDERING);
        cartRepo.save(cart);
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "CART_UPDATED",
                        "data", cartDTO
                )
        );

        List<CartItemDTO> itemDTOs = cart.getCartItems().stream()
                .map(this::toCartItemDTO)
                .toList();

        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "PAYMENT_DETAIL",
                        "data", Map.of(
                                "paymentId", cart.getId(),
                                "items", itemDTOs
                        )
                )
        );
        return cartItem;
    }

    @Override
    public String clearCartAll(Long cartId) {
        List<CartItem> cartItem = cartItemRepo.findByCartId(cartId);
        cartItemRepo.deleteAll(cartItem);
        return "delete foods success!!";
    }

    @Override
    public String deleteCartItem(Long cartItemId) {

        CartItem cartItem = cartItemRepo.findById(cartItemId)
                .orElseThrow(() -> new APIException("Foods not found"));

        Cart cart = cartItem.getCart();

        // remove item
        cart.getCartItems().remove(cartItem);
        cartItemRepo.delete(cartItem);

        // update total
        BigDecimal total = cart.getCartItems()
                .stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalPrice(total);
        if (cart.getCartItems().isEmpty()) {
            cart.setStatus(ECartStatus.ACTIVE);
        }
        cartRepo.save(cart);

        // CART UPDATED (list)
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "CART_UPDATED",
                        "data", cartDTO
                )
        );

        // 🔥 PAYMENT DETAIL (detail)
        List<CartItemDTO> itemDTOs = cart.getCartItems().stream()
                .map(this::toCartItemDTO)
                .toList();
        Long tableId = cart.getRestaurantTable().getTableId();

        messagingTemplate.convertAndSend(
                "/topic/cart/" + tableId,
                Map.of(
                        "event", "CART_UPDATED",
                        "data", cartDTO
                )
        );


        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "PAYMENT_DETAIL",
                        "data", Map.of(
                                "paymentId", cart.getId(),
                                "items", itemDTOs
                        )
                )
        );

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
            cart.getCartItems().remove(cartItem);
            cartItemRepo.delete(cartItem);
        } else {
            cartItem.setQuantity(newQuantity);
            cartItemRepo.save(cartItem);
        }

        // ==== RE-CALC TOTAL ====
        BigDecimal total = cart.getCartItems()
                .stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalPrice(total);
        cartRepo.save(cart);

        // ==== REALTIME (LUÔN GỬI) ====
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        messagingTemplate.convertAndSend(
                "/topic/cart/" + cart.getRestaurantTable().getTableId(),
                Map.of(
                        "event", "CART_UPDATED",
                        "data", cartDTO
                )
        );

        return newQuantity <= 0
                ? null
                : modelMapper.map(cartItem, CartItemDTO.class);
    }


    @Override
    public List<CartItemDTO> getAllCartItem(Long cartId) {
        Cart cartDB = cartRepo.findById(cartId).orElseThrow(() ->
                new APIException("Not fount any cart!!"));
        List<CartItem> cartItems = cartItemRepo.findByCartId(cartDB.getId());
        return cartItems.stream().map(items -> modelMapper.map(items, CartItemDTO.class)).toList();
    }

    @Override
    public CartItemNoteDTO noteCartItem(Long cartItemId, String note) {
        CartItem cartItem = cartItemRepo.findById(cartItemId).orElseThrow(() -> new APIException("Món ăn không tồn tại"));
        cartItem.setNote(note.trim());
        CartItem saveCartItem = cartItemRepo.save(cartItem);
        return modelMapper.map(saveCartItem, CartItemNoteDTO.class);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CartItemNoteViewDTO> getCartItemView(Long cartId) {
       List<CartItem>items=cartItemRepo.findByCartId(cartId);
       if (items.isEmpty()){
           throw new APIException("Không tìm thấy danh sách món ăn");
       }
        return items.stream().map(item -> {
            CartItemNoteViewDTO dto = new CartItemNoteViewDTO();
            dto.setCartItemId(item.getId());
            dto.setMenuName(item.getMenuItem().getName());
            dto.setNote(item.getNote());
            return dto;
        }).toList();

    }
}
