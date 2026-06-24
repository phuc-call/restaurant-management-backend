package com.example.shop.payloads;

import com.example.shop.entity.enums.CancelReason;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelBookingRequestDTO {
    @NotNull
    private CancelReason reason;

}
