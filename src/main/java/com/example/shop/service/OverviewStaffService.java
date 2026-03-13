package com.example.shop.service;

import com.example.shop.entity.enums.BookingStatus;

import java.util.Map;

public interface OverviewStaffService {
    Long countMyOrders();
    Map<BookingStatus, Long> getMyBookingOverview();
    Long countOrderingTables();
}
