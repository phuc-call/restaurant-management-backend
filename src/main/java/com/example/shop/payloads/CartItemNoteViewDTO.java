package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CartItemNoteViewDTO {
    private Long cartItemId;
    private String menuName;
    private String note;
}
