package com.norvya.norvya.service.user;

import com.norvya.norvya.dto.request.UpdateProfileRequest;
import com.norvya.norvya.dto.response.UserResponse;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.UserRepository;
import com.norvya.norvya.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StorageService storageService;

    // ── Récupérer le profil ──────────────────────────────
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserDetails userDetails) {
        User user = getUserByEmail(userDetails.getUsername());
        System.out.println("getCurrentUser {}"
                + userDetails.getUsername() );
        return mapToResponse(user);
    }

    // ── Mettre à jour le profil ──────────────────────────
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request, UserDetails userDetails) {
        User user = getUserByEmail(userDetails.getUsername());

        user.setFullName(request.getFullName());
        user.setFiliere(request.getFiliere());
        user.setNiveau(request.getNiveau());
        user.setEtablissement(request.getEtablissement());
        user.setBio(request.getBio());

        User savedUser = userRepository.save(user);
        log.info("Profil mis à jour pour l'utilisateur : {}", user.getEmail());

        return mapToResponse(savedUser);
    }

    // ── Upload photo de profil ───────────────────────────
    @Transactional
    public String uploadProfile(MultipartFile file, UserDetails userDetails) {
        User user = getUserByEmail(userDetails.getUsername());
        String fileName = storageService.uploadFile(file, "profile");

        log.info("Photo de profil uploadée : {} pour l'utilisateur : {}",
                fileName, user.getEmail());

        user.setProfileUrl(fileName);
        userRepository.save(user);

        return fileName;
    }

    // ── Helper ───────────────────────────────────────────
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    // ── Mapper User → UserResponse ───────────────────────
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .filiere(user.getFiliere())
                .niveau(user.getNiveau())
                .etablissement(user.getEtablissement())
                .profileUrl(user.getProfileUrl())
                .bio(user.getBio())
                .build();
    }
}