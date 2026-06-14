package com.norvya.norvya.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendOtpRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String type;  // REGISTER ou FORGOT_PASSWORD
}