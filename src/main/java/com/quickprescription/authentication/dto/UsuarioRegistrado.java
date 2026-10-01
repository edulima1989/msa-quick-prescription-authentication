package com.quickprescription.authentication.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Consumidor recién registrado")
public record UsuarioRegistrado(

        @Schema(description = "ID del usuario", example = "15", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Nombre del usuario", example = "Juan Pérez",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String userName,

        @Schema(description = "Email del usuario", example = "juan@example.com", format = "email",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String userMail
) {
}
