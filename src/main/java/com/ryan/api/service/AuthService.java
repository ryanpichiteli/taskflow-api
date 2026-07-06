package com.ryan.api.service;

import com.ryan.api.dto.auth.AuthResponse;
import com.ryan.api.dto.auth.LoginRequest;
import com.ryan.api.dto.auth.RegisterRequest;
import com.ryan.api.entity.User;
import com.ryan.api.enums.Role;
import com.ryan.api.exception.EmailAlreadyInUseException;
import com.ryan.api.exception.InvalidCredentialsException;
import com.ryan.api.mapper.UserMapper;
import com.ryan.api.repository.UserRepository;
import com.ryan.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }

        User user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);

        return AuthResponse.bearer(token, userMapper.toResponse(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, userMapper.toResponse(user));
    }
}
