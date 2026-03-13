package com.example.shop.hellper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileNameUtil {
    public static String buildExcelFileName(String fileName) {
        String baseName = (fileName == null || fileName.isBlank())
                ? "report"
                : fileName;

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        return baseName + "_" + timestamp + ".xlsx";
    }
}
