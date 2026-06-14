package com.norvya.norvya.service.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from.email}")
    private String fromEmail;

    @Value("${spring.mail.from.name:NorvyaStudy}")
    private String fromName;

    public void sendOtpEmail(String to, String otp, String type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // ✅ Affiche "NorvyaStudy <tonmail@gmail.com>"
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(to);

            if (type.equals("REGISTER")) {
                helper.setSubject("NorvyaStudy — Confirmation de votre compte");
                helper.setText(buildRegisterHtml(otp), true);
            } else {
                helper.setSubject("NorvyaStudy — Réinitialisation de mot de passe");
                helper.setText(buildResetHtml(otp), true);
            }

            mailSender.send(message);

        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email : " + e.getMessage());
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