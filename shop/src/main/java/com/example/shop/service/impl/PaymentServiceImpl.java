package com.example.shop.service.impl;

import com.example.shop.config.JsonUtil;
import com.example.shop.entity.*;
import com.example.shop.entity.enums.*;
import com.example.shop.exception.APIException;
import com.example.shop.hellper.SecuritySnapshotUtil;
import com.example.shop.payloads.*;
import com.example.shop.payloads.Event.PaymentEventDTO;
import com.example.shop.payloads.reponse.PaymentStatusChangedEventDTO;
import com.example.shop.repository.*;
import com.example.shop.service.ActivityLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

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
    PaymentRepo paymentRepo;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    CartItemRepo cartItemRepo;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    OrderItemRepo orderItemRepo;
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

    public String payCartByVnpay(Long tableId, String tableToken) {

        RestaurantTable table = restaurantRepo
                .findByTableIdAndAccessToken(tableId, tableToken)
                .orElseThrow(() -> new APIException("Invalid table token"));

        Cart cart = cartRepo.findByRestaurantTable_tableId(tableId)
                .orElseThrow(() -> new APIException("cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new APIException("cart is empty");
        }

        // snapshot giống MoMo
        List<CartItemSnapshot> snapshots = cart.getCartItems().stream().map(item -> {
            CartItemSnapshot s = new CartItemSnapshot();
            s.setMenuItemId(item.getMenuItem().getId());
            s.setMenuItemName(item.getMenuItem().getName());
            s.setUnitPrice(item.getUnitPrice());
            s.setQuantity(item.getQuantity());
            s.setDiscount(item.getDiscount());
            s.setTotalPrice(
                    item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
                            .subtract(item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO)
            );
            return s;
        }).toList();

        Payment payment = new Payment();
        payment.setMethod(PaymentMethod.VNPAY);
        payment.setAmount(cart.getTotalPrice());
        payment.setUpdateBy(
                Optional.ofNullable(SecuritySnapshotUtil.getUserName())
                        .orElse("CUSTOMER")
        );

        payment.setUpdatedRole(
                Optional.ofNullable(SecuritySnapshotUtil.getRole())
                        .orElse("CUSTOMER")
        );
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTable(table);

        try {
            payment.setCartSnapshot(objectMapper.writeValueAsString(snapshots));
        } catch (Exception e) {
            throw new APIException("Snapshot error");
        }

        payment.setUpdateAt(LocalDateTime.now());

        paymentRepo.save(payment);

        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(payment.getPaymentId());
        dto.setTableId(table.getTableId());
        dto.setTableName(table.getTableName());
        dto.setTotalAmount(payment.getAmount());
        dto.setStatus(PaymentStatus.PENDING);
        dto.setMethod(PaymentMethod.VNPAY);
        dto.setUpdateAt(LocalDateTime.now());

        dto.setItems(snapshots);

        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "VNPAY_CREATED",
                        "data", dto
                )
        );

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpTmnCode);
        params.put("vnp_Amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", String.valueOf(payment.getPaymentId()));
        params.put("vnp_OrderInfo", "PAY_ORDER_" + payment.getPaymentId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpReturnUrl);
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate",
                new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date()));

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

        hashData.deleteCharAt(hashData.length() - 1);
        query.deleteCharAt(query.length() - 1);

        String secureHash = hmacSHA512(vnpHashSecret, hashData.toString());

        return vnpPayUrl
                + "?"
                + query
                + "&vnp_SecureHash="
                + secureHash;
    }

    public InvoiceDTO cashierPayCash(Long paymentId, BigDecimal cashReceived) {

        Payment payment = paymentRepo.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new APIException("Không tìm thấy lượt mua"));
        Cart cart = cartRepo.findByRestaurantTable_tableId(payment.getTable().getTableId())
                .orElseThrow(() -> new APIException("Cart not found"));

        if (payment.getStatus() != PaymentStatus.UNPAID) {
            throw new APIException("Đã thanh toán");
        }


        if (!payment.getProcessing()) {
            payment.setProcessing(true);
            paymentRepo.save(payment);
        }
        if (orderRepo.existsByPayment_PaymentId(paymentId)) {
            throw new APIException("Đã xác nhận vào thực đơn");
        }
        payment.setProcessing(true);
        payment.setStatus(PaymentStatus.SUCCESS);

        BigDecimal total = payment.getAmount();

        if (cashReceived.compareTo(total) < 0) {
            throw new APIException("Không đủ tiền");
        }
        BigDecimal change = cashReceived.subtract(total);

        PaymentDTO paymentDto = new PaymentDTO();
        paymentDto.setPaymentId(payment.getPaymentId());
        paymentDto.setTableId(payment.getTable().getTableId());
        paymentDto.setTableName(payment.getTable().getTableName());
        if(cart.getCartItems().isEmpty()||cart.getTotalPrice().compareTo(BigDecimal.ZERO)==0){
            paymentDto.setTotalAmount(BigDecimal.ZERO);
        }else {
            paymentDto.setTotalAmount(payment.getAmount());
        }
        paymentDto.setStatus(PaymentStatus.SUCCESS);
        paymentDto.setMethod(payment.getMethod());
        paymentDto.setUpdateAt(payment.getUpdateAt());
        try {
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new APIException("Payment already process");
        }
        // Lấy cart thực tế


        Order order = new Order();
        order.setTable(payment.getTable());
        order.setTotalPrice(total);
        order.setPayment(payment);
        order.setTable(payment.getTable());
        order.setStatus(OrderStatus.COMPLETED);
        Order savedOrder = orderRepo.save(order);

        List<CartItemSnapshot> items;
        try {
            items = objectMapper.readValue(
                    payment.getCartSnapshot(),
                    new TypeReference<List<CartItemSnapshot>>() {
                    }
            );
        } catch (Exception e) {
            throw new APIException("Snapshot error");
        }

        orderItemService.createFromSnapshot(savedOrder, items);

        // Clear cart

        cartItemRepo.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepo.save(cart);
        CartDTO cartDTO=modelMapper.map(cart,CartDTO.class);

        //Invoice
        InvoiceDTO invoice = new InvoiceDTO();
        invoice.setOrderId(savedOrder.getId());
        invoice.setTableName(payment.getTable().getTableName());
        invoice.setTotalAmount(total);
        invoice.setCashReceived(cashReceived);
        invoice.setChangeAmount(change);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setCashierName(SecuritySnapshotUtil.getUserName());

        invoice.setItems(
                orderItemRepo.findById(savedOrder.getId())
                        .stream()
                        .map(i -> modelMapper.map(i, OrderItemDTO.class))
                        .toList()
        );


        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "CASH_PAID",
                        "data", paymentDto,
                        "cart",cartDTO
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


    //create order and notification

    public void confirmMockMomo(Long paymentId) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new APIException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) return;

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepo.save(payment);

        Cart cart = cartRepo.findByRestaurantTable_tableId(
                payment.getTable().getTableId()
        ).orElseThrow(() -> new APIException("Cart not found"));

        Order order = new Order();
        order.setTable(payment.getTable());

        order.setTotalPrice(payment.getAmount());
        order.setStatus(OrderStatus.PAID);

        Order savedOrder = orderRepo.save(order);

        List<CartItemSnapshot> items;
        try {
            items = objectMapper.readValue(
                    payment.getCartSnapshot(),
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException e) {
            throw new APIException("Failed to snapshot cart");
        }

        orderItemService.createFromSnapshot(savedOrder, items);

        // RESET CART
        cart.setStatus(ECartStatus.ORDERED);
        cartItemRepo.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepo.save(cart);

        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(payment.getPaymentId());
        dto.setTableId(payment.getTable().getTableId());
        dto.setTableName(payment.getTable().getTableName());
        dto.setTotalAmount(payment.getAmount());
        dto.setStatus(PaymentStatus.SUCCESS);
        dto.setMethod(PaymentMethod.VNPAY);
        try {
            List<CartItemSnapshot> itemSnapshots = objectMapper.readValue(
                    payment.getCartSnapshot(),
                    new TypeReference<List<CartItemSnapshot>>() {
                    });
        } catch (Exception e) {
            dto.setItems(items);
        }

        dto.setUpdateAt(LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "PAYMENT_COMPLETED",
                        "data", dto
                )
        );

    }

    public void customerCashRequest(Long tableId) {

        // 1. Lấy cart theo bàn
        Cart cart = cartRepo.findByRestaurantTable_tableId(tableId)
                .orElseThrow(() -> new APIException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty");
        }

        // 2. OLD SNAPSHOT (cart)
        String oldSnapshot = jsonUtil.toJson(cart);

        // 3. SNAPSHOT danh sách món
        List<CartItemSnapshot> snapshots = cart.getCartItems().stream()
                .map(item -> {
                    CartItemSnapshot s = new CartItemSnapshot();
                    s.setMenuItemId(item.getMenuItem().getId());
                    s.setMenuItemName(item.getMenuItem().getName());
                    s.setQuantity(item.getQuantity());
                    s.setUnitPrice(item.getUnitPrice());
                    s.setDiscount(item.getDiscount());
                    s.setTotalPrice(item.getTotalPrice());
                    return s;
                }).toList();


        //tìm payment unpaid
        Payment payment = paymentRepo.findEditablePayment(tableId)
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setTable(cart.getRestaurantTable());
                    p.setStatus(PaymentStatus.UNPAID);
                    p.setProcessing(false);
                    return p;
                });


        boolean isNew = payment.getPaymentId() == null;

        //luôn update dữ liệu
        payment.setTable(cart.getRestaurantTable());
        payment.setMethod(PaymentMethod.CASH);
        payment.setAmount(cart.getTotalPrice());
        payment.setStatus(PaymentStatus.UNPAID);
        payment.setMethod(PaymentMethod.CASH);
        payment.setCartSnapshot(jsonUtil.toJson(snapshots));

        payment.setUpdateBy(
                Optional.ofNullable(SecuritySnapshotUtil.getUserName())
                        .orElse("SYSTEM")
        );

        payment.setUpdatedRole(Optional.ofNullable(SecuritySnapshotUtil.getRole()).orElse("SYSTEM"));
        payment.setUpdateAt(LocalDateTime.now());

        Payment savedPaymentDB = paymentRepo.save(payment);
        // 5. NEW SNAPSHOT
        String newSnapshot = jsonUtil.toJson(savedPaymentDB);

        // 6. ACTIVITY LOG (GIỐNG BOOKING)
        activityLogService.log(
                "PAYMENT",
                savedPaymentDB.getPaymentId(),
                PaymentStatus.UNPAID.toString(),
                oldSnapshot,
                newSnapshot,
                null,
                null,
                "Customer requested cash payment at counter"
        );

        // 7. DTO gửi realtime cho nhân viên
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(savedPaymentDB.getPaymentId());
        dto.setTableId(cart.getRestaurantTable().getTableId());
        dto.setTableName(cart.getRestaurantTable().getTableName());
        dto.setCartId(cart.getId());
        dto.setItems(snapshots);
        dto.setTotalAmount(cart.getTotalPrice());
        dto.setStatus(PaymentStatus.UNPAID);
        dto.setMethod(PaymentMethod.CASH);

        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", isNew ? "CASH_REQUEST_CREATED" : "CASH_REQUEST_UPDATED",
                        "data", dto
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<PaymentDTO> getPayments(
            PaymentStatus status,
            String keyword,
            int pageNumber,
            int pageSize,
            String sortBy,
            String order
    ) {

        Sort sort = order.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<Payment> page = paymentRepo.searchPaymentsForStaff(
                status,
                (keyword == null || keyword.isBlank()) ? null : keyword,
                pageable
        );

        return page.map(p -> {
            PaymentDTO dto = new PaymentDTO();
            dto.setPaymentId(p.getPaymentId());
            dto.setTableId(p.getTable().getTableId());
            dto.setTableName(p.getTable().getTableName());
            dto.setTotalAmount(p.getAmount());
            dto.setStatus(p.getStatus());
            dto.setMethod(p.getMethod());
            dto.setUpdateAt(p.getUpdateAt());
            if (p.getCartSnapshot() != null && !p.getCartSnapshot().isEmpty()) {
                try {
                    List<CartItemSnapshot> itemSnapshots =
                            objectMapper.readValue(p.getCartSnapshot(), new TypeReference<List<CartItemSnapshot>>() {
                            });
                    dto.setItems(itemSnapshots);
                } catch (Exception e) {
                    dto.setItems(List.of());
                }
            }
            return dto;

        });
    }

    @Transactional(readOnly = true)
    public PaymentDTO getPaymentDetail(Long paymentId) {

        Payment p = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new APIException("Payment not found"));

        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(p.getPaymentId());
        dto.setTableId(p.getTable().getTableId());
        dto.setTableName(p.getTable().getTableName());
        dto.setTotalAmount(p.getAmount());
        dto.setMethod(p.getMethod());
        dto.setStatus(p.getStatus());
        dto.setUpdateAt(p.getUpdateAt());

        if (p.getCartSnapshot() != null && !p.getCartSnapshot().isEmpty()) {
            try {
                List<CartItemSnapshot> items =
                        objectMapper.readValue(
                                p.getCartSnapshot(),
                                new TypeReference<List<CartItemSnapshot>>() {
                                }
                        );
                dto.setItems(items);
            } catch (Exception e) {
                dto.setItems(List.of());
            }
        }
        return dto;
    }

    @Transactional
    public void hidePayment(Long paymentId) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new APIException("Không tìm thấy payment"));
        if(payment.getStatus()!=PaymentStatus.SUCCESS){
            throw new APIException("Chỉ xóa được giao dịch thành công");
        }
        if(payment.isHidden())return;

        payment.setHidden(true);

        activityLogService.log(
                "PAYMENT",
                paymentId,
                "HIDE",
                null,
                null,
                SecuritySnapshotUtil.getUserName(),
                null,
                null
        );
        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "PAYMENT_HIDDEN",
                        "paymentId", payment.getPaymentId()
                )
        );

    }
}