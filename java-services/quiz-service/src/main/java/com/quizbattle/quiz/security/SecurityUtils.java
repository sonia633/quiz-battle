package com.quizbattle.quiz.security;

import com.quizbattle.quiz.exception.UnauthorizedException;
import org.springframework.security.core.context.SecurityContextHolder;

/** Helpers to read the authenticated user id from the security context. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new UnauthorizedException("No authenticated user");
        }
        try {
            return Long.parseLong(auth.getPrincipal().toString());
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("Invalid principal");
        }
    }
}
