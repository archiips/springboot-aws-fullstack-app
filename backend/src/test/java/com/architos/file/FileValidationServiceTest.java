package com.architos.file;

import com.architos.exception.FileSizeExceededException;
import com.architos.exception.InvalidFileTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FileValidationServiceTest {

    private FileValidationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new FileValidationService();
        // Set the max file size for testing
        ReflectionTestUtils.setField(underTest, "maxFileSizeStr", "10MB");
    }

    @Test
    void shouldValidateValidImageFile() {
        // Given
        MultipartFile validFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes());

        // When & Then - should not throw any exception
        underTest.validateImageFile(validFile);
    }

    @Test
    void shouldThrowExceptionForNullFile() {
        // When & Then
        assertThatThrownBy(() -> underTest.validateImageFile(null))
                .isInstanceOf(InvalidFileTypeException.class)
                .hasMessage("File cannot be empty");
    }

    @Test
    void shouldThrowExceptionForEmptyFile() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                new byte[0]);

        // When & Then
        assertThatThrownBy(() -> underTest.validateImageFile(emptyFile))
                .isInstanceOf(InvalidFileTypeException.class)
                .hasMessage("File cannot be empty");
    }

    @Test
    void shouldThrowExceptionForInvalidFileType() {
        // Given
        MultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes());

        // When & Then
        assertThatThrownBy(() -> underTest.validateImageFile(invalidFile))
                .isInstanceOf(InvalidFileTypeException.class)
                .hasMessageContaining("Invalid file type: text/plain");
    }

    @Test
    void shouldThrowExceptionForFileSizeExceeded() {
        // Given - Create a file larger than 10MB
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent);

        // When & Then
        assertThatThrownBy(() -> underTest.validateImageFile(largeFile))
                .isInstanceOf(FileSizeExceededException.class)
                .hasMessageContaining("File size")
                .hasMessageContaining("exceeds maximum allowed size");
    }

    @Test
    void shouldReturnTrueForValidImageTypes() {
        // Given & When & Then
        assertThat(underTest.isValidImageType("image/jpeg")).isTrue();
        assertThat(underTest.isValidImageType("image/jpg")).isTrue();
        assertThat(underTest.isValidImageType("image/png")).isTrue();
        assertThat(underTest.isValidImageType("image/gif")).isTrue();
        assertThat(underTest.isValidImageType("image/webp")).isTrue();
        assertThat(underTest.isValidImageType("IMAGE/JPEG")).isTrue(); // case insensitive
    }

    @Test
    void shouldReturnFalseForInvalidImageTypes() {
        // Given & When & Then
        assertThat(underTest.isValidImageType("text/plain")).isFalse();
        assertThat(underTest.isValidImageType("application/pdf")).isFalse();
        assertThat(underTest.isValidImageType("video/mp4")).isFalse();
        assertThat(underTest.isValidImageType(null)).isFalse();
        assertThat(underTest.isValidImageType("")).isFalse();
    }

    @Test
    void shouldReturnTrueForValidFileSize() {
        // Given
        long validSize = 5 * 1024 * 1024; // 5MB

        // When & Then
        assertThat(underTest.isValidFileSize(validSize)).isTrue();
    }

    @Test
    void shouldReturnFalseForInvalidFileSize() {
        // Given
        long invalidSize = 15 * 1024 * 1024; // 15MB

        // When & Then
        assertThat(underTest.isValidFileSize(invalidSize)).isFalse();
    }

    @Test
    void shouldValidateAllSupportedImageFormats() {
        // Given
        String[] supportedTypes = { "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp" };

        for (String contentType : supportedTypes) {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test." + contentType.split("/")[1],
                    contentType,
                    "test content".getBytes());

            // When & Then - should not throw any exception
            underTest.validateImageFile(file);
        }
    }

    @Test
    void shouldHandleDifferentFileSizeFormats() {
        // Test with KB format
        ReflectionTestUtils.setField(underTest, "maxFileSizeStr", "1024KB");
        assertThat(underTest.isValidFileSize(1024 * 1024)).isTrue(); // 1MB should be valid
        assertThat(underTest.isValidFileSize(2 * 1024 * 1024)).isFalse(); // 2MB should be invalid

        // Test with B format
        ReflectionTestUtils.setField(underTest, "maxFileSizeStr", "1024B");
        assertThat(underTest.isValidFileSize(1024)).isTrue();
        assertThat(underTest.isValidFileSize(2048)).isFalse();

        // Test with plain number
        ReflectionTestUtils.setField(underTest, "maxFileSizeStr", "1024");
        assertThat(underTest.isValidFileSize(1024)).isTrue();
        assertThat(underTest.isValidFileSize(2048)).isFalse();
    }
}