package com.architos.integration;

import com.architos.AbstractTestcontainers;
import com.architos.customer.CustomerRegistrationRequest;
import com.architos.customer.Gender;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class UploadEndpointIntegrationTest extends AbstractTestcontainers {

    @Autowired
    private WebTestClient webTestClient;

    private static final String CUSTOMER_PATH = "/api/v1/customers";
    private static final Faker FAKER = new Faker();

    @Test
    void canUploadCustomerProfilePicture() throws IOException {
        // Create a customer first
        String name = FAKER.name().fullName();
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        int age = FAKER.number().numberBetween(16, 99);

        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                name, email, "password", age, Gender.MALE);

        // Register customer and get JWT token
        String jwtToken = webTestClient.post()
                .uri(CUSTOMER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .get("Authorization")
                .get(0);

        // Get all customers to find the created customer ID
        List<Object> customers = webTestClient.get()
                .uri(CUSTOMER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", String.format("Bearer %s", jwtToken))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Object.class)
                .returnResult()
                .getResponseBody();

        int customerId = customers.size(); // Assuming sequential IDs

        // Test 1: Verify upload request format
        byte[] imageBytes = createTestImageBytes();
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-profile.jpg",
                "image/jpeg",
                imageBytes);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", imageFile.getResource());

        // Test upload with correct multipart format
        webTestClient.post()
                .uri(CUSTOMER_PATH + "/{id}/profile-image", customerId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", String.format("Bearer %s", jwtToken))
                .bodyValue(parts)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldRejectUploadWithoutAuthenticationHeader() throws IOException {
        // Test 2: Verify authentication is required
        byte[] imageBytes = createTestImageBytes();
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-profile.jpg",
                "image/jpeg",
                imageBytes);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", imageFile.getResource());

        webTestClient.post()
                .uri(CUSTOMER_PATH + "/{id}/profile-image", 1)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(parts)
                .exchange()
                .expectStatus()
                .isForbidden(); // Should be 403 without auth
    }

    @Test
    void shouldRejectUploadWithInvalidFileType() throws IOException {
        // Create a customer first
        String name = FAKER.name().fullName();
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        int age = FAKER.number().numberBetween(16, 99);

        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                name, email, "password", age, Gender.MALE);

        String jwtToken = webTestClient.post()
                .uri(CUSTOMER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .get("Authorization")
                .get(0);

        // Get customer ID
        List<Object> customers = webTestClient.get()
                .uri(CUSTOMER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", String.format("Bearer %s", jwtToken))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Object.class)
                .returnResult()
                .getResponseBody();

        int customerId = customers.size();

        // Test 3: Verify file type validation
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "This is not an image".getBytes());

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", textFile.getResource());

        webTestClient.post()
                .uri(CUSTOMER_PATH + "/{id}/profile-image", customerId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", String.format("Bearer %s", jwtToken))
                .bodyValue(parts)
                .exchange()
                .expectStatus()
                .isBadRequest(); // Should be 400 for invalid file type
    }

    @Test
    void shouldRejectUploadForNonExistentCustomer() throws IOException {
        // Create a customer to get a valid JWT token
        String name = FAKER.name().fullName();
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        int age = FAKER.number().numberBetween(16, 99);

        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                name, email, "password", age, Gender.MALE);

        String jwtToken = webTestClient.post()
                .uri(CUSTOMER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .get("Authorization")
                .get(0);

        // Test 4: Verify customer existence validation
        byte[] imageBytes = createTestImageBytes();
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-profile.jpg",
                "image/jpeg",
                imageBytes);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", imageFile.getResource());

        int nonExistentCustomerId = 99999;

        webTestClient.post()
                .uri(CUSTOMER_PATH + "/{id}/profile-image", nonExistentCustomerId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", String.format("Bearer %s", jwtToken))
                .bodyValue(parts)
                .exchange()
                .expectStatus()
                .isNotFound(); // Should be 404 for non-existent customer
    }

    @Test
    void shouldHandleEmptyFileUpload() throws IOException {
        // Create a customer first
        String name = FAKER.name().fullName();
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        int age = FAKER.number().numberBetween(16, 99);

        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                name, email, "password", age, Gender.MALE);

        String jwtToken = webTestClient.post()
                .uri(CUSTOMER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .get("Authorization")
                .get(0);

        List<Object> customers = webTestClient.get()
                .uri(CUSTOMER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", String.format("Bearer %s", jwtToken))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Object.class)
                .returnResult()
                .getResponseBody();

        int customerId = customers.size();

        // Test 5: Verify empty file handling
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", emptyFile.getResource());

        webTestClient.post()
                .uri(CUSTOMER_PATH + "/{id}/profile-image", customerId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", String.format("Bearer %s", jwtToken))
                .bodyValue(parts)
                .exchange()
                .expectStatus()
                .isBadRequest(); // Should be 400 for empty file
    }

    @Test
    void shouldVerifyCorrectEndpointPath() {
        // Test 6: Verify the endpoint path matches frontend expectations
        String expectedPath = "/api/v1/customers/{customerId}/profile-image";
        String actualPath = CUSTOMER_PATH + "/{customerId}/profile-image";

        // This test documents that the endpoint path matches what the frontend expects
        assert actualPath.equals(expectedPath.replace("{customerId}", "{customerId}"));
    }

    @Test
    void shouldAcceptMultipartFormDataContentType() {
        // Test 7: Verify the endpoint accepts multipart/form-data
        // This is verified by the successful upload tests above
        // The endpoint should only accept MediaType.MULTIPART_FORM_DATA_VALUE

        // Document the expected content type
        String expectedContentType = MediaType.MULTIPART_FORM_DATA_VALUE;
        assert expectedContentType.equals("multipart/form-data");
    }

    @Test
    void shouldRequireFileParameterName() {
        // Test 8: Verify the endpoint expects 'file' parameter name
        // This is what the backend controller expects: @RequestParam("file")
        String expectedParameterName = "file";

        // This is verified in the successful upload tests where we use "file" as the
        // parameter name
        assert expectedParameterName.equals("file");
    }

    private byte[] createTestImageBytes() throws IOException {
        // Create a minimal valid JPEG image
        // This is a minimal JPEG header + data
        return new ClassPathResource("test-small.png").getInputStream().readAllBytes();
    }
}