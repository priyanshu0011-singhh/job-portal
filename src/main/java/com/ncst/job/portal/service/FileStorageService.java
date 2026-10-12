package com.ncst.job.portal.service;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path root;

    @PostConstruct
    public void init() {
        try {
            root = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    public String store(MultipartFile file, String ownerId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file upload not allowed");
        }

        String originalName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename(), "File name is required"));
        String ext = "";
        int i = originalName.lastIndexOf('.');
        if (i >= 0) {
            ext = originalName.substring(i).toLowerCase();
        }

        if (!Set.of(".pdf", ".doc", ".docx").contains(ext)) {
            throw new IllegalArgumentException("Only PDF, DOC, and DOCX resume files are allowed");
        }

        String filename = ownerId + "_" + UUID.randomUUID() + ext;
        Path targetLocation = root.resolve(filename).normalize();

        if (!targetLocation.startsWith(root)) {
            throw new SecurityException("Invalid upload path");
        }

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/api/files/resumes/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + filename, e);
        }
    }

    public Resource loadAsResource(String filename) {
        try {
            Path filePath = root.resolve(filename).normalize();
            if (!filePath.startsWith(root)) {
                throw new SecurityException("Invalid file path");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (Files.isRegularFile(filePath) && resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("Could not read file: " + filename);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed file URL: " + filename, e);
        }
    }
}

