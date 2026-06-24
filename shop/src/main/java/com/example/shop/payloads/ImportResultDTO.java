package com.example.shop.payloads;

import lombok.Data;


import java.util.List;

@Data
public class ImportResultDTO
{
    private int successCount;
    private List<String>errors;
}
