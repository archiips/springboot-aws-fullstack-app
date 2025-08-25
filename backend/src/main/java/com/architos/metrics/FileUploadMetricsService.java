package com.architos.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class FileUploadMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadMetricsService.class);

    private final Counter uploadSuccessCounter;
    private final Counter uploadFailureCounter;
    private final Counter validationFailureCounter;
    private final Counter s3FailureCounter;
    private final Timer uploadTimer;

    public FileUploadMetricsService(MeterRegistry meterRegistry) {
        this.uploadSuccessCounter = Counter.builder("file_upload_success_total")
                .description("Total number of successful file uploads")
                .register(meterRegistry);

        this.uploadFailureCounter = Counter.builder("file_upload_failure_total")
                .description("Total number of failed file uploads")
                .tag("type", "general")
                .register(meterRegistry);

        this.validationFailureCounter = Counter.builder("file_upload_failure_total")
                .description("Total number of file validation failures")
                .tag("type", "validation")
                .register(meterRegistry);

        this.s3FailureCounter = Counter.builder("file_upload_failure_total")
                .description("Total number of S3 service failures")
                .tag("type", "s3")
                .register(meterRegistry);

        this.uploadTimer = Timer.builder("file_upload_duration")
                .description("Time taken for file upload operations")
                .register(meterRegistry);
    }

    public void recordUploadSuccess() {
        uploadSuccessCounter.increment();
        logger.debug("Recorded successful upload metric");
    }

    public void recordUploadFailure() {
        uploadFailureCounter.increment();
        logger.debug("Recorded general upload failure metric");
    }

    public void recordUploadFailure(String reason) {
        uploadFailureCounter.increment();
        logger.info("Recorded upload failure metric - reason: {}", reason);
    }

    public void recordValidationFailure() {
        validationFailureCounter.increment();
        logger.debug("Recorded validation failure metric");
    }

    public void recordValidationFailure(String validationType, String reason) {
        validationFailureCounter.increment();
        logger.info("Recorded validation failure metric - type: {}, reason: {}", validationType, reason);
    }

    public void recordS3Failure() {
        s3FailureCounter.increment();
        logger.debug("Recorded S3 failure metric");
    }

    public void recordS3Failure(String operation, String reason) {
        s3FailureCounter.increment();
        logger.info("Recorded S3 failure metric - operation: {}, reason: {}", operation, reason);
    }

    public Timer.Sample startUploadTimer() {
        return Timer.start();
    }

    public void recordUploadDuration(Timer.Sample sample) {
        Duration duration = Duration.ofNanos(sample.stop(uploadTimer));
        logger.debug("Recorded upload duration: {} ms", duration.toMillis());
    }

    public void recordUploadDuration(long durationMs) {
        uploadTimer.record(durationMs, TimeUnit.MILLISECONDS);
        logger.debug("Recorded upload duration: {} ms", durationMs);
    }

    // Getter methods for testing and monitoring
    public double getSuccessCount() {
        return uploadSuccessCounter.count();
    }

    public double getFailureCount() {
        return uploadFailureCounter.count();
    }

    public double getValidationFailureCount() {
        return validationFailureCounter.count();
    }

    public double getS3FailureCount() {
        return s3FailureCounter.count();
    }

    public double getAverageUploadDuration() {
        return uploadTimer.mean(TimeUnit.MILLISECONDS);
    }

    /**
     * Records a successful upload with additional context for troubleshooting
     */
    public void recordUploadSuccess(String customerId, String fileName, long fileSize, String contentType) {
        uploadSuccessCounter.increment();
        logger.info(
                "Recorded successful upload - customer_id: {}, file_name: {}, file_size: {} bytes, content_type: {}",
                customerId, fileName, fileSize, contentType);
    }

    /**
     * Records upload metrics with full context for detailed analysis
     */
    public void recordUploadAttempt(String customerId, String fileName, long fileSize, String contentType,
            boolean success, String failureReason, long durationMs) {
        if (success) {
            uploadSuccessCounter.increment();
            logger.info(
                    "Upload successful - customer_id: {}, file_name: {}, file_size: {} bytes, content_type: {}, duration: {} ms",
                    customerId, fileName, fileSize, contentType, durationMs);
        } else {
            uploadFailureCounter.increment();
            logger.warn(
                    "Upload failed - customer_id: {}, file_name: {}, file_size: {} bytes, content_type: {}, duration: {} ms, reason: {}",
                    customerId, fileName, fileSize, contentType, durationMs, failureReason);
        }

        uploadTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Logs current metrics state for troubleshooting
     */
    public void logCurrentMetrics() {
        logger.info(
                "Current upload metrics - success: {}, failures: {}, validation_failures: {}, s3_failures: {}, avg_duration: {} ms",
                getSuccessCount(), getFailureCount(), getValidationFailureCount(), getS3FailureCount(),
                getAverageUploadDuration());
    }

    /**
     * Gets metrics summary for health checks
     */
    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();

        double successCount = getSuccessCount();
        double failureCount = getFailureCount();
        double totalUploads = successCount + failureCount;

        summary.put("total_uploads", totalUploads);
        summary.put("success_count", successCount);
        summary.put("failure_count", failureCount);
        summary.put("validation_failure_count", getValidationFailureCount());
        summary.put("s3_failure_count", getS3FailureCount());
        summary.put("average_duration_ms", getAverageUploadDuration());

        if (totalUploads > 0) {
            summary.put("success_rate", successCount / totalUploads);
            summary.put("failure_rate", failureCount / totalUploads);
        } else {
            summary.put("success_rate", 0.0);
            summary.put("failure_rate", 0.0);
        }

        return summary;
    }
}