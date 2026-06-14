package com.norvya.norvya.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 6, message = "Le code OTP doit contenir 6 chiffres")
    private String code;

    @NotBlank
    private String type;  // REGISTER ou FORGOT_PASSWORD
}
