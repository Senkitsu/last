// src/main/java/com/example/demo/controller/AuthController.java
package com.example.demo.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.security.JwtUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;  // ✅ ИСПРАВЛЕНО: jwtUtil (со строчной буквы!)

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            String username = authentication.getName();
            String jwt = jwtUtil.generateToken(username);  // ✅ Теперь правильно

            Cookie jwtCookie = new Cookie("jwt", jwt);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false);  // для localhost
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(3600);
            response.addCookie(jwtCookie);

            return ResponseEntity.ok(new LoginResponse("Login successful", username));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(new LoginResponse("Invalid username or password", null));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(new LoginResponse("Authentication failed: " + e.getMessage(), null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("jwt", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

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