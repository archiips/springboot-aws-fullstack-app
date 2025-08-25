package com.architos.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileUploadMetricsServiceTest {

    private FileUploadMetricsService metricsService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new FileUploadMetricsService(meterRegistry);
    }

    @Test
    void shouldRecordUploadSuccess() {
        // Given
        double initialCount = metricsService.getSuccessCount();

        // When
        metricsService.recordUploadSuccess();

        // Then
        assertThat(metricsService.getSuccessCount()).isEqualTo(initialCount + 1);
    }

    @Test
    void shouldRecordUploadFailure() {
        // Given
        double initialCount = metricsService.getFailureCount();

        // When
        metricsService.recordUploadFailure();

        // Then
        assertThat(metricsService.getFailureCount()).isEqualTo(initialCount + 1);
    }

    @Test
    void shouldRecordValidationFailure() {
        // Given
        double initialCount = metricsService.getValidationFailureCount();

        // When
        metricsService.recordValidationFailure();

        // Then
        assertThat(metricsService.getValidationFailureCount()).isEqualTo(initialCount + 1);
    }

    @Test
    void shouldRecordS3Failure() {
        // Given
        double initialCount = metricsService.getS3FailureCount();

        // When
        metricsService.recordS3Failure();

        // Then
        assertThat(metricsService.getS3FailureCount()).isEqualTo(initialCount + 1);
    }

    @Test
    void shouldRecordUploadDuration() {
        // Given
        long durationMs = 1500;

        // When
        metricsService.recordUploadDuration(durationMs);

        // Then
        // Verify that the timer has recorded at least one measurement
        assertThat(meterRegistry.get("file_upload_duration").timer().count()).isGreaterThan(0);
    }

    @Test
    void shouldStartAndStopTimer() {
        // Given
        var sample = metricsService.startUploadTimer();

        // When
        try {
            Thread.sleep(10); // Small delay to ensure measurable duration
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        metricsService.recordUploadDuration(sample);

        // Then
        assertThat(meterRegistry.get("file_upload_duration").timer().count()).isEqualTo(1);
        assertThat(metricsService.getAverageUploadDuration()).isGreaterThan(0);
    }

    @Test
    void shouldTrackMultipleMetrics() {
        // When
        metricsService.recordUploadSuccess();
        metricsService.recordUploadSuccess();
        metricsService.recordUploadFailure();
        metricsService.recordValidationFailure();
        metricsService.recordS3Failure();

        // Then
        assertThat(metricsService.getSuccessCount()).isEqualTo(2);
        assertThat(metricsService.getFailureCount()).isEqualTo(1);
        assertThat(metricsService.getValidationFailureCount()).isEqualTo(1);
        assertThat(metricsService.getS3FailureCount()).isEqualTo(1);
    }

    @Test
    void shouldRecordUploadSuccessWithContext() {
        // Given
        double initialCount = metricsService.getSuccessCount();

        // When
        metricsService.recordUploadSuccess("123", "test.jpg", 1024L, "image/jpeg");

        // Then
        assertThat(metricsService.getSuccessCount()).isEqualTo(initialCount + 1);
    }

    @Test
    void shouldRecordValidationFailureWithContext() {
        // Given
        double initialCount = metricsService.getValidationFailureCount();

        // When
        metricsService.recordValidationFailure("file_type", "Invalid file type: text/plain");

        // Then
        assertThat(metricsService.getValidationFailureCount()).isEqualTo(initialCount + 1);
    }

    @Test
    void shouldRecordS3FailureWithContext() {
        // Given
        double initialCount = metricsService.getS3FailureCount();

        // When
        metricsService.recordS3Failure("upload", "Access denied");

        // Then
        assertThat(metricsService.getS3FailureCount()).isEqualTo(initialCount + 1);
    }

    @Test
    void shouldRecordUploadAttemptSuccess() {
        // Given
        double initialSuccessCount = metricsService.getSuccessCount();

        // When
        metricsService.recordUploadAttempt("123", "test.jpg", 1024L, "image/jpeg", true, null, 1500L);

        // Then
        assertThat(metricsService.getSuccessCount()).isEqualTo(initialSuccessCount + 1);
        assertThat(meterRegistry.get("file_upload_duration").timer().count()).isGreaterThan(0);
    }

    @Test
    void shouldRecordUploadAttemptFailure() {
        // Given
        double initialFailureCount = metricsService.getFailureCount();

        // When
        metricsService.recordUploadAttempt("123", "test.jpg", 1024L, "image/jpeg", false, "S3 error", 800L);

        // Then
        assertThat(metricsService.getFailureCount()).isEqualTo(initialFailureCount + 1);
        assertThat(meterRegistry.get("file_upload_duration").timer().count()).isGreaterThan(0);
    }

    @Test
    void shouldReturnMetricsSummary() {
        // Given
        metricsService.recordUploadSuccess();
        metricsService.recordUploadFailure();
        metricsService.recordValidationFailure();

        // When
        var summary = metricsService.getMetricsSummary();

        // Then
        assertThat(summary).containsKeys(
                "total_uploads", "success_count", "failure_count",
                "validation_failure_count", "s3_failure_count",
                "average_duration_ms", "success_rate", "failure_rate");
        assertThat(summary.get("total_uploads")).isEqualTo(2.0);
        assertThat(summary.get("success_rate")).isEqualTo(0.5);
        assertThat(summary.get("failure_rate")).isEqualTo(0.5);
    }
}