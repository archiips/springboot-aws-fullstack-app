package com.architos.customer;

import com.architos.exception.FileSizeExceededException;
import com.architos.exception.InvalidFileTypeException;
import com.architos.exception.ResourceNotFoundException;
import com.architos.exception.S3ServiceException;
import com.architos.jwt.JWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/v1/customers")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final JWTUtil jwtUtil;

    public CustomerController(CustomerService customerService,
            JWTUtil jwtUtil) {
        this.customerService = customerService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public List<CustomerDTO> getCustomers() {
        return customerService.getAllCustomers();
    }

    @GetMapping("{customerId}")
    public CustomerDTO getCustomer(
            @PathVariable("customerId") Integer customerId) {
        return customerService.getCustomer(customerId);
    }

    @PostMapping
    public ResponseEntity<?> registerCustomer(
            @RequestBody CustomerRegistrationRequest request) {
        customerService.addCustomer(request);
        String jwtToken = jwtUtil.issueToken(request.email(), "ROLE_USER");
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, jwtToken)
                .build();
    }

    @DeleteMapping("{customerId}")
    public void deleteCustomer(
            @PathVariable("customerId") Integer customerId) {
        customerService.deleteCustomerById(customerId);
    }

    @PutMapping("{customerId}")
    public void updateCustomer(
            @PathVariable("customerId") Integer customerId,
            @RequestBody CustomerUpdateRequest updateRequest) {
        customerService.updateCustomer(customerId, updateRequest);
    }

    @PostMapping(value = "{customerId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCustomerProfileImage(@PathVariable("customerId") Integer customerId,
            @RequestParam("file") MultipartFile file) {
        try {
            logger.info("Uploading profile image for customer ID: {}, file name: {}, file size: {} bytes",
                    customerId, file.getOriginalFilename(), file.getSize());

            customerService.uploadCustomerProfileImage(customerId, file);

            logger.info("Successfully uploaded profile image for customer ID: {}", customerId);
            return ResponseEntity.ok().build();

        } catch (InvalidFileTypeException e) {
            logger.warn("Invalid file type for customer ID: {}, error: {}", customerId, e.getMessage());
            throw e; // Let the global exception handler handle it
        } catch (FileSizeExceededException e) {
            logger.warn("File size exceeded for customer ID: {}, error: {}", customerId, e.getMessage());
            throw e; // Let the global exception handler handle it
        } catch (ResourceNotFoundException e) {
            logger.warn("Customer not found for ID: {}, error: {}", customerId, e.getMessage());
            throw e; // Let the global exception handler handle it
        } catch (S3ServiceException e) {
            logger.error("S3 service error for customer ID: {}, error: {}", customerId, e.getMessage());
            throw e; // Let the global exception handler handle it
        } catch (Exception e) {
            logger.error("Unexpected error uploading profile image for customer ID: {}", customerId, e);
            throw new S3ServiceException("Failed to upload profile image", e);
        }
    }

    @GetMapping(value = "{customerId}/profile-image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getCustomerProfileImage(@PathVariable("customerId") Integer customerId) {
        try {
            logger.debug("Retrieving profile image for customer ID: {}", customerId);

            byte[] imageData = customerService.getCustomerProfileImage(customerId);

            logger.debug("Successfully retrieved profile image for customer ID: {}, size: {} bytes",
                    customerId, imageData.length);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageData);

        } catch (ResourceNotFoundException e) {
            logger.warn("Customer or profile image not found for ID: {}, error: {}", customerId, e.getMessage());
            throw e; // Let the global exception handler handle it
        } catch (S3ServiceException e) {
            logger.error("S3 service error retrieving profile image for customer ID: {}, error: {}", customerId,
                    e.getMessage());
            throw e; // Let the global exception handler handle it
        } catch (Exception e) {
            logger.error("Unexpected error retrieving profile image for customer ID: {}", customerId, e);
            throw new S3ServiceException("Failed to retrieve profile image", e);
        }
    }
}
