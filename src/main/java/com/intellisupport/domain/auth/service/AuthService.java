package com.intellisupport.domain.auth.service;

import com.intellisupport.domain.auth.dto.AuthResponse;
import com.intellisupport.domain.auth.dto.RegisterRequest;

/**
 * Service contract for authentication workflows.
 */
public interface AuthService {

    /**
     * Registers a new user, hashes password, saves to database, and issues an access token.
     *
     * @param request Registration details
     * @return AuthResponse containing token and user profile
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates user with email and password, returning an access token.
     *
     * @param request Login credentials
     * @return AuthResponse containing token and user profile
     */
    AuthResponse login(com.intellisupport.domain.auth.dto.LoginRequest request);
}
