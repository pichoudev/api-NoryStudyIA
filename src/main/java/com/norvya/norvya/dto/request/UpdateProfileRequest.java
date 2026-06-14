package com.norvya.norvya.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String fullName;

    private String filiere;

    private String niveau;

    private String etablissement;

    private String bio;

    private String ville;
}