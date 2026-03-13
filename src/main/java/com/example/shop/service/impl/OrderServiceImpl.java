package com.example.shop.service.impl;

import com.example.shop.entity.*;
import com.example.shop.entity.enums.*;
import com.example.shop.exception.APIException;
import com.example.shop.hellper.BillHellper;
import com.example.shop.hellper.SecuritySnapshotUtil;
import com.example.shop.payloads.*;
import com.example.shop.repository.*;
import com.example.shop.service.ActivityLogService;
import com.example.shop.service.KitChenService;
import com.example.shop.service.OrderItemService;
import com.example.shop.service.OrderService;
import com.lowagie.text.*;


import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    @Autowired
    ActivityLogService activityLogService;
    @Autowired
    OrderItemRepo orderItemRepo;
    @Autowired
    OrderRepo orderRepo;
    @Autowired
    CartRepo cartRepo;
    @Autowired
    RestaurantRepo restaurantRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    CartItemRepo cartItemRepo;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    UserRepo userRepo;
    @Autowired
    KitChenService kitChenService;

    @Override
    public OrderDTO createOrder(Long tableId) {
        Cart cart = cartRepo.findByRestaurantTable_tableId(tableId).orElseThrow(() ->
                new APIException("Cart not found for this table"));
        if (cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty!");
        }
        // Create new order
        Order order = new Order();
        order.setTable(cart.getRestaurantTable());
        order.setTable(cart.getRestaurantTable());
        order.setTotalPrice(cart.getTotalPrice());
        order.setStatus(OrderStatus.PENDING);
        Order saved = orderRepo.save(order);

        //main create Order
        orderItemService.CreateOrderItemFromCart(saved, cart.getCartItems());

        // update status of tables
        RestaurantTable table = cart.getRestaurantTable();
        table.setStatus(TableStatus.OCCUPIED);
        restaurantRepo.save(table);
        //reset cart
        cart.setStatus(ECartStatus.CLOSED);
        cart.getRestaurantTable().setStatus(TableStatus.AVAILABLE);
        cartItemRepo.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepo.save(cart);
        return modelMapper.map(saved, OrderDTO.class);
    }

    @Override
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new APIException("Order not found"));

        return modelMapper.map(order, OrderDTO.class);
    }


    @Override
    public String checkout(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() ->
                new APIException("Order not found"));
        //waiting kitchen
        order.setStatus(OrderStatus.WAITING);
        orderRepo.save(order);

        RestaurantTable table = order.getTable();
        //after customer hangout need clear table
        if (table.getStatus() == TableStatus.AVAILABLE) {
            table.setStatus(TableStatus.OCCUPIED);
        }
        restaurantRepo.save(table);
        activityLogService.log(
                "ORDER",
                order.getId(),
                "CHECKOUT_ORDER",
                "OrderStatus=PREVIOUS",
                "OrderStatus=WAITING, TableStatus="
                        + table.getStatus().name(),
                SecuritySnapshotUtil.getEmployeeName(),
                EActivityResult.SUCCESS,
                null,
                "Nhân viên checkout đơn hàng"
        );



        return "Checkout successful.";
    }

    @Override
    public byte[] generateBillPdf(Long orderId) {

        Order order = orderRepo.findByIdWithItems(orderId)
                .orElseThrow(() -> new APIException("Order not found"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A5, 20, 20, 20, 20);
            PdfWriter.getInstance(document, out);
            document.open();

            Font normal = new Font(Font.HELVETICA, 9);
            Font bold = new Font(Font.HELVETICA, 9, Font.BOLD);
            Font title = new Font(Font.HELVETICA, 12, Font.BOLD);

            // ===== HEADER =====
            Paragraph shop = new Paragraph("NHÀ HÀNG Hồng Phúc", bold);
            shop.setAlignment(Element.ALIGN_CENTER);
            document.add(shop);

            document.add(new Paragraph("23/57 Thái Thịnh,Phước Long B, Thành Phố Hồ Chí Minh", normal));
            document.add(new Paragraph("0327568095", normal));

            Paragraph billTitle = new Paragraph("HÓA ĐƠN THANH TOÁN", title);
            billTitle.setAlignment(Element.ALIGN_CENTER);
            billTitle.setSpacingBefore(8);
            document.add(billTitle);

            document.add(new Paragraph("Số HD: " + order.getBillCode(), normal));
            document.add(Chunk.NEWLINE);

            // ===== INFO =====
            document.add(new Paragraph("Ngày: " + order.getPaidAt(), normal));
            document.add(new Paragraph("Bàn: " + order.getTable().getTableName(), normal));
            document.add(new Paragraph("Thu ngân: " + order.getCashierName(), normal));
            document.add(Chunk.NEWLINE);

            // ===== LINE =====
            document.add(new LineSeparator());

            // ===== ITEMS TABLE =====
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4, 1, 2, 2});
            BillHellper billHellper = new BillHellper();
            billHellper.addCell(table, "Tên món", bold, Element.ALIGN_LEFT);
            billHellper.addCell(table, "SL", bold, Element.ALIGN_CENTER);
            billHellper.addCell(table, "Đơn giá", bold, Element.ALIGN_RIGHT);
            billHellper.addCell(table, "Thành tiền", bold, Element.ALIGN_RIGHT);

            document.add(new LineSeparator());

            for (OrderItem item : order.getOrderItems()) {
                billHellper.addCell(table, item.getMenuItem().getName(), normal, Element.ALIGN_LEFT);
                billHellper.addCell(table, String.valueOf(item.getQuantity()), normal, Element.ALIGN_CENTER);
                billHellper.addCell(table, billHellper.formatMoney(item.getPrice()), normal, Element.ALIGN_RIGHT);
                billHellper.addCell(table, billHellper.formatMoney(item.getTotalPrice()), normal, Element.ALIGN_RIGHT);
            }

            document.add(table);
            document.add(new LineSeparator());

            // ===== TOTAL =====
            document.add(billHellper.totalLine("Thành tiền", order.getTotalPrice(), normal));
            document.add(billHellper.totalLine("Tiền mặt", order.getCashReceived(), normal));
            document.add(billHellper.totalLine("Trả lại khách", order.getChangeAmount(), normal));

            document.add(Chunk.NEWLINE);

            // ===== FOOTER =====
            Paragraph thanks = new Paragraph("Trân trọng cảm ơn!", bold);
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(thanks);

            Paragraph note = new Paragraph("(Hóa đơn chưa bao gồm thuế GTGT)", normal);
            note.setAlignment(Element.ALIGN_CENTER);
            document.add(note);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Generate PDF failed", e);
        }
    }

    @Override
    public InvoiceDTO payMergedCarts(PayMergedCartsRequestDTO request) {

        List<Cart> carts = cartRepo.findAllByIdIn(request.getCartIds());
        if (carts.size() < 2) {
            throw new APIException("Phải chọn ít nhất 2 cart để gộp");
        }
        Long userId = Objects.requireNonNull(SecuritySnapshotUtil.getUserId(), "Bạn cần đăng nhập");
        User user = userRepo.findById(userId).orElseThrow(() -> new APIException("Bạn không có quyền thực hiện hành động này"));

        if (user.getPositions().stream()
                .noneMatch(p -> p.getName() == EPositionType.COUNTER)) {
            throw new APIException("Bạn phải là nhân viên quầy");
        }

        BigDecimal total = carts.stream()
                .map(Cart::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        if (request.getCashReceived() == null
                || request.getCashReceived().compareTo(BigDecimal.ZERO) <= 0) {

            InvoiceDTO preview = new InvoiceDTO();
            preview.setOrderId(null);
            preview.setTableName("PREVIEW");
            preview.setTotalAmount(total);
            return preview;
        }


        if (request.getCashReceived().compareTo(total) < 0) {
            throw new APIException("Không đủ tiền");
        }

        BigDecimal step = new BigDecimal("100000");

        BigDecimal cashBase = request.getCashReceived()
                .divide(step, 0, RoundingMode.FLOOR)
                .multiply(step);

        BigDecimal cashMax = cashBase.add(step);

        if (request.getCashReceived().compareTo(cashBase) < 0
                || request.getCashReceived().compareTo(cashMax) > 0) {
            throw new APIException(
                    "Số tiền khách đưa phải trong khoảng "
                            + cashBase.toPlainString()
                            + " đến "
                            + cashMax.toPlainString()
            );
        }

        BigDecimal hardLimit = total.add(step.multiply(new BigDecimal("10")));
        if (request.getCashReceived().compareTo(hardLimit) > 0) {
            throw new APIException("Số tiền quá lớn, vui lòng kiểm tra lại");
        }

        BigDecimal change = request.getCashReceived().subtract(total);


        Cart masterCart = carts.stream()
                .filter(c -> c.getId().equals(request.getMasterCartId()))
                .findFirst()
                .orElseThrow(() -> new APIException("Cart chính không hợp lệ"));


        for (Cart cart : carts) {
            if (cart.getCartItems().isEmpty()) {
                throw new APIException("Có bàn rỗng");
            }
            if (cart.getStatus() == ECartStatus.CLOSED) {
                throw new APIException("Bàn không hoạt động");
            }
        }


        String mergedTablesNote = "Gộp bàn: " +
                carts.stream()
                        .map(c -> c.getRestaurantTable().getTableName())
                        .collect(Collectors.joining(", "));


        Order order = new Order();
        order.setTable(masterCart.getRestaurantTable());
        order.setStatus(OrderStatus.WAITING);
        order.setTotalPrice(total);
        order.setCashierName(SecuritySnapshotUtil.getEmployeeName());
        order.setBillCode(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        order.setPaidAt(LocalDateTime.now());
        order.setNoteOrder("Combine dining tables");
        order.setFloops(masterCart.getRestaurantTable().getFloor());
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setCashReceived(request.getCashReceived());
        order.setChangeAmount(change);
        order.setCart(masterCart);

        Order savedOrder = orderRepo.save(order);


        for (Cart cart : carts) {
            for (CartItem cartItem : cart.getCartItems()) {

                OrderItem oi = new OrderItem();
                oi.setOrder(savedOrder);
                oi.setMenuItem(cartItem.getMenuItem());
                oi.setQuantity(cartItem.getQuantity());
                oi.setPrice(cartItem.getUnitPrice());
                oi.setOrderItemStatus(EOrderItem.WAITING);
                oi.setDiscount(cartItem.getDiscount());
                oi.setTotalPrice(cartItem.getTotalPrice());
                oi.setNameTable(cart.getRestaurantTable().getTableName());
                oi.setNoteOrder(cartItem.getNote());

                orderItemRepo.save(oi);
            }
        }

        for (Cart cart : carts) {
            cartItemRepo.deleteAll(cart.getCartItems());
            cart.getCartItems().clear();
            cart.setStatus(ECartStatus.CLOSED);
            cart.setTotalPrice(BigDecimal.ZERO);
            cartRepo.save(cart);
        }

        masterCart.getRestaurantTable().setStatus(TableStatus.AVAILABLE);


        InvoiceDTO invoice = new InvoiceDTO();
        invoice.setOrderId(savedOrder.getId());
        invoice.setTableName(masterCart.getRestaurantTable().getTableName());
        invoice.setTotalAmount(total);
        invoice.setCashReceived(request.getCashReceived());
        invoice.setChangeAmount(change);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setCashierName(SecuritySnapshotUtil.getEmployeeName());

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
                "CASH_PAYMENT_MERGED_CARTS",
                "MergedCartIds=" + request.getCartIds()
                        + ", MasterCartId=" + masterCart.getId(),
                "BillCode=" + savedOrder.getBillCode()
                        + ", Total=" + total.toPlainString()
                        + ", CashReceived=" + request.getCashReceived().toPlainString()
                        + ", Change=" + change.toPlainString(),
                SecuritySnapshotUtil.getEmployeeName(), // thu ngân
                EActivityResult.SUCCESS,                 // chỉ SUCCESS
                null,
                "Thanh toán gộp bàn: "
                        + carts.stream()
                        .map(c -> c.getRestaurantTable().getTableName())
                        .collect(Collectors.joining(", "))
        );


        simpMessagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "PAY_MERGED_CARTS",
                        "data", Map.of(
                                "orderId", savedOrder.getId(),
                                "mergedCartIds", request.getCartIds()
                        )
                )
        );

        return invoice;
    }

    @Override
    public List<KitchenOrderDTO> getOrdersForKitchen() {

        List<OrderStatus> statuses = List.of(OrderStatus.WAITING);

        List<KitchenOrderFlatDTO> rows =
                orderRepo.findKitchenOrdersFlat(statuses);

        Map<Long, KitchenOrderDTO> map = new LinkedHashMap<>();

        for (KitchenOrderFlatDTO r : rows) {
            map.computeIfAbsent(
                    r.getOrderId(),
                    id -> new KitchenOrderDTO(
                            r.getOrderId(),
                            r.getBillCode(),
                            r.getTableName(),
                            r.getNoteOrder(),
                            r.getKitchenStaffName(),
                            new ArrayList<>()
                    )
            ).getItems().add(
                    new KitChenOrderItemDTO(
                            r.getOrderItemId(),
                            r.getMenuName(),
                            r.getQuantity(),
                            r.getNameTable(),
                            r.getItemNote(),
                            r.getStatus()
                    )
            );
        }

        return new ArrayList<>(map.values());
    }

    @Override
    public List<KitchenOrderDTO> getOrdersOfKitchen() {

        List<OrderStatus> statuses = List.of(OrderStatus.PENDING);

        List<KitchenOrderFlatDTO> rows =
                orderRepo.findKitchenOrdersFlat(statuses);

        Map<Long, KitchenOrderDTO> map = new LinkedHashMap<>();

        for (KitchenOrderFlatDTO r : rows) {
            map.computeIfAbsent(
                    r.getOrderId(),
                    id -> new KitchenOrderDTO(
                            r.getOrderId(),
                            r.getBillCode(),
                            r.getTableName(),
                            r.getNoteOrder(),
                            r.getKitchenStaffName(),
                            new ArrayList<>()
                    )
            ).getItems().add(
                    new KitChenOrderItemDTO(
                            r.getOrderItemId(),
                            r.getMenuName(),
                            r.getQuantity(),
                            r.getNameTable(),
                            r.getItemNote(),
                            r.getStatus()
                    )
            );
        }

        return new ArrayList<>(map.values());
    }
    @Override
    public List<KitchenOrderDTO> getOrderDoneForKitChen() {

        List<OrderStatus> statuses = List.of(OrderStatus.DONE);

        List<KitchenOrderFlatDTO> rows =
                orderRepo.findKitchenOrdersFlat(statuses);

        Map<Long, KitchenOrderDTO> map = new LinkedHashMap<>();

        for (KitchenOrderFlatDTO r : rows) {
            map.computeIfAbsent(
                    r.getOrderId(),
                    id -> new KitchenOrderDTO(
                            r.getOrderId(),
                            r.getBillCode(),
                            r.getTableName(),
                            r.getNoteOrder(),
                            r.getKitchenStaffName(),
                            new ArrayList<>()
                    )
            ).getItems().add(
                    new KitChenOrderItemDTO(
                            r.getOrderItemId(),
                            r.getMenuName(),
                            r.getQuantity(),
                            r.getNameTable(),
                            r.getItemNote(),
                            r.getStatus()
                    )
            );
        }

        return new ArrayList<>(map.values());
    }
}
