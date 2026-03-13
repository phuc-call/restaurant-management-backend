package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KitchenNode {
    private Long userId;
    private String userName;
    private Integer ActiveOrderCount;
    private LocalDateTime LastAssignedAt;
}
