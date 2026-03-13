package com.example.shop.payloads;

import com.example.shop.entity.enums.EAccountStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAccountStatusDTO {
    private EAccountStatus status;
}