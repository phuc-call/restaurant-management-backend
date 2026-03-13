package com.example.shop.payloads;

import com.example.shop.entity.enums.EAccountStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long userId;
    private String userName;
    private String phone;
    private String email;
    private ImageDTO imageDTO;
    private String password;
    private Set<String> roleNames;
    private String position;
    private AddressDTO address;
    private LocalDate brithDay;
    private LocalDateTime createAt;
    private EAccountStatus accountStatus;

}
