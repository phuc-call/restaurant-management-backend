package com.example.shop.service;

import com.example.shop.payloads.InvoiceDTO;
import com.example.shop.payloads.KitchenOrderDTO;
import com.example.shop.payloads.OrderDTO;
import com.example.shop.payloads.PayMergedCartsRequestDTO;

import java.util.List;


public interface OrderService {
    OrderDTO createOrder(Long tableId);

    String checkout(Long orderId);

    byte[] generateBillPdf(Long orderId);

    OrderDTO getOrderById(Long orderId);

    public InvoiceDTO payMergedCarts(PayMergedCartsRequestDTO request);

    public List<KitchenOrderDTO> getOrdersForKitchen();
    public List<KitchenOrderDTO> getOrdersOfKitchen();
    public List<KitchenOrderDTO> getOrderDoneForKitChen();


}
