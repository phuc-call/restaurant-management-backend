package com.example.shop.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartNoteRequest {
    @NotBlank(message = "Chú thích không được dài quá 255 kí tự")
    @Size(max = 255)
    private String note;
}
