package com.norvya.norvya.controller;

import com.norvya.norvya.dto.request.*;
import com.norvya.norvya.dto.response.AuthResponse;
import com.norvya.norvya.dto.response.MessageResponse;
import com.norvya.norvya.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ══════════════════════════════════════════════════════
    // REGISTER — Étape 1 : envoyer OTP
    // POST /api/auth/register
    // ══════════════════════════════════════════════════════
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);
        return ResponseEntity.ok(
                new MessageResponse("Code OTP envoyé à " + request.getEmail())
        );
    }

    // ══════════════════════════════════════════════════════
    // REGISTER — Étape 2 : confirmer OTP + créer compte
    // POST /api/auth/register/confirm
    // ══════════════════════════════════════════════════════
    @PostMapping("/register/confirm")
    public ResponseEntity<AuthResponse> confirmRegister(
            @Valid @RequestBody RegisterConfirmRequest request) {

        VerifyOtpRequest otpRequest = new VerifyOtpRequest();
        otpRequest.setEmail(request.getEmail());
        otpRequest.setCode(request.getCode());
        otpRequest.setType("REGISTER");

        RegisterRequest originalRequest = new RegisterRequest();
        originalRequest.setEmail(request.getEmail());
        originalRequest.setFullName(request.getFullName());
        originalRequest.setPassword(request.getPassword());

        return ResponseEntity.ok(
                authService.confirmRegister(otpRequest, originalRequest)
        );
    }

    // ══════════════════════════════════════════════════════
    // LOGIN
    // POST /api/auth/login
    // ══════════════════════════════════════════════════════
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    // ══════════════════════════════════════════════════════
    // FORGOT PASSWORD — Étape 1 : envoyer OTP reset
    // POST /api/auth/forgot-password
    // ══════════════════════════════════════════════════════
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);
        return ResponseEntity.ok(
                new MessageResponse(
                        "un code de réinitialisation a été envoyé"
                )
        );
    }

    // ══════════════════════════════════════════════════════
    // FORGOT PASSWORD — Étape 2 : vérifier OTP
    // POST /api/auth/verify-otp
    // ══════════════════════════════════════════════════════
    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        authService.verifyOtp(request);
        return ResponseEntity.ok(
                new MessageResponse("Code OTP valide")
        );
    }

    // ══════════════════════════════════════════════════════
    // FORGOT PASSWORD — Étape 3 : nouveau mot de passe
    // POST /api/auth/reset-password
    // ══════════════════════════════════════════════════════
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(
                new MessageResponse("Mot de passe réinitialisé avec succès")
        );
    }

    // ══════════════════════════════════════════════════════
    // REFRESH TOKEN
    // POST /api/auth/refresh
    // ══════════════════════════════════════════════════════
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String refreshToken = authHeader.substring(7);
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    // ══════════════════════════════════════════════════════
    // RESEND OTP — renvoyer un OTP expiré
    // POST /api/auth/resend-otp
    // ══════════════════════════════════════════════════════
    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        authService.resendOtp(request);
        return ResponseEntity.ok(
                new MessageResponse("Nouveau code OTP envoyé à " + request.getEmail())
        );
    }
}