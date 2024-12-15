package com.servicetransitcalculator;

import com.servicetransitcalculator.config.AuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Mockito.*;

class AuthenticationFilterTest {

    private AuthenticationFilter authenticationFilter;

    @Value("${servicetransitcalculator.api.tokens}")
    private String[] tokens = {"token1", "token2"};

    @BeforeEach
    void setUp() {
        authenticationFilter = new AuthenticationFilter(Arrays.asList(tokens));
    }

    @Test
    void shouldAllowRequestWithValidToken() throws IOException, jakarta.servlet.ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer token1");

        authenticationFilter.doFilter(request, response, filterChain); // Call doFilter instead

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void shouldBlockRequestWithInvalidToken() throws IOException, jakarta.servlet.ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer invalidToken");

        authenticationFilter.doFilter(request, response, filterChain); // Call doFilter instead

        verify(response, times(1)).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Invalid token");
        verify(filterChain, never()).doFilter(request, response);
    }
}