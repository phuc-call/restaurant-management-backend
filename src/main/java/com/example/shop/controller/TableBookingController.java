package com.example.shop.controller;

import com.example.shop.entity.enums.BookingStatus;
import com.example.shop.entity.enums.CancelReason;
import com.example.shop.payloads.*;
import com.example.shop.service.RestaurantTableService;
import com.example.shop.service.TableBookingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
public class TableBookingController {
    @Autowired
    TableBookingService tableBookingService;
    @Autowired
    RestaurantTableService restaurantTableService;
    CancelBookingRequestDTO cancelBookingRequestDTO;

    @GetMapping("/employee/staff/bookings")
    public ResponseEntity<Page<TableBookingResponseDTO>> getAllBookings(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,

            @RequestParam(required = false) Integer minGuests,
            @RequestParam(required = false) Integer maxGuests,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) String customerName
    ) {
        return ResponseEntity.ok(
                tableBookingService.getAllBookings(
                        page,
                        size,
                        minGuests,
                        maxGuests,
                        fromDate,
                        toDate,
                        customerPhone,
                        customerName
                )
        );
    }

    @GetMapping("/employee/staff/bookings/table/{tableName}")
    public ResponseEntity<List<TableBookingResponseDTO>> getBookingsByTable(
            @PathVariable String tableName
    ) {
        return ResponseEntity.ok(
                tableBookingService.getBookingsByTable(tableName)
        );
    }

    @GetMapping("/employee/staff/tables/{tableId}/availability")
    public ResponseEntity<Boolean> checkAvailability(
            @PathVariable Long tableId,
            @RequestParam LocalDateTime time
    ) {
        return ResponseEntity.ok(
                tableBookingService.isTimeSlotAvailable(tableId, time)
        );
    }
    @PostMapping("/employee/staff/bookings/{id}/complete")
    public ResponseEntity<TableBookingResponseDTO> completeBooking(
            @Valid
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                tableBookingService.completeBooking(id)
        );
    }


    @PostMapping("/shared/bookings")
    public ResponseEntity<TableBookingResponseDTO> createBooking(
            @RequestBody @Valid TableBookingRequestDTO request
    ) {
        return ResponseEntity.ok(
                tableBookingService.createBooking(request)
        );
    }

    @PostMapping("/employee/staff/bookings/{id}/confirm")
    public ResponseEntity<TableBookingResponseDTO> confirmBooking(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                tableBookingService.confirmBooking(id)
        );
    }

    @PostMapping("/shared/bookings/{id}/cancel")
    public ResponseEntity<TableBookingResponseDTO> cancelBooking(

            @PathVariable Long id,
            @RequestBody CancelBookingRequestDTO requestDTO

    ) {
        return ResponseEntity.ok(
                tableBookingService.cancelBooking(id,requestDTO.getReason())
        );
    }

    @PostMapping("/employee/staff/bookings/{id}/check-in")
    public ResponseEntity<TableBookingResponseDTO> checkIn(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                tableBookingService.checkIn(id)
        );
    }

    @GetMapping("/employee/staff/bookings/{id}")
    public ResponseEntity<TableBookingResponseDTO> getBookingById(
            @Valid
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                tableBookingService.getBookingById(id)
        );
    }

    @GetMapping("/employee/staff/bookings/status/{status}")
    public ResponseEntity<List<TableBookingResponseDTO>> getBookingsByStatus(
            @PathVariable BookingStatus status
    ) {
        return ResponseEntity.ok(
                tableBookingService.getBookingsByStatus(status)
        );
    }

    @GetMapping("/employee/staff/tables/suggest")
    public ResponseEntity<List<Long>> suggestTables(
            @RequestParam int guests,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime time
    ) {
        return ResponseEntity.ok(
                tableBookingService.suggestTableIds(guests, time)
        );
    }

    @GetMapping("/public/bookings/lookup")
    public ResponseEntity<List<TableBookingResponseDTO>> lookupBookingsByPhone(
            @RequestParam String phone
    ) {
        return ResponseEntity.ok(
                tableBookingService.getBookingsByCustomerPhone(phone)
        );
    }

    @GetMapping("/employee/staff/status")
    public ResponseEntity<List<BookingStatusDTO>>getStatus(){
        return ResponseEntity.ok(tableBookingService.getBookingStatus());
    }

    @GetMapping("/admin/bookings/summary/status")
    public ResponseEntity<List<BookingStatusSummaryDTO>>getBookingByStatus(){
        return ResponseEntity.ok(tableBookingService.getBookingByStatus());
    }


    @PutMapping("/employee/staff/{bookingId}/change-table/{tableId}")
    public ResponseEntity<?> changeTableForBooking(
            @PathVariable Long bookingId,
            @PathVariable Long tableId
    ) {
        tableBookingService.changeTableForBooking(bookingId, tableId);
        return ResponseEntity.ok("Đổi bàn thành công");
    }

}
