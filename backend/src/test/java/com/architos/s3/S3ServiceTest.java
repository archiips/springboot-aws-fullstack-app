package com.architos.s3;

import com.architos.exception.S3ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

        @Mock
        private S3Client s3Client;
        private S3Service underTest;

        @BeforeEach
        void setUp() {
                underTest = new S3Service(s3Client);
        }

        @Test
        void canPutObject() throws IOException {
                // Given
                String bucket = "customer";
                String key = "foo";
                byte[] data = "Hello World".getBytes();

                // When
                underTest.putObject(bucket, key, data);

                // Then
                ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor
                                .forClass(PutObjectRequest.class);

                ArgumentCaptor<RequestBody> requestBodyArgumentCaptor = ArgumentCaptor.forClass(RequestBody.class);

                verify(s3Client).putObject(
                                putObjectRequestArgumentCaptor.capture(),
                                requestBodyArgumentCaptor.capture());

                PutObjectRequest putObjectRequestArgumentCaptorValue = putObjectRequestArgumentCaptor.getValue();

                assertThat(putObjectRequestArgumentCaptorValue.bucket()).isEqualTo(bucket);
                assertThat(putObjectRequestArgumentCaptorValue.key()).isEqualTo(key);

                RequestBody requestBodyArgumentCaptorValue = requestBodyArgumentCaptor.getValue();

                assertThat(
                                requestBodyArgumentCaptorValue.contentStreamProvider().newStream().readAllBytes())
                                .isEqualTo(
                                                RequestBody.fromBytes(data).contentStreamProvider().newStream()
                                                                .readAllBytes());
        }

        @Test
        void canGetObject() throws IOException {
                // Given
                String bucket = "customer";
                String key = "foo";
                byte[] data = "Hello World".getBytes();

                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build();

                ResponseInputStream<GetObjectResponse> res = new ResponseInputStream<>(
                                GetObjectResponse.builder().build(),
                                new ByteArrayInputStream(data));

                when(s3Client.getObject(eq(getObjectRequest))).thenReturn(res);

                // When
                byte[] bytes = underTest.getObject(bucket, key);

                // Then
                assertThat(bytes).isEqualTo(data);
        }

        @Test
        void willThrowS3ServiceExceptionWhenGetObjectIOException() throws IOException {
                // Given
                String bucket = "customer";
                String key = "foo";

                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build();

                // Simulate IOException when reading
                ResponseInputStream<GetObjectResponse> res = new ResponseInputStream<>(
                                GetObjectResponse.builder().build(),
                                new java.io.InputStream() {
                                        @Override
                                        public int read() throws IOException {
                                                throw new IOException("Cannot read bytes");
                                        }
                                });

                when(s3Client.getObject(eq(getObjectRequest))).thenReturn(res);

                // When / Then
                assertThatThrownBy(() -> underTest.getObject(bucket, key))
                                .isInstanceOf(S3ServiceException.class)
                                .hasMessageContaining("Failed to read file data from S3")
                                .hasRootCauseInstanceOf(IOException.class);
        }

        @Test
        void willThrowS3ServiceExceptionWhenPutObjectAwsServiceException() {
                // Given
                String bucket = "customer";
                String key = "foo";
                byte[] data = "Hello World".getBytes();

                AwsServiceException awsException = S3Exception.builder()
                                .message("Access Denied")
                                .statusCode(403)
                                .build();

                when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                                .thenThrow(awsException);

                // When / Then
                assertThatThrownBy(() -> underTest.putObject(bucket, key, data))
                                .isInstanceOf(S3ServiceException.class)
                                .hasMessageContaining("Failed to upload file to S3")
                                .hasCause(awsException);
        }

        @Test
        void willThrowS3ServiceExceptionWhenPutObjectSdkClientException() {
                // Given
                String bucket = "customer";
                String key = "foo";
                byte[] data = "Hello World".getBytes();

                SdkClientException clientException = SdkClientException.builder()
                                .message("Unable to connect to S3")
                                .build();

                when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                                .thenThrow(clientException);

                // When / Then
                assertThatThrownBy(() -> underTest.putObject(bucket, key, data))
                                .isInstanceOf(S3ServiceException.class)
                                .hasMessageContaining("S3 client error during upload")
                                .hasCause(clientException);
        }

        @Test
        void willThrowS3ServiceExceptionWhenGetObjectNoSuchKey() {
                // Given
                String bucket = "customer";
                String key = "nonexistent";

                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build();

                NoSuchKeyException noSuchKeyException = NoSuchKeyException.builder()
                                .message("The specified key does not exist")
                                .build();

                when(s3Client.getObject(eq(getObjectRequest))).thenThrow(noSuchKeyException);

                // When / Then
                assertThatThrownBy(() -> underTest.getObject(bucket, key))
                                .isInstanceOf(S3ServiceException.class)
                                .hasMessageContaining("File not found in S3")
                                .hasCause(noSuchKeyException);
        }

        @Test
        void willThrowS3ServiceExceptionWhenGetObjectAwsServiceException() {
                // Given
                String bucket = "customer";
                String key = "foo";

                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build();

                AwsServiceException awsException = S3Exception.builder()
                                .message("Internal Server Error")
                                .statusCode(500)
                                .build();

                when(s3Client.getObject(eq(getObjectRequest))).thenThrow(awsException);

                // When / Then
                assertThatThrownBy(() -> underTest.getObject(bucket, key))
                                .isInstanceOf(S3ServiceException.class)
                                .hasMessageContaining("Failed to retrieve file from S3")
                                .hasCause(awsException);
        }

        @Test
        void canCheckObjectExists() {
                // Given
                String bucket = "customer";
                String key = "foo";

                HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build();

                HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                                .contentLength(100L)
                                .build();

                when(s3Client.headObject(eq(headObjectRequest))).thenReturn(headObjectResponse);

                // When
                boolean exists = underTest.objectExists(bucket, key);

                // Then
                assertThat(exists).isTrue();
                verify(s3Client).headObject(eq(headObjectRequest));
        }

        @Test
        void willReturnFalseWhenObjectDoesNotExist() {
                // Given
                String bucket = "customer";
                String key = "nonexistent";

                HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build();

                NoSuchKeyException noSuchKeyException = NoSuchKeyException.builder()
                                .message("The specified key does not exist")
                                .build();

                when(s3Client.headObject(eq(headObjectRequest))).thenThrow(noSuchKeyException);

                // When
                boolean exists = underTest.objectExists(bucket, key);

                // Then
                assertThat(exists).isFalse();
        }

        @Test
        void willThrowS3ServiceExceptionWhenObjectExistsAwsServiceException() {
                // Given
                String bucket = "customer";
                String key = "foo";

                HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build();

                AwsServiceException awsException = S3Exception.builder()
                                .message("Access Denied")
                                .statusCode(403)
                                .build();

                when(s3Client.headObject(eq(headObjectRequest))).thenThrow(awsException);

                // When / Then
                assertThatThrownBy(() -> underTest.objectExists(bucket, key))
                                .isInstanceOf(S3ServiceException.class)
                                .hasMessageContaining("Failed to check file existence in S3")
                                .hasCause(awsException);
        }

        @Test
        void willThrowS3ServiceExceptionWhenObjectExistsSdkClientException() {
                // Given
                String bucket = "customer";
                String key = "foo";

                HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build();

                SdkClientException clientException = SdkClientException.builder()
                                .message("Unable to connect to S3")
                                .build();

                when(s3Client.headObject(eq(headObjectRequest))).thenThrow(clientException);

                // When / Then
                assertThatThrownBy(() -> underTest.objectExists(bucket, key))
                                .isInstanceOf(S3ServiceException.class)
                                .hasMessageContaining("S3 client error during existence check")
                                .hasCause(clientException);
        }
}