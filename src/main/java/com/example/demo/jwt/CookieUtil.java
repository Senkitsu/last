package com.example.demo.jwt;

import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
    // ЗАМЕНИТЕ @Value на константы
    private final String accessCookieName = "access_token";
    private final String refreshCookieName = "refresh_token";

    public HttpCookie createAccessTokenCookie(String token, long duration) {
        return ResponseCookie.from(accessCookieName, token)
                .maxAge(duration)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .build();
    }

    public HttpCookie deleteAccessTokenCookie() {
        return ResponseCookie.from(accessCookieName, "")
                .maxAge(0)
                .httpOnly(true)
                .path("/")
                .build();
    }

    public HttpCookie createRefreshTokenCookie(String token, long duration) {
        return ResponseCookie.from(refreshCookieName, token)
                .maxAge(duration)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .build();
    }

    public HttpCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from(refreshCookieName, "")
                .maxAge(0)
                .httpOnly(true)
                .path("/")
                .build();
    }
}