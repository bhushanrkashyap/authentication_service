package com.example.authentication_module.Service;


import com.example.authentication_module.DTO.*;
import com.example.authentication_module.Exception.*;
import com.example.authentication_module.Repository.PasswordResetRepository;
import com.example.authentication_module.Repository.PendingRegistrationRepository;
import com.example.authentication_module.Repository.RefreshTokenRepository;
import com.example.authentication_module.Repository.UserRepository;
import com.example.authentication_module.mapper.UserMapper;
import com.example.authentication_module.model.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshRepo;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final EmailSenderService emailSenderService;
    private final PasswordResetRepository passwordResetRepository;
    private final RateLimiterService rateLimiterService;
    private final TokenRevocationService tokenRevocationService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    @Value("${app.ratelimit.login.max-attempts}")
    private int loginMaxAttempts;

    @Value("${app.ratelimit.login.window-minutes}")
    private long loginWindowMinutes;

    @Value("${app.ratelimit.forgot-password.max-attempts}")
    private int forgotPasswordMaxAttempts;

    @Value("${app.ratelimit.forgot-password.window-minutes}")
    private long forgotPasswordWindowMinutes;

    public UserService(
            UserRepository repo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            RefreshTokenRepository refreshRepo,
            PendingRegistrationRepository pendingRegistrationRepository,
            EmailSenderService emailSenderService ,
            PasswordResetRepository passwordResetRepository,
            RateLimiterService rateLimiterService,
            TokenRevocationService tokenRevocationService,
            GoogleTokenVerifierService googleTokenVerifierService) {

        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshRepo = refreshRepo;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.emailSenderService = emailSenderService;
        this.passwordResetRepository = passwordResetRepository;
        this.rateLimiterService = rateLimiterService;
        this.tokenRevocationService = tokenRevocationService;
        this.googleTokenVerifierService = googleTokenVerifierService;
    }

    public RegisterResponseDTO registerUser(RegisterRequestDTO requestDTO) {
        log.debug("Registration started for email: {}", requestDTO.getEmail());

        if (repo.existsByEmail(requestDTO.getEmail())) {
            throw new EmailAlreadyexistsException("Email already exists!");
        }
        if (pendingRegistrationRepository.existsByEmail(requestDTO.getEmail())) {
            throw new EmailAlreadyexistsException("Email already registered. Please verify the email we sent you, or wait for it to expire.");
        }
        if (!requestDTO.getPassword().equals(requestDTO.getConfirmPassword())) {
            throw new PasswordMismatchException("Password Mismatch!");
        }
        if (repo.existsByPhoneNumber((requestDTO.getPhoneNumber()))) {
            throw new PhoneNumberAlreadyExistsException("Phone number already exists!");
        }
        if (pendingRegistrationRepository.existsByPhoneNumber((requestDTO.getPhoneNumber()))) {
            throw new PhoneNumberAlreadyExistsException("Phone number already registered. Please verify the email we sent you, or wait for it to expire.");
        }

        // IMPORTANT: a normal `users` record is NOT created yet. The user must
        // first verify their email. Only then is the actual users row created.
        PendingRegistration pending = new PendingRegistration();
        pending.setUserName(requestDTO.getUserName());
        pending.setEmail(requestDTO.getEmail());
        pending.setPhoneNumber(requestDTO.getPhoneNumber());
        pending.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        pending.setDateOfBirth(requestDTO.getDateOfBirth());
        pending.setCity(requestDTO.getCity());
        pending.setState(requestDTO.getState());
        pending.setAddress(requestDTO.getAddress());
        pending.setCountry(requestDTO.getCountry());
        pending.setUsed(false);
        pending.setCreatedAt(LocalDateTime.now());

        String token = UUID.randomUUID().toString();
        pending.setToken(token);
        pending.setExpiryDate(LocalDateTime.now().plusHours(24));

        pendingRegistrationRepository.save(pending);
        log.debug("Pending registration saved for email: {}", requestDTO.getEmail());

        log.debug("Attempting to send verification email to: {}", requestDTO.getEmail());
        try {
            emailSenderService.sendVerificationEmail(
                    requestDTO.getEmail(),
                    token
            );
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", requestDTO.getEmail(), e.getMessage(), e);
            // do not rethrow - registration should succeed even if email fails
        }

        RegisterResponseDTO response = new RegisterResponseDTO();
        response.setId(null);
        response.setUsername(requestDTO.getUserName());
        response.setEmail(requestDTO.getEmail());
        response.setRole(Role.Roles.USER);
        response.setRegisteredAt(LocalDateTime.now());
        return response;
    }

    public LoginResponseDTO loginUser(LoginRequestDTO requestDTO) {

        UserModel user = null;
        if (requestDTO.getEmail() != null && !requestDTO.getEmail().isBlank()) {
            user = repo.findByEmail(requestDTO.getEmail()).orElse(null);
        }
        if (user == null && requestDTO.getPhoneNumber() != null && !requestDTO.getPhoneNumber().isBlank()) {
            user = repo.findByPhoneNumber(requestDTO.getPhoneNumber()).orElse(null);
        }
        if (user == null && requestDTO.getUsername() != null && !requestDTO.getUsername().isBlank()) {
            user = repo.findByUsername(requestDTO.getUsername()).orElse(null);
        }
        if (user == null) {
            throw new UserNotFoundException("User not found!");
        }

        if (!user.isEmailVerified()) {
            throw new AccountLockedException("Please verify your email before logging in.");
        }

        String loginFailKey = "ratelimit:login-fail:" + user.getEmail();

        if (rateLimiterService.getCount(loginFailKey) >= loginMaxAttempts) {
            throw new AccountLockedException(
                    "Too many failed login attempts. Please try again later.");
        }

        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            rateLimiterService.increment(loginFailKey, Duration.ofMinutes(loginWindowMinutes));
            throw new PasswordMismatchException("Password mismatch!");
        }

        rateLimiterService.reset(loginFailKey);

        if (!user.isEnabled()) {
            throw new AccountLockedException("Account is disabled!");
        }

        if (!user.isAccountNonLocked()) {
            throw new AccountLockedException("Account is locked!");
        }

        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("Credentials expired!");
        }

        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException("Account expired!");
        }
        LocalDateTime now = LocalDateTime.now();

        user.setLoginTime(now);
        user.setLastAccessedAt(now);

        repo.save(user);

        String accessToken = jwtService.generateToken(user);

        RefreshToken refreshToken =
                refreshTokenService.createrefreshToken(user);

        LoginResponseDTO responseDTO = new LoginResponseDTO();

        responseDTO.setAccessToken(accessToken);
        responseDTO.setRefreshToken(refreshToken.getToken());
        responseDTO.setExpiresIn(jwtService.getAccessTokenExpiryInSeconds());

        return responseDTO;
    }

    /**
     * SSO login via Google. The idToken has already been verified by
     * GoogleTokenVerifierService (signature, issuer, audience, email
     * verified) by the time we get here. Google-provisioned accounts skip
     * password/email-verification checks entirely since Google already
     * vouched for the email; profile fields Google doesn't supply (phone,
     * address, DOB...) stay null until the user fills them in via
     * /profile/update.
     */
    public LoginResponseDTO loginWithGoogle(GoogleLoginRequestDTO request) {

        GoogleUserInfo googleUser = googleTokenVerifierService.verify(request.getIdToken());

        UserModel user = repo.findByEmail(googleUser.email())
                .orElseGet(() -> provisionGoogleUser(googleUser));

        if (!user.isEnabled()) {
            throw new AccountLockedException("Account is disabled!");
        }
        if (!user.isAccountNonLocked()) {
            throw new AccountLockedException("Account is locked!");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setLoginTime(now);
        user.setLastAccessedAt(now);
        repo.save(user);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createrefreshToken(user);

        LoginResponseDTO responseDTO = new LoginResponseDTO();
        responseDTO.setAccessToken(accessToken);
        responseDTO.setRefreshToken(refreshToken.getToken());
        responseDTO.setExpiresIn(jwtService.getAccessTokenExpiryInSeconds());

        return responseDTO;
    }

    private UserModel provisionGoogleUser(GoogleUserInfo googleUser) {
        if (repo.existsByEmail(googleUser.email())) {
            return repo.findByEmail(googleUser.email())
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
        }

        UserModel user = new UserModel();
        user.setUsername(
                googleUser.name() != null && !googleUser.name().isBlank()
                        ? googleUser.name()
                        : googleUser.email());
        user.setEmail(googleUser.email());
        // Random, never-shown password: this account can only be accessed via
        // Google sign-in until the user sets a password through the normal
        // "forgot password" flow.
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(Role.Roles.USER);
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        user.setCredentialsNonExpired(true);
        user.setEmailVerified(true);

        return repo.save(user);
    }


    public ProfileResponseDTO updateProfile(String email,
                                            UpdateProfileRequestDTO dto) {

        UserModel user = repo.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setUsername(dto.getUsername());

        user.setPhoneNumber(dto.getPhoneNumber());
        user.setCity(dto.getCity());
        user.setState(dto.getState());
        user.setCountry(dto.getCountry());
        user.setAddress(dto.getAddress());

        UserModel updatedUser = repo.save(user);

        log.debug("Profile updated for user: {}", email);

        return new ProfileResponseDTO(
                updatedUser.getUsername(),
                updatedUser.getEmail(),
                updatedUser.getRole(),
                updatedUser.getPhoneNumber()
        );
    }


    public MessageResponseDTO changePassword(
            String email,
            ChangePasswordRequestDTO request) {

        UserModel user = repo.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword())) {

            throw new PasswordMismatchException("Current password is incorrect");
        }

        if (!request.getNewPassword()
                .equals(request.getConfirmNewPassword())) {

            throw new PasswordMismatchException("Passwords do not match");
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword()));

        repo.save(user);
        tokenRevocationService.revokeAllTokens(user.getId());

        return new MessageResponseDTO("Password changed successfully");
    }


    @Transactional
    public void logout(String refreshTokenValue) {

        RefreshToken refreshToken = refreshRepo
                .findByToken(refreshTokenValue)
                .orElseThrow(() ->
                        new InvalidCredentialsException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            throw new InvalidCredentialsException("Already logged out");
        }

        refreshToken.setRevoked(true);

        refreshRepo.save(refreshToken);
        tokenRevocationService.revokeAllTokens(refreshToken.getUser().getId());
    }
    public MessageResponseDTO verifyEmail(String token) {

        PendingRegistration pending =
                pendingRegistrationRepository.findByToken(token)
                        .orElseThrow(() ->
                                new InvalidVerificationTokenException("Invalid verification token"));

        if (pending.isUsed()) {
            throw new InvalidVerificationTokenException("Verification token has already been used.");
        }

        if (pending.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ExpiredVerificationTokenException("Verification token has expired.");
        }

        // The user has verified their email. Create the actual users record now.
        UserModel user = new UserModel();
        user.setUsername(pending.getUserName());
        user.setEmail(pending.getEmail());
        user.setPhoneNumber(pending.getPhoneNumber());
        user.setPassword(pending.getPassword()); // already BCrypt-hashed
        user.setDateOfBirth(pending.getDateOfBirth());
        user.setCity(pending.getCity());
        user.setState(pending.getState());
        user.setAddress(pending.getAddress());
        user.setCountry(pending.getCountry());
        user.setRole(Role.Roles.USER);
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        user.setCredentialsNonExpired(true);
        user.setEmailVerified(true);
        repo.save(user);

        pending.setUsed(true);
        pendingRegistrationRepository.save(pending);

        return new MessageResponseDTO("Email verified successfully.");
    }
    public MessageResponseDTO forgotPassword(ForgotPasswordRequestDTO request) {

        String key = "ratelimit:email:forgot-password:" + request.getEmail();

        if (!rateLimiterService.isAllowed(
                key, forgotPasswordMaxAttempts, Duration.ofMinutes(forgotPasswordWindowMinutes))) {

            return new MessageResponseDTO(
                    "If the email exists, a password reset link has been sent."
            );
        }

        Optional<UserModel> optionalUser = repo.findByEmail(request.getEmail());

        if (optionalUser.isEmpty()) {
            return new MessageResponseDTO(
                    "If the email exists, a password reset link has been sent."
            );
        }

        UserModel user = optionalUser.get();

        String token = UUID.randomUUID().toString();

        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();

        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setUsed(false);
        resetToken.setExpiryDate(
                LocalDateTime.now().plusMinutes(30)
        );

        passwordResetRepository.save(resetToken);

        try {
            emailSenderService.sendPasswordResetEmail(
                    user.getEmail(),
                    token
            );
        } catch (org.springframework.mail.MailException e) {
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage(), e);
        }

        return new MessageResponseDTO(
                "If the email exists, a password reset link has been sent."
        );
    }

    public MessageResponseDTO resetPassword(ResetPasswordRequestDTO request) {

        PasswordResetTokenEntity resetToken =
                passwordResetRepository.findByToken(request.getToken())
                        .orElseThrow(() ->
                                new InvalidPasswordResetTokenException("Invalid reset token."));

        if (resetToken.isUsed()) {
            throw new InvalidPasswordResetTokenException("Reset token has already been used.");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ExpiredPasswordResetTokenException("Reset token has expired.");
        }

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords do not match.");
        }

        UserModel user = resetToken.getUser();

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        repo.save(user);
        List<RefreshToken> refreshTokens =
                refreshRepo.findAllByUser(user);

        for (RefreshToken token : refreshTokens) {
            token.setRevoked(true);
        }

        refreshRepo.saveAll(refreshTokens);
        resetToken.setUsed(true);
        passwordResetRepository.save(resetToken);
        tokenRevocationService.revokeAllTokens(user.getId());

        return new MessageResponseDTO("Password reset successfully.");
    }
}