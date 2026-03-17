package com.example.wallet_task.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenUtil {
    @Value("${configuration.security.jwt.secret}")
    private String secretKey;

    @Value("${configuration.security.jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8))).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }





//=====================
//
//    /**
//     * Извлечение имени пользователя из токена
//     */
//    public String extractUsername(String token) {
//        return extractClaim(token, Claims::getSubject);
//    }
//
//    /**
//     * Извлечение даты истечения токена
//     */
//    public Date extractExpiration(String token) {
//        return extractClaim(token, Claims::getExpiration);
//    }
//
//    /**
//     * Извлечение конкретного claim из токена
//     */
//    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
//        final Claims claims = extractAllClaims(token);
//        return claimsResolver.apply(claims);
//    }
//
//    /**
//     * Извлечение всех claims из токена
//     */
//    private Claims extractAllClaims(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(getSignKey())
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//    }
//
//    /**
//     * Проверка, истек ли токен
//     */
//    private Boolean isTokenExpired(String token) {
//        return extractExpiration(token).before(new Date());
//    }
//
//    /**
//     * Генерация токена для пользователя
//     */
//    public String generateToken(UserDetails userDetails) {
//        Map<String, Object> claims = new HashMap<>();
//
//        // Добавляем роли пользователя в токен
//        claims.put("roles", userDetails.getAuthorities().stream()
//                .map(GrantedAuthority::getAuthority)
//                .toList());
//
//        // Добавляем имя пользователя
//        claims.put("username", userDetails.getUsername());
//
//        return createToken(claims, userDetails.getUsername());
//    }
//
//    /**
//     * Создание токена с указанными claims и subject
//     */
//    private String createToken(Map<String, Object> claims, String subject) {
//        return Jwts.builder()
//                .setClaims(claims)
//                .setSubject(subject)
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
//                .signWith(getSignKey(), SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    /**
//     * Валидация токена
//     */
//    public Boolean validateToken(String token, UserDetails userDetails) {
//        final String username = extractUsername(token);
//        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
//    }
//
//    /**
//     * Получение ключа для подписи
//     */
//    private Key getSignKey() {
//        return Keys.hmacShaKeyFor(secretKey.getBytes());
//    }
//
//    /**
//     * Проверка валидности токена (без UserDetails)
//     */
//    public Boolean validateToken(String token) {
//        try {
//            extractAllClaims(token);
//            return !isTokenExpired(token);
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    /**
//     * Извлечение ролей из токена
//     */
//    @SuppressWarnings("unchecked")
//    public java.util.List<String> extractRoles(String token) {
//        Claims claims = extractAllClaims(token);
//        return (java.util.List<String>) claims.get("roles");
//    }
}
