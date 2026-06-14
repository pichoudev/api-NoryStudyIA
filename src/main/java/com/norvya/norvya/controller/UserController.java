package com.norvya.norvya.controller;

import com.norvya.norvya.dto.request.UpdateProfileRequest;
import com.norvya.norvya.dto.response.UserResponse;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.UserRepository;
import com.norvya.norvya.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    // ── Récupérer le profil connecté ─────────────────────
    // GET /api/users/me
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("getCurrentUser {}"
                + userDetails.getUsername() );
        return ResponseEntity.ok(
                userService.getCurrentUser(userDetails)
        );
    }

    // ── Mettre à jour le profil ──────────────────────────
    // PUT /api/users/profile
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        return ResponseEntity.ok(
                userService.updateProfile(request, userDetails)
        );
    }

    // ── Upload photo de profil ───────────────────────────
    // POST /api/users/profile-picture
    @PostMapping(value = "/profile-picture",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Vérifie que l'utilisateur est bien authentifié
        getUser(userDetails);
        return ResponseEntity.ok(
                userService.uploadProfile(file ,userDetails)
        );
    }

    // ── Helper ─────────────────────────────────────────────
    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }
}