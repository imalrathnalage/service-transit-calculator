package com.servicetransitcalculator.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Filter to log request and response details for debugging and tracing purposes.
 */
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Value("${logging.request-response.enabled:false}")
    private boolean loggingEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString(); // Generate unique ID for tracing

        if (loggingEnabled) {
            logRequestDetails(request, requestId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (loggingEnabled) {
                logResponseDetails(response, requestId);
            }
        }
    }

    private void logRequestDetails(HttpServletRequest request, String requestId) {
        logger.info("Request ID: {} | Incoming Request: {}", requestId, request.getRequestURI());
        logger.info("Method: {}", request.getMethod());
        logger.info("Headers:");
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            Collections.list(headerNames).forEach(headerName ->
                    logger.info("  {}: {}", headerName, request.getHeader(headerName))
            );
        }
    }

    private void logResponseDetails(HttpServletResponse response, String requestId) {
        logger.info("Request ID: {} | Outgoing Response Status: {}", requestId, response.getStatus());
    }
}