# Restaurant Management System — Backend

RESTful API for a restaurant management system built with Spring Boot, supporting user management, booking, and staff scheduling with real-time updates.

---

## Project Overview

[VI]  
Hệ thống backend quản lý nhà hàng, cung cấp API cho các chức năng: quản lý người dùng, nhân viên, đặt bàn, ca làm việc và thống kê. Hệ thống sử dụng JWT để xác thực và WebSocket để cập nhật dữ liệu real-time.

[EN]  
A backend system for restaurant management that provides APIs for managing users, employees, table bookings, work shifts, and statistics. It uses JWT authentication and real-time updates via WebSocket.

---

## Key Features

Authentication & Authorization
- JWT-based authentication
- Role-based access control (ADMIN / EMPLOYEE / USER)
- Password encryption with BCrypt

User & Employee Management
- Register users and employees with validation
- Assign roles and positions
- Upload avatar images
- Manage account status
- Pagination, sorting, filtering

Table & Booking Management
- Manage table types (seat count, extra fee)
- Booking overview by status
- Track bookings by user

Work Shift Management
- Assign shifts (single, weekly, bulk)
- Prevent duplicate shift assignment
- Shift grouping for batch scheduling
- Employee schedule tracking
- Shift statistics

Dashboard & Statistics
- Count personal orders
- Booking statistics grouped by status
- Monitor ordering tables

Real-time System
- WebSocket for real-time updates
- Events: employee created/updated, shift assigned/updated/cancelled

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Security + JWT
- Spring Data JPA (Hibernate)
- MySQL
- WebSocket (STOMP)
- ModelMapper
- Maven

---

## Installation Guide

### Prerequisites
- Java 21+
- Maven 3.8+
- MySQL 8+

### Steps

```bash
# Clone repository
git clone https://github.com/phuc-call/restaurant-management-backend.git
cd restaurant-management-backend

# Create database
CREATE DATABASE restaurant_db;

# Configure application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/restaurant_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# JWT secret
jwt.secret=your_secret_key

# Run project
mvn clean install
mvn spring-boot:run
