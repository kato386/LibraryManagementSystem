package com.cagatayergunes.library.service;

import com.cagatayergunes.library.exception.handler.OperationNotPermittedException;
import com.cagatayergunes.library.model.*;
import com.cagatayergunes.library.model.request.AuthenticationRequest;
import com.cagatayergunes.library.model.request.RegistrationRequest;
import com.cagatayergunes.library.model.response.AuthenticationResponse;
import com.cagatayergunes.library.repository.RoleRepository;
import com.cagatayergunes.library.repository.TokenRepository;
import com.cagatayergunes.library.repository.UserRepository;
import com.cagatayergunes.library.security.JwtService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserRepository userRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        authenticationService = new AuthenticationService(
                roleRepository, passwordEncoder, userRepository,
                tokenRepository, emailService, authenticationManager, jwtService
        );
    }

    @Test
    void testRegister_Success() throws MessagingException {

        RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password")
                .build();
        Role role = Role.builder()
                .name(RoleName.PATRON)
                .id(1L)
                .build();
        when(roleRepository.findByName(RoleName.PATRON)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("encodedPassword")
                .roles(java.util.List.of(role))
                .enabled(false)
                .accountLocked(false)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ReflectionTestUtils.setField(authenticationService, "activationUrl", "http://localhost:8080/activate");


        authenticationService.register(request);

        verify(userRepository).save(any(User.class));
        verify(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void testAuthenticate_Success() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john@example.com")
                .password("password")
                .build();


        User user = new User();
        user.setEmail("john@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        Authentication authMock = mock(Authentication.class);
        when(authMock.getPrincipal()).thenReturn(user);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);

        when(jwtService.generateToken(anyMap(), eq(user))).thenReturn("fake-jwt-token");

        AuthenticationResponse response = authenticationService.authenticate(request);

        assertEquals("fake-jwt-token", response.getToken());
    }

    @Test
    void testActivateAccount_Success() throws MessagingException {
        User user = new User();
        user.setId(1L);
        user.setEmail("john@example.com");

        Token token = Token.builder()
                .token("123456")
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(tokenRepository.findByToken("123456")).thenReturn(Optional.of(token));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authenticationService.activateAccount("123456");

        assertTrue(user.isEnabled());
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void testActivateAccount_TokenExpired() throws MessagingException {
        User user = new User();
        user.setEmail("expired@example.com");

        Token expiredToken = Token.builder()
                .token("expired")
                .user(user)
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(expiredToken));

        assertThrows(OperationNotPermittedException.class,
                () -> authenticationService.activateAccount("expired"));

        verify(emailService).sendEmail(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testActivateAccount_TokenNotFound() {
        when(tokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThrows(OperationNotPermittedException.class,
                () -> authenticationService.activateAccount("invalid"));
    }
}
