package com.quickprescription.authentication.service.impl;

import com.quickprescription.authentication.dto.LoginRequest;
import com.quickprescription.authentication.dto.RegisterRequest;
import com.quickprescription.authentication.dto.TokenResponse;
import com.quickprescription.authentication.dto.TokenValidationResponse;
import com.quickprescription.authentication.dto.UsuarioRegistrado;
import com.quickprescription.authentication.exception.EmailAlreadyRegisteredException;
import com.quickprescription.authentication.exception.InvalidCredentialsException;
import com.quickprescription.authentication.mapper.UserMapper;
import com.quickprescription.authentication.model.User;
import com.quickprescription.authentication.repository.UserRepository;
import com.quickprescription.authentication.security.JwtTokenProvider;
import com.quickprescription.authentication.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "USUARIO_FINAL";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    public UsuarioRegistrado register(RegisterRequest request) {
        if (userRepository.existsByUserMailIgnoreCase(request.getUserMail())) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = userMapper.toUser(request);
        user.setUserPassword(passwordEncoder.encode(request.getUserPassword()));
        user.setUserRole(DEFAULT_ROLE);

        User savedUser = userRepository.save(user);
        return userMapper.toUsuarioRegistrado(savedUser);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUserMailIgnoreCase(request.getUserMail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getUserPassword(), user.getUserPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getUserMail(), user.getUserRole());

        return TokenResponse.bearer(token, jwtTokenProvider.getExpirationSeconds());
    }

    public TokenValidationResponse validateSessionToken(String token) {
        String normalizedToken = normalizeToken(token);
        if (normalizedToken.isEmpty() || !jwtTokenProvider.validateToken(normalizedToken)) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .build();
        }

        return TokenValidationResponse.builder()
                .valid(true)
                .userId(jwtTokenProvider.extractUserId(normalizedToken))
                .userMail(jwtTokenProvider.extractUserMail(normalizedToken))
                .userRole(jwtTokenProvider.extractUserRole(normalizedToken))
                .build();
    }

    private String normalizeToken(String token) {
        if (token == null) {
            return "";
        }

        String normalizedToken = token.trim();
        if (normalizedToken.startsWith("Bearer ")) {
            return normalizedToken.substring("Bearer ".length()).trim();
        }
        return normalizedToken;
    }

}
