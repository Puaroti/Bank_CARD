package com.example.bankcards.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Сервис шифрования/кодирования данных.
 *
 * ВНИМАНИЕ: Текущая реализация использует Base64 как заглушку и не обеспечивает безопасность.
 * В продакшене замените на стойкое шифрование (например, AES-GCM) и защищенное хранение ключей.
 */
@Service
public class EncryptionService {
    /**
     * Кодирует переданную строку в Base64 (заглушка вместо шифрования).
     *
     * @param plain исходная строка
     * @return строка в формате Base64
     */
    public String encrypt(String plain) {
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }
}
