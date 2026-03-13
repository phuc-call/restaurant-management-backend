package com.example.shop.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter

public enum ECartItemDeleteReason {

    CUSTOMER_LEFT("Khách bỏ bàn"),
    CUSTOMER_CHANGED_MIND("Khách đổi ý"),
    ORDERED_BY_MISTAKE("Nhập nhầm món"),
    TABLE_CHANGED("Khách chuyển bàn"),
    DUPLICATE_ORDER("Đơn bị trùng"),
    STAFF_CANCEL("Nhân viên hủy"),
    EMPTY_TIMEOUT("Cart trống quá thời gian");

    private final String label;

    ECartItemDeleteReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
