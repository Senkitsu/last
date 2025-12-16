package com.example.demo.dto;

import java.util.Set;

public record UserResponseDto(
    Long id,
    String username,
    String role,
    Set<String> permissions
) {
    public UserResponseDto(Long id, String username, String role) {
        this(id, username, role, null);
    }
    
    // Builder
    public static UserResponseDtoBuilder builder() {
        return new UserResponseDtoBuilder();
    }
    
    public static class UserResponseDtoBuilder {
        private Long id;
        private String username;
        private String role;
        private Set<String> permissions;
        
        public UserResponseDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public UserResponseDtoBuilder username(String username) {
            this.username = username;
            return this;
        }
        
        public UserResponseDtoBuilder role(String role) {
            this.role = role;
            return this;
        }
        
        public UserResponseDtoBuilder permissions(Set<String> permissions) {
            this.permissions = permissions;
            return this;
        }
        
        public UserResponseDtoBuilder message(String message) {
            // Игнорируем для совместимости
            return this;
        }
        
        public UserResponseDto build() {
            return new UserResponseDto(id, username, role, permissions);
        }
    }
}