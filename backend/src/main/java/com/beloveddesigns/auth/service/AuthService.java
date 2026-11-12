package com.beloveddesigns.auth.service;

import com.beloveddesigns.auth.dto.AuthResponse;
import com.beloveddesigns.auth.dto.LoginRequest;
import com.beloveddesigns.auth.dto.RegisterRequest;
import com.beloveddesigns.auth.dto.RefreshRequest;
import com.beloveddesigns.except.BadRequestException;
import com.beloveddesigns.except.ErrorMessages;
import com.beloveddesigns.security.CustomUserDetailsService;
import com.beloveddesigns.security.JwtService;
import com.beloveddesigns.user.entity.Role;
import com.beloveddesigns.user.entity.User;
import com.beloveddesigns.user.repository.RoleRepository;
import com.beloveddesigns.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "CLIENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.phone())) {
            throw new BadRequestException(ErrorMessages.PHONE_ALREADY_USED);
        }

        Role clientRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException(ErrorMessages.defaultRoleMissing(DEFAULT_ROLE)));

        User user = User.builder()
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .roles(Set.of(clientRole))
                .build();

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.phone(), request.password())
        );

        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new IllegalStateException(ErrorMessages.AUTHENTICATED_USER_NOT_FOUND));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return buildAuthResponse(userDetailsService.loadUserByUsername(user.getPhone()));
    }

    private AuthResponse buildAuthResponse(UserDetails userDetails) {
        String phone = userDetails.getUsername();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalStateException(ErrorMessages.AUTHENTICATED_USER_NOT_FOUND));
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(accessToken, refreshToken, user.getPhone(), user.getFirstName(), user.getLastName());
    }

    public AuthResponse refresh(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new BadRequestException(ErrorMessages.TOKEN_INVALID);
        }
        return buildAuthResponse(userDetails);
    }
}
