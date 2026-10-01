package com.quickprescription.authentication.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acceso emitido por el Auth Service")
public record TokenResponse(

        @Schema(description = "Token JWT firmado", example = "eyJhbGciOiJIUzUxMiJ9...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String accessToken,

        @Schema(description = "Tipo de token", allowableValues = {"Bearer"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String tokenType,

        @Schema(description = "Vigencia del token en segundos", example = "3600", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int expiresIn
) {

    public static final String BEARER = "Bearer";

    public static TokenResponse bearer(String accessToken, int expiresIn) {
        return new TokenResponse(accessToken, BEARER, expiresIn);
    }
}
