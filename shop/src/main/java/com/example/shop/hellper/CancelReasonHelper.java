package com.example.shop.hellper;

import com.example.shop.entity.enums.CancelReason;
import com.example.shop.exception.APIException;

import java.util.EnumSet;

public class CancelReasonHelper {

    public static void validateCancelReason(String role, CancelReason reason) {
        if ("STAFF".equals(role)) {
            if (!EnumSet.of(
                    CancelReason.CUSTOMER_NO_SHOW,
                    CancelReason.CUSTOMER_REQUEST,
                    CancelReason.CUSTOMER_LATE
            ).contains(reason)) {
                throw new APIException("Nhân viên không được dùng lý do hủy này");
            }
        }
    }
}

