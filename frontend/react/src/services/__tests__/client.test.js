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
} from '../client.js';

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

describe('Client Error Handling', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockLocalStorage.getItem.mockReturnValue('mock-token');
        mockedAxios.post.mockClear();
        mockedAxios.isCancel.mockReturnValue(false);
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('validateFile', () => {
        it('should pass validation for valid image files', () => {
            const validFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
            Object.defineProperty(validFile, 'size', { value: 1024 * 1024 }); // 1MB
            
            expect(() => validateFile(validFile)).not.toThrow();
        });

        it('should throw UploadError for null file', () => {
            expect(() => validateFile(null)).toThrow(UploadError);
            expect(() => validateFile(null)).toThrow('No file selected');
        });

        it('should throw UploadError for invalid file type', () => {
            const invalidFile = new File(['test'], 'test.txt', { type: 'text/plain' });
            
            expect(() => validateFile(invalidFile)).toThrow(UploadError);
            expect(() => validateFile(invalidFile)).toThrow('Invalid file type');
        });

        it('should throw UploadError for oversized file', () => {
            const oversizedFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
            Object.defineProperty(oversizedFile, 'size', { value: 15 * 1024 * 1024 }); // 15MB
            
            expect(() => validateFile(oversizedFile)).toThrow(UploadError);
            expect(() => validateFile(oversizedFile)).toThrow('File size too large');
        });

        it('should validate all supported image types', () => {
            const supportedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
            
            supportedTypes.forEach(type => {
                const file = new File(['test'], `test.${type.split('/')[1]}`, { type });
                Object.defineProperty(file, 'size', { value: 1024 * 1024 }); // 1MB
                
                expect(() => validateFile(file)).not.toThrow();
            });
        });
    });

    describe('parseError', () => {
        it('should return UploadError as-is', () => {
            const originalError = new UploadError(ERROR_TYPES.FILE_SIZE, 'Test error');
            const result = parseError(originalError);
            
            expect(result).toBe(originalError);
        });

        it('should parse 400 Bad Request errors', () => {
            const axiosError = {
                response: {
                    status: 400,
                    data: { message: 'Invalid file type provided' }
                }
            };
            
            const result = parseError(axiosError);
            
            expect(result).toBeInstanceOf(UploadError);
            expect(result.type).toBe(ERROR_TYPES.FILE_TYPE);
            expect(result.message).toBe('Invalid file type');
        });

        it('should parse 401 Unauthorized errors', () => {
            const axiosError = {
                response: {
                    status: 401,
                    data: { message: 'Unauthorized' }
                }
            };
            
            const result = parseError(axiosError);
            
            expect(result).toBeInstanceOf(UploadError);
            expect(result.type).toBe(ERROR_TYPES.AUTHENTICATION);
            expect(result.message).toBe('Authentication required');
        });

        it('should parse 404 Not Found errors', () => {
            const axiosError = {
                response: {
                    status: 404,
                    data: { message: 'Customer not found' }
                }
            };
            
            const result = parseError(axiosError);
            
            expect(result).toBeInstanceOf(UploadError);
            expect(result.type).toBe(ERROR_TYPES.NOT_FOUND);
            expect(result.message).toBe('Customer not found');
        });

        it('should parse 413 Payload Too Large errors', () => {
            const axiosError = {
                response: {
                    status: 413,
                    data: { message: 'File too large' }
                }
            };
            
            const result = parseError(axiosError);
            
            expect(result).toBeInstanceOf(UploadError);
            expect(result.type).toBe(ERROR_TYPES.FILE_SIZE);
            expect(result.message).toBe('File size too large');
        });

        it('should parse 500 Server Error', () => {
            const axiosError = {
                response: {
                    status: 500,
                    data: { message: 'Internal server error' }
                }
            };
            
            const result = parseError(axiosError);
            
            expect(result).toBeInstanceOf(UploadError);
            expect(result.type).toBe(ERROR_TYPES.SERVER);
            expect(result.message).toBe('Server error');
        });

        it('should parse network errors', () => {
            const networkError = {
                request: {},
                message: 'Network Error'
            };
            
            const result = parseError(networkError);
            
            expect(result).toBeInstanceOf(UploadError);
            expect(result.type).toBe(ERROR_TYPES.NETWORK);
            expect(result.message).toBe('Network error');
        });

        it('should parse unknown errors', () => {
            const unknownError = {
                message: 'Something went wrong'
            };
            
            const result = parseError(unknownError);
            
            expect(result).toBeInstanceOf(UploadError);
            expect(result.type).toBe(ERROR_TYPES.UNKNOWN);
            expect(result.message).toBe('Unexpected error');
        });
    });

    describe('getErrorMessage', () => {
        it('should format UploadError correctly', () => {
            const error = new UploadError(
                ERROR_TYPES.FILE_SIZE,
                'File too large',
                'Please select a smaller file'
            );
            
            const result = getErrorMessage(error);
            
            expect(result).toEqual({
                title: 'File too large',
                description: 'Please select a smaller file',
                type: ERROR_TYPES.FILE_SIZE
            });
        });

        it('should format non-UploadError correctly', () => {
            const error = {
                response: {
                    status: 500,
                    data: { message: 'Server error' }
                }
            };
            
            const result = getErrorMessage(error);
            
            expect(result.title).toBe('Server error');
            expect(result.type).toBe(ERROR_TYPES.SERVER);
        });
    });

    describe('uploadCustomerProfilePicture', () => {
        it('should upload successfully', async () => {
            const mockResponse = { data: { success: true } };
            mockedAxios.post.mockResolvedValueOnce(mockResponse);
            
            const formData = new FormData();
            const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
            Object.defineProperty(file, 'size', { value: 1024 * 1024 }); // 1MB
            formData.append('file', file);
            
            const result = await uploadCustomerProfilePicture(1, formData);
            
            expect(result).toBe(mockResponse);
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
            const formData = new FormData();
            const invalidFile = new File(['test'], 'test.txt', { type: 'text/plain' });
            formData.append('file', invalidFile);
            
            await expect(uploadCustomerProfilePicture(1, formData))
                .rejects.toThrow(UploadError);
        });

        it('should skip validation when validateFile option is false', async () => {
            const mockResponse = { data: { success: true } };
            mockedAxios.post.mockResolvedValueOnce(mockResponse);
            
            const formData = new FormData();
            const invalidFile = new File(['test'], 'test.txt', { type: 'text/plain' });
            formData.append('file', invalidFile);
            
            const result = await uploadCustomerProfilePicture(1, formData, null, { validateFile: false });
            
            expect(result).toBe(mockResponse);
        });

        it('should retry on network errors', async () => {
            const networkError = {
                request: {},
                message: 'Network Error'
            };
            const mockResponse = { data: { success: true } };
            
            mockedAxios.post
                .mockRejectedValueOnce(networkError)
                .mockRejectedValueOnce(networkError)
                .mockResolvedValueOnce(mockResponse);
            
            const formData = new FormData();
            const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
            Object.defineProperty(file, 'size', { value: 1024 * 1024 }); // 1MB
            formData.append('file', file);
            
            // Use faster retry config for testing
            const fastRetryConfig = {
                maxRetries: 3,
                retryDelay: 10 // 10ms instead of 1000ms
            };
            
            const result = await uploadCustomerProfilePicture(1, formData, null, { retryConfig: fastRetryConfig });
            
            expect(result).toBe(mockResponse);
            expect(mockedAxios.post).toHaveBeenCalledTimes(3);
        });

        it('should not retry on validation errors', async () => {
            const validationError = {
                response: {
                    status: 400,
                    data: { message: 'Invalid file type' }
                }
            };
            
            mockedAxios.post.mockRejectedValueOnce(validationError);
            
            const formData = new FormData();
            const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
            Object.defineProperty(file, 'size', { value: 1024 * 1024 }); // 1MB
            formData.append('file', file);
            
            await expect(uploadCustomerProfilePicture(1, formData))
                .rejects.toThrow(UploadError);
            
            expect(mockedAxios.post).toHaveBeenCalledTimes(1);
        });

        it('should throw UploadError after max retries', async () => {
            const networkError = {
                request: {},
                message: 'Network Error'
            };
            
            mockedAxios.post.mockRejectedValue(networkError);
            
            const formData = new FormData();
            const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
            Object.defineProperty(file, 'size', { value: 1024 * 1024 }); // 1MB
            formData.append('file', file);
            
            // Use faster retry config for testing
            const fastRetryConfig = {
                maxRetries: 2,
                retryDelay: 10 // 10ms instead of 1000ms
            };
            
            await expect(uploadCustomerProfilePicture(1, formData, null, { retryConfig: fastRetryConfig }))
                .rejects.toThrow(UploadError);
            
            expect(mockedAxios.post).toHaveBeenCalledTimes(3); // 1 initial + 2 retries
        }, 10000);
    });

    describe('uploadCustomerProfilePictureWithProgress', () => {
        it('should track upload progress', async () => {
            const mockResponse = { data: { success: true } };
            mockedAxios.post.mockImplementation((url, data, config) => {
                // Simulate progress
                if (config.onUploadProgress) {
                    config.onUploadProgress({
                        loaded: 50,
                        total: 100,
                        lengthComputable: true
                    });
                }
                return Promise.resolve(mockResponse);
            });
            
            const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
            Object.defineProperty(file, 'size', { value: 1024 * 1024 }); // 1MB
            
            const progressCallback = vi.fn();
            
            const result = await uploadCustomerProfilePictureWithProgress(
                1, 
                file, 
                progressCallback
            );
            
            expect(result).toBe(mockResponse);
            expect(progressCallback).toHaveBeenCalledWith(
                expect.objectContaining({
                    loaded: 50,
                    total: 100,
                    progress: 50,
                    speed: expect.any(Number)
                })
            );
        });

        it('should handle cancellation', async () => {
            mockedAxios.isCancel.mockReturnValue(true);
            mockedAxios.post.mockRejectedValueOnce(new Error('Cancelled'));
            
            const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
            Object.defineProperty(file, 'size', { value: 1024 * 1024 }); // 1MB
            
            let cancelFunction;
            const onCancel = (cancel) => {
                cancelFunction = cancel;
            };
            
            const uploadPromise = uploadCustomerProfilePictureWithProgress(
                1, 
                file, 
                () => {}, 
                onCancel
            );
            
            // Simulate cancellation
            if (cancelFunction) {
                cancelFunction();
            }
            
            await expect(uploadPromise).rejects.toThrow('Upload cancelled');
        });
    });
});