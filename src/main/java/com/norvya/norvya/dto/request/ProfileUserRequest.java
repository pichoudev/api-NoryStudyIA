package com.norvya.norvya.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class ProfileUserRequest {

    @NotBlank
    private String profileUrl;
}
