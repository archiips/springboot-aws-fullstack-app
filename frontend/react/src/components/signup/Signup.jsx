import {useAuth} from "../context/AuthContext.jsx";
import {useNavigate} from "react-router-dom";
import {useEffect} from "react";
import {Flex, Heading, Image, Link, Stack} from "@chakra-ui/react";
import CreateCustomerForm from "../shared/CreateCustomerForm.jsx";

const Signup = () => {
    const { customer, setCustomerFromToken } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (customer) {
            navigate("/dashboard/customers");
        }
    })

    return (
        <Flex minH={'100vh'} alignItems={'center'} justifyContent={'center'}>
            <Stack spacing={4} w={'full'} maxW={'md'} p={8}>

                <Heading fontSize={'2xl'} mb={15}>Register for an account</Heading>
                <CreateCustomerForm onSuccess={(token) => {
                    localStorage.setItem("access_token", token)
                    setCustomerFromToken()
                    navigate("/dashboard");
                }}/>
                <Link color={"blue.500"} href={"/"}>
                    Have an account? Login now.
                </Link>
            </Stack>
        </Flex>
    );
}

export default Signup;