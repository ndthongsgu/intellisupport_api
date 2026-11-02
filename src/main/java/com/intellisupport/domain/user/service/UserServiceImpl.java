package com.intellisupport.domain.user.service;

import com.intellisupport.common.enums.ErrorCode;
import com.intellisupport.common.enums.UserRole;
import com.intellisupport.common.exception.DuplicateResourceException;
import com.intellisupport.common.exception.ForbiddenException;
import com.intellisupport.common.exception.ResourceNotFoundException;
import com.intellisupport.common.exception.UnauthorizedException;
import com.intellisupport.common.util.SecurityUtils;
import com.intellisupport.domain.user.dto.CreateUserRequest;
import com.intellisupport.domain.user.dto.UserResponse;
import com.intellisupport.domain.user.entity.User;
import com.intellisupport.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of UserService for user operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        return UserResponse.from(getCurrentUserEntity());
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUserEntity() {
        String email = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED, "User is not authenticated"));

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        String.format("User not found with email: '%s'", email)));
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        User currentUser = getCurrentUserEntity();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only administrators can create staff accounts");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("User creation rejected: email '{}' already in use", normalizedEmail);
            throw new DuplicateResourceException(ErrorCode.EMAIL_ALREADY_EXISTS,
                    String.format("Email '%s' is already registered", normalizedEmail));
        }

        UserRole targetRole = request.getRole() != null ? request.getRole() : UserRole.AGENT;

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(targetRole)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Admin '{}' successfully created new user: id={}, email={}, role={}",
                currentUser.getEmail(), savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        return UserResponse.from(savedUser);
    }
}
