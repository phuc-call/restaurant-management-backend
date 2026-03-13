package com.example.shop.hellper;

import com.example.shop.entity.User;
import com.example.shop.entity.enums.EAccountStatus;
import com.example.shop.entity.enums.EOnlineStatus;
import com.example.shop.exception.APIException;
import com.example.shop.repository.UserRepo;
import org.springframework.stereotype.Component;

@Component
public class StaffStatusValidator {

    private final UserRepo userRepo;

    public StaffStatusValidator(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public User validateStaffWorking() {
        Long userId = SecuritySnapshotUtil.getUserId();

        if (userId == null) {
            throw new APIException("Bạn chưa đăng nhập");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new APIException("Không tìm thấy nhân viên"));

        if (user.getAccountStatus() != EAccountStatus.ACTIVE) {
            throw new APIException("Tài khoản đang bị khóa");
        }

        if (user.getOnlineStatus() != EOnlineStatus.ONLINE) {
            throw new APIException("Bạn đang clock-out, không thể thực hiện thao tác này");
        }

        return user;
    }
}
