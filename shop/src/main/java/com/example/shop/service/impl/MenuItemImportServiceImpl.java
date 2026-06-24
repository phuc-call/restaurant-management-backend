package com.example.shop.service.impl;

import com.example.shop.config.ExcelHelper;
import com.example.shop.entity.Category;
import com.example.shop.entity.Image;
import com.example.shop.entity.MenuItem;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.ImportProgressDTO;
import com.example.shop.payloads.ImportResultDTO;
import com.example.shop.payloads.MenuItemImportDTO;
import com.example.shop.repository.CategoryRepo;
import com.example.shop.repository.ImageRepo;
import com.example.shop.repository.MenuItemRepo;
import com.example.shop.service.MenuItemImportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuItemImportServiceImpl implements MenuItemImportService {
    private final MenuItemRepo menuItemRepo;
    private final CategoryRepo categoryRepo;
    private final ImageRepo imageRepo;
    @Autowired
    FileStorageService fileStorageService;
    @Autowired
    FileSystemStorageService fileSystemStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    private final ExcelHelper excelHelper;

    @Override
    @Transactional
    public ImportResultDTO importMenu(MultipartFile excelFile, List<MultipartFile> images) {

        Map<String, String> imageMap = new HashMap<>();
        if (images != null) {
            for (MultipartFile img : images) {
                String path = fileSystemStorageService.save(img);
                imageMap.put(img.getOriginalFilename(), path);
            }
        }

        Sheet sheet = excelHelper.readFirstSheet(excelFile);

        ImportResultDTO result = new ImportResultDTO();
        result.setErrors(new java.util.ArrayList<>());

        int success = 0;
        int total = sheet.getLastRowNum();

        for (int i = 1; i <= total; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            try {
                MenuItemImportDTO dto = parseRow(row, i);
                saveMenu(dto, imageMap);
                success++;

                messagingTemplate.convertAndSend(
                        "/topic/import-progress",
                        new ImportProgressDTO(
                                i,
                                total,
                                success,
                                "Import thành công dòng " + (i + 1),
                                false
                        )
                );

            } catch (APIException ex) {
                result.getErrors().add(ex.getMessage());

                messagingTemplate.convertAndSend(
                        "/topic/import-progress",
                        new ImportProgressDTO(
                                i,
                                total,
                                success,
                                ex.getMessage(),
                                false
                        )
                );
            }
        }

        // THÔNG BÁO HOÀN THÀNH
        messagingTemplate.convertAndSend(
                "/topic/import-progress",
                new ImportProgressDTO(
                        total,
                        total,
                        success,
                        "Import hoàn tất",
                        true
                )
        );
        //BÁO MENU CHANGED
        messagingTemplate.convertAndSend(
                "/topic/menu-changed",
                "MENU_UPDATED"
        );

        result.setSuccessCount(success);
        return result;
    }

    // ================= PARSE + VALIDATE =================

    private MenuItemImportDTO parseRow(Row row, int rowIndex) {

        MenuItemImportDTO dto = new MenuItemImportDTO();

        dto.setName(getRequiredString(row, 0, rowIndex, "Tên món"));

        BigDecimal price = getRequiredNumber(row, 1, rowIndex, "Giá");
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new APIException("Dòng " + (rowIndex + 1) + ": Giá phải > 0");
        }
        dto.setPrice(price);

        dto.setDescription(getOptionalString(row, 2));

        dto.setCategoryId(getRequiredLong(row, 3, rowIndex, "Category ID"));

        if (row.getCell(4) != null && row.getCell(4).getCellType() != CellType.BLANK) {
            dto.setImageNames(
                    Arrays.stream(row.getCell(4).getStringCellValue().split(","))
                            .map(String::trim)
                            .toList()
            );
        } else {
            dto.setImageNames(List.of());
        }

        return dto;
    }

    // ================= SAVE DB =================

    private void saveMenu(MenuItemImportDTO dto, Map<String, String> imageMap) {

        Category category = categoryRepo.findById(dto.getCategoryId())
                .orElseThrow(() ->
                        new APIException("Category ID " + dto.getCategoryId() + " không tồn tại")
                );

        MenuItem menu = new MenuItem();
        menu.setName(dto.getName());
        menu.setPrice(dto.getPrice());
        menu.setDescription(dto.getDescription());
        menu.setCategory(category);

        MenuItem saved = menuItemRepo.save(menu);

        for (String imgName : dto.getImageNames()) {
            if (!imageMap.containsKey(imgName)) continue;

            Image img = new Image();
            img.setMenuItem(saved);
            img.setImageUrl(imageMap.get(imgName));
            imageRepo.save(img);
        }
    }
    // ================= HELPER =================

    private String getRequiredString(Row row, int col, int rowIndex, String field) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            throw new APIException("Dòng " + (rowIndex + 1) + ": " + field + " không được để trống");
        }
        return cell.getStringCellValue().trim();
    }

    private String getOptionalString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        return cell.getStringCellValue().trim();
    }

    private BigDecimal getRequiredNumber(Row row, int col, int rowIndex, String field) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            throw new APIException("Dòng " + (rowIndex + 1) + ": " + field + " phải là số");
        }
        return BigDecimal.valueOf(cell.getNumericCellValue());
    }

    private Long getRequiredLong(Row row, int col, int rowIndex, String field) {
        return getRequiredNumber(row, col, rowIndex, field).longValue();
    }

}
