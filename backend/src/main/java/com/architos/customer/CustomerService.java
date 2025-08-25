package com.architos.customer;

import com.architos.exception.DuplicateResourceException;
import com.architos.exception.FileSizeExceededException;
import com.architos.exception.InvalidFileTypeException;
import com.architos.exception.RequestValidationException;
import com.architos.exception.ResourceNotFoundException;
import com.architos.exception.S3ServiceException;
import com.architos.file.FileValidationService;
import com.architos.metrics.FileUploadMetricsService;
import com.architos.s3.S3Buckets;
import com.architos.s3.S3Service;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerDao customerDao;
    private final Function<Customer, CustomerDTO> customerDTOMapper;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final S3Buckets s3Buckets;
    private final FileValidationService fileValidationService;
    private final FileUploadMetricsService metricsService;

    public CustomerService(@Qualifier("jdbc") CustomerDao customerDao,
            CustomerDTOMapper customerDTOMapper,
            PasswordEncoder passwordEncoder,
            S3Service s3Service,
            S3Buckets s3Buckets,
            FileValidationService fileValidationService,
            FileUploadMetricsService metricsService) {
        this.customerDao = customerDao;
        this.customerDTOMapper = customerDTOMapper;
        this.passwordEncoder = passwordEncoder;
        this.s3Service = s3Service;
        this.s3Buckets = s3Buckets;
        this.fileValidationService = fileValidationService;
        this.metricsService = metricsService;
    }

    public List<CustomerDTO> getAllCustomers() {
        return customerDao.selectAllCustomers()
                .stream()
                .map(customerDTOMapper)
                .collect(Collectors.toList());
    }

    public CustomerDTO getCustomer(Integer id) {
        return customerDao.selectCustomerById(id)
                .map(customerDTOMapper)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "customer with id [%s] not found".formatted(id)));
    }

    public void addCustomer(CustomerRegistrationRequest customerRegistrationRequest) {
        // check if email exists
        String email = customerRegistrationRequest.email();
        if (customerDao.existsCustomerWithEmail(email)) {
            throw new DuplicateResourceException(
                    "email already taken");
        }

        // add
        Customer customer = new Customer(
                customerRegistrationRequest.name(),
                customerRegistrationRequest.email(),
                passwordEncoder.encode(customerRegistrationRequest.password()),
                customerRegistrationRequest.age(),
                customerRegistrationRequest.gender());

        customerDao.insertCustomer(customer);
    }

    public void deleteCustomerById(Integer customerId) {
        checkIfCustomerExists(customerId);

        customerDao.deleteCustomerById(customerId);
    }

    private void checkIfCustomerExists(Integer customerId) {
        if (!customerDao.existsCustomerById(customerId)) {
            throw new ResourceNotFoundException(
                    "customer with id [%s] not found".formatted(customerId));
        }
    }

    public void updateCustomer(Integer customerId,
            CustomerUpdateRequest updateRequest) {
        // TODO: for JPA use .getReferenceById(customerId) as it does does not bring
        // object into memory and instead a reference
        Customer customer = customerDao.selectCustomerById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "customer with id [%s] not found".formatted(customerId)));

        boolean changes = false;

        if (updateRequest.name() != null && !updateRequest.name().equals(customer.getName())) {
            customer.setName(updateRequest.name());
            changes = true;
        }

        if (updateRequest.age() != null && !updateRequest.age().equals(customer.getAge())) {
            customer.setAge(updateRequest.age());
            changes = true;
        }

        if (updateRequest.email() != null && !updateRequest.email().equals(customer.getEmail())) {
            if (customerDao.existsCustomerWithEmail(updateRequest.email())) {
                throw new DuplicateResourceException(
                        "email already taken");
            }
            customer.setEmail(updateRequest.email());
            changes = true;
        }

        if (!changes) {
            throw new RequestValidationException("no data changes found");
        }

        customerDao.updateCustomer(customer);
    }

    public void uploadCustomerProfileImage(Integer customerId, MultipartFile file) {
        Timer.Sample uploadTimer = metricsService.startUploadTimer();
        String profileImageId = UUID.randomUUID().toString();

        // Set up structured logging context
        MDC.put("operation", "profile_image_upload");
        MDC.put("customer_id", String.valueOf(customerId));
        MDC.put("profile_image_id", profileImageId);
        MDC.put("file_name", file.getOriginalFilename());
        MDC.put("file_size", String.valueOf(file.getSize()));
        MDC.put("content_type", file.getContentType());

        try {
            logger.info(
                    "Starting profile image upload - customer_id: {}, file_name: {}, file_size: {} bytes, content_type: {}",
                    customerId, file.getOriginalFilename(), file.getSize(), file.getContentType());

            // Validate customer exists first
            checkIfCustomerExists(customerId);
            logger.debug("Customer validation passed for customer ID: {}", customerId);

            // Validate the uploaded file
            try {
                fileValidationService.validateImageFile(file);
                logger.debug("File validation passed for customer ID: {}", customerId);
            } catch (InvalidFileTypeException e) {
                logger.warn("File type validation failed for customer ID: {}, validation_error: {}", customerId,
                        e.getMessage());
                metricsService.recordValidationFailure("file_type", e.getMessage());
                throw e;
            } catch (FileSizeExceededException e) {
                logger.warn("File size validation failed for customer ID: {}, validation_error: {}", customerId,
                        e.getMessage());
                metricsService.recordValidationFailure("file_size", e.getMessage());
                throw e;
            } catch (Exception e) {
                logger.error("Unexpected validation error for customer ID: {}, error: {}", customerId, e.getMessage(),
                        e);
                metricsService.recordValidationFailure("unexpected", e.getMessage());
                throw e;
            }

            String s3Key = "profile-images/%s/%s".formatted(customerId, profileImageId);
            MDC.put("s3_key", s3Key);

            try {
                logger.debug("Uploading file to S3 - bucket: {}, key: {}", s3Buckets.getCustomer(), s3Key);
                s3Service.putObject(
                        s3Buckets.getCustomer(),
                        s3Key,
                        file.getBytes());
                logger.info("Successfully uploaded profile image to S3 for customer ID: {}", customerId);
            } catch (IOException e) {
                logger.error("Failed to read file bytes for customer ID: {}, io_error: {}", customerId, e.getMessage(),
                        e);
                metricsService.recordUploadFailure("io_error: " + e.getMessage());
                throw new S3ServiceException("Failed to process uploaded file: " + e.getMessage(), e);
            } catch (S3ServiceException e) {
                logger.error("S3 service error during upload for customer ID: {}, s3_error: {}", customerId,
                        e.getMessage(), e);
                metricsService.recordS3Failure("upload", e.getMessage());
                throw e;
            } catch (Exception e) {
                logger.error("Unexpected error during S3 upload for customer ID: {}, error: {}", customerId,
                        e.getMessage(), e);
                metricsService.recordS3Failure("upload_unexpected", e.getMessage());
                throw new S3ServiceException("Unexpected error during profile image upload: " + e.getMessage(), e);
            }

            try {
                customerDao.updateCustomerProfileImageId(profileImageId, customerId);
                logger.info("Successfully updated customer profile image ID for customer ID: {}", customerId);
                metricsService.recordUploadSuccess(String.valueOf(customerId), file.getOriginalFilename(),
                        file.getSize(), file.getContentType());
            } catch (Exception e) {
                logger.error("Failed to update customer profile image ID for customer ID: {}, database_error: {}",
                        customerId, e.getMessage(), e);
                metricsService.recordUploadFailure("database_error: " + e.getMessage());
                // Note: At this point the file is already uploaded to S3, but we couldn't
                // update the database
                // In a production system, you might want to implement compensation logic to
                // delete the S3 object
                throw new RuntimeException("Failed to update customer profile image reference: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            // Record failure if not already recorded
            if (!(e instanceof InvalidFileTypeException || e instanceof FileSizeExceededException)) {
                metricsService.recordUploadFailure("general_error: " + e.getMessage());
            }
            throw e;
        } finally {
            metricsService.recordUploadDuration(uploadTimer);
            // Clean up MDC
            MDC.remove("operation");
            MDC.remove("customer_id");
            MDC.remove("profile_image_id");
            MDC.remove("file_name");
            MDC.remove("file_size");
            MDC.remove("content_type");
            MDC.remove("s3_key");
        }
    }

    public byte[] getCustomerProfileImage(Integer customerId) {
        // Set up structured logging context
        MDC.put("operation", "profile_image_retrieval");
        MDC.put("customer_id", String.valueOf(customerId));

        try {
            logger.debug("Retrieving profile image for customer ID: {}", customerId);

            var customer = customerDao.selectCustomerById(customerId)
                    .map(customerDTOMapper)
                    .orElseThrow(() -> {
                        logger.warn("Customer not found when retrieving profile image, customer ID: {}", customerId);
                        return new ResourceNotFoundException(
                                "customer with id [%s] not found".formatted(customerId));
                    });

            // Check if profileImageId is Empty or Null
            if (customer.profileImageId() == null || customer.profileImageId().isEmpty()) {
                logger.warn("No profile image found for customer ID: {}", customerId);
                throw new ResourceNotFoundException(
                        "customer with id [%s] profile image not found".formatted(customerId));
            }

            String s3Key = "profile-images/%s/%s".formatted(customerId, customer.profileImageId());
            MDC.put("s3_key", s3Key);
            MDC.put("profile_image_id", customer.profileImageId());

            logger.debug("Retrieving profile image from S3 - bucket: {}, key: {}", s3Buckets.getCustomer(), s3Key);

            try {
                byte[] profileImage = s3Service.getObject(s3Buckets.getCustomer(), s3Key);
                logger.info("Successfully retrieved profile image for customer ID: {}, size: {} bytes",
                        customerId, profileImage.length);
                return profileImage;
            } catch (S3ServiceException e) {
                logger.error("Failed to retrieve profile image from S3 for customer ID: {}, s3_error: {}",
                        customerId, e.getMessage(), e);
                throw e;
            } catch (Exception e) {
                logger.error("Unexpected error retrieving profile image for customer ID: {}, error: {}",
                        customerId, e.getMessage(), e);
                throw new S3ServiceException("Unexpected error retrieving profile image: " + e.getMessage(), e);
            }
        } finally {
            // Clean up MDC
            MDC.remove("operation");
            MDC.remove("customer_id");
            MDC.remove("s3_key");
            MDC.remove("profile_image_id");
        }
    }
}
