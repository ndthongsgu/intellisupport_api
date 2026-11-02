package com.intellisupport.domain.auth.service;

import com.intellisupport.common.enums.ErrorCode;
import com.intellisupport.common.enums.UserRole;
import com.intellisupport.common.exception.DuplicateResourceException;
import com.intellisupport.domain.auth.dto.AuthResponse;
import com.intellisupport.domain.auth.dto.RegisterRequest;
import com.intellisupport.domain.auth.security.JwtTokenProvider;
import com.intellisupport.domain.user.dto.UserResponse;
import com.intellisupport.domain.user.entity.User;
import com.intellisupport.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AuthService handling user registration and JWT issuance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration rejected: email '{}' already in use", normalizedEmail);
            throw new DuplicateResourceException(ErrorCode.EMAIL_ALREADY_EXISTS,
                    String.format("Email '%s' is already registered", normalizedEmail));
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered new user: id={}, email={}, role={}", savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        String accessToken = jwtTokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .user(UserResponse.from(savedUser))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(com.intellisupport.domain.auth.dto.LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new com.intellisupport.common.exception.UnauthorizedException(
                        ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for email: '{}'", normalizedEmail);
            throw new com.intellisupport.common.exception.UnauthorizedException(
                    ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        log.info("User logged in successfully: id={}, email={}", user.getId(), user.getEmail());
        String accessToken = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .user(UserResponse.from(user))
                .build();
    }
}
