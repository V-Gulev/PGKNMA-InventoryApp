package com.fittrack.inventoryapp.service.impl;

import com.fittrack.inventoryapp.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path rootLocation = Paths.get("uploads");

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            
            Path destinationDir = rootLocation;
            if (StringUtils.hasText(subDirectory)) {
                destinationDir = rootLocation.resolve(subDirectory);
            }
            
            Files.createDirectories(destinationDir);

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String newFilename = UUID.randomUUID().toString() + extension;
            Path destinationFile = destinationDir.resolve(Paths.get(newFilename))
                    .normalize().toAbsolutePath();
                    
            if (!destinationFile.getParent().startsWith(destinationDir.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }

            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Return URL path relative to uploads directory
            if (StringUtils.hasText(subDirectory)) {
                return subDirectory + "/" + newFilename;
            }
            return newFilename;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }
    @Override
    public void deleteFile(String path) {
        try {
            if (!org.springframework.util.StringUtils.hasText(path)) {
                return;
            }
            if (path.contains("..")) {
                throw new RuntimeException("Cannot delete file outside current directory.");
            }
            Path fileToDelete = rootLocation.resolve(path).normalize().toAbsolutePath();
            if (fileToDelete.startsWith(rootLocation.toAbsolutePath())) {
                Files.deleteIfExists(fileToDelete);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file.", e);
        }
    }
}
