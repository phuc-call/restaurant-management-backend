package com.example.shop.service.impl;

import com.example.shop.service.StorageService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Transactional
public class FileSystemStorageService implements StorageService{
    private final Path root = Paths.get("uploads").toAbsolutePath();
    @Override
    public String save(MultipartFile file) {
        try {
            // Tạo thư mục nếu chưa có
            Files.createDirectories(root);
            //Tạo file an toàn tránh trùng
            String filename=System.currentTimeMillis()+"_"+file.getOriginalFilename();
            Path destination = root.resolve(filename);
            file.transferTo(destination.toFile());
            return "/uploads/" + filename;

        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu file", e);
        }
    }
}
