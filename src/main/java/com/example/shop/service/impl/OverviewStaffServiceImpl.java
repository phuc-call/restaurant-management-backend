package com.example.shop.service.impl;

import com.example.shop.entity.enums.BookingStatus;
import com.example.shop.hellper.SecuritySnapshotUtil;
import com.example.shop.repository.OrderRepo;
import com.example.shop.repository.RestaurantRepo;
import com.example.shop.repository.TableBookingRepo;
import com.example.shop.repository.TableRepo;
import com.example.shop.service.OverviewStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OverviewStaffServiceImpl implements OverviewStaffService {
    private final OrderRepo orderRepository;
    private final TableBookingRepo tableBookingRepository;
    private final TableRepo tableRepo;
    @Override
    public Long countMyOrders() {
        LocalTime now=LocalTime.now();
        LocalTime start=LocalTime.of(6,30);
        LocalTime end=LocalTime.of(20,45);
        if(now.isBefore(start)||now.isAfter(end)){
            return 0L;
        }
        Long employeeName = SecuritySnapshotUtil.getUserId();
        return orderRepository.countOrdersByUserId(employeeName);
    }
@Override
    public Map<BookingStatus, Long> getMyBookingOverview() {

        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId == null) {
            return Map.of(); // hoặc throw exception
        }

        List<Object[]> rows =
                tableBookingRepository.getBookingGroupByStatusAndUser(userId);

        Map<BookingStatus, Long> result =
                new EnumMap<>(BookingStatus.class);

        // default = 0
        for (BookingStatus status : BookingStatus.values()) {
            result.put(status, 0L);
        }

        for (Object[] row : rows) {
            BookingStatus status = (BookingStatus) row[0];
            Long total = (Long) row[1];
            result.put(status, total);
        }

        return result;
    }
@Override
    public Long countOrderingTables() {
        return tableRepo.countTablesOrdering();
    }
}
