package com.example.shop.payloads.reponse;

import com.example.shop.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStatusChangedEventDTO {
    private Long paymentId;
    private PaymentStatus paymentStatus;
}
