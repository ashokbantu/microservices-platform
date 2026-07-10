package com.microservices.user.service;

import com.microservices.user.dto.*;
import com.microservices.user.entity.User;
import com.microservices.user.exception.UserNotFoundException;
import com.microservices.user.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    @Cacheable(value = "users", key = "#id")
    @CircuitBreaker(name = "userService")
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
            .map(this::mapToResponse)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    @Cacheable(value = "users", key = "#username")
    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(this::mapToResponse)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findByEnabledTrue(pageable).map(this::mapToResponse);
    }

    @CachePut(value = "users", key = "#id")
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, String currentUsername) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        if (!user.getUsername().equals(currentUsername) &&
            user.getRoles().stream().noneMatch(r -> r.name().equals("ROLE_ADMIN")))
            throw new AccessDeniedException("You can only update your own profile");
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        return mapToResponse(userRepository.save(user));
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        user.setEnabled(false);
        userRepository.save(user);
        log.info("User disabled: {}", id);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId()).username(user.getUsername()).email(user.getEmail())
            .firstName(user.getFirstName()).lastName(user.getLastName()).phone(user.getPhone())
            .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
            .enabled(user.isEnabled()).createdAt(user.getCreatedAt())
            .build();
    }
}
