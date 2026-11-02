package com.intellisupport.domain.user.service;

import com.intellisupport.domain.user.dto.CreateUserRequest;
import com.intellisupport.domain.user.dto.UserResponse;
import com.intellisupport.domain.user.entity.User;

/**
 * Service contract for user management operations.
 */
public interface UserService {

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    UserResponse getCurrentUserProfile();

    /**
     * Retrieves the entity of the currently authenticated user.
     */
    User getCurrentUserEntity();

    /**
     * Creates a new user/staff account (restricted to administrators).
     */
    UserResponse createUser(CreateUserRequest request);
}
