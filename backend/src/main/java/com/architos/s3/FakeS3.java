package com.architos.s3;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FakeS3 implements S3Client {

    private static final Logger logger = LoggerFactory.getLogger(FakeS3.class);
    private static final String PATH = System.getProperty("user.home") + "/.architos/s3";

    @Override
    public String serviceName() {
        return "fake";
    }

    @Override
    public void close() {

    }

    @Override
    public PutObjectResponse putObject(PutObjectRequest putObjectRequest,
            RequestBody requestBody)
            throws AwsServiceException, SdkClientException {
        String bucket = putObjectRequest.bucket();
        String key = putObjectRequest.key();
        String fullPath = buildObjectFullPath(bucket, key);

        logger.info("FakeS3: Attempting to put object - bucket: {}, key: {}, path: {}", bucket, key, fullPath);

        try {
            InputStream inputStream = requestBody.contentStreamProvider().newStream();
            byte[] bytes = IOUtils.toByteArray(inputStream);

            // Ensure directory exists
            File file = new File(fullPath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                logger.debug("FakeS3: Created directory structure: {} - success: {}", parentDir.getPath(), created);
            }

            FileUtils.writeByteArrayToFile(file, bytes);
            logger.info("FakeS3: Successfully wrote object - bucket: {}, key: {}, size: {} bytes", bucket, key,
                    bytes.length);
            return PutObjectResponse.builder().build();

        } catch (IOException e) {
            logger.error("FakeS3: Failed to put object - bucket: {}, key: {}, error: {}", bucket, key, e.getMessage(),
                    e);
            throw new RuntimeException("Failed to write file to fake S3: " + e.getMessage(), e);
        }
    }

    @Override
    public ResponseInputStream<GetObjectResponse> getObject(
            GetObjectRequest getObjectRequest)
            throws AwsServiceException, SdkClientException {
        String bucket = getObjectRequest.bucket();
        String key = getObjectRequest.key();
        String fullPath = buildObjectFullPath(bucket, key);

        logger.info("FakeS3: Attempting to get object - bucket: {}, key: {}, path: {}", bucket, key, fullPath);

        try {
            File file = new File(fullPath);
            if (!file.exists()) {
                logger.warn("FakeS3: Object not found - bucket: {}, key: {}, path: {}", bucket, key, fullPath);
                throw NoSuchKeyException.builder()
                        .message("The specified key does not exist: " + key)
                        .build();
            }

            FileInputStream fileInputStream = new FileInputStream(file);
            logger.info("FakeS3: Successfully retrieved object - bucket: {}, key: {}, size: {} bytes",
                    bucket, key, file.length());
            return new ResponseInputStream<>(
                    GetObjectResponse.builder().build(),
                    fileInputStream);

        } catch (FileNotFoundException e) {
            logger.error("FakeS3: File not found - bucket: {}, key: {}, path: {}", bucket, key, fullPath);
            throw NoSuchKeyException.builder()
                    .message("The specified key does not exist: " + key)
                    .cause(e)
                    .build();
        }
    }

    @Override
    public HeadObjectResponse headObject(HeadObjectRequest headObjectRequest)
            throws AwsServiceException, SdkClientException {
        String bucket = headObjectRequest.bucket();
        String key = headObjectRequest.key();
        String fullPath = buildObjectFullPath(bucket, key);

        logger.debug("FakeS3: Checking if object exists - bucket: {}, key: {}, path: {}", bucket, key, fullPath);

        File file = new File(fullPath);
        if (!file.exists()) {
            logger.debug("FakeS3: Object does not exist - bucket: {}, key: {}", bucket, key);
            throw NoSuchKeyException.builder()
                    .message("The specified key does not exist: " + key)
                    .build();
        }

        logger.debug("FakeS3: Object exists - bucket: {}, key: {}, size: {} bytes", bucket, key, file.length());
        return HeadObjectResponse.builder()
                .contentLength(file.length())
                .build();
    }

    private String buildObjectFullPath(String bucketName, String key) {
        return PATH + "/" + bucketName + "/" + key;
    }
}
