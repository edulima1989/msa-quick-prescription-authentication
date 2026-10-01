package com.quickprescription.authentication.service;

import com.quickprescription.authentication.dto.LoginRequest;
import com.quickprescription.authentication.dto.RegisterRequest;
import com.quickprescription.authentication.dto.TokenResponse;
import com.quickprescription.authentication.dto.TokenValidationResponse;
import com.quickprescription.authentication.dto.UsuarioRegistrado;

public interface UserService {

    UsuarioRegistrado register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenValidationResponse validateSessionToken(String token);
}
