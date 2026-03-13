package com.example.shop.service;

import com.example.shop.payloads.TableTypeDTO;

public interface TypeTableService {
    TableTypeDTO createTable(TableTypeDTO tableTypeDTO);
    TableTypeDTO updateTableType(TableTypeDTO tableTypeDTO, Long id);
    String deleted(Long id);
}
