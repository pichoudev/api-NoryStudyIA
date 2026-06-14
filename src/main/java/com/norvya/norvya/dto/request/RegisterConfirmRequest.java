package com.norvya.norvya.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterConfirmRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String fullName;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    @Size(min = 6, max = 6)
    private String code;
}