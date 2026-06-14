package com.norvya.norvya.service.course;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigInteger;

@Slf4j
@Service
public class PdfExtractorService {

    private static final int MAX_CHARS =1000000000;

    public String extractText(MultipartFile file) {
        try {
            // ✅ PDFBox 3.x — utiliser Loader.loadPDF()
            byte[] bytes = file.getBytes();

            try (PDDocument document = Loader.loadPDF(bytes)) {

                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                // Nettoyer le texte
                text = cleanText(text);

                // Tronquer si trop long pour Gemini
                if (text.length() > MAX_CHARS) {
                    log.warn("Texte tronqué à {} caractères", MAX_CHARS);
                    text = text.substring(0, MAX_CHARS);
                }

                log.info("PDF traité — Pages : {}, Caractères extraits : {}, Tronqué : {}",
                        document.getNumberOfPages(),
                        text.length(),
                        text.length() >= MAX_CHARS
                );

                return text;
            }

        } catch (IOException e) {
            throw new RuntimeException("Erreur extraction PDF : "
                    + e.getMessage());
        }
    }

    private String cleanText(String text) {
        return text
                .replaceAll("\\s+", " ")
                .replaceAll("\\n+", "\n")
                .trim();
    }


}