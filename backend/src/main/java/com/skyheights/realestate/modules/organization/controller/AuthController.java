package com.skyheights.realestate.modules.organization.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.organization.dto.JwtResponse;
import com.skyheights.realestate.modules.organization.dto.LoginRequest;
import com.skyheights.realestate.modules.organization.dto.RegisterRequest;
import com.skyheights.realestate.modules.organization.dto.UserResponse;
import com.skyheights.realestate.modules.organization.service.AuthService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwt = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(jwt, "Login successful"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<JwtResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        JwtResponse jwt = authService.register(registerRequest);
        return ResponseEntity.ok(ApiResponse.success(jwt, "Registration successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        UserResponse user = authService.getCurrentUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(user, "Current user fetched"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(@CurrentUser UserPrincipal currentUser) {
        // For Phase 2, simple endpoint - client can call login again for new token
        return ResponseEntity.ok(ApiResponse.success("OK", "Use login to get new access token"));
    }
}
