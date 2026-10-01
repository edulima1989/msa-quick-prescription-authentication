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
public class RegisterRequest {

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
    @NotBlank(message = "es obligatorio")
    @Size(min = 2, max = 150, message = "debe tener entre 2 y 150 caracteres")
    private String userName;

    @Schema(description = "Email del usuario", example = "juan@example.com")
    @NotBlank(message = "es obligatorio")
    @Email(message = "debe ser un correo electrónico válido")
    @Size(max = 254, message = "debe tener como máximo 254 caracteres")
    private String userMail;

    @Schema(description = "Contraseña del usuario", example = "password123")
    @NotBlank(message = "es obligatorio")
    @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres")
    @ToString.Exclude
    private String userPassword;
}
