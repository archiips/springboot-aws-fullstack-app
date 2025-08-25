package com.architos.file;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(FileUploadConfig.class)
@TestPropertySource(properties = {
        "file.upload.max-size=5MB",
        "file.upload.allowed-types[0]=image/jpeg",
        "file.upload.allowed-types[1]=image/png"
})
class FileUploadConfigIntegrationTest {

    @Autowired
    private FileUploadConfig fileUploadConfig;

    @Test
    void shouldLoadConfigurationFromProperties() {
        // Given & When - configuration is loaded by Spring

        // Then
        assertThat(fileUploadConfig.getMaxSize()).isEqualTo("5MB");
        assertThat(fileUploadConfig.getAllowedTypes())
                .containsExactly("image/jpeg", "image/png");
    }

    @Test
    void shouldConvertMaxSizeToBytes() {
        // Given - configuration loaded from properties (5MB)

        // When
        long sizeInBytes = fileUploadConfig.getMaxSizeInBytes();

        // Then
        assertThat(sizeInBytes).isEqualTo(5 * 1024 * 1024);
    }

    @Test
    void shouldValidateAllowedTypes() {
        // Given - configuration loaded from properties

        // When & Then
        assertThat(fileUploadConfig.isAllowedType("image/jpeg")).isTrue();
        assertThat(fileUploadConfig.isAllowedType("image/png")).isTrue();
        assertThat(fileUploadConfig.isAllowedType("image/gif")).isFalse();
        assertThat(fileUploadConfig.isAllowedType("text/plain")).isFalse();
    }
}