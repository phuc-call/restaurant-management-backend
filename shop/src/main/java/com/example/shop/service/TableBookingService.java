package com.example.shop.service;

import com.example.shop.entity.RestaurantTable;
import com.example.shop.entity.TableBooking;
import com.example.shop.entity.enums.BookingStatus;
import com.example.shop.entity.enums.CancelReason;
import com.example.shop.payloads.BookingStatusDTO;
import com.example.shop.payloads.BookingStatusSummaryDTO;
import com.example.shop.payloads.TableBookingRequestDTO;
import com.example.shop.payloads.TableBookingResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TableBookingService {
    TableBookingResponseDTO createBooking(TableBookingRequestDTO request);

    TableBookingResponseDTO confirmBooking(Long bookingId);

    TableBookingResponseDTO cancelBooking(Long bookingId, CancelReason cancelReason);

    TableBookingResponseDTO checkIn(Long bookingId);

    TableBookingResponseDTO completeBooking(Long bookingId);

    TableBookingResponseDTO getBookingById(Long bookingId);

    public Page<TableBookingResponseDTO> getAllBookings(
            Integer pageNumber,
            Integer pageSize,
            Integer minGuests,
            Integer maxGuests,
            LocalDate fromDate,
            LocalDate toDate,
            String customerPhone,
            String customerName
    );


    List<TableBookingResponseDTO> getBookingsByStatus(BookingStatus status);

    List<TableBookingResponseDTO> getBookingsByTable(String tableName);


    boolean isTimeSlotAvailable(Long tableId, LocalDateTime bookingTime);

    void autoMarkNoShow();

    List<Long> suggestTableIds(
            int numberOfGuests,
            LocalDateTime bookingTime
    );
    List<TableBookingResponseDTO> getBookingsByCustomerPhone(String phone);
    List<BookingStatusDTO>getBookingStatus();
    List<BookingStatusSummaryDTO>getBookingByStatus();
}
