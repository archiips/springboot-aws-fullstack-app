package com.architos.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadConfigTest {

    private FileUploadConfig fileUploadConfig;

    @BeforeEach
    void setUp() {
        fileUploadConfig = new FileUploadConfig();
    }

    @Test
    void shouldHaveDefaultConfiguration() {
        // Given & When - default configuration

        // Then
        assertThat(fileUploadConfig.getMaxSize()).isEqualTo("10MB");
        assertThat(fileUploadConfig.getAllowedTypes())
                .containsExactly("image/jpeg", "image/png", "image/gif", "image/webp");
    }

    @Test
    void shouldAllowSettingCustomConfiguration() {
        // Given
        fileUploadConfig.setMaxSize("5MB");
        fileUploadConfig.setAllowedTypes(List.of("image/jpeg", "image/png"));

        // When & Then
        assertThat(fileUploadConfig.getMaxSize()).isEqualTo("5MB");
        assertThat(fileUploadConfig.getAllowedTypes())
                .containsExactly("image/jpeg", "image/png");
    }

    @Test
    void shouldConvertMaxSizeToBytes() {
        // Given
        fileUploadConfig.setMaxSize("10MB");

        // When
        long sizeInBytes = fileUploadConfig.getMaxSizeInBytes();

        // Then
        assertThat(sizeInBytes).isEqualTo(10 * 1024 * 1024);
    }

    @Test
    void shouldConvertKilobytesToBytes() {
        // Given
        fileUploadConfig.setMaxSize("500KB");

        // When
        long sizeInBytes = fileUploadConfig.getMaxSizeInBytes();

        // Then
        assertThat(sizeInBytes).isEqualTo(500 * 1024);
    }

    @Test
    void shouldConvertGigabytesToBytes() {
        // Given
        fileUploadConfig.setMaxSize("2GB");

        // When
        long sizeInBytes = fileUploadConfig.getMaxSizeInBytes();

        // Then
        assertThat(sizeInBytes).isEqualTo(2L * 1024 * 1024 * 1024);
    }

    @Test
    void shouldHandlePlainBytesFormat() {
        // Given
        fileUploadConfig.setMaxSize("1024B");

        // When
        long sizeInBytes = fileUploadConfig.getMaxSizeInBytes();

        // Then
        assertThat(sizeInBytes).isEqualTo(1024);
    }

    @Test
    void shouldHandleNumberWithoutUnit() {
        // Given
        fileUploadConfig.setMaxSize("2048");

        // When
        long sizeInBytes = fileUploadConfig.getMaxSizeInBytes();

        // Then
        assertThat(sizeInBytes).isEqualTo(2048);
    }

    @Test
    void shouldThrowExceptionForInvalidSizeFormat() {
        // Given
        fileUploadConfig.setMaxSize("invalid");

        // When & Then
        assertThatThrownBy(() -> fileUploadConfig.getMaxSizeInBytes())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file size format: invalid");
    }

    @Test
    void shouldCheckAllowedContentTypes() {
        // Given
        fileUploadConfig.setAllowedTypes(java.util.List.of("image/jpeg", "image/png"));

        // When & Then
        assertThat(fileUploadConfig.isAllowedType("image/jpeg")).isTrue();
        assertThat(fileUploadConfig.isAllowedType("image/png")).isTrue();
        assertThat(fileUploadConfig.isAllowedType("image/gif")).isFalse();
        assertThat(fileUploadConfig.isAllowedType("text/plain")).isFalse();
    }

    @Test
    void shouldReturnFalseForNullContentType() {
        // Given
        fileUploadConfig.setAllowedTypes(java.util.List.of("image/jpeg"));

        // When & Then
        assertThat(fileUploadConfig.isAllowedType(null)).isFalse();
    }
}