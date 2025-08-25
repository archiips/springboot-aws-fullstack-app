import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import axios from 'axios';
import {
    uploadCustomerProfilePicture,
    uploadCustomerProfilePictureWithProgress,
    validateFile,
    parseError,
    getErrorMessage,
    UploadError,
    ERROR_TYPES
} from '../../../services/client.js';

// Mock axios
vi.mock('axios');
const mockedAxios = vi.mocked(axios);

// Mock localStorage
const mockLocalStorage = {
    getItem: vi.fn(),
    setItem: vi.fn(),
    removeItem: vi.fn(),
    clear: vi.fn()
};
Object.defineProperty(window, 'localStorage', {
    value: mockLocalStorage
});

// Mock environment variables
vi.mock('import.meta', () => ({
    env: {
        VITE_API_BASE_URL: 'http://localhost:8080'
    }
}));

// Mock URL.createObjectURL
global.URL.createObjectURL = vi.fn(() => 'mocked-url');
global.URL.revokeObjectURL = vi.fn();

// Mock FileReader
global.FileReader = class {
    constructor() {
        this.readAsDataURL = vi.fn(() => {
            this.onload({ target: { result: 'data:image/jpeg;base64,mock-data' } });
        });
    }
};

// Helper function to create mock files
const createMockFile = (name, type, size) => {
    const file = new File(['test content'], name, { type });
    Object.defineProperty(file, 'size', { value: size });
    return file;
};

describe('Image Upload End-to-End Service Tests', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockLocalStorage.getItem.mockReturnValue('mock-token');
        mockedAxios.post.mockClear();
        mockedAxios.isCancel.mockReturnValue(false);
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('Complete Upload Flow', () => {
        it('should successfully upload image through service layer', async () => {
            // Mock successful upload
            mockedAxios.post.mockResolvedValueOnce({ data: { success: true } });
            
            // Create a valid image file
            const file = createMockFile('test.jpg', 'image/jpeg', 1024 * 1024); // 1MB
            const formData = new FormData();
            formData.append('file', file);

            // Call the upload service
            const result = await uploadCustomerProfilePicture(1, formData);

            // Verify upload was successful
            expect(result.data.success).toBe(true);
            expect(mockedAxios.post).toHaveBeenCalledWith(
                'http://localhost:8080/api/v1/customers/1/profile-image',
                formData,
                expect.objectContaining({
                    headers: expect.objectContaining({
                        'Authorization': 'Bearer mock-token',
                        'Content-Type': 'multipart/form-data'
                    }),
                    timeout: 30000
                })
            );
        });

        it('should validate file before upload', async () => {
            // Create a valid image file
            const validFile = createMockFile('test.jpg', 'image/jpeg', 1024 * 1024); // 1MB
            
            // Validation should pass
            expect(() => validateFile(validFile)).not.toThrow();
        });

        it('should handle multiple upload attempts correctly', async () => {
            // Mock successful uploads
            mockedAxios.post
                .mockResolvedValueOnce({ data: { success: true } })
                .mockResolvedValueOnce({ data: { success: true } });
            
            // Upload first image
            const file1 = createMockFile('first.jpg', 'image/jpeg', 1024 * 1024);
            const formData1 = new FormData();
            formData1.append('file', file1);
            
            const result1 = await uploadCustomerProfilePicture(1, formData1);
            expect(result1.data.success).toBe(true);

            // Upload second image (replacement)
            const file2 = createMockFile('second.jpg', 'image/jpeg', 1024 * 1024);
            const formData2 = new FormData();
            formData2.append('file', file2);
            
            const result2 = await uploadCustomerProfilePicture(1, formData2);
            expect(result2.data.success).toBe(true);
            
            // Verify both uploads were called
            expect(mockedAxios.post).toHaveBeenCalledTimes(2);
        });
    });

    describe('Error Scenarios', () => {
        it('should validate and reject invalid file type', async () => {
            // Create invalid file type
            const invalidFile = createMockFile('test.txt', 'text/plain', 1024);

            // Validation should throw error
            expect(() => validateFile(invalidFile)).toThrow(UploadError);
            expect(() => validateFile(invalidFile)).toThrow('Invalid file type');
        });

        it('should validate and reject oversized file', async () => {
            // Create oversized file (15MB)
            const oversizedFile = createMockFile('large.jpg', 'image/jpeg', 15 * 1024 * 1024);

            // Validation should throw error
            expect(() => validateFile(oversizedFile)).toThrow(UploadError);
            expect(() => validateFile(oversizedFile)).toThrow('File size too large');
        });

        it('should handle server errors gracefully', async () => {
            // Mock server error (reject multiple times to exhaust retries)
            const serverError = {
                response: {
                    status: 500,
                    data: { message: 'Internal server error' }
                }
            };
            mockedAxios.post.mockRejectedValue(serverError);
            
            // Create valid file
            const file = createMockFile('test.jpg', 'image/jpeg', 1024 * 1024);
            const formData = new FormData();
            formData.append('file', file);

            // Use faster retry config to speed up test
            const fastRetryConfig = {
                maxRetries: 1,
                retryDelay: 10
            };

            // Upload should throw parsed error after retries are exhausted
            await expect(uploadCustomerProfilePicture(1, formData, null, { retryConfig: fastRetryConfig }))
                .rejects.toThrow(UploadError);
        });

        it('should handle network errors with retry', async () => {
            // Mock network error then success
            const networkError = {
                request: {},
                message: 'Network Error'
            };
            
            mockedAxios.post
                .mockRejectedValueOnce(networkError)
                .mockResolvedValueOnce({ data: { success: true } });
            
            // Create valid file
            const file = createMockFile('test.jpg', 'image/jpeg', 1024 * 1024);
            const formData = new FormData();
            formData.append('file', file);

            // Use faster retry config for testing
            const fastRetryConfig = {
                maxRetries: 2,
                retryDelay: 10 // 10ms instead of 1000ms
            };

            // Upload should succeed after retry
            const result = await uploadCustomerProfilePicture(1, formData, null, { retryConfig: fastRetryConfig });
            expect(result.data.success).toBe(true);

            // Verify retry happened
            expect(mockedAxios.post).toHaveBeenCalledTimes(2);
        });

        it('should handle authentication errors', async () => {
            // Mock authentication error
            mockedAxios.post.mockRejectedValueOnce({
                response: {
                    status: 401,
                    data: { message: 'Unauthorized' }
                }
            });
            
            // Create valid file
            const file = createMockFile('test.jpg', 'image/jpeg', 1024 * 1024);
            const formData = new FormData();
            formData.append('file', file);

            // Upload should throw authentication error
            await expect(uploadCustomerProfilePicture(1, formData))
                .rejects.toThrow(UploadError);
        });
    });

    describe('Upload Progress and Advanced Features', () => {
        it('should handle upload timeout correctly', async () => {
            // Mock timeout error
            mockedAxios.post.mockRejectedValueOnce({
                code: 'ECONNABORTED',
                message: 'timeout of 30000ms exceeded'
            });
            
            // Create valid file
            const file = createMockFile('test.jpg', 'image/jpeg', 1024 * 1024);
            const formData = new FormData();
            formData.append('file', file);

            // Upload should throw timeout error
            await expect(uploadCustomerProfilePicture(1, formData))
                .rejects.toThrow(UploadError);
        });

        it('should track upload progress correctly', async () => {
            // Mock upload with progress
            mockedAxios.post.mockImplementation((url, data, config) => {
                if (config.onUploadProgress) {
                    // Simulate progress updates
                    config.onUploadProgress({ loaded: 50, total: 100, lengthComputable: true });
                }
                return Promise.resolve({ data: { success: true } });
            });
            
            // Create valid file
            const file = createMockFile('test.jpg', 'image/jpeg', 1024 * 1024);
            
            const progressCallback = vi.fn();
            
            // Upload with progress tracking
            const result = await uploadCustomerProfilePictureWithProgress(1, file, progressCallback);
            
            expect(result.data.success).toBe(true);
            expect(progressCallback).toHaveBeenCalledWith(
                expect.objectContaining({
                    loaded: 50,
                    total: 100,
                    progress: 50,
                    speed: expect.any(Number)
                })
            );
        });

        it('should handle upload cancellation', async () => {
            // Mock cancellation
            mockedAxios.isCancel.mockReturnValue(true);
            mockedAxios.post.mockRejectedValueOnce(new Error('Cancelled'));
            
            // Create valid file
            const file = createMockFile('test.jpg', 'image/jpeg', 1024 * 1024);
            
            let cancelFunction;
            const onCancel = (cancel) => {
                cancelFunction = cancel;
            };
            
            // Start upload with cancellation support
            const uploadPromise = uploadCustomerProfilePictureWithProgress(1, file, () => {}, onCancel);
            
            // Simulate cancellation
            if (cancelFunction) {
                cancelFunction();
            }
            
            // Upload should be cancelled
            await expect(uploadPromise).rejects.toThrow('Upload cancelled');
        });
    });

    describe('Error Message Parsing', () => {
        it('should parse different error types correctly', async () => {
            const errorScenarios = [
                {
                    error: {
                        response: {
                            status: 400,
                            data: { message: 'Invalid file type provided' }
                        }
                    },
                    expectedType: ERROR_TYPES.FILE_TYPE,
                    expectedMessage: 'Invalid file type'
                },
                {
                    error: {
                        response: {
                            status: 413,
                            data: { message: 'File too large' }
                        }
                    },
                    expectedType: ERROR_TYPES.FILE_SIZE,
                    expectedMessage: 'File size too large'
                },
                {
                    error: {
                        response: {
                            status: 404,
                            data: { message: 'Customer not found' }
                        }
                    },
                    expectedType: ERROR_TYPES.NOT_FOUND,
                    expectedMessage: 'Customer not found'
                }
            ];

            for (const scenario of errorScenarios) {
                const parsedError = parseError(scenario.error);
                
                expect(parsedError).toBeInstanceOf(UploadError);
                expect(parsedError.type).toBe(scenario.expectedType);
                expect(parsedError.message).toBe(scenario.expectedMessage);
                
                const errorMessage = getErrorMessage(parsedError);
                expect(errorMessage.type).toBe(scenario.expectedType);
                expect(errorMessage.title).toBe(scenario.expectedMessage);
            }
        });
    });
});