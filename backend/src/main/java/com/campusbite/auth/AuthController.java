package com.campusbite.auth;

import com.campusbite.auth.dto.AuthResponse;
import com.campusbite.auth.dto.CreateAccountRequest;
import com.campusbite.user.UserDto;
import com.campusbite.auth.dto.ForgotPasswordRequest;
import com.campusbite.auth.dto.ForgotPasswordResponse;
import com.campusbite.auth.dto.LoginRequest;
import com.campusbite.auth.dto.RefreshRequest;
import com.campusbite.auth.dto.RegisterRequest;
import com.campusbite.auth.dto.ResetPasswordRequest;
import com.campusbite.auth.dto.RevokeSessionRequest;
import com.campusbite.auth.dto.SessionDto;
import com.campusbite.auth.dto.VerifyEmailRequest;
import com.campusbite.common.exception.UnauthorizedException;
import com.campusbite.common.security.SecurityUser;
import com.campusbite.common.security.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * The refresh token is deliberately NEVER present in any JSON response
 * body here -- it's set as an httpOnly cookie instead, so it's invisible
 * to JavaScript entirely (immune to theft via XSS, unlike the access
 * token or anything stored in localStorage). AuthService is untouched by
 * this: it still returns the token value internally, this controller
 * layer is what intercepts it and writes it to a cookie instead of
 * passing it through to the client's response body.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final long refreshTokenExpiryMs;
    private final boolean cookieSecure;
    private final RateLimitService rateLimitService;

    public AuthController(AuthService authService,
                          @Value("${campusbite.jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs,
                          @Value("${campusbite.cookie.secure}") boolean cookieSecure,
                          RateLimitService rateLimitService) {
        this.authService = authService;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
        this.cookieSecure = cookieSecure;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/admin/create-account")
    public ResponseEntity<UserDto> createPrivilegedAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.ok(authService.createPrivilegedAccount(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest,
                                                 HttpServletResponse response) {
        rateLimitService.check("register:" + httpRequest.getRemoteAddr(), 5);
        AuthResponse serviceResponse = authService.register(request, httpRequest);
        setRefreshCookie(response, serviceResponse.refreshToken());
        return ResponseEntity.ok(stripRefreshToken(serviceResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse response) {
        rateLimitService.check("login:" + httpRequest.getRemoteAddr(), 10);
        AuthResponse serviceResponse = authService.login(request, httpRequest);
        setRefreshCookie(response, serviceResponse.refreshToken());
        return ResponseEntity.ok(stripRefreshToken(serviceResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = COOKIE_NAME, required = false) String refreshTokenCookie,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse response) {
        if (refreshTokenCookie == null) {
            throw new UnauthorizedException("No refresh token present. Please log in again.");
        }
        AuthResponse serviceResponse = authService.refresh(new RefreshRequest(refreshTokenCookie), httpRequest);
        setRefreshCookie(response, serviceResponse.refreshToken());
        return ResponseEntity.ok(stripRefreshToken(serviceResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE_NAME, required = false) String refreshTokenCookie,
                                       HttpServletResponse response) {
        if (refreshTokenCookie != null) {
            authService.logout(new RefreshRequest(refreshTokenCookie));
        }
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerification(@AuthenticationPrincipal SecurityUser principal) {
        String link = authService.resendVerification(principal.getUser());
        return ResponseEntity.ok(new AuthResponse(null, null, null, link));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                                   HttpServletRequest httpRequest) {
        rateLimitService.check("forgot:" + httpRequest.getRemoteAddr(), 5);
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions")
    public ResponseEntity<List<SessionDto>> listSessions(@AuthenticationPrincipal SecurityUser principal,
                                                         @CookieValue(name = COOKIE_NAME, required = false) String refreshTokenCookie) {
        return ResponseEntity.ok(authService.listSessions(principal.getUser(), refreshTokenCookie));
    }

    @PostMapping("/sessions/revoke")
    public ResponseEntity<Void> revokeSession(@AuthenticationPrincipal SecurityUser principal,
                                              @Valid @RequestBody RevokeSessionRequest request) {
        authService.revokeSession(principal.getUser(), request.sessionId());
        return ResponseEntity.noContent().build();
    }

    // ---- cookie helpers ----

    private void setRefreshCookie(HttpServletResponse response, String token) {
        if (token == null) return;
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofMillis(refreshTokenExpiryMs))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /** The refresh token must never reach the client in a JSON body -- only ever via the httpOnly cookie above. */
    private AuthResponse stripRefreshToken(AuthResponse response) {
        return new AuthResponse(response.accessToken(), null, response.user(), response.devVerificationLink());
    }
}