package com.architos.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

    private String maxSize = "10MB";
    private List<String> allowedTypes = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp");

    public String getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = maxSize;
    }

    public List<String> getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(List<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

    /**
     * Convert max size string to bytes for validation
     * Supports formats like "10MB", "5KB", "1GB"
     */
    public long getMaxSizeInBytes() {
        String size = maxSize.toUpperCase();
        long multiplier = 1;

        if (size.endsWith("KB")) {
            multiplier = 1024;
            size = size.substring(0, size.length() - 2);
        } else if (size.endsWith("MB")) {
            multiplier = 1024 * 1024;
            size = size.substring(0, size.length() - 2);
        } else if (size.endsWith("GB")) {
            multiplier = 1024 * 1024 * 1024;
            size = size.substring(0, size.length() - 2);
        } else if (size.endsWith("B")) {
            size = size.substring(0, size.length() - 1);
        }

        try {
            return Long.parseLong(size.trim()) * multiplier;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid file size format: " + maxSize);
        }
    }

    /**
     * Check if the given content type is allowed
     */
    public boolean isAllowedType(String contentType) {
        return contentType != null && allowedTypes.contains(contentType);
    }
}