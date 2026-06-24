<<<<<<< HEAD
# 🍽️ QuickShop – Restaurant Management System

A monolithic backend system for managing restaurant operations, built with Spring Boot. Supports role-based access for Admin, Manager, and Staff, with features ranging from table/booking management to QR ordering and payment processing.

---

## 📌 Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Getting Started](#getting-started)
- [API Overview](#api-overview)
- [Role & Permission Model](#role--permission-model)
- [Notes](#notes)

---

## ✨ Features

- **Authentication**: JWT-based login/register + OAuth2 (Google login)
- **Menu Management**: Create, update, delete menu items with image upload; bulk import via Excel
- **Category Management**: CRUD categories with image support
- **Table Management**: Multi-floor restaurant layout, QR code generation per table, hide/show tables
- **Cart & Order Flow**: Customers scan QR → add to cart → request payment; staff confirm and process
- **Table Booking**: Reservation system with status lifecycle (PENDING → CONFIRMED → CHECKED_IN → COMPLETED / CANCELLED), table suggestion by guest count
- **Payment**: Cash payment, VNPay integration, mock MoMo confirmation
- **Activity Log**: Track all staff actions with Excel export, filterable by role/entity/date
- **Real-time**: WebSocket (STOMP over SockJS) for kitchen order updates
- **PDF Export**: Provisional invoices and order bills
- **API Docs**: Swagger UI via SpringDoc OpenAPI

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.7 |
| Security | Spring Security + JWT (`com.auth0:java-jwt`) + OAuth2 |
| Database | MySQL + Spring Data JPA |
| Cache | Redis (AI memory / session state) |
| Real-time | WebSocket (STOMP + SockJS) |
| File Processing | Apache POI (Excel), OpenPDF (PDF), ZXing (QR Code) |
| Mapping | ModelMapper |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI 2.7.0 (Swagger UI) |
| HTTP Client | OkHttp3 (OpenAI integration) |
| Build Tool | Maven |

---

## 🏗️ Architecture Overview

```
src/main/java/com/example/shop/
├── config/          # Security, CORS, WebSocket, Swagger, constants
├── controller/      # REST API endpoints
├── entity/          # JPA entities + enums
├── payloads/        # DTOs (request & response)
├── repository/      # Spring Data JPA repositories
├── security/        # JWTFilter, JWTUtil, OAuth2Handler
└── service/         # Business logic (interfaces + impl)
```

The system uses **role-based URL prefixes** to enforce access control:

| URL Prefix | Accessible By |
|---|---|
| `/api/public/**` | Anyone (no auth required) |
| `/api/admin/**` | ADMIN only |
| `/api/employee/manager/**` | ADMIN, MANAGER |
| `/api/employee/staff/**` | ADMIN, MANAGER, STAFF |
| `/api/shared/**` | Any authenticated user |

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- MySQL 8+
- Redis (running locally or Docker)
- Maven

### 1. Clone the repository

```bash
git clone <your-repo-url>
cd shop
```

### 2. Configure database & environment

Create an `application.properties` (or `application.yml`) under `src/main/resources/`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/quickshop?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=your_password

# JPA
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secret=your_jwt_secret_key
jwt.expiry=86400000

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# OAuth2 (Google)
spring.security.oauth2.client.registration.google.client-id=your_client_id
spring.security.oauth2.client.registration.google.client-secret=your_client_secret

# VNPay (optional)
vnpay.tmnCode=your_tmn_code
vnpay.hashSecret=your_hash_secret
vnpay.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.returnUrl=http://localhost:8080/api/vnpay/return
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The server starts at `http://localhost:8080`.

### 4. Access Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

---

## 📡 API Overview

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/public/login` | Login, returns JWT |
| POST | `/api/public/register` | Register new account |

### Menu Items
| Method | Endpoint | Description | Role |
|---|---|---|---|
| GET | `/api/public/menuItems` | List menu (filter by category, keyword, price) | Public |
| GET | `/api/public/menus/{menuId}` | Get single menu item | Public |
| POST | `/api/admin/menuItem` | Create menu item (multipart) | Admin |
| PUT | `/api/admin/menus/{menuId}` | Update menu item | Admin |
| DELETE | `/api/admin/menus/{menuId}` | Delete menu item | Admin |
| POST | `/api/admin/excel/menuItem` | Bulk import from Excel | Admin |

### Categories
| Method | Endpoint | Description | Role |
|---|---|---|---|
| GET | `/api/public/categories` | List categories | Public |
| GET | `/api/public/categories/{id}` | Get by ID | Public |
| POST | `/api/admin/category` | Create category | Admin |
| DELETE | `/api/admin/categories/{id}` | Delete category | Admin |

### Tables
| Method | Endpoint | Description | Role |
|---|---|---|---|
| GET | `/api/public/tables/floors` | Get tables grouped by floor | Public |
| GET | `/api/public/tables/{tableId}/qr` | Get QR code image | Public |
| GET | `/api/employee/staff/tables` | List all visible tables | Staff+ |
| POST | `/api/admin/tables` | Create table (multipart) | Admin |
| POST | `/api/admin/tables/multiple` | Create multiple tables | Admin |
| PUT | `/api/admin/tables/{tableId}` | Update table info | Admin |
| PUT | `/api/admin/{tableId}/hide` | Hide table | Admin |
| DELETE | `/api/admin/tables/{tableId}/delete` | Delete table | Admin |

### Cart & Orders
| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | `/api/public/cart/{tableId}` | Add item to cart | Public |
| GET | `/api/public/cart/{cartId}` | Get cart | Public |
| PUT | `/api/public/payment/{cartId}/request` | Customer requests to pay | Public |
| POST | `/api/employee/staff/provInvoice/{cartId}` | Print provisional PDF bill | Staff+ |
| POST | `/api/employee/staff/orders/pay-merged-carts` | Pay merged carts | Staff+ |
| GET | `/api/employee/staff/orders/waiting` | Kitchen queue (waiting) | Staff+ |
| GET | `/api/employee/staff/orders/pending` | Kitchen queue (pending) | Staff+ |

### Payment
| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | `/api/employee/staff/{cartId}/pay-cash` | Cash payment | Staff+ |
| POST | `/api/cart/{tableId}/vnpay` | Pay via VNPay | Authenticated |
| GET | `/api/vnpay/return` | VNPay callback handler | – |
| PUT | `/api/employee/staff/payments/{cartId}/cancel` | Cancel payment | Staff+ |

### Table Booking
| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | `/api/shared/bookings` | Create booking | Authenticated |
| POST | `/api/shared/bookings/{id}/cancel` | Cancel booking | Authenticated |
| GET | `/api/public/bookings/lookup` | Lookup by phone | Public |
| GET | `/api/employee/staff/bookings` | List all bookings (filterable) | Staff+ |
| POST | `/api/employee/staff/bookings/{id}/confirm` | Confirm booking | Staff+ |
| POST | `/api/employee/staff/bookings/{id}/check-in` | Check-in guest | Staff+ |
| POST | `/api/employee/staff/bookings/{id}/complete` | Complete booking | Staff+ |
| GET | `/api/employee/staff/tables/suggest` | Suggest available tables | Staff+ |

### Activity Logs
| Method | Endpoint | Description | Role |
|---|---|---|---|
| GET | `/api/admin/activity` | List all logs (filterable) | Admin |
| GET | `/api/admin/activity/{logId}` | Get log detail | Admin |
| DELETE | `/api/admin/activity/{logId}` | Delete log | Admin |
| GET | `/api/employee/staff/my-activity/log` | My activity log | Staff+ |
| GET | `/api/employee/staff/my-activity/log/export` | Export my log to Excel | Staff+ |

---

## 🔐 Role & Permission Model

```
ADMIN
  └─ Full access to all endpoints

MANAGER  
  └─ /api/employee/manager/**
  └─ /api/employee/staff/**

STAFF
  └─ /api/employee/staff/**
```

JWT token is passed via `Authorization: Bearer <token>` header.

---

## 📝 Notes

- CORS is configured for `http://localhost:3000` and `http://localhost:5173` (React/Vite dev servers)
- WebSocket endpoint: `ws://localhost:8080/ws` (SockJS fallback enabled)
- This is a **monolithic** architecture — all features run in a single Spring Boot application
- File uploads (images) are handled as `multipart/form-data` with JSON DTO passed as a separate `dto` part

---

## 👤 Author

**Võ Hồng Phúc**  
Fresher Java Developer
=======
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
>>>>>>> 3ab175f9eb05a835f19cc80ae887898d966d4bdd
