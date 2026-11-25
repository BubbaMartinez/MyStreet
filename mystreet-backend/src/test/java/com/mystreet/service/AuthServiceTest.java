package com.mystreet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mystreet.domain.User;
import com.mystreet.dto.AuthResponse;
import com.mystreet.dto.LoginRequest;
import com.mystreet.dto.RegisterRequest;
import com.mystreet.exception.ConflictException;
import com.mystreet.repository.UserRepository;
import com.mystreet.security.JwtService;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "test-secret-for-unit-tests-only-0123456789abcdef";

    @Mock
    UserRepository users;

    @Mock
    PasswordEncoder encoder;

    private JwtService jwtService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60);
        service = new AuthService(users, encoder, jwtService);
    }

    private User existingUser() {
        User u = new User();
        u.setEmail("ana@test.com");
        u.setFullName("Ana");
        u.setPasswordHash("HASH");
        return u;
    }

    @Test
    void register_duplicateEmail_throwsConflictAndDoesNotSave() {
        when(users.existsByEmail("ana@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Ana", "Ana@Test.com", "password123")))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any(User.class));
    }

    @Test
    void register_newUser_hashesPasswordNormalizesEmailAndIsNotAdmin() {
        when(users.existsByEmail("ana@test.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("HASH");
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = service.register(new RegisterRequest(" Ana ", "Ana@Test.com", "password123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("HASH");
        assertThat(saved.getValue().getEmail()).isEqualTo("ana@test.com");
        assertThat(saved.getValue().getFullName()).isEqualTo("Ana");

        Claims claims = jwtService.parse(response.token());
        assertThat(claims.getSubject()).isEqualTo("ana@test.com");
        assertThat(claims.get("isAdmin", Boolean.class)).isFalse();
        assertThat(response.isAdmin()).isFalse();
    }

    @Test
    void login_validCredentials_returnsValidToken() {
        when(users.findByEmail("ana@test.com")).thenReturn(Optional.of(existingUser()));
        when(encoder.matches("password123", "HASH")).thenReturn(true);

        AuthResponse response = service.login(new LoginRequest("Ana@Test.com", "password123"));

        assertThat(response.email()).isEqualTo("ana@test.com");
        assertThat(jwtService.parse(response.token()).getSubject()).isEqualTo("ana@test.com");
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        when(users.findByEmail("ana@test.com")).thenReturn(Optional.of(existingUser()));
        when(encoder.matches("wrong", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@test.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throwsBadCredentials() {
        when(users.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody@test.com", "whatever")))
                .isInstanceOf(BadCredentialsException.class);
    }
}