package com.architos.journey;

import com.architos.customer.*;
import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class CustomerIT {

        @Autowired
        private WebTestClient webTestClient;

        private static final Random RANDOM = new Random();
        private static final String CUSTOMER_PATH = "/api/v1/customers";

        @Test
        void canRegisterCustomer() {
                // create registration request
                Faker faker = new Faker();
                Name fakerName = faker.name();

                String name = fakerName.fullName();
                String email = fakerName.lastName() + "-" + UUID.randomUUID() + "@architos.com";
                int age = RANDOM.nextInt(1, 100);

                Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;

                CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                                name, email, "password", age, gender);
                // send a post request
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

                // get all customers
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

                int id = allCustomers.stream()
                                .filter(customer -> customer.email().equals(email))
                                .map(CustomerDTO::id)
                                .findFirst()
                                .orElseThrow();

                // make sure that customer is present
                CustomerDTO expectedCustomer = new CustomerDTO(
                                id,
                                name,
                                email,
                                gender,
                                age,
                                List.of("ROLE_USER"),
                                email,
                                null);

                assertThat(allCustomers).contains(expectedCustomer);

                // get customer by id
                webTestClient.get()
                                .uri(CUSTOMER_PATH + "/{id}", id)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .expectBody(new ParameterizedTypeReference<CustomerDTO>() {
                                })
                                .isEqualTo(expectedCustomer);
        }

        @Test
        void canDeleteCustomer() {
                // create registration request
                Faker faker = new Faker();
                Name fakerName = faker.name();

                String name = fakerName.fullName();
                String email = fakerName.lastName() + "-" + UUID.randomUUID() + "@architos.com";
                int age = RANDOM.nextInt(1, 100);

                Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;

                CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                                name, email, "password", age, gender);

                CustomerRegistrationRequest request2 = new CustomerRegistrationRequest(
                                name, email + ".uk", "password", age, gender);

                // send a post request to create customer 1
                webTestClient.post()
                                .uri(CUSTOMER_PATH)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(request), CustomerRegistrationRequest.class)
                                .exchange()
                                .expectStatus()
                                .isOk();

                // send a post request to create customer 2
                String jwtToken = webTestClient.post()
                                .uri(CUSTOMER_PATH)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(request2), CustomerRegistrationRequest.class)
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .returnResult(Void.class)
                                .getResponseHeaders()
                                .get(AUTHORIZATION)
                                .get(0);

                // get all customers
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

                int id = allCustomers.stream()
                                .filter(customer -> customer.email().equals(email))
                                .map(CustomerDTO::id)
                                .findFirst()
                                .orElseThrow();

                // customer 2 deletes customer 1
                webTestClient.delete()
                                .uri(CUSTOMER_PATH + "/{id}", id)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .accept(MediaType.APPLICATION_JSON)
                                .exchange()
                                .expectStatus()
                                .isOk();

                // customer 2 gets customer 1 by id
                webTestClient.get()
                                .uri(CUSTOMER_PATH + "/{id}", id)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .exchange()
                                .expectStatus()
                                .isNotFound();
        }

        @Test
        void canUpdateCustomer() {
                // create registration request
                Faker faker = new Faker();
                Name fakerName = faker.name();

                String name = fakerName.fullName();
                String email = fakerName.lastName() + "-" + UUID.randomUUID() + "@architos.com";
                int age = RANDOM.nextInt(1, 100);

                Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;

                CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                                name, email, "password", age, gender);

                // send a post request
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

                // get all customers
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

                int id = allCustomers.stream()
                                .filter(customer -> customer.email().equals(email))
                                .map(CustomerDTO::id)
                                .findFirst()
                                .orElseThrow();

                // update customer

                String newName = "Ali";

                CustomerUpdateRequest updateRequest = new CustomerUpdateRequest(
                                newName, null, null);

                webTestClient.put()
                                .uri(CUSTOMER_PATH + "/{id}", id)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(updateRequest), CustomerUpdateRequest.class)
                                .exchange()
                                .expectStatus()
                                .isOk();

                // get customer by id
                CustomerDTO updatedCustomer = webTestClient.get()
                                .uri(CUSTOMER_PATH + "/{id}", id)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .expectBody(CustomerDTO.class)
                                .returnResult()
                                .getResponseBody();

                CustomerDTO expected = new CustomerDTO(
                                id, newName, email, gender, age, List.of("ROLE_USER"), email, null);

                assertThat(updatedCustomer).isEqualTo(expected);
        }

        @Test
        void canUploadAndRetrieveCustomerProfileImage() throws IOException {
                // create registration request
                Faker faker = new Faker();
                Name fakerName = faker.name();

                String name = fakerName.fullName();
                String email = fakerName.lastName() + "-" + UUID.randomUUID() + "@architos.com";
                int age = RANDOM.nextInt(1, 100);

                Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;

                CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                                name, email, "password", age, gender);

                // send a post request
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

                // get all customers
                List<CustomerDTO> allCustomers = webTestClient.get()
                                .uri(CUSTOMER_PATH)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .exchange()
                                .expectStatus()
                                .is2xxSuccessful()
                                .expectBodyList(new ParameterizedTypeReference<CustomerDTO>() {
                                })
                                .returnResult()
                                .getResponseBody();

                int id = allCustomers.stream()
                                .filter(customer -> customer.email().equals(email))
                                .map(CustomerDTO::id)
                                .findFirst()
                                .orElseThrow();

                // upload profile image
                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("file", new ClassPathResource("female.jpeg"));

                webTestClient.post()
                                .uri(CUSTOMER_PATH + "/{id}/profile-image", id)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(builder.build()))
                                .exchange()
                                .expectStatus()
                                .is2xxSuccessful();

                // retrieve profile image
                byte[] imageData = webTestClient.get()
                                .uri(CUSTOMER_PATH + "/{id}/profile-image", id)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
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
        }

        @Test
        void shouldReturnBadRequestForInvalidFileType() throws IOException {
                // create registration request
                Faker faker = new Faker();
                Name fakerName = faker.name();

                String name = fakerName.fullName();
                String email = fakerName.lastName() + "-" + UUID.randomUUID() + "@architos.com";
                int age = RANDOM.nextInt(1, 100);

                Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;

                CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                                name, email, "password", age, gender);

                // send a post request
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

                // get all customers
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

                int id = allCustomers.stream()
                                .filter(customer -> customer.email().equals(email))
                                .map(CustomerDTO::id)
                                .findFirst()
                                .orElseThrow();

                // try to upload invalid file type (text file)
                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("file", "This is not an image").filename("test.txt");

                webTestClient.post()
                                .uri(CUSTOMER_PATH + "/{id}/profile-image", id)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(builder.build()))
                                .exchange()
                                .expectStatus()
                                .isBadRequest();
        }

        @Test
        void shouldReturnNotFoundForNonExistentCustomerProfileImage() {
                // create registration request
                Faker faker = new Faker();
                Name fakerName = faker.name();

                String name = fakerName.fullName();
                String email = fakerName.lastName() + "-" + UUID.randomUUID() + "@architos.com";
                int age = RANDOM.nextInt(1, 100);

                Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;

                CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                                name, email, "password", age, gender);

                // send a post request
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

                // get all customers
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

                int id = allCustomers.stream()
                                .filter(customer -> customer.email().equals(email))
                                .map(CustomerDTO::id)
                                .findFirst()
                                .orElseThrow();

                // try to retrieve profile image that doesn't exist
                webTestClient.get()
                                .uri(CUSTOMER_PATH + "/{id}/profile-image", id)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .exchange()
                                .expectStatus()
                                .isNotFound();
        }

        @Test
        void shouldReturnNotFoundForUploadToNonExistentCustomer() throws IOException {
                // create a customer to get a valid JWT token
                Faker faker = new Faker();
                Name fakerName = faker.name();

                String name = fakerName.fullName();
                String email = fakerName.lastName() + "-" + UUID.randomUUID() + "@architos.com";
                int age = RANDOM.nextInt(1, 100);

                Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;

                CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                                name, email, "password", age, gender);

                // send a post request
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

                // try to upload to non-existent customer ID
                int nonExistentId = 999999;

                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("file", new ClassPathResource("female.jpeg"));

                webTestClient.post()
                                .uri(CUSTOMER_PATH + "/{id}/profile-image", nonExistentId)
                                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(builder.build()))
                                .exchange()
                                .expectStatus()
                                .isNotFound();
        }
}
