package com.campus.campus_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "username required")
    @Size(max = 50, message = "username too long")
    private String username;
    @NotBlank(message = "email required")
    @Email(message = "invalid email")
    @Size(max = 100, message = "email too long")
    private String email;
    @NotBlank(message = "password required")
    @Size(min = 6, max = 128, message = "password length 6-128")
    private String password;
}

