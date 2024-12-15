package com.servicetransitcalculator.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filter to handle API authentication via token validation.
 */
@Configuration
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final Set<String> validTokens;

    public AuthenticationFilter(@Value("${servicetransitcalculator.api.tokens}") List<String> tokens) {
        this.validTokens = Collections.unmodifiableSet(new HashSet<>(tokens));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !isTokenValid(authorizationHeader)) {
            logger.warn("Unauthorized access attempt: {}", request.getRequestURI());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized: Invalid token");
            return;
        }

        logger.debug("Authorized request: {}", request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    private boolean isTokenValid(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // Remove "Bearer " prefix
        }
        return validTokens.contains(token);
    }
}