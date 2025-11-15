// src/main/java/com/example/demo/controller/AuthController.java
package com.example.demo.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import com.example.demo.jwt.JwtUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            String username = auth.getName();
            String jwt = jwtUtil.generateToken(username);

            // 🔹 Устанавливаем JWT в HttpOnly Cookie
            Cookie cookie = new Cookie("jwt", jwt);
            cookie.setHttpOnly(true);
            cookie.setSecure(false);  // ← false для localhost
            cookie.setPath("/");
            cookie.setMaxAge(3600);   // 1 час
            response.addCookie(cookie);

            return ResponseEntity.ok(new LoginResponse("Login successful", username));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(401)
                    .body(new LoginResponse("Invalid username or password", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(new LoginResponse("Logout successful", null));
    }

    // --- DTOs ---
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private String message;
        private String username;

        public LoginResponse(String message, String username) {
            this.message = message;
            this.username = username;
        }

        public String getMessage() { return message; }
        public String getUsername() { return username; }
    }
}