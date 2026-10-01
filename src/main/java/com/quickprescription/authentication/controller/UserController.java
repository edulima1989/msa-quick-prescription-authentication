package com.quickprescription.authentication.controller;

import com.quickprescription.authentication.dto.LoginRequest;
import com.quickprescription.authentication.dto.RegisterRequest;
import com.quickprescription.authentication.dto.TokenValidationRequest;
import com.quickprescription.authentication.dto.TokenResponse;
import com.quickprescription.authentication.dto.TokenValidationResponse;
import com.quickprescription.authentication.dto.UsuarioRegistrado;
import com.quickprescription.authentication.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints de autenticación")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario", description = "Crea un nuevo usuario con los datos proporcionados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioRegistrado.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "409", description = "Correo ya registrado",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/problem+json"))
    })
    public ResponseEntity<UsuarioRegistrado> register(@Valid @RequestBody RegisterRequest request) {
        UsuarioRegistrado response = userService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuario", description = "Valida credenciales y retorna JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/problem+json"))
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-session")
    @Operation(summary = "Validar token de sesión", description = "Valida un token JWT de sesión y retorna su estado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado de validación",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenValidationResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<TokenValidationResponse> validateSessionToken(@RequestBody TokenValidationRequest request) {
        TokenValidationResponse response = userService.validateSessionToken(request.getToken());
        return ResponseEntity.ok(response);
    }
}
