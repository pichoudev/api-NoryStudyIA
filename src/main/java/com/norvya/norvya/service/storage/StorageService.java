package com.norvya.norvya.service.storage;

import com.norvya.norvya.config.R2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client     s3Client;
    private final R2Properties r2Properties;

    // ── Upload ─────────────────────────────────────────────
    public String uploadFile(MultipartFile file, String folder) {
        try {
            String fileName = folder + "/"
                    + UUID.randomUUID()
                    + "_" + file.getOriginalFilename();

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request,
                    RequestBody.fromBytes(file.getBytes()));

            String fileUrl = r2Properties.getPublicUrl() + "/" + fileName;
            System.out.println("Uploading file: " + r2Properties.getPublicUrl());
            log.info("Fichier uploadé sur R2 : {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            throw new RuntimeException("Erreur upload R2 : " + e.getMessage());
        }
    }

    // ✅ Télécharger le PDF depuis R2 ──────────────────────
    public byte[] downloadFile(String fileUrl) {
        try {
            // Extraire la clé depuis l'URL publique
            String key = extractKey(fileUrl);

            log.info("Téléchargement depuis R2 : {}", key);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> response =
                    s3Client.getObjectAsBytes(request);

            byte[] bytes = response.asByteArray();
            log.info("PDF téléchargé — {} octets", bytes.length);
            return bytes;

        } catch (Exception e) {
            log.error("Erreur téléchargement R2 : {}", e.getMessage());
            throw new RuntimeException("Impossible de télécharger le fichier : "
                    + e.getMessage());
        }
    }

    // ── Supprimer ──────────────────────────────────────────
    public void deleteFile(String fileUrl) {
        try {
            String key = extractKey(fileUrl);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("Fichier supprimé de R2 : {}", key);

        } catch (Exception e) {
            log.error("Erreur suppression R2 : {}", e.getMessage());
        }
    }

    // ── Extraire la clé depuis l'URL ───────────────────────
    private String extractKey(String fileUrl) {
        // Supporte les deux formats d'URL R2
        String key = fileUrl;

        if (fileUrl.contains(r2Properties.getPublicUrl())) {
            key = fileUrl.replace(r2Properties.getPublicUrl() + "/", "");
        } else if (fileUrl.contains(".r2.cloudflarestorage.com/")) {
            // Format : https://accountid.r2.cloudflarestorage.com/bucket/key
            key = fileUrl.substring(
                    fileUrl.indexOf(".r2.cloudflarestorage.com/")
                            + ".r2.cloudflarestorage.com/".length()
            );
            // Supprimer le nom du bucket si présent
            if (key.startsWith(r2Properties.getBucketName() + "/")) {
                key = key.substring(r2Properties.getBucketName().length() + 1);
            }
        }

        log.info("Clé extraite : {}", key);
        return key;
    }
}