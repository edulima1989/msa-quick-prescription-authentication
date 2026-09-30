package com.quickprescription.authentication.service;

import com.quickprescription.authentication.dto.LoginRequest;
import com.quickprescription.authentication.dto.LoginResponse;
import com.quickprescription.authentication.dto.RegisterRequest;
import com.quickprescription.authentication.dto.TokenValidationResponse;
import com.quickprescription.authentication.dto.UserResponse;

public interface UserService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    TokenValidationResponse validateSessionToken(String token);
}
