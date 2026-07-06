package com.ryan.api.service;

import com.ryan.api.dto.auth.AuthResponse;
import com.ryan.api.dto.auth.LoginRequest;
import com.ryan.api.dto.auth.RegisterRequest;
import com.ryan.api.dto.user.UserResponse;
import com.ryan.api.entity.User;
import com.ryan.api.enums.Role;
import com.ryan.api.exception.EmailAlreadyInUseException;
import com.ryan.api.exception.InvalidCredentialsException;
import com.ryan.api.mapper.UserMapper;
import com.ryan.api.repository.UserRepository;
import com.ryan.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Ada Lovelace", "ada@taskflow.dev", "encoded-password", Role.USER);
    }

    @Test
    void registerShouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest("Ada Lovelace", "ada@taskflow.dev", "S3cret!23");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(userMapper.toResponse(user)).thenReturn(
                new UserResponse(UUID.randomUUID(), user.getName(), user.getEmail(), Role.USER, Instant.now()));

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("ada@taskflow.dev");
    }

    @Test
    void registerShouldThrowWhenEmailAlreadyInUse() {
        RegisterRequest request = new RegisterRequest("Ada", "ada@taskflow.dev", "S3cret!23");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyInUseException.class);
    }

    @Test
    void loginShouldReturnTokenForValidCredentials() {
        LoginRequest request = new LoginRequest("ada@taskflow.dev", "S3cret!23");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(userMapper.toResponse(user)).thenReturn(
                new UserResponse(UUID.randomUUID(), user.getName(), user.getEmail(), Role.USER, Instant.now()));

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void loginShouldThrowForUnknownEmail() {
        LoginRequest request = new LoginRequest("unknown@taskflow.dev", "S3cret!23");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginShouldThrowForWrongPassword() {
        LoginRequest request = new LoginRequest("ada@taskflow.dev", "wrong-password");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
