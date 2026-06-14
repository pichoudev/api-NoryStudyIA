package com.norvya.norvya.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseRequest {

    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    // Texte brut si pas de PDF
    private String contentText;

    private String language = "fr";

    private Integer nombrePage =10;

    private String matiere ;
}