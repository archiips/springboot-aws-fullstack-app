# Enhanced Error Handling for React Customer Service

This document describes the comprehensive error handling improvements implemented for the React customer service, specifically for file upload operations.

## Features Implemented

### 1. Enhanced Error Types and Classes

- **UploadError Class**: Custom error class with structured error information
- **Error Types**: Categorized error types for better handling:
  - `FILE_SIZE`: File size exceeds limits
  - `FILE_TYPE`: Invalid file type
  - `NETWORK`: Network connectivity issues
  - `SERVER`: Server-side errors
  - `AUTHENTICATION`: Authentication failures
  - `NOT_FOUND`: Resource not found
  - `UNKNOWN`: Unexpected errors

### 2. Client-Side File Validation

- **File Type Validation**: Supports JPEG, PNG, GIF, WebP formats
- **File Size Validation**: Maximum 10MB file size limit
- **Pre-upload Validation**: Validates files before sending to server

### 3. Retry Logic with Exponential Backoff

- **Automatic Retries**: Retries failed uploads for network and server errors
- **Exponential Backoff**: Increasing delay between retry attempts
- **Smart Retry Logic**: Only retries appropriate error types
- **Configurable**: Retry settings can be customized per upload

### 4. Enhanced Upload Functions

#### `uploadCustomerProfilePicture(id, formData, onUploadProgress, options)`

Enhanced version of the original upload function with:

- File validation
- Retry logic
- Better error handling
- Configurable options

#### `uploadCustomerProfilePictureWithProgress(id, file, onProgress, onCancel, options)`

Advanced upload function with:

- Progress tracking with speed calculation
- Upload cancellation support
- Enhanced progress information
- User-friendly error messages

### 5. Error Message Utilities

#### `parseError(error)`

Converts various error types into structured UploadError instances with:

- HTTP status code mapping
- User-friendly error messages
- Detailed error information

#### `getErrorMessage(error)`

Formats errors for UI display with:

- Title and description
- Error type classification
- Actionable user guidance

## Usage Examples

### Basic Upload with Error Handling

```javascript
import { uploadCustomerProfilePicture, getErrorMessage } from "./client.js";

try {
  const formData = new FormData();
  formData.append("file", file);

  const result = await uploadCustomerProfilePicture(customerId, formData);
  console.log("Upload successful:", result);
} catch (error) {
  const errorInfo = getErrorMessage(error);
  console.error("Upload failed:", errorInfo.title, errorInfo.description);
}
```

### Advanced Upload with Progress and Cancellation

```javascript
import { uploadCustomerProfilePictureWithProgress } from "./client.js";

let cancelUpload;

const onProgress = (progressInfo) => {
  console.log(`Progress: ${progressInfo.progress}%`);
  console.log(`Speed: ${(progressInfo.speed / (1024 * 1024)).toFixed(2)} MB/s`);
};

const onCancel = (cancelFn) => {
  cancelUpload = cancelFn;
};

try {
  const result = await uploadCustomerProfilePictureWithProgress(
    customerId,
    file,
    onProgress,
    onCancel
  );
  console.log("Upload successful:", result);
} catch (error) {
  if (error.type === "UNKNOWN" && error.message === "Upload cancelled") {
    console.log("Upload was cancelled by user");
  } else {
    const errorInfo = getErrorMessage(error);
    console.error("Upload failed:", errorInfo.title, errorInfo.description);
  }
}

// To cancel upload
if (cancelUpload) {
  cancelUpload();
}
```

### Custom Retry Configuration

```javascript
const customRetryConfig = {
  maxRetries: 5,
  retryDelay: 2000, // 2 seconds
  retryableErrors: ["NETWORK", "SERVER"],
};

const result = await uploadCustomerProfilePicture(customerId, formData, null, {
  retryConfig: customRetryConfig,
});
```

## Error Handling in Components

The UpdateCustomerForm component has been enhanced to use these new error handling features:

- **Client-side validation**: Files are validated before upload attempts
- **Progress tracking**: Shows upload progress with speed information
- **Cancellation support**: Users can cancel ongoing uploads
- **Enhanced error messages**: Displays specific, actionable error messages
- **Retry transparency**: Automatic retries happen seamlessly in the background

## Testing

Comprehensive tests are included in `__tests__/client.test.js` covering:

- File validation scenarios
- Error parsing and formatting
- Retry logic with different error types
- Upload progress tracking
- Cancellation functionality
- Various HTTP error status codes

Run tests with:

```bash
npm run test:run -- client.test.js
```

## Configuration

### File Upload Limits

```javascript
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ALLOWED_FILE_TYPES = [
  "image/jpeg",
  "image/png",
  "image/gif",
  "image/webp",
];
```

### Retry Configuration

```javascript
const RETRY_CONFIG = {
  maxRetries: 3,
  retryDelay: 1000, // 1 second
  retryableErrors: [ERROR_TYPES.NETWORK, ERROR_TYPES.SERVER],
};
```

## Benefits

1. **Better User Experience**: Clear, actionable error messages
2. **Improved Reliability**: Automatic retry for transient failures
3. **Enhanced Feedback**: Real-time progress and speed information
4. **User Control**: Ability to cancel uploads
5. **Developer Experience**: Structured error handling and comprehensive testing
6. **Maintainability**: Well-organized error types and utilities
