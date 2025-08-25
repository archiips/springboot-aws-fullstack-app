import {Form, Formik, useField} from 'formik';
import * as Yup from 'yup';
import {Alert, AlertIcon, Box, Button, FormLabel, Image, Input, Stack, VStack, Text, Progress, Spinner} from "@chakra-ui/react";
import {customerProfilePictureUrl, updateCustomer, uploadCustomerProfilePictureWithProgress, getErrorMessage} from "../../services/client.js";
import {errorNotification, successNotification} from "../../services/notification.js";
import {useCallback, useState} from "react";
import {useDropzone} from "react-dropzone";

const MyTextInput = ({label, ...props}) => {
    // useField() returns [formik.getFieldProps(), formik.getFieldMeta()]
    // which we can spread on <input>. We can use field meta to show an error
    // message if the field is invalid and it has been touched (i.e. visited)
    const [field, meta] = useField(props);
    return (
        <Box>
            <FormLabel htmlFor={props.id || props.name}>{label}</FormLabel>
            <Input className="text-input" {...field} {...props} />
            {meta.touched && meta.error ? (
                <Alert className="error" status={"error"} mt={2}>
                    <AlertIcon/>
                    {meta.error}
                </Alert>
            ) : null}
        </Box>
    );
};

// File validation constants
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

const MyDropzone = ({ customerId, fetchCustomers }) => {
    const [isUploading, setIsUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [validationError, setValidationError] = useState(null);
    const [uploadSpeed, setUploadSpeed] = useState(0);
    const [cancelUpload, setCancelUpload] = useState(null);

    const onDrop = useCallback(acceptedFiles => {
        const file = acceptedFiles[0];
        
        // Clear previous validation errors
        setValidationError(null);
        
        // Start upload process
        setIsUploading(true);
        setUploadProgress(0);
        setUploadSpeed(0);

        // Progress handler with enhanced information
        const onProgress = (progressInfo) => {
            setUploadProgress(progressInfo.progress);
            setUploadSpeed(progressInfo.speed);
        };

        // Cancel handler
        const onCancel = (cancelFn) => {
            setCancelUpload(() => cancelFn);
        };

        uploadCustomerProfilePictureWithProgress(
            customerId,
            file,
            onProgress,
            onCancel
        ).then(() => {
            successNotification("Success", "Profile picture uploaded successfully");
            fetchCustomers();
            setUploadProgress(100);
        }).catch((error) => {
            console.error("Upload error:", error);
            const errorInfo = getErrorMessage(error);
            errorNotification(errorInfo.title, errorInfo.description);
            setValidationError(errorInfo.description);
        }).finally(() => {
            setIsUploading(false);
            setUploadProgress(0);
            setUploadSpeed(0);
            setCancelUpload(null);
        });
    }, [customerId, fetchCustomers]);

    const handleCancelUpload = () => {
        if (cancelUpload) {
            cancelUpload();
        }
    };

    const {getRootProps, getInputProps, isDragActive} = useDropzone({
        onDrop,
        accept: {
            'image/*': ['.jpeg', '.jpg', '.png', '.gif', '.webp']
        },
        maxSize: MAX_FILE_SIZE,
        multiple: false,
        disabled: isUploading
    });

    return (
        <VStack spacing={3} w={'100%'}>
            <Box {...getRootProps()}
                 w={'100%'}
                 textAlign={'center'}
                 border={'dashed'}
                 borderColor={validationError ? 'red.300' : 'gray.200'}
                 borderRadius={'3xl'}
                 p={6}
                 rounded={'md'}
                 cursor={isUploading ? 'not-allowed' : 'pointer'}
                 opacity={isUploading ? 0.6 : 1}
                 _hover={!isUploading ? { borderColor: 'blue.300' } : {}}>
                <input {...getInputProps()} />
                {isUploading ? (
                    <VStack spacing={2}>
                        <Spinner size="md" color="blue.500" />
                        <Text fontSize="sm" color="blue.600">
                            Uploading... {uploadProgress}%
                        </Text>
                        {uploadSpeed > 0 && (
                            <Text fontSize="xs" color="gray.500">
                                Speed: {(uploadSpeed / (1024 * 1024)).toFixed(2)} MB/s
                            </Text>
                        )}
                        <Button size="sm" variant="outline" onClick={handleCancelUpload}>
                            Cancel Upload
                        </Button>
                    </VStack>
                ) : isDragActive ? (
                    <Text color="blue.600">Drop the picture here...</Text>
                ) : (
                    <VStack spacing={2}>
                        <Text>Drag 'n' drop picture here, or click to select</Text>
                        <Text fontSize="xs" color="gray.500">
                            Supports JPEG, PNG, GIF, WebP (max 10MB)
                        </Text>
                    </VStack>
                )}
            </Box>
            
            {isUploading && uploadProgress > 0 && (
                <Progress 
                    value={uploadProgress} 
                    w={'100%'} 
                    colorScheme="blue" 
                    size="sm" 
                    borderRadius="md"
                />
            )}
            
            {validationError && (
                <Alert status="error" borderRadius="md">
                    <AlertIcon />
                    <Text fontSize="sm">{validationError}</Text>
                </Alert>
            )}
        </VStack>
    );
}

// And now we can use these
const UpdateCustomerForm = ({fetchCustomers, initialValues, customerId}) => {
    return (
        <>
            <VStack spacing={'5'} mb={'5'}>
                <Image
                    borderRadius={'full'}
                    boxSize={'150px'}
                    objectFit={'cover'}
                    src={customerProfilePictureUrl(customerId)}
                />
                <MyDropzone
                    customerId={customerId}
                    fetchCustomers={fetchCustomers}
                />
            </VStack>
            <Formik
                initialValues={initialValues}
                validationSchema={Yup.object({
                    name: Yup.string()
                        .max(15, 'Must be 15 characters or less')
                        .required('Required'),
                    email: Yup.string()
                        .email('Must be 20 characters or less')
                        .required('Required'),
                    age: Yup.number()
                        .min(16, 'Must be at least 16 years of age')
                        .max(100, 'Must be less than 100 years of age')
                        .required(),
                })}
                onSubmit={(updatedCustomer, {setSubmitting}) => {
                    setSubmitting(true);
                    updateCustomer(customerId, updatedCustomer)
                        .then(res => {
                            console.log(res);
                            successNotification(
                                "Customer updated",
                                `${updatedCustomer.name} was successfully updated`
                            )
                            fetchCustomers();
                        }).catch(err => {
                        console.log(err);
                        errorNotification(
                            err.code,
                            err.response.data.message
                        )
                    }).finally(() => {
                        setSubmitting(false);
                    })
                }}
            >
                {({isValid, isSubmitting, dirty}) => (
                    <Form>
                        <Stack spacing={"24px"}>
                            <MyTextInput
                                label="Name"
                                name="name"
                                type="text"
                                placeholder="Jane"
                            />

                            <MyTextInput
                                label="Email Address"
                                name="email"
                                type="email"
                                placeholder="jane@formik.com"
                            />

                            <MyTextInput
                                label="Age"
                                name="age"
                                type="number"
                                placeholder="20"
                            />

                            <Button disabled={!(isValid && dirty) || isSubmitting} type="submit">Submit</Button>
                        </Stack>
                    </Form>
                )}
            </Formik>
        </>
    );
};

export default UpdateCustomerForm;