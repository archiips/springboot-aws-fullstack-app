package com.architos.s3;

import com.architos.exception.S3ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    private final S3Client s3;

    public S3Service(S3Client s3Client) {
        this.s3 = s3Client;
    }

    public void putObject(String bucketName, String key, byte[] file) {
        // Add structured logging context for S3 operations
        MDC.put("s3_operation", "put_object");
        MDC.put("s3_bucket", bucketName);
        MDC.put("s3_key", key);
        MDC.put("s3_file_size", String.valueOf(file.length));

        long startTime = System.currentTimeMillis();

        try {
            logger.info("Attempting to upload object to S3 - bucket: {}, key: {}, size: {} bytes",
                    bucketName, key, file.length);

            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3.putObject(objectRequest, RequestBody.fromBytes(file));

            long duration = System.currentTimeMillis() - startTime;
            MDC.put("s3_duration_ms", String.valueOf(duration));

            logger.info("Successfully uploaded object to S3 - bucket: {}, key: {}, duration: {} ms",
                    bucketName, key, duration);

        } catch (AwsServiceException e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("s3_duration_ms", String.valueOf(duration));
            MDC.put("s3_error_code", e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "unknown");

            logger.error(
                    "AWS service error while uploading object to S3 - bucket: {}, key: {}, error_code: {}, error: {}, duration: {} ms",
                    bucketName, key,
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "unknown",
                    e.getMessage(), duration, e);

            String errorMessage = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            throw new S3ServiceException("Failed to upload file to S3: " + errorMessage, e);

        } catch (SdkClientException e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("s3_duration_ms", String.valueOf(duration));

            logger.error(
                    "SDK client error while uploading object to S3 - bucket: {}, key: {}, error: {}, duration: {} ms",
                    bucketName, key, e.getMessage(), duration, e);
            throw new S3ServiceException("S3 client error during upload: " + e.getMessage(), e);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("s3_duration_ms", String.valueOf(duration));

            logger.error(
                    "Unexpected error while uploading object to S3 - bucket: {}, key: {}, error: {}, duration: {} ms",
                    bucketName, key, e.getMessage(), duration, e);
            throw new S3ServiceException("Unexpected error during S3 upload: " + e.getMessage(), e);
        } finally {
            // Clean up MDC
            MDC.remove("s3_operation");
            MDC.remove("s3_bucket");
            MDC.remove("s3_key");
            MDC.remove("s3_file_size");
            MDC.remove("s3_duration_ms");
            MDC.remove("s3_error_code");
        }
    }

    public byte[] getObject(String bucketName, String key) {
        // Add structured logging context for S3 operations
        MDC.put("s3_operation", "get_object");
        MDC.put("s3_bucket", bucketName);
        MDC.put("s3_key", key);

        long startTime = System.currentTimeMillis();

        try {
            logger.info("Attempting to retrieve object from S3 - bucket: {}, key: {}", bucketName, key);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3.getObject(getObjectRequest);
            byte[] data = response.readAllBytes();

            long duration = System.currentTimeMillis() - startTime;
            MDC.put("s3_duration_ms", String.valueOf(duration));
            MDC.put("s3_file_size", String.valueOf(data.length));

            logger.info("Successfully retrieved object from S3 - bucket: {}, key: {}, size: {} bytes, duration: {} ms",
                    bucketName, key, data.length, duration);
            return data;

        } catch (NoSuchKeyException e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("s3_duration_ms", String.valueOf(duration));
            MDC.put("s3_error_code", "NoSuchKey");

            logger.warn("Object not found in S3 - bucket: {}, key: {}, duration: {} ms", bucketName, key, duration);
            throw new S3ServiceException("File not found in S3: " + key, e);

        } catch (AwsServiceException e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("s3_duration_ms", String.valueOf(duration));
            MDC.put("s3_error_code", e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "unknown");

            logger.error(
                    "AWS service error while retrieving object from S3 - bucket: {}, key: {}, error_code: {}, error: {}, duration: {} ms",
                    bucketName, key,
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "unknown",
                    e.getMessage(), duration, e);

            String errorMessage = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            throw new S3ServiceException("Failed to retrieve file from S3: " + errorMessage, e);

        } catch (SdkClientException e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("s3_duration_ms", String.valueOf(duration));

            logger.error(
                    "SDK client error while retrieving object from S3 - bucket: {}, key: {}, error: {}, duration: {} ms",
                    bucketName, key, e.getMessage(), duration, e);
            throw new S3ServiceException("S3 client error during retrieval: " + e.getMessage(), e);

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("s3_duration_ms", String.valueOf(duration));

            logger.error("IO error while reading object from S3 - bucket: {}, key: {}, error: {}, duration: {} ms",
                    bucketName, key, e.getMessage(), duration, e);
            throw new S3ServiceException("Failed to read file data from S3: " + e.getMessage(), e);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("s3_duration_ms", String.valueOf(duration));

            logger.error(
                    "Unexpected error while retrieving object from S3 - bucket: {}, key: {}, error: {}, duration: {} ms",
                    bucketName, key, e.getMessage(), duration, e);
            throw new S3ServiceException("Unexpected error during S3 retrieval: " + e.getMessage(), e);
        } finally {
            // Clean up MDC
            MDC.remove("s3_operation");
            MDC.remove("s3_bucket");
            MDC.remove("s3_key");
            MDC.remove("s3_file_size");
            MDC.remove("s3_duration_ms");
            MDC.remove("s3_error_code");
        }
    }

    public boolean objectExists(String bucketName, String key) {
        logger.debug("Checking if object exists in S3 - bucket: {}, key: {}", bucketName, key);

        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3.headObject(headObjectRequest);
            logger.debug("Object exists in S3 - bucket: {}, key: {}", bucketName, key);
            return true;

        } catch (NoSuchKeyException e) {
            logger.debug("Object does not exist in S3 - bucket: {}, key: {}", bucketName, key);
            return false;

        } catch (AwsServiceException e) {
            logger.error("AWS service error while checking object existence in S3 - bucket: {}, key: {}, error: {}",
                    bucketName, key, e.getMessage(), e);
            String errorMessage = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            throw new S3ServiceException("Failed to check file existence in S3: " + errorMessage, e);

        } catch (SdkClientException e) {
            logger.error("SDK client error while checking object existence in S3 - bucket: {}, key: {}, error: {}",
                    bucketName, key, e.getMessage(), e);
            throw new S3ServiceException("S3 client error during existence check: " + e.getMessage(), e);

        } catch (Exception e) {
            logger.error("Unexpected error while checking object existence in S3 - bucket: {}, key: {}, error: {}",
                    bucketName, key, e.getMessage(), e);
            throw new S3ServiceException("Unexpected error during S3 existence check: " + e.getMessage(), e);
        }
    }
}
