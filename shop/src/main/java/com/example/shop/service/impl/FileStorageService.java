package com.example.shop.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Transactional
public class FileStorageService {
    //SAVE IN SOURCE!
    private final Path root = Path.of("src/main/resources/static/uploads");
    public FileStorageService() {
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
        }catch (IOException e){
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }
    public String save(MultipartFile file){
        try {
            String fileName= UUID.randomUUID()+"_"+file.getOriginalFilename();
            Path filepath=this.root.resolve(fileName);
            Files.copy(file.getInputStream(),filepath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + fileName;
        }catch (IOException e){
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }
}
