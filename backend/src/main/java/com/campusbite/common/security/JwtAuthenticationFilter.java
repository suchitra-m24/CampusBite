package com.campusbite.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.isTokenValid(token)) {
            respondUnauthorized(response, "TOKEN_EXPIRED", "Your session has expired. Please log in again.");
            return;
        }

        try {
            String loginId = jwtService.extractLoginId(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginId);
            if (!userDetails.isEnabled()) {
                SecurityContextHolder.clearContext();
                respondUnauthorized(response, "ACCOUNT_DISABLED", "Your account has been deactivated.");
                return;
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            respondUnauthorized(response, "INVALID_TOKEN", "Your session is no longer valid. Please log in again.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void respondUnauthorized(HttpServletResponse response, String errorCode, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":401,\"errorCode\":\"" + errorCode + "\",\"message\":\"" + message + "\"}");
    }
}
