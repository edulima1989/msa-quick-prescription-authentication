package com.quickprescription.authentication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(description = "Email del usuario", example = "juan@example.com")
    @NotBlank(message = "es obligatorio")
    @Email(message = "debe ser un correo electrónico válido")
    @Size(max = 254, message = "debe tener como máximo 254 caracteres")
    private String userMail;

    @Schema(description = "Contraseña del usuario", example = "password123")
    @NotBlank(message = "es obligatorio")
    @ToString.Exclude
    private String userPassword;
}
