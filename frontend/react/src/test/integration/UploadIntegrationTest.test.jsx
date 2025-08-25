import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import axios from 'axios';
import { uploadCustomerProfilePicture, uploadCustomerProfilePictureWithProgress } from '../../services/client.js';

// Mock axios for integration testing
vi.mock('axios');
const mockedAxios = vi.mocked(axios);

describe('Frontend-Backend Upload Integration Tests', () => {
    let mockFile;
    let mockFormData;
    
    beforeEach(() => {
        // Reset all mocks
        vi.clearAllMocks();
        
        // Mock localStorage for auth token
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: vi.fn(() => 'mock-jwt-token'),
                setItem: vi.fn(),
                removeItem: vi.fn(),
            },
            writable: true,
        });
        
        // Create mock file
        mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });
        
        // Create mock FormData
        mockFormData = new FormData();
        mockFormData.append('file', mockFile);
        
        // Mock environment variable
        import.meta.env.VITE_API_BASE_URL = 'http://localhost:8080';
    });
    
    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('Upload Request Format Verification', () => {
        it('should send upload request with correct URL format', async () => {
            // Given
            const customerId = 123;
            const mockResponse = { data: { success: true } };
            mockedAxios.post.mockResolvedValue(mockResponse);
            
            // When
            await uploadCustomerProfilePicture(customerId, mockFormData);
            
            // Then
            expect(mockedAxios.post).toHaveBeenCalledWith(
                'http://localhost:8080/api/v1/customers/123/profile-image',
                expect.any(FormData),
                expect.any(Object)
            );
        });

        it('should send multipart form data with correct content type', async () => {
            // Given
            const customerId = 123;
            const mockResponse = { data: { success: true } };
            mockedAxios.post.mockResolvedValue(mockResponse);
            
            // When
            await uploadCustomerProfilePicture(customerId, mockFormData);
            
            // Then
            const [, , config] = mockedAxios.post.mock.calls[0];
            expect(config.headers['Content-Type']).toBe('multipart/form-data');
        });

        it('should include file parameter in FormData', async () => {
            // Given
            const customerId = 123;
            const mockResponse = { data: { success: true } };
            mockedAxios.post.mockResolvedValue(mockResponse);
            
            // When
            await uploadCustomerProfilePicture(customerId, mockFormData);
            
            // Then
            const [, formData] = mockedAxios.post.mock.calls[0];
            expect(formData).toBeInstanceOf(FormData);
            expect(formData.get('file')).toBe(mockFile);
        });
    });

    describe('Authentication Header Verification', () => {
        it('should include Authorization header with Bearer token', async () => {
            // Given
            const customerId = 123;
            const mockResponse = { data: { success: true } };
            mockedAxios.post.mockResolvedValue(mockResponse);
            
            // When
            await uploadCustomerProfilePicture(customerId, mockFormData);
            
            // Then
            const [, , config] = mockedAxios.post.mock.calls[0];
            expect(config.headers.Authorization).toBe('Bearer mock-jwt-token');
        });

        it('should handle missing auth token gracefully', async () => {
            // Given
            window.localStorage.getItem.mockReturnValue(null);
            const customerId = 123;
            const mockResponse = { data: { success: true } };
            mockedAxios.post.mockResolvedValue(mockResponse);
            
            // When
            await uploadCustomerProfilePicture(customerId, mockFormData);
            
            // Then
            const [, , config] = mockedAxios.post.mock.calls[0];
            expect(config.headers.Authorization).toBe('Bearer null');
        });
    });

    describe('Backend Response Handling', () => {
        it('should handle successful upload response', async () => {
            // Given
            const customerId = 123;
            const mockResponse = { 
                data: { success: true },
                status: 200,
                statusText: 'OK'
            };
            mockedAxios.post.mockResolvedValue(mockResponse);
            
            // When
            const result = await uploadCustomerProfilePicture(customerId, mockFormData);
            
            // Then
            expect(result).toEqual(mockResponse);
        });

        it('should handle 400 Bad Request with file type error', async () => {
            // Given
            const customerId = 123;
            const errorResponse = {
                response: {
                    status: 400,
                    data: { message: 'Invalid file type. Only JPEG, PNG, GIF, and WebP images are allowed.' }
                }
            };
            mockedAxios.post.mockRejectedValue(errorResponse);
            
            // When & Then
            await expect(uploadCustomerProfilePicture(customerId, mockFormData))
                .rejects.toThrow('Invalid file type');
        });

        it('should handle 401 Unauthorized error', async () => {
            // Given
            const customerId = 123;
            const errorResponse = {
                response: {
                    status: 401,
                    data: { message: 'Unauthorized' }
                }
            };
            mockedAxios.post.mockRejectedValue(errorResponse);
            
            // When & Then
            await expect(uploadCustomerProfilePicture(customerId, mockFormData))
                .rejects.toThrow('Authentication required');
        });

        it('should handle 404 Customer Not Found error', async () => {
            // Given
            const customerId = 999;
            const errorResponse = {
                response: {
                    status: 404,
                    data: { message: 'Customer not found' }
                }
            };
            mockedAxios.post.mockRejectedValue(errorResponse);
            
            // When & Then
            await expect(uploadCustomerProfilePicture(customerId, mockFormData))
                .rejects.toThrow('Customer not found');
        });

        it('should handle 413 File Size Too Large error', async () => {
            // Given
            const customerId = 123;
            const errorResponse = {
                response: {
                    status: 413,
                    data: { message: 'File size exceeds maximum allowed size' }
                }
            };
            mockedAxios.post.mockRejectedValue(errorResponse);
            
            // When & Then
            await expect(uploadCustomerProfilePicture(customerId, mockFormData))
                .rejects.toThrow('File size too large');
        });

        it('should handle 500 Internal Server Error', async () => {
            // Given
            const customerId = 123;
            const errorResponse = {
                response: {
                    status: 500,
                    data: { message: 'Internal server error' }
                }
            };
            mockedAxios.post.mockRejectedValue(errorResponse);
            
            // When & Then - Use fast retry config to avoid timeout
            await expect(uploadCustomerProfilePicture(customerId, mockFormData, null, {
                retryConfig: { maxRetries: 1, retryDelay: 10, retryableErrors: ['SERVER'] }
            })).rejects.toThrow('Server error');
        });

        it('should handle network errors', async () => {
            // Given
            const customerId = 123;
            const networkError = {
                request: {},
                message: 'Network Error'
            };
            mockedAxios.post.mockRejectedValue(networkError);
            
            // When & Then - Use fast retry config to avoid timeout
            await expect(uploadCustomerProfilePicture(customerId, mockFormData, null, {
                retryConfig: { maxRetries: 1, retryDelay: 10, retryableErrors: ['NETWORK'] }
            })).rejects.toThrow('Network error');
        });
    });

    describe('Request Configuration Verification', () => {
        it('should set appropriate timeout for upload requests', async () => {
            // Given
            const customerId = 123;
            const mockResponse = { data: { success: true } };
            mockedAxios.post.mockResolvedValue(mockResponse);
            
            // When
            await uploadCustomerProfilePicture(customerId, mockFormData);
            
            // Then
            const [, , config] = mockedAxios.post.mock.calls[0];
            expect(config.timeout).toBe(30000); // 30 seconds
        });

        it('should support progress tracking', async () => {
            // Given
            const customerId = 123;
            const mockResponse = { data: { success: true } };
            const progressCallback = vi.fn();
            mockedAxios.post.mockResolvedValue(mockResponse);
            
            // When
            await uploadCustomerProfilePicture(customerId, mockFormData, progressCallback);
            
            // Then
            const [, , config] = mockedAxios.post.mock.calls[0];
            expect(config.onUploadProgress).toBe(progressCallback);
        });

        it('should support custom axios configuration', async () => {
            // Given
            const customerId = 123;
            const mockResponse = { data: { success: true } };
            const customConfig = { timeout: 60000 };
            mockedAxios.post.mockResolvedValue(mockResponse);
            
            // When
            await uploadCustomerProfilePicture(customerId, mockFormData, null, {
                axiosConfig: customConfig
            });
            
            // Then
            const [, , config] = mockedAxios.post.mock.calls[0];
            expect(config.timeout).toBe(60000);
        });
    });

    describe('Progress Tracking Integration', () => {
        it('should track upload progress correctly', async () => {
            // Given
            const customerId = 123;
            const mockResponse = { data: { success: true } };
            const progressCallback = vi.fn();
            mockedAxios.post.mockResolvedValue(mockResponse);
            
            // When
            await uploadCustomerProfilePictureWithProgress(customerId, mockFile, progressCallback);
            
            // Then
            expect(mockedAxios.post).toHaveBeenCalledWith(
                'http://localhost:8080/api/v1/customers/123/profile-image',
                expect.any(FormData),
                expect.objectContaining({
                    onUploadProgress: expect.any(Function)
                })
            );
        });

        it('should handle progress events correctly', async () => {
            // Given
            const customerId = 123;
            const mockResponse = { data: { success: true } };
            const progressCallback = vi.fn();
            let capturedProgressHandler;
            
            mockedAxios.post.mockImplementation((url, data, config) => {
                capturedProgressHandler = config.onUploadProgress;
                return Promise.resolve(mockResponse);
            });
            
            // When
            await uploadCustomerProfilePictureWithProgress(customerId, mockFile, progressCallback);
            
            // Simulate progress event
            const progressEvent = {
                lengthComputable: true,
                loaded: 500,
                total: 1000
            };
            capturedProgressHandler(progressEvent);
            
            // Then
            expect(progressCallback).toHaveBeenCalledWith(
                expect.objectContaining({
                    loaded: 500,
                    total: 1000,
                    progress: 50
                })
            );
        });
    });

    describe('File Validation Integration', () => {
        it('should validate file before sending request', async () => {
            // Given
            const customerId = 123;
            const invalidFile = new File(['content'], 'test.txt', { type: 'text/plain' });
            const invalidFormData = new FormData();
            invalidFormData.append('file', invalidFile);
            
            // When & Then
            await expect(uploadCustomerProfilePicture(customerId, invalidFormData))
                .rejects.toThrow('Invalid file type');
            
            // Verify no request was made
            expect(mockedAxios.post).not.toHaveBeenCalled();
        });

        it('should validate file size before sending request', async () => {
            // Given
            const customerId = 123;
            const largeContent = new Array(11 * 1024 * 1024).fill('a').join(''); // 11MB
            const largeFile = new File([largeContent], 'large.jpg', { type: 'image/jpeg' });
            const largeFormData = new FormData();
            largeFormData.append('file', largeFile);
            
            // When & Then
            await expect(uploadCustomerProfilePicture(customerId, largeFormData))
                .rejects.toThrow('File size too large');
            
            // Verify no request was made
            expect(mockedAxios.post).not.toHaveBeenCalled();
        });

        it('should allow skipping validation when specified', async () => {
            // Given
            const customerId = 123;
            const mockResponse = { data: { success: true } };
            const invalidFile = new File(['content'], 'test.txt', { type: 'text/plain' });
            const invalidFormData = new FormData();
            invalidFormData.append('file', invalidFile);
            mockedAxios.post.mockResolvedValue(mockResponse);
            
            // When
            const result = await uploadCustomerProfilePicture(customerId, invalidFormData, null, {
                validateFile: false
            });
            
            // Then
            expect(result).toEqual(mockResponse);
            expect(mockedAxios.post).toHaveBeenCalled();
        });
    });
});