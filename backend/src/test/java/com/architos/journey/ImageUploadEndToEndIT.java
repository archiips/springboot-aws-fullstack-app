package com.architos.journey;

import com.architos.customer.*;
import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@DisplayName("Image Upload End-to-End Integration Tests")
public class ImageUploadEndToEndIT {

        @Autowired
        private WebTestClient webTestClient;

        private static final Random RANDOM = new Random();
        private static final String CUSTOMER_PATH = "/api/v1/customers";

        @Nested
        @DisplayName("Complete Upload Flow Tests")
        class CompleteUploadFlowTests {

                @Test
                @DisplayName("Should successfully upload and retrieve JPEG image")
                void shouldUploadAndRetrieveJpegImage() throws IOException {
                        // Given - Create a customer
                        CustomerTestData testData = createTestCustomer();

                        // When - Upload JPEG image
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("female.jpeg"));

                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful();

                        // Then - Verify image can be retrieved
                        byte[] imageData = webTestClient.get()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful()
                                        .expectHeader()
                                        .contentType(MediaType.IMAGE_JPEG)
                                        .expectBody(byte[].class)
                                        .returnResult()
                                        .getResponseBody();

                        assertThat(imageData).isNotNull();
                        assertThat(imageData.length).isGreaterThan(0);

                        // Verify customer profile shows image is available
                        CustomerDTO customer = getCustomerById(testData.customerId(), testData.jwtToken());
                        assertThat(customer.profileImageId()).isNotNull();
                }

                @Test
                @DisplayName("Should successfully upload and retrieve PNG image")
                void shouldUploadAndRetrievePngImage() throws IOException {
                        // Given - Create a customer
                        CustomerTestData testData = createTestCustomer();

                        // When - Upload PNG image (using small test PNG)
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("test-small.png"));

                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful();

                        // Then - Verify image can be retrieved
                        byte[] imageData = webTestClient.get()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful()
                                        .expectBody(byte[].class)
                                        .returnResult()
                                        .getResponseBody();

                        assertThat(imageData).isNotNull();
                        assertThat(imageData.length).isGreaterThan(0);
                }

                @Test
                @DisplayName("Should handle image replacement correctly")
                void shouldHandleImageReplacement() throws IOException {
                        // Given - Create a customer and upload first image
                        CustomerTestData testData = createTestCustomer();

                        MultipartBodyBuilder builder1 = new MultipartBodyBuilder();
                        builder1.part("file", new ClassPathResource("female.jpeg"));

                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder1.build()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful();

                        // When - Upload second image to replace the first
                        MultipartBodyBuilder builder2 = new MultipartBodyBuilder();
                        builder2.part("file", new ClassPathResource("male.jpeg"));

                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder2.build()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful();

                        // Then - Verify new image is retrieved
                        byte[] imageData = webTestClient.get()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful()
                                        .expectBody(byte[].class)
                                        .returnResult()
                                        .getResponseBody();

                        assertThat(imageData).isNotNull();
                        assertThat(imageData.length).isGreaterThan(0);
                }
        }

        @Nested
        @DisplayName("Error Scenario Tests")
        class ErrorScenarioTests {

                @Test
                @DisplayName("Should return 400 Bad Request for invalid file type")
                void shouldReturnBadRequestForInvalidFileType() throws IOException {
                        // Given - Create a customer
                        CustomerTestData testData = createTestCustomer();

                        // When - Try to upload invalid file type (text file)
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("test-invalid.txt"));

                        // Then - Should return 400 Bad Request
                        String errorResponse = webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .isBadRequest()
                                        .expectBody(String.class)
                                        .returnResult()
                                        .getResponseBody();

                        assertThat(errorResponse).contains("Invalid file type");
                }

                @Test
                @DisplayName("Should return 400 Bad Request for file with wrong extension")
                void shouldReturnBadRequestForWrongExtension() throws IOException {
                        // Given - Create a customer
                        CustomerTestData testData = createTestCustomer();

                        // When - Try to upload text file with image extension
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", "This is not an image").filename("fake-image.jpg");

                        // Then - Should return 400 Bad Request
                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .isBadRequest();
                }

                @Test
                @DisplayName("Should return 404 Not Found for non-existent customer")
                void shouldReturnNotFoundForNonExistentCustomer() throws IOException {
                        // Given - Create a customer to get valid JWT token
                        CustomerTestData testData = createTestCustomer();
                        int nonExistentId = 999999;

                        // When - Try to upload to non-existent customer
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("female.jpeg"));

                        // Then - Should return 404 Not Found
                        String errorResponse = webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", nonExistentId)
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .isNotFound()
                                        .expectBody(String.class)
                                        .returnResult()
                                        .getResponseBody();

                        assertThat(errorResponse).contains("customer with id [" + nonExistentId + "] not found");
                }

                @Test
                @DisplayName("Should return 404 Not Found when retrieving non-existent image")
                void shouldReturnNotFoundForNonExistentImage() {
                        // Given - Create a customer without uploading image
                        CustomerTestData testData = createTestCustomer();

                        // When - Try to retrieve profile image that doesn't exist
                        // Then - Should return 404 Not Found
                        webTestClient.get()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .exchange()
                                        .expectStatus()
                                        .isNotFound();
                }

                @Test
                @DisplayName("Should return 403 Forbidden without authentication")
                void shouldReturnForbiddenWithoutAuth() throws IOException {
                        // Given - Create a customer
                        CustomerTestData testData = createTestCustomer();

                        // When - Try to upload without authentication
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("female.jpeg"));

                        // Then - Should return 403 Forbidden
                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .isForbidden();
                }

                @Test
                @DisplayName("Should return 403 Forbidden with invalid token")
                void shouldReturnForbiddenWithInvalidToken() throws IOException {
                        // Given - Create a customer
                        CustomerTestData testData = createTestCustomer();

                        // When - Try to upload with invalid token
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("female.jpeg"));

                        // Then - Should return 403 Forbidden
                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, "Bearer invalid-token")
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .isForbidden();
                }
        }

        @Nested
        @DisplayName("File Size and Validation Tests")
        class FileSizeAndValidationTests {

                @Test
                @DisplayName("Should accept files within size limit")
                void shouldAcceptFilesWithinSizeLimit() throws IOException {
                        // Given - Create a customer
                        CustomerTestData testData = createTestCustomer();

                        // When - Upload file within size limit (using existing test images)
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("female.jpeg"));

                        // Then - Should succeed
                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful();
                }

                @Test
                @DisplayName("Should validate empty file upload")
                void shouldValidateEmptyFileUpload() throws IOException {
                        // Given - Create a customer
                        CustomerTestData testData = createTestCustomer();

                        // When - Try to upload empty file
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", "").filename("empty.jpg");

                        // Then - Should return 400 Bad Request
                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .isBadRequest();
                }

                @Test
                @DisplayName("Should validate missing file parameter")
                void shouldValidateMissingFileParameter() throws IOException {
                        // Given - Create a customer
                        CustomerTestData testData = createTestCustomer();

                        // When - Try to upload without file parameter
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        // Not adding any file part

                        // Then - Should return 500 Internal Server Error (Spring's default for missing
                        // required parameter)
                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .is5xxServerError();
                }
        }

        @Nested
        @DisplayName("Error Message Verification Tests")
        class ErrorMessageVerificationTests {

                @Test
                @DisplayName("Should provide clear error message for invalid file type")
                void shouldProvidesClearErrorMessageForInvalidFileType() throws IOException {
                        // Given - Create a customer
                        CustomerTestData testData = createTestCustomer();

                        // When - Upload invalid file type
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("test-invalid.txt"));

                        // Then - Should return specific error message
                        String errorResponse = webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .isBadRequest()
                                        .expectBody(String.class)
                                        .returnResult()
                                        .getResponseBody();

                        assertThat(errorResponse)
                                        .contains("Invalid file type")
                                        .containsAnyOf("image/jpeg", "image/png", "image/gif", "image/webp");
                }

                @Test
                @DisplayName("Should provide clear error message for customer not found")
                void shouldProvideClearErrorMessageForCustomerNotFound() throws IOException {
                        // Given - Create a customer to get valid JWT token
                        CustomerTestData testData = createTestCustomer();
                        int nonExistentId = 999999;

                        // When - Upload to non-existent customer
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("female.jpeg"));

                        // Then - Should return specific error message
                        String errorResponse = webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", nonExistentId)
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .isNotFound()
                                        .expectBody(String.class)
                                        .returnResult()
                                        .getResponseBody();

                        assertThat(errorResponse)
                                        .contains("customer with id [" + nonExistentId + "] not found");
                }
        }

        @Nested
        @DisplayName("Image Display After Upload Tests")
        class ImageDisplayAfterUploadTests {

                @Test
                @DisplayName("Should update customer profile with image ID after successful upload")
                void shouldUpdateCustomerProfileWithImageId() throws IOException {
                        // Given - Create a customer
                        CustomerTestData testData = createTestCustomer();

                        // Verify initially no profile image
                        CustomerDTO customerBefore = getCustomerById(testData.customerId(), testData.jwtToken());
                        assertThat(customerBefore.profileImageId()).isNull();

                        // When - Upload image
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("female.jpeg"));

                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful();

                        // Then - Verify customer profile is updated with image ID
                        CustomerDTO customerAfter = getCustomerById(testData.customerId(), testData.jwtToken());
                        assertThat(customerAfter.profileImageId()).isNotNull();
                        assertThat(customerAfter.profileImageId()).isNotEmpty();
                }

                @Test
                @DisplayName("Should serve image with correct content type")
                void shouldServeImageWithCorrectContentType() throws IOException {
                        // Given - Create a customer and upload JPEG image
                        CustomerTestData testData = createTestCustomer();

                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("female.jpeg"));

                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful();

                        // When & Then - Retrieve image and verify content type
                        webTestClient.get()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful()
                                        .expectHeader()
                                        .contentType(MediaType.IMAGE_JPEG);
                }

                @Test
                @DisplayName("Should serve image with correct content length")
                void shouldServeImageWithCorrectContentLength() throws IOException {
                        // Given - Create a customer and upload image
                        CustomerTestData testData = createTestCustomer();

                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                        builder.part("file", new ClassPathResource("female.jpeg"));

                        webTestClient.post()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful();

                        // When & Then - Retrieve image and verify content length
                        byte[] imageData = webTestClient.get()
                                        .uri(CUSTOMER_PATH + "/{id}/profile-image", testData.customerId())
                                        .header(AUTHORIZATION, String.format("Bearer %s", testData.jwtToken()))
                                        .exchange()
                                        .expectStatus()
                                        .is2xxSuccessful()
                                        .expectHeader()
                                        .exists("Content-Length")
                                        .expectBody(byte[].class)
                                        .returnResult()
                                        .getResponseBody();

                        assertThat(imageData).isNotNull();
                        assertThat(imageData.length).isGreaterThan(0);
                }
        }

        // Helper methods
        private CustomerTestData createTestCustomer() {
                Faker faker = new Faker();
                Name fakerName = faker.name();

                String name = fakerName.fullName();
                String email = fakerName.lastName() + "-" + UUID.randomUUID() + "@architos.com";
                int age = RANDOM.nextInt(1, 100);
                Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;

                CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                                name, email, "password", age, gender);

                String jwtToken = webTestClient.post()
                                .uri(CUSTOMER_PATH)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(request), CustomerRegistrationRequest.class)
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .returnResult(Void.class)
                                .getResponseHeaders()
                                .get(AUTHORIZATION)
                                .get(0);

                List<CustomerDTO> allCustomers = webTestClient.get()
                                .uri(CUSTOMER_PATH)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .expectBodyList(new ParameterizedTypeReference<CustomerDTO>() {
                                })
                                .returnResult()
                                .getResponseBody();

                int customerId = allCustomers.stream()
                                .filter(customer -> customer.email().equals(email))
                                .map(CustomerDTO::id)
                                .findFirst()
                                .orElseThrow();

                return new CustomerTestData(customerId, jwtToken, name, email, age, gender);
        }

        private CustomerDTO getCustomerById(int customerId, String jwtToken) {
                return webTestClient.get()
                                .uri(CUSTOMER_PATH + "/{id}", customerId)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .expectBody(CustomerDTO.class)
                                .returnResult()
                                .getResponseBody();
        }

        private record CustomerTestData(
                        int customerId,
                        String jwtToken,
                        String name,
                        String email,
                        int age,
                        Gender gender) {
        }
}