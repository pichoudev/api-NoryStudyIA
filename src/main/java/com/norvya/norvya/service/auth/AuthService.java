package com.norvya.norvya.service.auth;


import com.norvya.norvya.dto.request.*;
import com.norvya.norvya.dto.response.AuthResponse;
import com.norvya.norvya.entity.OtpCode;
import com.norvya.norvya.entity.Subscription;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.exception.ResourceNotFoundException;
import com.norvya.norvya.exception.UnauthorizedException;
import com.norvya.norvya.repository.OtpCodeRepository;
import com.norvya.norvya.repository.SubscriptionRepository;
import com.norvya.norvya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository       userRepository;
    private final OtpCodeRepository    otpCodeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final JwtService           jwtService;
    private final EmailService         emailService;
    private final PasswordEncoder      passwordEncoder;

    // ── REGISTER ───────────────────────────────────────────
    @Transactional
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // Supprimer les anciens OTP du même email
        otpCodeRepository.deleteAllByEmailAndType(
                request.getEmail(), OtpCode.OtpType.REGISTER
        );

        // Générer et envoyer l'OTP
        String otp = generateOtp();
        OtpCode otpCode = OtpCode.builder()
                .email(request.getEmail())
                .code(otp)
                .type(OtpCode.OtpType.REGISTER)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        otpCodeRepository.save(otpCode);
        emailService.sendOtpEmail(request.getEmail(), otp, "REGISTER");
    }

    // ── CONFIRMER OTP REGISTER ─────────────────────────────
    @Transactional
    public AuthResponse confirmRegister(VerifyOtpRequest request,
                                        RegisterRequest originalRequest) {

        OtpCode otpCode = validateOtp(
                request.getEmail(), request.getCode(), OtpCode.OtpType.REGISTER
        );

        // Créer l'utilisateur
        User user = User.builder()
                .email(originalRequest.getEmail())
                .fullName(originalRequest.getFullName())
                .passwordHash(passwordEncoder.encode(originalRequest.getPassword()))
                .build();

        userRepository.save(user);

        // Créer l'abonnement FREE par défaut
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(Subscription.Plan.FREE)
                .status(Subscription.Status.ACTIVE)
                .startedAt(LocalDateTime.now())
                .build();

        subscriptionRepository.save(subscription);

        // Marquer l'OTP comme utilisé
        otpCode.setUsed(true);
        otpCodeRepository.save(otpCode);

        return buildAuthResponse(user);
    }

    // ── LOGIN ──────────────────────────────────────────────
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email ou mot de passe incorrect");
        }

        return buildAuthResponse(user);
    }

    // ── FORGOT PASSWORD ────────────────────────────────────
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        // On ne révèle pas si l'email existe ou non (sécurité)
        if (!userRepository.existsByEmail(request.getEmail())) {
            return;
        }

        otpCodeRepository.deleteAllByEmailAndType(
                request.getEmail(), OtpCode.OtpType.FORGOT_PASSWORD
        );

        String otp = generateOtp();
        OtpCode otpCode = OtpCode.builder()
                .email(request.getEmail())
                .code(otp)
                .type(OtpCode.OtpType.FORGOT_PASSWORD)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        otpCodeRepository.save(otpCode);
        emailService.sendOtpEmail(request.getEmail(), otp, "FORGOT_PASSWORD");
    }

    // ── VERIFY OTP ─────────────────────────────────────────
    public void verifyOtp(VerifyOtpRequest request) {
        validateOtp(
                request.getEmail(),
                request.getCode(),
                OtpCode.OtpType.valueOf(request.getType())
        );
    }

    // ── RESET PASSWORD ─────────────────────────────────────
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        OtpCode otpCode = validateOtp(
                request.getEmail(), request.getCode(), OtpCode.OtpType.FORGOT_PASSWORD
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpCode.setUsed(true);
        otpCodeRepository.save(otpCode);
    }

    // ── REFRESH TOKEN ──────────────────────────────────────
    public AuthResponse refreshToken(String refreshToken) {

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new UnauthorizedException("Token de rafraîchissement invalide");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur introuvable"));

        return buildAuthResponse(user);
    }

    // ── RESEND OTP ─────────────────────────────────────────
    @Transactional
    public void resendOtp(ResendOtpRequest request) {

        OtpCode.OtpType otpType = OtpCode.OtpType.valueOf(request.getType());

        // Supprimer les anciens OTP
        otpCodeRepository.deleteAllByEmailAndType(request.getEmail(), otpType);

        // Générer un nouveau
        String otp = generateOtp();
        OtpCode otpCode = OtpCode.builder()
                .email(request.getEmail())
                .code(otp)
                .type(otpType)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        otpCodeRepository.save(otpCode);
        emailService.sendOtpEmail(request.getEmail(), otp, request.getType());
    }
    
    // ── HELPERS ────────────────────────────────────────────
    private OtpCode validateOtp(String email, String code, OtpCode.OtpType type) {
        OtpCode otpCode = otpCodeRepository
                .findByEmailAndCodeAndTypeAndUsedFalse(email, code, type)
                .orElseThrow(() -> new UnauthorizedException("Code OTP invalide"));

        if (otpCode.isExpired()) {
            throw new UnauthorizedException("Code OTP expiré");
        }

        return otpCode;
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private AuthResponse buildAuthResponse(User user) {
        // Récupérer le plan actif
        String plan = subscriptionRepository
                .findTopByUserAndStatusOrderByStartedAtDesc(user, Subscription.Status.ACTIVE)
                .map(s -> s.getPlan().name())
                .orElse("FREE");

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user.getEmail()))
                .refreshToken(jwtService.generateRefreshToken(user.getEmail()))
                .email(user.getEmail())
                .fullName(user.getFullName())
                .plan(plan)
                .build();
    }
}
