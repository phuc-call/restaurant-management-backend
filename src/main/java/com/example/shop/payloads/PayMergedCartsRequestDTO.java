package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PayMergedCartsRequestDTO {
    private Long masterCartId;
    private List<Long> cartIds;
    private BigDecimal cashReceived;
    private Map<Long, String>itemNotes;
}
