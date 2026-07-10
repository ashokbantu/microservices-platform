package com.microservices.user.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Set<String> roles;
    private boolean enabled;
    private LocalDateTime createdAt;
}
