package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportProgressDTO {
    private int currentRow;
    private int totalRow;
    private int successCount;
    private String message;
    private boolean done;
}

