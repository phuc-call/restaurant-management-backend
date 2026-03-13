package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEmployeeDTO {
    private String userName;
    private String phone;
    private LocalDate brithDay;
    private String position;     // VD: STAFF, MANAGER
    private AddressDTO address;  // update địa chỉ
}
