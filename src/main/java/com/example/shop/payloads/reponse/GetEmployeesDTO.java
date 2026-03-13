package com.example.shop.payloads.reponse;

import com.example.shop.entity.enums.EAccountStatus;
import com.example.shop.payloads.ImageDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GetEmployeesDTO {
    private Long userId;
    private String userName;
    private String phone;
private ImageDTO imageDTO;
    private String email;
    private String position;
    private EAccountStatus accountStatus;
}
