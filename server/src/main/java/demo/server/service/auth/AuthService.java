package demo.server.service.auth;

import demo.server.common.enums.RoleCode;
import demo.server.common.enums.UserStatus;
import demo.server.common.security.AuthTokenProperties;
import demo.server.common.security.CurrentUserProvider;
import demo.server.common.security.JwtProperties;
import demo.server.common.security.JwtService;
import demo.server.common.security.SecureTokenGenerator;
import demo.server.common.security.TokenHashService;
import demo.server.dto.auth.AuthResponse;
import demo.server.dto.auth.ChangePasswordRequest;
import demo.server.dto.auth.CurrentUserResponse;
import demo.server.dto.auth.ForgotPasswordRequest;
import demo.server.dto.auth.LoginRequest;
import demo.server.dto.auth.LogoutRequest;
import demo.server.dto.auth.RefreshTokenRequest;
import demo.server.dto.auth.RegisterGamerRequest;
import demo.server.dto.auth.ResetPasswordRequest;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.PasswordResetToken;
import demo.server.entity.auth.RefreshToken;
import demo.server.entity.auth.Role;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.DuplicateResourceException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.TokenException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.PasswordResetTokenRepository;
import demo.server.repository.auth.RefreshTokenRepository;
import demo.server.repository.auth.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthTokenProperties tokenProperties;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHashService tokenHashService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthMapper authMapper;

    public AuthService(
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AuthTokenProperties tokenProperties,
            SecureTokenGenerator tokenGenerator,
            TokenHashService tokenHashService,
            CurrentUserProvider currentUserProvider,
            AuthMapper authMapper
    ) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.tokenProperties = tokenProperties;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashService = tokenHashService;
        this.currentUserProvider = currentUserProvider;
        this.authMapper = authMapper;
    }

    @Transactional
    public AuthResponse registerGamer(RegisterGamerRequest request, String ipAddress, String userAgent) {
        ensureUnique(request.email(), request.phone());
        Role gamerRole = roleRepository.findByCode(RoleCode.GAMER)
                .orElseThrow(() -> new ResourceNotFoundException("GAMER role is not configured"));
        AppUser user = new AppUser();
        user.setEmail(normalize(request.email()));
        user.setPhone(blankToNull(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setDisplayName(request.displayName());
        user.setStatus(UserStatus.ACTIVE);
        user.setActivatedAt(Instant.now());
        user.getRoles().add(gamerRole);
        appUserRepository.save(user);
        return issueAuthResponse(user, UUID.randomUUID(), ipAddress, userAgent);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        AppUser user = appUserRepository.findByEmailOrPhone(normalize(request.identifier()))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        ensureLoginAllowed(user);
        user.setLastLoginAt(Instant.now());
        return issueAuthResponse(user, UUID.randomUUID(), ipAddress, userAgent);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        String hash = tokenHashService.hash(request.refreshToken());
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new TokenException("Refresh token is invalid"));
        Instant now = Instant.now();
        if (current.getUsedAt() != null || current.getRevokedAt() != null) {
            current.setReuseDetectedAt(now);
            revokeFamily(current.getFamilyId(), "Refresh token reuse detected", now);
            throw new TokenException("Refresh token reuse detected");
        }
        if (current.getExpiresAt().isBefore(now)) {
            current.setRevokedAt(now);
            current.setRevokeReason("Refresh token expired");
            throw new TokenException("Refresh token has expired");
        }
        AppUser user = current.getUser();
        ensureLoginAllowed(user);
        String rawReplacement = tokenGenerator.generate();
        String replacementHash = tokenHashService.hash(rawReplacement);
        current.setUsedAt(now);
        current.setRevokedAt(now);
        current.setReplacedByTokenHash(replacementHash);
        current.setRevokeReason("Refresh token rotated");
        RefreshToken replacement = buildRefreshToken(user, current.getFamilyId(), replacementHash, ipAddress, userAgent, now);
        refreshTokenRepository.save(replacement);
        return new AuthResponse(jwtService.createAccessToken(user), rawReplacement, "Bearer",
                jwtProperties.accessTokenTtl().toSeconds(), authMapper.toCurrentUser(user));
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByTokenHash(tokenHashService.hash(request.refreshToken())).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            token.setRevokeReason("User logout");
        });
    }

    @Transactional
    public void logoutAll() {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        Instant now = Instant.now();
        refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(userId).forEach(token -> {
            token.setRevokedAt(now);
            token.setRevokeReason("User logout all devices");
        });
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        appUserRepository.findByEmailOrPhone(normalize(request.identifier())).ifPresent(user -> {
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(tokenHashService.hash(tokenGenerator.generate()));
            token.setIssuedAt(Instant.now());
            token.setExpiresAt(Instant.now().plus(tokenProperties.passwordResetTokenTtl()));
            passwordResetTokenRepository.save(token);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHashService.hash(request.token()))
                .orElseThrow(() -> new TokenException("Password reset token is invalid"));
        Instant now = Instant.now();
        if (token.getUsedAt() != null || token.getRevokedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new TokenException("Password reset token is invalid");
        }
        token.getUser().setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(now);
        token.setRevokedAt(now);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        AppUser user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is invalid");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        logoutAll();
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUserResponse() {
        return authMapper.toCurrentUser(currentUser());
    }

    private AppUser currentUser() {
        UUID userId = currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        return appUserRepository.findById(userId).orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
    }

    private AuthResponse issueAuthResponse(AppUser user, UUID familyId, String ipAddress, String userAgent) {
        String rawRefreshToken = tokenGenerator.generate();
        RefreshToken refreshToken = buildRefreshToken(user, familyId, tokenHashService.hash(rawRefreshToken), ipAddress, userAgent, Instant.now());
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(jwtService.createAccessToken(user), rawRefreshToken, "Bearer",
                jwtProperties.accessTokenTtl().toSeconds(), authMapper.toCurrentUser(user));
    }

    private RefreshToken buildRefreshToken(AppUser user, UUID familyId, String hash, String ipAddress, String userAgent, Instant now) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setFamilyId(familyId);
        token.setTokenHash(hash);
        token.setIssuedAt(now);
        token.setExpiresAt(now.plus(tokenProperties.refreshTokenTtl()));
        token.setIpAddress(ipAddress);
        token.setUserAgent(userAgent);
        return token;
    }

    private void revokeFamily(UUID familyId, String reason, Instant now) {
        refreshTokenRepository.findAllByFamilyId(familyId).forEach(token -> {
            token.setRevokedAt(now);
            token.setRevokeReason(reason);
        });
    }

    private void ensureUnique(String email, String phone) {
        if (appUserRepository.existsByEmail(normalize(email))) {
            throw new DuplicateResourceException("Account already exists");
        }
        if (StringUtils.hasText(phone) && appUserRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("Account already exists");
        }
    }

    private void ensureLoginAllowed(AppUser user) {
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new UnauthorizedException("Account is locked");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
