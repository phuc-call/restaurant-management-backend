package com.example.shop.payloads;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CartItemNoteDTO {
    @NotBlank(message = "Chú thích không được dài quá 255 kí tự")
    @Size(max = 255)
    private String note;
}
