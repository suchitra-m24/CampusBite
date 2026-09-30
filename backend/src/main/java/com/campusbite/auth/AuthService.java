package com.campusbite.auth;

import com.campusbite.user.PaymentMode;
import com.campusbite.auth.dto.AuthResponse;
import com.campusbite.auth.dto.CreateAccountRequest;
import com.campusbite.auth.dto.ForgotPasswordRequest;
import com.campusbite.auth.dto.ForgotPasswordResponse;
import com.campusbite.auth.dto.LoginRequest;
import com.campusbite.auth.dto.RefreshRequest;
import com.campusbite.auth.dto.RegisterRequest;
import com.campusbite.auth.dto.ResetPasswordRequest;
import com.campusbite.auth.dto.SessionDto;
import com.campusbite.auth.dto.VerifyEmailRequest;
import com.campusbite.common.exception.BadRequestException;
import com.campusbite.common.exception.ConflictException;
import com.campusbite.common.exception.ResourceNotFoundException;
import com.campusbite.common.exception.UnauthorizedException;
import com.campusbite.common.security.JwtService;
import com.campusbite.common.util.TokenGenerator;
import com.campusbite.common.util.TokenHashUtil;
import com.campusbite.trust.TrustScoreService;
import com.campusbite.user.Role;
import com.campusbite.user.User;
import com.campusbite.user.UserDto;
import com.campusbite.user.UserRepository;
import com.campusbite.wallet.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuthService {

    private static final int EMAIL_VERIFICATION_EXPIRY_HOURS = 24;
    private static final int PASSWORD_RESET_EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TrustScoreService trustScoreService;
    private final WalletService walletService;
    private final long refreshTokenExpiryMs;
    private final String frontendBaseUrl;
    private final boolean devResetLinksEnabled;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            TrustScoreService trustScoreService,
            WalletService walletService,
            @Value("${campusbite.jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs,
            @Value("${campusbite.frontend-base-url:http://localhost:5500}") String frontendBaseUrl,
            @Value("${campusbite.auth.dev-reset-links-enabled:false}") boolean devResetLinksEnabled) {

        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.trustScoreService = trustScoreService;
        this.walletService = walletService;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
        this.frontendBaseUrl = frontendBaseUrl;
        this.devResetLinksEnabled = devResetLinksEnabled;
    }


    // =====================================================
    // PUBLIC REGISTRATION
    // =====================================================

    @Transactional
    public AuthResponse register(
            RegisterRequest request,
            HttpServletRequest httpRequest) {

        /*
         * Only STUDENT and TEACHER can self-register.
         *
         * STAFF and ADMIN accounts represent privileged /
         * operational access and must be created through the
         * admin-only account provisioning flow.
         *
         * This check is intentionally performed on the backend
         * so that a direct API request cannot bypass the frontend.
         */

        if (request.role() != Role.STUDENT) {
            throw new BadRequestException(
                    "Only student accounts can be self-registered. " +
                            "Staff, teacher and admin accounts must be provisioned by an administrator."
            );
        }


        if (userRepository.existsByEmail(
                request.email().toLowerCase())) {

            throw new ConflictException(
                    "An account with this email already exists"
            );
        }


        if (userRepository.existsByLoginId(
                request.loginId())) {

            throw new ConflictException(
                    "This login ID is already taken"
            );
        }


        if (request.role() == Role.STUDENT) {
            if (isBlank(request.department()) || isBlank(request.academicYear()) || isBlank(request.section())) {
                throw new BadRequestException("Students must provide department, academic year and section.");
            }
        }

        User user = new User(
                request.fullName(),
                request.email().toLowerCase(),
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.role()
        );
        user.setDepartment(trimToNull(request.department()));
        user.setAcademicYear(trimToNull(request.academicYear()));
        user.setSection(trimToNull(request.section()));


        user = userRepository.save(user);


        /*
         * Initialize trust score for students and teachers.
         */

        trustScoreService.initializeFor(user);


        /*
         * Students receive a wallet.
         */

        if (user.getRole() == Role.STUDENT) {

            walletService.initializeFor(user);
        }


        /*
         * Create email verification link.
         */

        String verificationLink =
                devResetLinksEnabled ? createEmailVerificationLink(user) : null;


        /*
         * Issue authentication tokens for normal
         * student / teacher self-registration.
         */

        AuthResponse tokens =
                issueTokens(user, httpRequest);


        return new AuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.user(),
                verificationLink
        );
    }


    // =====================================================
    // ADMIN-CONTROLLED STAFF / ADMIN ACCOUNT CREATION
    // =====================================================

    /**
     * The controlled counterpart to public registration.
     *
     * An already-authenticated administrator can use this
     * method to create a STAFF or ADMIN account.
     *
     * No session is issued for the newly created account.
     * The administrator is only provisioning credentials;
     * the new user will log in separately.
     */
    @Transactional
    public UserDto createPrivilegedAccount(
            CreateAccountRequest request) {

        if (request.role() != Role.ADMIN && request.role() != Role.STAFF && request.role() != Role.TEACHER) {
            throw new BadRequestException("Admin provisioning is only for STAFF, TEACHER or ADMIN accounts");
        }

        /*
         * Prevent duplicate email addresses.
         */

        if (userRepository.existsByEmail(
                request.email().toLowerCase())) {

            throw new ConflictException(
                    "An account with this email already exists"
            );
        }


        /*
         * Prevent duplicate Login IDs.
         */

        if (userRepository.existsByLoginId(
                request.loginId())) {

            throw new ConflictException(
                    "This login ID is already taken"
            );
        }


        /*
         * Create the account with an encoded password.
         */

        User user = new User(
                request.fullName(),
                request.email().toLowerCase(),
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.role()
        );


        /*
         * Save and return the newly created user.
         *
         * No access token or refresh token is issued here.
         */

        user = userRepository.save(user);
        if (user.getRole() == Role.TEACHER) {
            user.setPaymentMode(PaymentMode.POSTPAID);
            trustScoreService.initializeFor(user);
            user = userRepository.save(user);
        }
        return UserDto.from(user);
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @Transactional
    public AuthResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.loginId(),
                        request.password()
                )
        );


        User user =
                userRepository.findByLoginId(
                        request.loginId()
                ).orElseThrow(
                        () -> new UnauthorizedException(
                                "Invalid login ID or password"
                        )
                );


        if (!user.isActive()) {

            throw new UnauthorizedException(
                    "This account has been deactivated. " +
                            "Contact an administrator."
            );
        }


        return issueTokens(
                user,
                httpRequest
        );
    }


    // =====================================================
    // REFRESH TOKEN
    // =====================================================

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refresh(
            RefreshRequest request,
            HttpServletRequest httpRequest) {

        RefreshToken existing =
                refreshTokenRepository
                        .findByTokenForUpdate(TokenHashUtil.sha256(request.refreshToken()))
                        .orElseThrow(
                                () -> new UnauthorizedException(
                                        "Invalid refresh token"
                                )
                        );


        if (existing.isRevoked()) {

            /*
             * Reuse of a revoked token is a strong signal
             * of theft: revoke the whole chain by revoking
             * every active token for this user.
             */

            revokeAllForUser(existing.getUser());

            throw new UnauthorizedException(
                    "Refresh token has already been used. " +
                            "Please log in again."
            );
        }


        if (existing.isExpired()) {

            throw new UnauthorizedException(
                    "Refresh token has expired. " +
                            "Please log in again."
            );
        }


        User user =
                existing.getUser();

        if (!user.isActive()) {
            revokeAllForUser(user);
            throw new UnauthorizedException("This account has been deactivated. Contact an administrator.");
        }

        /*
         * Rotate:
         * revoke the old token and mint a new pair.
         */

        RefreshToken newRefreshToken =
                createRefreshToken(
                        user,
                        httpRequest
                );


        existing.setRevoked(true);

        existing.setReplacedByTokenHash(
                newRefreshToken.getTokenHash()
        );

        refreshTokenRepository.save(existing);


        String accessToken =
                jwtService.generateAccessToken(
                        user.getId(),
                        user.getLoginId(),
                        user.getRole().name()
                );


        return new AuthResponse(
                accessToken,
                newRefreshToken.getToken(),
                UserDto.from(user),
                null
        );
    }


    // =====================================================
    // LOGOUT
    // =====================================================

    @Transactional
    public void logout(
            RefreshRequest request) {

        refreshTokenRepository
                .findByTokenHash(TokenHashUtil.sha256(request.refreshToken()))
                .ifPresent(token -> {

                    token.setRevoked(true);

                    refreshTokenRepository.save(token);
                });
    }


    // =====================================================
    // EMAIL VERIFICATION
    // =====================================================

    @Transactional
    public String resendVerification(User user) {

        if (user.isEmailVerified()) {

            throw new BadRequestException(
                    "This email is already verified"
            );
        }


        return devResetLinksEnabled ? createEmailVerificationLink(user) : null;
    }


    @Transactional
    public void verifyEmail(
            VerifyEmailRequest request) {

        EmailVerificationToken token =
                emailVerificationTokenRepository
                        .findByTokenForUpdate(TokenHashUtil.sha256(request.token()))
                        .orElseThrow(
                                () -> new BadRequestException(
                                        "Invalid or expired verification link"
                                )
                        );


        if (!token.isValid()) {

            throw new BadRequestException(
                    "This verification link has expired. " +
                            "Request a new one."
            );
        }


        User user =
                token.getUser();


        user.setEmailVerified(true);

        userRepository.save(user);


        token.setUsed(true);

        emailVerificationTokenRepository.save(token);
    }


    private String createEmailVerificationLink(
            User user) {

        String tokenValue =
                TokenGenerator.generateOpaqueToken();


        Instant expiresAt =
                Instant.now().plus(
                        EMAIL_VERIFICATION_EXPIRY_HOURS,
                        ChronoUnit.HOURS
                );


        emailVerificationTokenRepository.save(
                new EmailVerificationToken(
                        user,
                        tokenValue,
                        expiresAt
                )
        );


        return frontendBaseUrl +
                "/verify-email.html?token=" +
                tokenValue;
    }


    // =====================================================
    // PASSWORD RESET
    // =====================================================

    @Transactional
    public ForgotPasswordResponse forgotPassword(
            ForgotPasswordRequest request) {

        User user = userRepository.findByLoginId(request.loginId()).orElse(null);
        String message = "If that Login ID exists, password reset instructions have been requested.";

        // Production never exposes whether an account exists. Local development can
        // explicitly opt in because this project does not wire an email provider.
        if (user == null || !devResetLinksEnabled) {
            return new ForgotPasswordResponse(message, null);
        }

        String tokenValue = TokenGenerator.generateOpaqueToken();
        Instant expiresAt = Instant.now().plus(
                PASSWORD_RESET_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        passwordResetTokenRepository.save(
                new PasswordResetToken(user, tokenValue, expiresAt));

        String link = frontendBaseUrl + "/reset-password.html?token=" + tokenValue;
        return new ForgotPasswordResponse(message, link);
    }


    @Transactional
    public void resetPassword(
            ResetPasswordRequest request) {

        PasswordResetToken token =
                passwordResetTokenRepository
                        .findByTokenForUpdate(TokenHashUtil.sha256(request.token()))
                        .orElseThrow(
                                () -> new BadRequestException(
                                        "Invalid or expired reset link"
                                )
                        );


        if (!token.isValid()) {

            throw new BadRequestException(
                    "This reset link has expired. " +
                            "Request a new one."
            );
        }


        User user =
                token.getUser();


        user.setPasswordHash(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );


        userRepository.save(user);


        token.setUsed(true);

        passwordResetTokenRepository.save(token);


        /*
         * A password reset is a strong security event --
         * sign out every other session.
         */

        revokeAllForUser(user);
    }


    // =====================================================
    // SESSION MANAGEMENT
    // =====================================================

    public List<SessionDto> listSessions(
            User user,
            String currentRefreshToken) {

        return refreshTokenRepository
                .findByUserAndRevokedFalse(user)
                .stream()
                .filter(RefreshToken::isActive)
                .map(t ->
                        SessionDto.from(
                                t,
                                t.getTokenHash().equals(
                                        TokenHashUtil.sha256(currentRefreshToken == null ? "" : currentRefreshToken)
                                )
                        )
                )
                .toList();
    }


    @Transactional
    public void revokeSession(
            User user,
            Long sessionId) {

        RefreshToken token =
                refreshTokenRepository
                        .findById(sessionId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Session not found"
                                )
                        );


        if (!token.getUser().getId().equals(
                user.getId())) {

            throw new BadRequestException(
                    "You do not have access to this session"
            );
        }


        token.setRevoked(true);

        refreshTokenRepository.save(token);
    }


    // =====================================================
    // HELPERS
    // =====================================================

    private AuthResponse issueTokens(
            User user,
            HttpServletRequest httpRequest) {

        String accessToken =
                jwtService.generateAccessToken(
                        user.getId(),
                        user.getLoginId(),
                        user.getRole().name()
                );


        RefreshToken refreshToken =
                createRefreshToken(
                        user,
                        httpRequest
                );


        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                UserDto.from(user),
                null
        );
    }


    private RefreshToken createRefreshToken(
            User user,
            HttpServletRequest httpRequest) {

        String tokenValue =
                TokenGenerator.generateOpaqueToken();


        String userAgent =
                httpRequest != null
                        ? httpRequest.getHeader("User-Agent")
                        : null;


        RefreshToken refreshToken =
                new RefreshToken(
                        tokenValue,
                        user,
                        Instant.now().plusMillis(
                                refreshTokenExpiryMs
                        ),
                        userAgent
                );


        return refreshTokenRepository.save(
                refreshToken
        );
    }


    @Transactional
    public void revokeAllForUser(User user) {

        refreshTokenRepository
                .findByUserAndRevokedFalse(user)
                .forEach(t -> {

                    t.setRevoked(true);

                    refreshTokenRepository.save(t);
                });
    }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private String trimToNull(String value) { return isBlank(value) ? null : value.trim(); }

}

