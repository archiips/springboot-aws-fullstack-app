package com.architos.file;

import com.architos.exception.FileSizeExceededException;
import com.architos.exception.InvalidFileTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
public class FileValidationService {

    private static final Logger logger = LoggerFactory.getLogger(FileValidationService.class);

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp");

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSizeStr;

    public void validateImageFile(MultipartFile file) {
        // Add structured logging context
        MDC.put("operation", "file_validation");
        MDC.put("file_name", file != null ? file.getOriginalFilename() : "null");
        MDC.put("file_size", file != null ? String.valueOf(file.getSize()) : "0");
        MDC.put("content_type", file != null ? file.getContentType() : "null");

        try {
            logger.info("Starting file validation - name: {}, size: {} bytes, type: {}",
                    file != null ? file.getOriginalFilename() : "null",
                    file != null ? file.getSize() : 0,
                    file != null ? file.getContentType() : "null");

            if (file == null || file.isEmpty()) {
                logger.warn("File validation failed: file is null or empty");
                throw new InvalidFileTypeException("File cannot be empty");
            }

            validateFileType(file);
            validateFileSize(file);

            logger.info("File validation completed successfully");
        } catch (Exception e) {
            logger.error("File validation failed: {}", e.getMessage());
            throw e;
        } finally {
            // Clean up MDC
            MDC.remove("operation");
            MDC.remove("file_name");
            MDC.remove("file_size");
            MDC.remove("content_type");
        }
    }

    public boolean isValidImageType(String contentType) {
        return contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
    }

    public boolean isValidFileSize(long size) {
        long maxSizeBytes = parseMaxFileSize();
        return size <= maxSizeBytes;
    }

    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        logger.debug("Validating file type: {}", contentType);

        if (!isValidImageType(contentType)) {
            logger.warn("Invalid file type detected: {}, allowed types: {}",
                    contentType, String.join(", ", ALLOWED_IMAGE_TYPES));
            throw new InvalidFileTypeException(
                    String.format("Invalid file type: %s. Allowed types are: %s",
                            contentType, String.join(", ", ALLOWED_IMAGE_TYPES)));
        }

        logger.debug("File type validation passed: {}", contentType);
    }

    private void validateFileSize(MultipartFile file) {
        long fileSize = file.getSize();
        long maxSizeBytes = parseMaxFileSize();

        logger.debug("Validating file size: {} bytes, max allowed: {} bytes", fileSize, maxSizeBytes);

        if (!isValidFileSize(fileSize)) {
            logger.warn("File size exceeded: {} bytes, max allowed: {} bytes", fileSize, maxSizeBytes);
            throw new FileSizeExceededException(
                    String.format("File size %d bytes exceeds maximum allowed size of %d bytes",
                            fileSize, maxSizeBytes));
        }

        logger.debug("File size validation passed: {} bytes", fileSize);
    }

    private long parseMaxFileSize() {
        // Parse Spring's file size format (e.g., "10MB", "1024KB")
        String sizeStr = maxFileSizeStr.toUpperCase();

        if (sizeStr.endsWith("MB")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024 * 1024;
        } else if (sizeStr.endsWith("KB")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024;
        } else if (sizeStr.endsWith("B")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 1));
        } else {
            // Assume bytes if no unit specified
            return Long.parseLong(sizeStr);
        }
    }
}