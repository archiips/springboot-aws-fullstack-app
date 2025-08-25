import axios from 'axios';

const getAuthConfig = () => ({
    headers: {
        Authorization: `Bearer ${localStorage.getItem("access_token")}`
    }
})

// Error types for better error handling
export const ERROR_TYPES = {
    FILE_SIZE: 'FILE_SIZE',
    FILE_TYPE: 'FILE_TYPE',
    NETWORK: 'NETWORK',
    SERVER: 'SERVER',
    AUTHENTICATION: 'AUTHENTICATION',
    NOT_FOUND: 'NOT_FOUND',
    UNKNOWN: 'UNKNOWN'
};

// Enhanced error class for upload operations
export class UploadError extends Error {
    constructor(type, message, details = null, originalError = null) {
        super(message);
        this.name = 'UploadError';
        this.type = type;
        this.details = details;
        this.originalError = originalError;
        this.timestamp = new Date().toISOString();
    }
}

// File validation constants
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ALLOWED_FILE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

// Client-side file validation
export const validateFile = (file) => {
    if (!file) {
        throw new UploadError(
            ERROR_TYPES.FILE_TYPE,
            "No file selected",
            "Please select a file to upload"
        );
    }

    if (!ALLOWED_FILE_TYPES.includes(file.type)) {
        throw new UploadError(
            ERROR_TYPES.FILE_TYPE,
            "Invalid file type",
            `Please select a JPEG, PNG, GIF, or WebP image. Selected: ${file.type}`
        );
    }

    if (file.size > MAX_FILE_SIZE) {
        throw new UploadError(
            ERROR_TYPES.FILE_SIZE,
            "File size too large",
            `Please select an image under ${MAX_FILE_SIZE / (1024 * 1024)}MB. Current size: ${(file.size / (1024 * 1024)).toFixed(2)}MB`
        );
    }

    return true;
};

// Enhanced error parsing function
export const parseError = (error) => {
    if (error instanceof UploadError) {
        return error;
    }

    if (error.response) {
        const status = error.response.status;
        const data = error.response.data;
        
        switch (status) {
            case 400:
                if (data.message && data.message.toLowerCase().includes('file type')) {
                    return new UploadError(
                        ERROR_TYPES.FILE_TYPE,
                        "Invalid file type",
                        "Please select a JPEG, PNG, GIF, or WebP image",
                        error
                    );
                }
                return new UploadError(
                    ERROR_TYPES.SERVER,
                    "Invalid request",
                    data.message || "Please check your file and try again",
                    error
                );
            case 401:
                return new UploadError(
                    ERROR_TYPES.AUTHENTICATION,
                    "Authentication required",
                    "Please log in again to continue",
                    error
                );
            case 404:
                return new UploadError(
                    ERROR_TYPES.NOT_FOUND,
                    "Customer not found",
                    "Please refresh the page and try again",
                    error
                );
            case 413:
                return new UploadError(
                    ERROR_TYPES.FILE_SIZE,
                    "File size too large",
                    "Please select an image under 10MB",
                    error
                );
            case 500:
                return new UploadError(
                    ERROR_TYPES.SERVER,
                    "Server error",
                    "Please try again later",
                    error
                );
            default:
                return new UploadError(
                    ERROR_TYPES.SERVER,
                    "Upload failed",
                    data.message || "Please try again",
                    error
                );
        }
    } else if (error.request) {
        return new UploadError(
            ERROR_TYPES.NETWORK,
            "Network error",
            "Please check your connection and try again",
            error
        );
    } else {
        return new UploadError(
            ERROR_TYPES.UNKNOWN,
            "Unexpected error",
            error.message || "Please try again",
            error
        );
    }
};

// Retry configuration
const RETRY_CONFIG = {
    maxRetries: 3,
    retryDelay: 1000, // 1 second
    retryableErrors: [ERROR_TYPES.NETWORK, ERROR_TYPES.SERVER]
};

// Sleep utility for retry delays
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Enhanced retry logic with exponential backoff
const withRetry = async (operation, config = RETRY_CONFIG) => {
    let lastError;
    
    for (let attempt = 0; attempt <= config.maxRetries; attempt++) {
        try {
            return await operation();
        } catch (error) {
            const parsedError = parseError(error);
            lastError = parsedError;
            
            // Don't retry for certain error types
            if (!config.retryableErrors.includes(parsedError.type)) {
                throw parsedError;
            }
            
            // Don't retry on the last attempt
            if (attempt === config.maxRetries) {
                throw parsedError;
            }
            
            // Exponential backoff: delay increases with each attempt
            const delay = config.retryDelay * Math.pow(2, attempt);
            console.log(`Upload attempt ${attempt + 1} failed, retrying in ${delay}ms...`, parsedError);
            await sleep(delay);
        }
    }
    
    throw lastError;
};

export const getCustomers = async () => {
    try {
        return await axios.get(
            `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers`,
            getAuthConfig()
        )
    } catch (e) {
        throw e;
    }
}

export const saveCustomer = async (customer) => {
    try {
        return await axios.post(
            `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers`,
            customer
        )
    } catch (e) {
        throw e;
    }
}

export const updateCustomer = async (id, update) => {
    try {
        return await axios.put(
            `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers/${id}`,
            update,
            getAuthConfig()
        )
    } catch (e) {
        throw e;
    }
}

export const deleteCustomer = async (id) => {
    try {
        return await axios.delete(
            `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers/${id}`,
            getAuthConfig()
        )
    } catch (e) {
        throw e;
    }
}

export const login = async (usernameAndPassword) => {
    try {
        return await axios.post(
            `${import.meta.env.VITE_API_BASE_URL}/api/v1/auth/login`,
            usernameAndPassword
        )
    } catch (e) {
        throw e;
    }
}

export const uploadCustomerProfilePicture = async (id, formData, onUploadProgress, options = {}) => {
    // Extract file from formData for validation
    const file = formData.get('file');
    
    // Validate file before upload
    if (file && options.validateFile !== false) {
        validateFile(file);
    }
    
    // Create upload operation
    const uploadOperation = () => {
        return axios.post(
            `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers/${id}/profile-image`,
            formData,
            {
                ...getAuthConfig(),
                headers: {
                    ...getAuthConfig().headers,
                    'Content-Type': 'multipart/form-data'
                },
                onUploadProgress: onUploadProgress,
                timeout: options.timeout || 30000, // 30 second timeout
                ...options.axiosConfig
            }
        );
    };
    
    try {
        // Use retry logic for upload operation
        const retryConfig = {
            ...RETRY_CONFIG,
            ...options.retryConfig
        };
        
        return await withRetry(uploadOperation, retryConfig);
    } catch (error) {
        // Parse and throw enhanced error
        throw parseError(error);
    }
};

// Get user-friendly error message from UploadError
export const getErrorMessage = (error) => {
    if (error instanceof UploadError) {
        return {
            title: error.message,
            description: error.details || "Please try again",
            type: error.type
        };
    }
    
    // Fallback for non-UploadError instances
    const parsedError = parseError(error);
    return {
        title: parsedError.message,
        description: parsedError.details || "Please try again",
        type: parsedError.type
    };
};

// Enhanced upload with progress tracking and cancellation support
export const uploadCustomerProfilePictureWithProgress = async (
    id, 
    file, 
    onProgress = () => {}, 
    onCancel = null,
    options = {}
) => {
    // Validate file
    validateFile(file);
    
    // Create FormData
    const formData = new FormData();
    formData.append('file', file);
    
    // Create cancel token if cancellation is supported
    const cancelToken = onCancel ? axios.CancelToken.source() : null;
    if (onCancel) {
        onCancel(() => {
            if (cancelToken) {
                cancelToken.cancel('Upload cancelled by user');
            }
        });
    }
    
    // Enhanced progress handler
    const progressHandler = (progressEvent) => {
        if (progressEvent.lengthComputable) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            onProgress({
                loaded: progressEvent.loaded,
                total: progressEvent.total,
                progress: progress,
                speed: progressEvent.loaded / ((Date.now() - startTime) / 1000) // bytes per second
            });
        }
    };
    
    const startTime = Date.now();
    
    try {
        return await uploadCustomerProfilePicture(id, formData, progressHandler, {
            ...options,
            axiosConfig: {
                cancelToken: cancelToken?.token,
                ...options.axiosConfig
            }
        });
    } catch (error) {
        if (axios.isCancel(error)) {
            throw new UploadError(
                ERROR_TYPES.UNKNOWN,
                "Upload cancelled",
                "The upload was cancelled by the user"
            );
        }
        throw error;
    }
};

export const customerProfilePictureUrl = (id) =>
    `${import.meta.env.VITE_API_BASE_URL}/api/v1/customers/${id}/profile-image`;