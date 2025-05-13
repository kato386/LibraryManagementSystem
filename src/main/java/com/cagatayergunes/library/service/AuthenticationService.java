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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;


    public void register(RegistrationRequest request) throws MessagingException {
        log.info("Registering user: {}", request.getEmail());
        var userRole = roleRepository.findByName(RoleName.PATRON)
                .orElseThrow(() -> {
                    log.error("ROLE PATRON was not initialized.");
                    return new IllegalStateException("ROLE PATRON was not initialized.");
                });

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();
        userRepository.save(user);
        log.info("User {} saved successfully", user.getEmail());
        sendValidationEmail(user);
    }

    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);

        emailService.sendEmail(
                user.getEmail(),
                user.getFullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account Activation"
        );
        log.info("Activation email sent to {}", user.getEmail());
    }

    private String generateAndSaveActivationToken(User user){
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        tokenRepository.save(token);

        log.debug("Activation token saved for user: {}", user.getEmail());
        return generatedToken;
    }

    private String generateActivationCode(int length){
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for(int i = 0 ; i< length ; i++){
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("Authentication attempt for user: {}", request.getEmail());
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var claims = new HashMap<String, Object>();
        var user = ((User)auth.getPrincipal());
        if(user.isAccountLocked()) {
            log.warn("User account is locked: {}", user.getEmail());
            throw new OperationNotPermittedException("Your account has been locked.");
        }
        claims.put("fullName", user.getFullName());
        var jwt = jwtService.generateToken(claims, user);
        log.info("JWT generated for user {}", user.getEmail());

        return AuthenticationResponse.builder()
                .token(jwt).build();
    }

    public void activateAccount(String token) throws MessagingException {
        log.info("Activating account using token: {}", token);
        Token savedToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Activation token not found: {}", token);
                    return new OperationNotPermittedException("Token not found.");
                });

        if(LocalDateTime.now().isAfter(savedToken.getExpiresAt())){
            log.warn("Token expired for user {}, resending activation email", savedToken.getUser().getEmail());
            sendValidationEmail(savedToken.getUser());
            throw new OperationNotPermittedException("Activation token has expired. A new token has been send.");
        }

        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> {
                    log.error("User not found for activation.");
                    return new UsernameNotFoundException("User not found.");
                });

        user.setEnabled(true);

        userRepository.save(user);
        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
        log.info("User {} activated successfully", user.getEmail());
    }
}
