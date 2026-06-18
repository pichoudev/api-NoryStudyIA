//package com.norvya.norvya.service.auth;
//
//import jakarta.mail.MessagingException;
//import jakarta.mail.internet.InternetAddress;
//import jakarta.mail.internet.MimeMessage;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.stereotype.Service;
//
//import java.io.UnsupportedEncodingException;
//
//@Service
//@RequiredArgsConstructor
//public class EmailService {
//
//    private final JavaMailSender mailSender;
//
//    @Value("${spring.mail.from.email}")
//    private String fromEmail;
//
//    @Value("${spring.mail.from.name:NorvyaStudy}")
//    private String fromName;
//
//    public void sendOtpEmail(String to, String otp, String type) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//            // ✅ Affiche "NorvyaStudy <tonmail@gmail.com>"
//            helper.setFrom(new InternetAddress(fromEmail, fromName));
//            helper.setTo(to);
//
//            if (type.equals("REGISTER")) {
//                helper.setSubject("NorvyaStudy — Confirmation de votre compte");
//                helper.setText(buildRegisterHtml(otp), true);
//            } else {
//                helper.setSubject("NorvyaStudy — Réinitialisation de mot de passe");
//                helper.setText(buildResetHtml(otp), true);
//            }
//
//            mailSender.send(message);
//
//        } catch (MessagingException | UnsupportedEncodingException e) {
//            throw new RuntimeException("Erreur lors de l'envoi de l'email : " + e.getMessage());
//        }
//    }
//
//    private String buildRegisterHtml(String otp) {
//        return """
//            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto;">
//                <h2 style="color: #1F4E79;">Bienvenue sur %s 🎓</h2>
//                <p>Voici votre code de confirmation :</p>
//                <div style="background: #D6E4F0; padding: 20px; text-align: center;
//                            border-radius: 8px; font-size: 32px; font-weight: bold;
//                            letter-spacing: 8px; color: #1F4E79;">
//                    %s
//                </div>
//                <p style="color: #595959;">Ce code expire dans <strong>10 minutes</strong>.</p>
//                <p style="color: #595959;">Si vous n'avez pas créé de compte, ignorez cet email.</p>
//                <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">
//                <p style="color: #9e9e9e; font-size: 12px; text-align: center;">
//                    %s — %s
//                </p>
//            </div>
//        """.formatted(fromName, otp, fromName, fromEmail);
//    }
//
//    private String buildResetHtml(String otp) {
//        return """
//            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto;">
//                <h2 style="color: #1F4E79;">Réinitialisation de mot de passe</h2>
//                <p>Voici votre code de réinitialisation :</p>
//                <div style="background: #D6E4F0; padding: 20px; text-align: center;
//                            border-radius: 8px; font-size: 32px; font-weight: bold;
//                            letter-spacing: 8px; color: #1F4E79;">
//                    %s
//                </div>
//                <p style="color: #595959;">Ce code expire dans <strong>10 minutes</strong>.</p>
//                <p style="color: #595959;">Si vous n'avez pas demandé cette réinitialisation,
//                ignorez cet email.</p>
//                <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">
//                <p style="color: #9e9e9e; font-size: 12px; text-align: center;">
//                    %s — %s
//                </p>
//            </div>
//        """.formatted(otp, fromName, fromEmail);
//    }
//}

package com.norvya.norvya.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${spring.mail.from.email}")
    private String fromEmail;

    @Value("${spring.mail.from.name:NorvyaStudy}")
    private String fromName;

    public void sendOtpEmail(String to, String otp, String type) {
        String subject;
        String htmlContent;

        if (type.equals("REGISTER")) {
            subject = "NorvyaStudy — Confirmation de votre compte";
            htmlContent = buildRegisterHtml(otp);
        } else {
            subject = "NorvyaStudy — Réinitialisation de mot de passe";
            htmlContent = buildResetHtml(otp);
        }

        sendViaBrevoApi(to, subject, htmlContent);
    }

    private void sendViaBrevoApi(String to, String subject, String htmlContent) {
        log.info(">>> Clé Brevo utilisée : [{}]", brevoApiKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);
        headers.set("accept", "application/json");

        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", fromName,
                        "email", fromEmail
                ),
                "to", List.of(
                        Map.of("email", to)
                ),
                "subject", subject,
                "htmlContent", htmlContent
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Brevo a renvoyé un statut inattendu: {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Erreur lors de l'envoi de l'email : statut " + response.getStatusCode());
            }

            log.info("Email envoyé avec succès à {} via Brevo API", to);

        } catch (RestClientException e) {
            log.error("Echec de l'envoi de l'email via Brevo API pour {} : {}", to, e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email : " + e.getMessage(), e);
        }
    }

    private String buildRegisterHtml(String otp) {
        return """
                    <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto;">
                        <h2 style="color: #1F4E79;">Bienvenue sur %s 🎓</h2>
                        <p>Voici votre code de confirmation :</p>
                        <div style="background: #D6E4F0; padding: 20px; text-align: center;
                                    border-radius: 8px; font-size: 32px; font-weight: bold;
                                    letter-spacing: 8px; color: #1F4E79;">
                            %s
                        </div>
                        <p style="color: #595959;">Ce code expire dans <strong>10 minutes</strong>.</p>
                        <p style="color: #595959;">Si vous n'avez pas créé de compte, ignorez cet email.</p>
                        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">
                        <p style="color: #9e9e9e; font-size: 12px; text-align: center;">
                            %s — %s
                        </p>
                    </div>
                """.formatted(fromName, otp, fromName, fromEmail);
    }

    private String buildResetHtml(String otp) {
        return """
                    <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto;">
                        <h2 style="color: #1F4E79;">Réinitialisation de mot de passe</h2>
                        <p>Voici votre code de réinitialisation :</p>
                        <div style="background: #D6E4F0; padding: 20px; text-align: center;
                                    border-radius: 8px; font-size: 32px; font-weight: bold;
                                    letter-spacing: 8px; color: #1F4E79;">
                            %s
                        </div>
                        <p style="color: #595959;">Ce code expire dans <strong>10 minutes</strong>.</p>
                        <p style="color: #595959;">Si vous n'avez pas demandé cette réinitialisation,
                        ignorez cet email.</p>
                        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">
                        <p style="color: #9e9e9e; font-size: 12px; text-align: center;">
                            %s — %s
                        </p>
                    </div>
                """.formatted(otp, fromName, fromEmail);
    }
}