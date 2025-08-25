package com.architos.auth;

public record AuthenticationRequest(
        String username,
        String password
) {
}
