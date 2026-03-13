package com.example.shop.service.impl;

import com.example.shop.config.JsonUtil;
import com.example.shop.entity.*;

import com.example.shop.entity.enums.*;
import com.example.shop.exception.APIException;
import com.example.shop.hellper.SecuritySnapshotUtil;
import com.example.shop.payloads.*;
import com.example.shop.repository.*;
import com.example.shop.service.ActivityLogService;
import com.example.shop.service.KitChenService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl {
    @Autowired
    CartRepo cartRepo;
    @Autowired
    OrderRepo orderRepo;
    @Autowired
    RestaurantRepo restaurantRepo;
    @Autowired
    OrderItemServiceImpl orderItemService;
    @Autowired
    SimpMessagingTemplate messagingTemplate;
    @Autowired
    OrderItemRepo orderItemRepo;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    CartItemRepo cartItemRepo;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    ModelMapper modelMapper;

    @Value("${vnpay.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnpHashSecret;

    @Value("${vnpay.payUrl}")
    private String vnpPayUrl;

    @Value("${vnpay.returnUrl}")
    private String vnpReturnUrl;
    @Autowired
    JsonUtil jsonUtil;
    @Autowired
    ActivityLogService activityLogService;
    @Autowired
    KitChenService kitChenService;

    private String buildQuery(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private String buildHashData(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    public String payCartByVnpay(Long tableId, String tableToken) {

        RestaurantTable table = restaurantRepo
                .findByTableIdAndAccessToken(tableId, tableToken)
                .orElseThrow(() -> new APIException("Invalid table token"));

        Cart cart = cartRepo.findByRestaurantTable_tableId(tableId)
                .orElseThrow(() -> new APIException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty");
        }

        // 1️⃣ TẠO ORDER (CHƯA THANH TOÁN)
        Order order = new Order();
        order.setTable(table);
        order.setCart(cart);
        order.setTotalPrice(cart.getTotalPrice());
        order.setPaymentMethod(PaymentMethod.VNPAY);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStatus(OrderStatus.PENDING);
        order.setCashierName("CUSTOMER");

        Order savedOrder = orderRepo.save(order);

        // 2️⃣ VNPAY dùng ORDER_ID
        String txnRef = String.valueOf(savedOrder.getId());

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpTmnCode);
        params.put("vnp_Amount",
                order.getTotalPrice()
                        .multiply(BigDecimal.valueOf(100))
                        .toBigInteger()
                        .toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "ORDER_" + txnRef);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpReturnUrl);
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate",
                new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {

                String key = entry.getKey();
                String value = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);

                hashData.append(key).append("=").append(value).append("&");
                query.append(key).append("=").append(value).append("&");
            }
        }

// remove last &
        hashData.deleteCharAt(hashData.length() - 1);
        query.deleteCharAt(query.length() - 1);

        String secureHash = hmacSHA512(vnpHashSecret, hashData.toString());

        return vnpPayUrl
                + "?"
                + query
                + "&vnp_SecureHash="
                + secureHash;

    }

    public void handleVnpaySuccess(Long orderId) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new APIException("Order not found"));

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) return;

        Cart cart = order.getCart();


        // snapshot CartItem → OrderItem
        for (CartItem ci : cart.getCartItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setMenuItem(ci.getMenuItem());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getUnitPrice());
            oi.setDiscount(ci.getDiscount());
            oi.setTotalPrice(ci.getTotalPrice());
            orderItemRepo.save(oi);
        }

        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setStatus(OrderStatus.WAITING);
        order.setPaidAt(LocalDateTime.now());

        orderRepo.save(order);

        // clear cart
        cartItemRepo.deleteAll(cart.getCartItems());
    }


    public InvoiceDTO cashierPayCash(Long cartId, BigDecimal cashReceived) {

        //  Lấy cart theo bàn
        Cart cart = cartRepo.findByRestaurantTable_tableId(cartId)
                .orElseThrow(() -> new APIException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty");
        }


        BigDecimal total = cart.getTotalPrice();
        if (cashReceived.compareTo(total) < 0) {
            throw new APIException("Không đủ tiền");
        }

        BigDecimal step = new BigDecimal("100000");

        BigDecimal cashBase = cashReceived
                .divide(step, 0, RoundingMode.FLOOR)
                .multiply(step);

        BigDecimal cashMax = cashBase.add(step);
        if (cashReceived.compareTo(cashBase) < 0 ||
                cashReceived.compareTo(cashMax) > 0) {

            throw new APIException(
                    "Số tiền khách đưa phải trong khoảng "
                            + cashBase.toPlainString()
                            + " đến "
                            + cashMax.toPlainString()
            );
        }
        BigDecimal hardLimit = total.add(step.multiply(new BigDecimal("10")));

        if (cashReceived.compareTo(hardLimit) > 0) {
            throw new APIException("Số tiền quá lớn, vui lòng kiểm tra lại");
        }

        BigDecimal change = cashReceived.subtract(total);

        // TẠO ORDER (ĐÃ THANH TOÁN)
        Order order = new Order();
        order.setTable(cart.getRestaurantTable());
        order.setCart(cart);
        order.setTotalPrice(total);
        order.setPaymentMethod(PaymentMethod.CASH);
        String billCode = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", new Random().nextInt(10000));

        order.setBillCode(billCode);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setStatus(OrderStatus.WAITING);
        order.setCashReceived(cashReceived);
        order.setChangeAmount(change);
        order.setFloops(cart.getRestaurantTable().getFloor());
        order.setPaidAt(LocalDateTime.now());
        order.setCashierName(SecuritySnapshotUtil.getEmployeeName());
        Long userId = SecuritySnapshotUtil.getUserId();
        User userRef = entityManager.getReference(User.class, userId);
        order.setCustomer(userRef);



        Order savedOrder = orderRepo.save(order);

        // 4️⃣ CartItem → OrderItem
        for (CartItem ci : cart.getCartItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(savedOrder);
            oi.setMenuItem(ci.getMenuItem());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getUnitPrice());
            oi.setDiscount(ci.getDiscount());
            oi.setOrderItemStatus(EOrderItem.WAITING);
            oi.setNoteOrder(ci.getNote());
            oi.setTotalPrice(ci.getTotalPrice());
            orderItemRepo.save(oi);
        }

        // 5️⃣ Clear cart
        cartItemRepo.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cart.getRestaurantTable().setStatus(TableStatus.AVAILABLE);
        cart.setStatus(ECartStatus.CLOSED);
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepo.save(cart);

        // 6️⃣ Tạo hóa đơn trả về
        InvoiceDTO invoice = new InvoiceDTO();
        invoice.setOrderId(savedOrder.getId());
        invoice.setTableName(savedOrder.getTable().getTableName());
        invoice.setTotalAmount(total);
        invoice.setCashReceived(cashReceived);
        invoice.setChangeAmount(change);
        invoice.setPaidAt(savedOrder.getPaidAt());
        invoice.setCashierName(savedOrder.getCashierName());

        invoice.setItems(
                orderItemRepo.findByOrderId(savedOrder.getId())
                        .stream()
                        .map(i -> modelMapper.map(i, OrderItemDTO.class))
                        .toList()
        );
        kitChenService.autoAssignKitchenForWaitingOrders();


        activityLogService.log(
                "ORDER",
                savedOrder.getId(),
                "CASH_PAYMENT",
                "CartId=" + cart.getId()
                        + ", Total=" + total.toPlainString(),
                "BillCode=" + savedOrder.getBillCode()
                        + ", CashReceived=" + cashReceived.toPlainString()
                        + ", Change=" + change.toPlainString(),
                SecuritySnapshotUtil.getEmployeeName(), // thu ngân
                EActivityResult.SUCCESS,
                null,
                "Thu ngân thanh toán tiền mặt"
        );


        // 7️⃣ Realtime cho cashier / staff
        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "CASH_PAID",
                        "data", modelMapper.map(savedOrder, OrderDTO.class)
                )
        );



        return invoice;
    }


    private String hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey =
                    new javax.crypto.spec.SecretKeySpec(key.getBytes(), "HmacSHA512");
            mac.init(secretKey);
            return java.util.HexFormat.of().formatHex(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("HMAC error");
        }
    }


    @Transactional
    public void confirmMockMomo(Long orderId) {

        // 1️⃣ Lấy Order
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new APIException("Order not found"));

        // Nếu đã thanh toán thì bỏ qua
        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        Cart cart = order.getCart();
        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty");
        }

        // 2️⃣ CartItem → OrderItem
        for (CartItem ci : cart.getCartItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setMenuItem(ci.getMenuItem());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getUnitPrice());
            oi.setDiscount(ci.getDiscount());
            oi.setTotalPrice(ci.getTotalPrice());
            orderItemRepo.save(oi);
        }

        // 3️⃣ Update Order
        order.setPaymentMethod(PaymentMethod.VNPAY);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setStatus(OrderStatus.WAITING);
        order.setPaidAt(LocalDateTime.now());
        orderRepo.save(order);

        // 4️⃣ Clear Cart
        cartItemRepo.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepo.save(cart);

        // 5️⃣ Realtime cho cashier
        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "ORDER_COMPLETED",
                        "data", modelMapper.map(order, OrderDTO.class),
                        "cart", modelMapper.map(cart, CartDTO.class)
                )
        );
    }


    @Transactional
    public void customerCashRequest(Long tableId) {

        // Lấy cart theo bàn
        Cart cart = cartRepo.findByRestaurantTable_tableId(tableId)
                .orElseThrow(() -> new APIException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty");
        }

        // Đánh dấu cart đã sẵn sàng thanh toán
        cart.setStatus(ECartStatus.ORDERING);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepo.save(cart);

        // Chuẩn bị DTO gửi realtime
        CartDTO dto = new CartDTO();
        dto.setCartId(cart.getId());
        dto.setTableId(cart.getRestaurantTable().getTableId());
        dto.setFloops(cart.getRestaurantTable().getFloor());
        dto.setStatus(cart.getStatus().toString());
        dto.setUpdatedAt(cart.getUpdatedAt());
        dto.setRestaurantTable(cart.getRestaurantTable().getTableName());
        dto.setTotalPrice(cart.getTotalPrice());
        dto.setItems(
                cart.getCartItems().stream()
                        .map(ci -> {
                            CartItemDTO item = new CartItemDTO();
                            item.setMenuItemId(ci.getMenuItem().getId());
                            item.setMenuItemName(ci.getMenuItem().getName());
                            item.setQuantity(ci.getQuantity());
                            item.setUnitPrice(ci.getUnitPrice());
                            item.setSubTotal(ci.getTotalPrice());
                            return item;
                        })
                        .toList()
        );

        // Realtime cho cashier
        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "CART_UPDATED",
                        "data", dto
                )
        );
    }


    public Page<CartDTO> getPayments(
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder
    ) {

        Sort sort = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<Cart> page = cartRepo.findCashierCarts(pageable);
        return page.map(c -> {
            CartDTO dto = new CartDTO();
            dto.setCartId(c.getId());
            dto.setTableId(c.getRestaurantTable().getTableId());
            dto.setFloops(c.getRestaurantTable().getFloor());
            dto.setRestaurantTable(c.getRestaurantTable().getTableName());
            dto.setTotalPrice(c.getTotalPrice());
            dto.setStatus(c.getStatus().toString());
            dto.setUpdatedAt(c.getUpdatedAt());

            return dto;
        });
    }


    public List<CartItemDTO> getPaymentDetail(Long cartId) {
        return cartItemRepo.findCartItemDTO(cartId);
    }


    public void clearCartItemsAndAutoCancel(Long cartId, ECartItemDeleteReason reason) {
        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new APIException("Không tìm thấy cart"));

        if (cart.getStatus() == ECartStatus.READY_TO_PAY) {
            throw new APIException("Không thể xóa món khi đang thanh toán");
        }

        if (cart.getStatus() == ECartStatus.CLOSED) {
            throw new APIException("Không thể xóa cart khi trạng thái đang đóng");
        }

        if(cart.getStatus()==ECartStatus.IN_PROGRESS){
            throw new APIException("Khách thành chưa trả tiền không thể xóa");
        }

        cartItemRepo.deleteByCartId(cartId);

        // CẬP NHẬT TRẠNG THÁI
        cart.setStatus(ECartStatus.CLOSED);
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.getRestaurantTable().setStatus(TableStatus.AVAILABLE);

        activityLogService.log(
                "CART",
                cart.getId(),
                reason == ECartItemDeleteReason.EMPTY_TIMEOUT
                        ? "AUTO_CLEAR_EMPTY_CART"
                        : "CLEAR_CART",
                null,
                null,
                reason == ECartItemDeleteReason.EMPTY_TIMEOUT
                        ? "SYSTEM"
                        : SecuritySnapshotUtil.getEmployeeName(),
                EActivityResult.SUCCESS,
                SecuritySnapshotUtil.getEmail(),
                reason.getLabel()
        );

        // REALTIME
        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", reason == ECartItemDeleteReason.EMPTY_TIMEOUT
                                ? "CART_AUTO_CANCELLED"
                                : "CART_CANCELLED",
                        "cartId", cart.getId(),
                        "reason", reason.name()
                )
        );
    }

    //khách hàng order xong
    public String customerRequestREADY_TO_PAY(Long cartId) {

        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new APIException("Không tìm thấy giỏ hàng"));

        if (cart.getCartItems().isEmpty()) {
            throw new APIException("Không có món ăn trong giỏ");
        }
        if (cart.getStatus().equals(ECartStatus.READY_TO_PAY)) {
            throw new APIException("Đã gửi yêu cầu thanh toán");
        }
        cart.setStatus(ECartStatus.READY_TO_PAY);

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepo.save(cart);

        // DTO gọn cho staff
        CartDTO dto = new CartDTO();
        dto.setCartId(cart.getId());
        dto.setTableId(cart.getRestaurantTable().getTableId());
        dto.setRestaurantTable(cart.getRestaurantTable().getTableName());
        dto.setTotalPrice(cart.getTotalPrice());
        dto.setStatus(cart.getStatus().name());
        dto.setUpdatedAt(cart.getUpdatedAt());


        // realtime cho staff
        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "CART_READY_TO_PAY",
                        "data", dto
                )
        );

        return "Bàn của bạn đã gửi yêu cầu thanh toán";
    }

    public void sendCartToKitchen(Long cartId) {
        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new APIException("Cart không tồn tại"));
        if(cart.getStatus()==ECartStatus.IN_PROGRESS){
            throw new APIException("Đã gửi bếp");
        }
        if (!EnumSet.of(ECartStatus.ORDERING,ECartStatus.READY_TO_PAY).contains(cart.getStatus())) {
            throw new APIException(
                    "Chỉ cart đang",
                    ECartStatus.ORDERING.name(),
                    "hoặc",
                    ECartStatus.READY_TO_PAY.name(),
                    "mới được gửi bếp"
            );
        }


        cart.setStatus(ECartStatus.IN_PROGRESS);
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepo.save(cart);

        activityLogService.log(
                "CART",
                cart.getId(),
                "SEND_TO_KITCHEN",
                null,
                null,
                SecuritySnapshotUtil.getEmployeeName(),
                EActivityResult.SUCCESS,
                null,
                "Gửi cart sang bếp"
        );

        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "CART_STATUS_CHANGED",
                        "data", Map.of(
                                "cartId", cart.getId(),
                                "status", cart.getStatus().name()
                        )
                )
        );

        messagingTemplate.convertAndSend(
                "/topic/kitchen",
                Map.of(
                        "event", "NEW_CART_IN_PROGRESS",
                        "data", Map.of(
                                "cartId", cart.getId()
                        )
                )
        );

    }

}