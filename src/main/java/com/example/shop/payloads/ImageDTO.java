package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ImageDTO {
    private Long id;
    private String imageUrl;
    private String altText;
}
