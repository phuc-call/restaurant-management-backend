package com.example.shop.config;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ExcelHelper {

    public Sheet readFirstSheet(MultipartFile file) {
        try {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            return workbook.getSheetAt(0);
        } catch (Exception e) {
            throw new RuntimeException("Cat not read file Excel", e);
        }
    }
}