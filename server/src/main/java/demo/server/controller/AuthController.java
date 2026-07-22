package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.auth.AuthResponse;
import demo.server.dto.auth.ChangePasswordRequest;
import demo.server.dto.auth.CurrentUserResponse;
import demo.server.dto.auth.ForgotPasswordRequest;
import demo.server.dto.auth.LoginRequest;
import demo.server.dto.auth.LogoutRequest;
import demo.server.dto.auth.RefreshTokenRequest;
import demo.server.dto.auth.RegisterGamerRequest;
import demo.server.dto.auth.ResetPasswordRequest;
import demo.server.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterGamerRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.registerGamer(request, clientIp(servletRequest), servletRequest.getHeader(HttpHeaders.USER_AGENT)), "Registered");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.login(request, clientIp(servletRequest), servletRequest.getHeader(HttpHeaders.USER_AGENT)), "Logged in");
    }

    @PostMapping({"/refresh-token", "/refresh"})
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.refresh(request, clientIp(servletRequest), servletRequest.getHeader(HttpHeaders.USER_AGENT)), "Token refreshed");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.ok(null, "Logged out");
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logoutAll() {
        authService.logoutAll();
        return ApiResponse.ok(null, "Logged out from all devices");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.ok(null, "If the account exists, reset instructions were created");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok(null, "Password reset");
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok(null, "Password changed");
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.ok(authService.currentUserResponse());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null ? request.getRemoteAddr() : forwardedFor.split(",")[0].trim();
    }
}
