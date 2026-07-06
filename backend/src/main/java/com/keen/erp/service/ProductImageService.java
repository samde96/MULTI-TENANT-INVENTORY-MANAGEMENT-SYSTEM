package com.keen.erp.service;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path uploadRoot;
    private final CurrentUserService currentUserService;

    public ProductImageService(@Value("${app.upload-dir:uploads}") String uploadDir,
                               CurrentUserService currentUserService) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.currentUserService = currentUserService;
        try {
            Files.createDirectories(uploadRoot.resolve("products"));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize upload directory", ex);
        }
    }

    public ApiDtos.UploadResponse storeProductImage(MultipartFile image) {
        currentUserService.requirePermission("catalog:manage");
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        String contentType = Optional.ofNullable(image.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported image type. Use JPG, PNG, WEBP, or GIF.");
        }

        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".img";
        };

        String fileName = "product-" + UUID.randomUUID() + extension;
        Path destination = uploadRoot.resolve("products").resolve(fileName);
        try (InputStream inputStream = image.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store product image", ex);
        }

        return new ApiDtos.UploadResponse("/uploads/products/" + fileName, fileName);
    }
}
