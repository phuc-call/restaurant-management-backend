package com.example.shop.service;

import com.example.shop.payloads.ImportResultDTO;
import com.example.shop.payloads.MenuItemImportDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface MenuItemImportService {
    ImportResultDTO importMenu(MultipartFile excelFile, List<MultipartFile> images);
}
