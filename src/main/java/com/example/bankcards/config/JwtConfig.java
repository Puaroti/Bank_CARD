package com.example.bankcards.config;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
/**
 * Конфигурация компонентов JWT: секретный ключ, парсер и билдер токенов.
 */
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    // По умолчанию dev должен быть >= 32 bytes для HS256
    @Value("${app.security.jwt.secret:b1a2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6A7B8C9D0E1F2}")
    /** Секрет для подписи JWT (HS256). */
    private String secret;

    @Value("${app.security.jwt.expiration-seconds:3600}")
    /** Время жизни токена в секундах. */
    private long expirationSeconds;

    @Bean
    /**
     * Создает и возвращает SecretKey для подписи JWT.
     * Если секрет короче 32 байт, генерируется случайный ключ и выводится предупреждение.
     */
    public SecretKey jwtSecretKey() {
        byte[] bytes = secret.getBytes();
        if (bytes.length < 32) {
            log.warn("JWT secret is too short ({} bytes). Generating a secure random key for HS256. Set app.security.jwt.secret to a value >= 32 bytes for stable tokens.", bytes.length);
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    @Bean
    /**
     * Создает парсер JWT для валидации/чтения токенов.
     */
    public JwtParser jwtParser(SecretKey jwtSecretKey) {
        return Jwts.parserBuilder().setSigningKey(jwtSecretKey).build();
    }

    @Bean
    /**
     * Создает билдер JWT для выпуска токенов.
     */
    public io.jsonwebtoken.JwtBuilder jwtBuilder(SecretKey jwtSecretKey) {
        return Jwts.builder().signWith(jwtSecretKey, SignatureAlgorithm.HS256);
    }

    /**
     * Возвращает время жизни токена в секундах.
     */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
