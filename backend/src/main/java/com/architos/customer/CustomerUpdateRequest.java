package com.architos.customer;

public record CustomerUpdateRequest(
        String name,
        String email,
        Integer age
) {
}
