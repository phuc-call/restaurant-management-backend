package com.example.shop.config;


public class AppConstants {
    public static final String PAGE_NUMBER = "0";
    public static final String PAGE_SIZE = "2";
    public static final String SORT_CATEGORY_BY = "categoryId";
    public static final String SORT_MENU_BY = "id";
    public static final String SORT_TABLE_ID = "tableId"; // đúng

    public static final String SORT_USER_BY = "userId";

    public static final String SORT_ORDER_BY = "totalAmount";
    public static final String SORT_DIR = "asc";      // đúng

    public static final Long ADMIN_ID = 101L;
    public static final Long USER_ID = 102L;
    public static final Long EMPLOYEE = 104L;
    public static final Long MANAGER = 105L;

    public static final String[] POSITION = {"Manager", "Receptionist", "ServiceStaff", "KitchenStaff"};
    public static final long JWT_TOKEN_VALIDATION = 5 * 60 * 60;
    //Url
    public static final String[] PUBLIC_URLS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/api/register",
            "/api/login",
            "/api/cart/**",
            "/api/momo/**",
            "/api/mock/**",
            "/api/sepay/**",
            "/admin/public/products/image/**",
            "/api/public/carts/**",
            "/api/public/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/ws/**",
            "/topic/**",
            "/app/**"
    };

    public static final String[] USER_URL = {"/api/public/**"};
    public static final String[] ADMIN_URL = {"/api/admin/**"};
    public static final String[] EMPLOYEE_MANA_URL = {"/api/employee/manager/**"};
    public static final String[] EMPLOYEE_STAFF_URL = {"/api/employee/staff/**"};
    public static final String[] SHARED_URL = {"/api/shared/**"};
    // Positions
    public static final String POSITION_ADMIN = "ADMIN";
    public static final String POSITION_MANAGER = "MANAGER";
    public static final String POSITION_STAFF = "STAFF";
    public static final String POSITION_CUSTOMER = "USER";
}
