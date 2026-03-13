package com.example.shop.service;

import com.example.shop.payloads.HistoryOrderDTO;
import com.example.shop.payloads.KitchenOrderDTO;
import com.example.shop.payloads.ServiceOrderDTO;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface KitChenService {
    void takeOrder(Long orderId);
    Map<String, List<KitchenOrderDTO>> getOrdersGroupedByKitchen();
    public void finishOrder(Long orderId);
    public Page<KitchenOrderDTO> getDoneOrdersOfKitchen(
            int page,
            int size
    );
    public Page<HistoryOrderDTO> getKitchenOrderHistory(
            int page,
            int size
    );
    public byte[] exportKitchenHistoryExcel(List<HistoryOrderDTO> data) throws IOException;
    public List<HistoryOrderDTO> getKitchenHistoryForExport(
            LocalDateTime fromTime,
            LocalDateTime toTime
    );
    public void finishOrderItem(Long orderItemId);
    public ServiceOrderDTO getOrderDetail(Long orderId);
    public void autoAssignKitchenForWaitingOrders();
    public List<KitchenOrderDTO>getOrderForPersonal();
}
