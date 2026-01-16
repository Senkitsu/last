package com.example.demo.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.example.demo.model.TokenType;
import com.example.demo.model.Token;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtTokenProvider {
    
    private final String secret = "66546A555E5A7234753778214123222A472D4B614E645262356B587032733576";
    private final long jwtExpiration = 86400000L;

    public Token generateAccessToken(
        Map<String, Object> extractClaims,
        Long duration,
        TemporalUnit durationType,
        UserDetails user
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plus(duration, durationType);
        String token = Jwts.builder()
                .setClaims(extractClaims)
                .setSubject(user.getUsername())
                .setIssuedAt(toDate(now))
                .setExpiration(toDate(expiryDate))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        return new Token(TokenType.ACCESS, token, expiryDate, false, null);
    }

    public Token generateRefreshToken(
        Long duration,
        TemporalUnit durationType,
        UserDetails user
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plus(duration, durationType);
        String token = Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(toDate(now))
                .setExpiration(toDate(expiryDate))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        return new Token(TokenType.REFRESH, token, expiryDate, false, null);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Date toDate(LocalDateTime time) {
        return Date.from(time.toInstant(ZoneOffset.UTC));
    }

    private Key getSigningKey() {
        // Ваш секрет в HEX формате - преобразуем его в байты
        byte[] keyBytes = hexStringToByteArray(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public LocalDateTime getExpiryDate(String token) {
        return toLocalDateTime(extractClaim(token, Claims::getExpiration));
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    public String getUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean validateToken(String token) {
        if (token == null) return false;
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}