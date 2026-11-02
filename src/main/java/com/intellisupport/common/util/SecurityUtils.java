package com.intellisupport.common.util;

import com.intellisupport.common.enums.UserRole;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Utility methods for accessing the current Spring Security context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Prevent instantiation
    }

    /**
     * Gets the current Authentication object from SecurityContext.
     */
    public static Optional<Authentication> getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }

    /**
     * Retrieves the username/email of the currently authenticated user.
     */
    public static Optional<String> getCurrentUserEmail() {
        return getAuthentication().map(authentication -> {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                return userDetails.getUsername();
            } else if (principal instanceof String principalString) {
                return principalString;
            }
            return null;
        });
    }

    /**
     * Checks if a user is currently authenticated.
     */
    public static boolean isAuthenticated() {
        return getAuthentication().isPresent();
    }

    /**
     * Checks if current user has a specific role.
     */
    public static boolean hasRole(UserRole role) {
        if (role == null) {
            return false;
        }
        String expectedAuthority = "ROLE_" + role.name();
        return getAuthentication().map(auth ->
                auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> a.equals(expectedAuthority) || a.equals(role.name()))
        ).orElse(false);
    }
}
