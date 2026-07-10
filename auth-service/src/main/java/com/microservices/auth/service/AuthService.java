package com.microservices.auth.service;

import com.microservices.auth.dto.*;
import com.microservices.auth.entity.User;
import com.microservices.auth.exception.*;
import com.microservices.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklisted_token:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new UserAlreadyExistsException("Username already taken: " + request.getUsername());
        if (userRepository.existsByEmail(request.getEmail()))
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .roles(Set.of(User.Role.ROLE_USER))
            .build();
        userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        storeRefreshToken(user.getUsername(), refreshToken);

        return AuthResponse.builder()
            .accessToken(accessToken).refreshToken(refreshToken)
            .username(user.getUsername()).email(user.getEmail())
            .roles(user.getRoles().stream().map(Enum::name).toList())
            .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
            .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        storeRefreshToken(user.getUsername(), refreshToken);
        log.info("User logged in: {}", user.getUsername());
        return AuthResponse.builder()
            .accessToken(accessToken).refreshToken(refreshToken)
            .username(user.getUsername()).email(user.getEmail())
            .roles(user.getRoles().stream().map(Enum::name).toList())
            .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String username = jwtService.extractUsername(refreshToken);
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + username);
        if (storedToken == null || !storedToken.equals(refreshToken))
            throw new InvalidTokenException("Refresh token is invalid or expired");
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(refreshToken, userDetails))
            throw new InvalidTokenException("Refresh token validation failed");
        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);
        storeRefreshToken(username, newRefreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new InvalidTokenException("User not found"));
        return AuthResponse.builder()
            .accessToken(newAccessToken).refreshToken(newRefreshToken)
            .username(user.getUsername()).email(user.getEmail())
            .roles(user.getRoles().stream().map(Enum::name).toList())
            .build();
    }

    public void logout(String token, String username) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "blacklisted", 24, TimeUnit.HOURS);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + username);
        log.info("User logged out: {}", username);
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    private void storeRefreshToken(String username, String refreshToken) {
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + username, refreshToken, 7, TimeUnit.DAYS);
    }
}
