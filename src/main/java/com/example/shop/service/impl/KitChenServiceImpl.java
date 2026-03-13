package com.example.shop.service.impl;

import com.example.shop.config.JsonUtil;
import com.example.shop.entity.Order;
import com.example.shop.entity.OrderItem;

import com.example.shop.entity.User;
import com.example.shop.entity.enums.*;
import com.example.shop.exception.APIException;
import com.example.shop.hellper.SecuritySnapshotUtil;
import com.example.shop.payloads.*;
import com.example.shop.repository.OrderItemRepo;
import com.example.shop.repository.OrderRepo;
import com.example.shop.repository.UserRepo;
import com.example.shop.service.ActivityLogService;
import com.example.shop.service.KitChenService;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class KitChenServiceImpl implements KitChenService {
    @Autowired
    OrderRepo orderRepo;
    @Autowired
    UserRepo userRepo;
    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    OrderItemRepo orderItemRepo;
    @Autowired
    JsonUtil jsonUtil;
    @Autowired
    ActivityLogService activityLogService;
    @Autowired
    ModelMapper modelMapper;

    @Override
    public void takeOrder(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() ->
                new APIException("Không tìm thấy order với " + orderId));
        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId == null) {
            throw new APIException("Chưa đăng nhập");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new APIException("Không tìm thấy user"));
        if(order.getKitchenStaff()==null)
        {
            throw new APIException("Không tìm thấy nhân viên");
        }
        if (user.getPositions() == null ||
                user.getPositions().stream()
                        .noneMatch(p -> p.getName().equals(EPositionType.KITCHEN))) {

            throw new APIException("User không phải nhân viên bếp");
        }
        if (!order.getStatus().equals(OrderStatus.WAITING)) {
            throw new APIException("Order này đã được xử lý");
        }

        List<OrderStatus> activeStatuses = List.of(OrderStatus.PENDING);
        long activeOrderCount =
                orderRepo.countActiveOrdersByKitchen(userId, activeStatuses);

        if (activeOrderCount >= 5) {
            throw new APIException("Bạn chỉ được nhận tối đa 5 order cùng lúc");
        }
        order.setKitchenStaff(user);

        order.setStatus(OrderStatus.PENDING);

        order.setUpdatedAt(LocalDateTime.now());
        for (OrderItem orderItem : order.getOrderItems()) {
            orderItem.setOrderItemStatus(EOrderItem.WAITING);
        }
        Order saveOder = orderRepo.save(order);

        String newSnapshot = jsonUtil.toJson(saveOder);
        activityLogService.log(
                "ORDER",
                saveOder.getId(),
                "TAKE_ORDER",
                null,
                newSnapshot,
                saveOder.getCashierName(),
                EActivityResult.SUCCESS,
                saveOder.getBillCode(),
                "Bếp nhận order"
        );
        simpMessagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "KITCHEN",
                        "data", Map.of(
                                "orderId", saveOder.getId()
                        )
                )
        );
        simpMessagingTemplate.convertAndSend(
                "/topic/kitchen",
                Map.of(
                        "event", "ORDER_TAKEN",
                        "data", modelMapper.map(saveOder, KitchenOrderDTO.class)
                )
        );

    }

    @Override
    public Map<String, List<KitchenOrderDTO>> getOrdersGroupedByKitchen() {
        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId == null) {
            throw new APIException("Chưa đăng nhập");
        }


        List<Order> orders =
                orderRepo.findAllKitchenWorkingOrders(OrderStatus.PENDING);

        return orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getKitchenStaff().getUserName(),   // key = tên bếp
                        Collectors.mapping(
                                o -> new KitchenOrderDTO(
                                        o.getId(),
                                        o.getBillCode(),
                                        o.getTable().getTableName(),
                                        o.getKitchenStaff().getUserName(),
                                        o.getNoteOrder(),
                                        o.getOrderItems().stream()
                                                .map(i -> new KitChenOrderItemDTO(
                                                        i.getId(),
                                                        i.getMenuItem().getName(),
                                                        i.getQuantity(),
                                                        i.getNameTable(),
                                                        i.getNoteOrder(),
                                                        i.getOrderItemStatus()
                                                ))
                                                .toList()
                                ),
                                Collectors.toList()
                        )
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenOrderDTO>getOrderForPersonal(){
        Long userId=SecuritySnapshotUtil.getUserId();
        if(userId==null){ throw new APIException("Bạn chưa đăng nhập");
        }
        User user=userRepo.findById(userId).orElseThrow(()->new APIException("Tài khoản không tồn tại"));
        boolean kitchen=user.getPositions().stream().anyMatch(p->p.getName()==EPositionType.KITCHEN);
        if(!kitchen){
            throw new APIException("Phải là nhân viên bếp");
        }
        List<KitchenOrderFlatDTO>findByOrder=orderRepo.findPersonalKitchenOrdersFlat(userId,OrderStatus.PENDING);
        Map<String, List<KitchenOrderFlatDTO>>grouped=
                findByOrder.stream().collect(Collectors.groupingBy(KitchenOrderFlatDTO::getKitchenStaffName));
        return grouped.values()
                .stream()
                .map(list -> {
                    KitchenOrderFlatDTO first = list.get(0);

                    List<KitChenOrderItemDTO> items =
                            list.stream()
                                    .map(f -> new KitChenOrderItemDTO(
                                            f.getOrderItemId(),
                                            f.getMenuName(),
                                            f.getQuantity(),
                                            f.getNameTable(),
                                            f.getNoteOrder(),
                                            f.getStatus()
                                    ))
                                    .toList();

                    return new KitchenOrderDTO(
                            first.getOrderId(),
                            first.getBillCode(),
                            first.getTableName(),
                            first.getNoteOrder(),
                            first.getKitchenStaffName(),
                            items
                    );
                })
                .toList();
    }

    @Override
    public void finishOrder(Long orderId) {

        // 1. Kiểm tra đăng nhập
        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId == null) {
            throw new APIException("Chưa đăng nhập");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new APIException("Không tìm thấy user"));

        // 2. Kiểm tra có phải nhân viên bếp không
        if (user.getPositions() == null ||
                user.getPositions().stream()
                        .noneMatch(p -> p.getName().equals(EPositionType.KITCHEN))) {
            throw new APIException("User không phải nhân viên bếp");
        }

        // 3. Lấy order
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new APIException("Không tìm thấy order"));

        // 4. Chỉ bếp đã nhận order mới được DONE
        if (user.getPositions() == null ||
                user.getPositions().stream()
                        .noneMatch(p -> p.getName().equals(EPositionType.KITCHEN))) {

            throw new APIException("Không có quyền hoàn thành order này");
        }

        // 5. Chỉ DONE khi đang PENDING
        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            throw new APIException("Chỉ có thể hoàn thành order đang xử lý");
        }

        // 6. Kiểm tra thời gian >= 20 giây
        LocalDateTime receivedTime = order.getUpdatedAt();
        if (receivedTime == null ||
                receivedTime.plusSeconds(20).isAfter(LocalDateTime.now())) {
            throw new APIException("Vui lòng chờ ít nhất 20 giây trước khi hoàn thành order");
        }

        // 7. DONE
        order.setStatus(OrderStatus.DONE);
        order.setUpdatedAt(LocalDateTime.now());
        Order saveOder = orderRepo.save(order);
        for (OrderItem item : saveOder.getOrderItems()) {
            item.setOrderItemStatus(EOrderItem.FINISH);
        }

        String newSnapshot = jsonUtil.toJson(saveOder);
        activityLogService.log(
                "ORDER",
                saveOder.getId(),
                "FINISH_ORDER",
                null,
                newSnapshot,
                saveOder.getCashierName(),
                EActivityResult.SUCCESS,
                saveOder.getBillCode(),
                "Bếp làm xong món"
        );


        simpMessagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "KITCHEN_DONE",
                        "data", Map.of("orderId", order.getId())
                )
        );
        simpMessagingTemplate.convertAndSend(
                "/topic/kitchen",
                Map.of(
                        "event", "KITCHEN_DONE",
                        "data", Map.of("orderId", order.getId())
                )
        );
    }

    @Override
    public Page<KitchenOrderDTO> getDoneOrdersOfKitchen(
            int page,
            int size
    ) {

        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId == null) {
            throw new APIException("Chưa đăng nhập");
        }
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new APIException("Không tìm thấy user"));
        boolean isKitchen = user.getPositions() != null &&
                user.getPositions().stream()
                        .anyMatch(p -> p.getName() == EPositionType.KITCHEN);

        if (!isKitchen) {
            throw new APIException("User không phải nhân viên bếp");
        }

        LocalDateTime fromTime = LocalDateTime.now().minusHours(24);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("updatedAt").descending()
        );

        Page<Order> orderPage =
                orderRepo.findDoneOrdersOfKitchenStaff(
                        userId,
                        OrderStatus.DONE,
                        fromTime,
                        pageable
                );


        List<Long> orderIds = orderPage.getContent()
                .stream()
                .map(Order::getId)
                .toList();


        Map<Long, List<OrderItem>> itemMap = orderIds.isEmpty()
                ? Map.of()
                : orderItemRepo.findItemsByOrderIds(orderIds)
                .stream()
                .collect(Collectors.groupingBy(
                        oi -> oi.getOrder().getId()
                ));


        return orderPage.map(order ->
                new KitchenOrderDTO(
                        order.getId(),
                        order.getBillCode(),
                        order.getTable().getTableName(),
                        order.getNoteOrder(),
                        order.getKitchenStaff().getUserName(),
                        itemMap.getOrDefault(order.getId(), List.of())
                                .stream()
                                .map(i -> new KitChenOrderItemDTO(
                                        i.getId(),
                                        i.getMenuItem().getName(),
                                        i.getQuantity(),
                                        i.getNameTable(),
                                        i.getNoteOrder(),
                                        i.getOrderItemStatus()
                                ))
                                .toList()
                )
        );
    }

    @Override
    public Page<HistoryOrderDTO> getKitchenOrderHistory(
            int page,
            int size
    ) {


        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId == null) {
            throw new APIException("Chưa đăng nhập");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new APIException("Không tìm thấy user"));


        boolean isKitchen = user.getPositions() != null &&
                user.getPositions().stream()
                        .anyMatch(p -> p.getName() == EPositionType.KITCHEN);

        if (!isKitchen) {
            throw new APIException("User không phải nhân viên bếp");
        }


        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("updatedAt").descending()
        );


        Page<Order> orderPage =
                orderRepo.findKitchenOrderHistory(
                        userId,
                        OrderStatus.DONE,
                        pageable
                );


        List<Long> orderIds = orderPage.getContent()
                .stream()
                .map(Order::getId)
                .toList();

        Map<Long, List<OrderItem>> itemMap = orderIds.isEmpty()
                ? Map.of()
                : orderItemRepo.findItemsByOrderIds(orderIds)
                .stream()
                .collect(Collectors.groupingBy(
                        oi -> oi.getOrder().getId()
                ));


        return orderPage.map(order ->
                new HistoryOrderDTO(
                        order.getId(),
                        order.getBillCode(),
                        order.getTable().getTableName(),

                        order.getNoteOrder(),
                        order.getCreatedAt(),
                        order.getKitchenStaff().getUserName(),
                        itemMap.getOrDefault(order.getId(), List.of())
                                .stream()
                                .map(i -> new KitChenOrderItemDTO(
                                        i.getId(),
                                        i.getMenuItem().getName(),
                                        i.getQuantity(),
                                        i.getNameTable(),
                                        i.getNoteOrder(),
                                        i.getOrderItemStatus()
                                ))
                                .toList()
                )
        );
    }

    @Override
    public List<HistoryOrderDTO> getKitchenHistoryForExport(
            LocalDateTime fromTime,
            LocalDateTime toTime
    ) {

        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId == null) {
            throw new APIException("Chưa đăng nhập");
        }

        if (fromTime.isAfter(toTime)) {
            throw new APIException("Thời gian không hợp lệ");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new APIException("Không tìm thấy user"));

        boolean isKitchen = user.getPositions() != null &&
                user.getPositions().stream()
                        .anyMatch(p -> p.getName() == EPositionType.KITCHEN);

        if (!isKitchen) {
            throw new APIException("User không phải nhân viên bếp");
        }

        List<Order> orders =
                orderRepo.findKitchenHistoryForExport(
                        userId,
                        OrderStatus.DONE,
                        fromTime,
                        toTime
                );

        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .toList();

        Map<Long, List<OrderItem>> itemMap = orderIds.isEmpty()
                ? Map.of()
                : orderItemRepo.findItemsByOrderIds(orderIds)
                .stream()
                .collect(Collectors.groupingBy(
                        oi -> oi.getOrder().getId()
                ));

        return orders.stream()
                .map(order -> new HistoryOrderDTO(
                        order.getId(),
                        order.getBillCode(),
                        order.getTable().getTableName(),
                        order.getNoteOrder(),
                        order.getCreatedAt(),
                        order.getKitchenStaff().getUserName(),
                        itemMap.getOrDefault(order.getId(), List.of())
                                .stream()
                                .map(i -> new KitChenOrderItemDTO(
                                        i.getId(),
                                        i.getMenuItem().getName(),
                                        i.getQuantity(),
                                        i.getNameTable(),
                                        i.getNoteOrder(),
                                        i.getOrderItemStatus()
                                ))
                                .toList()
                ))
                .toList();
    }


    @Override
    public byte[] exportKitchenHistoryExcel(List<HistoryOrderDTO> data) throws IOException {


        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Kitchen History");

        XSSFCellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(
                workbook.getCreationHelper()
                        .createDataFormat()
                        .getFormat("yyyy-MM-dd HH:mm")
        );

        // Header
        Row header = sheet.createRow(0);
        String[] columns = {
                "Order ID",
                "Bill Code",
                "Table",
                "Kitchen Staff",
                "Created At",
                "Note",
                "Menu Name",
                "Quantity"
        };

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data

        int rowIdx = 1;

        for (HistoryOrderDTO order : data) {

            if (order.getItems() == null || order.getItems().isEmpty()) {
                Row row = sheet.createRow(rowIdx++);
                fillOrderRow(row, order, null, dateStyle);
                continue;
            }

            for (KitChenOrderItemDTO item : order.getItems()) {
                Row row = sheet.createRow(rowIdx++);
                fillOrderRow(row, order, item, dateStyle);
            }
        }

        // Excel Features

        sheet.setAutoFilter(new CellRangeAddress(
                0,
                rowIdx - 1,
                0,
                columns.length - 1
        ));

        sheet.createFreezePane(0, 1); // freeze header

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        sheet.protectSheet("readonly");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return out.toByteArray();
    }

    private void fillOrderRow(
            Row row,
            HistoryOrderDTO order,
            KitChenOrderItemDTO item,
            CellStyle dateStyle
    ) {
        row.createCell(0).setCellValue(order.getOrderId());
        row.createCell(1).setCellValue(order.getBillCode());
        row.createCell(2).setCellValue(order.getTableName());
        row.createCell(3).setCellValue(order.getKitchenStaffName());

        Cell dateCell = row.createCell(4);
        if (order.getCreatedAt() != null) {
            dateCell.setCellValue(
                    java.sql.Timestamp.valueOf(order.getCreatedAt())
            );
            dateCell.setCellStyle(dateStyle);
        }

        row.createCell(5).setCellValue(order.getNoteOrder());

        if (item != null) {
            row.createCell(6).setCellValue(item.getMenuName());
            row.createCell(7).setCellValue(item.getQuantity());
        }
    }

    @Override
    public void finishOrderItem(Long orderItemId) {

        OrderItem orderItem = orderItemRepo.findById(orderItemId)
                .orElseThrow(() -> new APIException("Không tìm thấy món này"));


        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId == null) throw new APIException("Chưa đăng nhập");

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new APIException("Không tìm thấy user"));

        boolean isKitchen = user.getPositions() != null &&
                user.getPositions().stream()
                        .anyMatch(p -> p.getName() == EPositionType.KITCHEN);

        if (!isKitchen) throw new APIException("User không phải nhân viên bếp");


        Order order = orderItem.getOrder();
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new APIException("Order chưa ở trạng thái đang xử lý");
        }
        if (order.getKitchenStaff() == null) {
            throw new APIException("Order này chưa có nhân viên bếp xử lý");
        }
        if (!user.getUserId().equals(order.getKitchenStaff().getUserId())) {
            throw new APIException(user.getUserName() + "Bạn chỉ có thể hoàn thành món của mình");
        }
        if (orderItem.getOrderItemStatus() == EOrderItem.FINISH) {
            throw new APIException("Món này đã hoàn thành");
        }
        // Finish món
        orderItem.setOrderItemStatus(EOrderItem.FINISH);
        orderItemRepo.save(orderItem);


        // BẮN REALTIME CHO TỪNG MÓN
        simpMessagingTemplate.convertAndSend(
                "/topic/kitchen",
                Map.of(
                        "event", "ORDER_DONE",
                        "data", Map.of(
                                "orderItemId", orderItem.getId(),
                                "orderId", order.getId()
                        )
                )
        );

        boolean allFinished = order.getOrderItems().stream()
                .allMatch(i -> i.getOrderItemStatus() == EOrderItem.FINISH);

        // BẮN REALTIME KHI CẢ ORDER DONE
        if (allFinished && order.getStatus() != OrderStatus.DONE) {
            order.setStatus(OrderStatus.DONE);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(order);

            simpMessagingTemplate.convertAndSend(
                    "/topic/cashier",
                    Map.of(
                            "event", "ORDER_DONE",
                            "data", Map.of(
                                    "orderId", order.getId()
                            )
                    )
            );
            simpMessagingTemplate.convertAndSend(
                    "/topic/kitchen",
                    Map.of(
                            "event", "ORDER_ITEM_DONE",
                            "data", Map.of(
                                    "orderItemId", orderItem.getId(),
                                    "orderId", order.getId()
                            )
                    )
            );
        }
    }

    @Override
    public ServiceOrderDTO getOrderDetail(Long orderId) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() ->
                        new APIException("Không tìm thấy order " + orderId));
        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId == null) {
            throw new APIException("Chưa đăng nhập");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new APIException("Không tìm thấy user"));

        boolean isKitchen = user.getPositions() != null &&
                user.getPositions().stream()
                        .anyMatch(p -> p.getName() == EPositionType.KITCHEN);

        if (!isKitchen) {
            throw new APIException("User không phải nhân viên bếp");
        }

        // map OrderItem → DTO
        List<ServiceOrderItemDTO> items = order.getOrderItems()
                .stream()
                .map(i -> new ServiceOrderItemDTO(
                        i.getMenuItem().getName(),
                        i.getQuantity(),
                        i.getNameTable(),
                        i.getOrderItemStatus()
                ))
                .toList();

        return new ServiceOrderDTO(
                order.getId(),
                order.getTable().getTableName(),
                order.getNoteOrder(),
                order.getBillCode(),
                items
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoAssignKitchenForWaitingOrders() {
        List<Order> orders = orderRepo
                .findWaitingOrdersForAutoAssign(
                        OrderStatus.WAITING,
                        PageRequest.of(0, 100)
                );
        if (orders.isEmpty()) {
            return;
        }

        List<User> users = userRepo.findActiveKitchenStaff();
        if (users == null || users.isEmpty()) {
            throw new APIException("Không có nhân viên bếp hoạt động");
        }

        List<Order> pendingStatus = orderRepo.findByStatus(OrderStatus.PENDING);
        Map<Long, Integer> activePendingKitchen = new HashMap<>();
        for (Order order : pendingStatus) {
            if (order.getKitchenStaff() == null) continue;
            Long userId = order.getKitchenStaff().getUserId();
            activePendingKitchen.put(
                    userId,
                    activePendingKitchen.getOrDefault(userId, 0) + 1);
        }

        //Tạo Min-Heap
       PriorityQueue<KitchenNode>heap=new PriorityQueue<>(
               Comparator
                       .comparingInt(KitchenNode::getActiveOrderCount)
                       .thenComparing(KitchenNode::getLastAssignedAt,
                               Comparator.nullsFirst(Comparator.naturalOrder()))

       );
        //Đưa từng bếp vào heap
        for (User user : users) {
            Long userId = user.getUserId();
            Integer activeCount = activePendingKitchen.getOrDefault(userId, 0);
            if (activeCount < 15) {
                heap.offer(new KitchenNode(
                        userId,
                        user.getUserName(),
                        activeCount,
                        null
                ));
            }
        }
        //kiểm tra heap
        if (heap.isEmpty()) {
            throw new APIException("Nhân viên quá tải");
        }
        // phân công từng order
        int assignedCount = 0;
        for (Order order : orders) {
            if (heap.isEmpty()) {
                break;
            }
            KitchenNode kitchen = heap.poll();
            Order freshOrder = orderRepo.findById(order.getId()).orElseThrow(() ->
                    new APIException("Order không tồn tại"));
            if (freshOrder.getKitchenStaff() != null) {
                heap.offer(kitchen);
                continue;
            }
            //Lấy user
            User kitchenStaff = userRepo.findById(kitchen.getUserId()).orElseThrow(() ->
                    new APIException("Bếp không tồn tại"));
            if (kitchenStaff.getAccountStatus() != EAccountStatus.ACTIVE) {
                continue;
            }
            //Gán order
            freshOrder.setKitchenStaff(kitchenStaff);
            freshOrder.setStatus(OrderStatus.PENDING);
            freshOrder.setUpdatedAt(LocalDateTime.now());
            //Cập nhật orderItems
            for (OrderItem orderItem : freshOrder.getOrderItems()) {
                orderItem.setOrderItemStatus(EOrderItem.WAITING);
            }
            //save
            orderRepo.save(freshOrder);
            orderRepo.flush();

            //cập nhật heap
            kitchen.setActiveOrderCount(kitchen.getActiveOrderCount()+1);
            kitchen.setLastAssignedAt(LocalDateTime.now());
            if (kitchen.getActiveOrderCount() < 15) {
                heap.offer(kitchen);
            }
            assignedCount++;

            simpMessagingTemplate.convertAndSend(
                    "/topic/kitchen",
                    Map.of(
                            "event", "ORDER_ASSIGNED",
                            "orderId", freshOrder.getId(),
                            "kitchenStaffId", kitchenStaff.getUserId()
                    )
            );
        }

    }
}
