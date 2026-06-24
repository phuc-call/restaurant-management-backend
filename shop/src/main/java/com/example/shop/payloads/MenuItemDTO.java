package com.example.shop.payloads;

import com.example.shop.entity.Category;
import com.example.shop.entity.OrderItem;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;


import java.math.BigDecimal;
import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MenuItemDTO {
    private String id;
    private String name;
    private BigDecimal price;
    private String description;
    private Long categoryId;
    private List<ImageDTO>images;
}