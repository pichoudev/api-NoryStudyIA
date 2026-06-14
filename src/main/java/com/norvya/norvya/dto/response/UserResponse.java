package com.norvya.norvya.dto.response;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private String email;
    private String fullName;
    private String filiere;
    private String niveau;
    private String etablissement;
    private String profileUrl;
    private String bio;
}