package com.example.bankcards.util;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.UserEntity;

import java.time.format.DateTimeFormatter;

public final class MapperUtils {

    private MapperUtils() {}

    public static CardDto toCardDto(CardEntity entity) {
        if (entity == null) return null;
        return CardDto.builder()
                .id(entity.getId())
                .maskedCardNumber(maskCardNumber(entity.getCardNumberEncrypted()))
                .owner(entity.getOwner())
                .expiryDate(entity.getExpiryDate())
                .status(entity.getStatus())
                .balance(entity.getBalance())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static CardEntity toCardEntity(CardCreateRequest req, String encryptedNumber, UserEntity user) {
        CardEntity e = new CardEntity();
        e.setCardNumberEncrypted(encryptedNumber);
        e.setOwner(req.owner());
        e.setExpiryDate(req.expiryDate());
        e.setStatus(CardStatus.ACTIVE);
        e.setBalance(java.math.BigDecimal.ZERO);
        e.setUser(user);
        return e;
    }

    // В этом месте мы предполагаем, что cardNumberEncrypted хранит уже зашифрованное значение.
    // Для получения маски, как правило, нужна исходная PAN. Здесь предоставляем упрощённую маску,
    // применяя последние 4 символа из шифротекста (в реале используйте отдельное поле с последними 4).
    public static String maskCardNumber(String encrypted) {
        if (encrypted == null || encrypted.length() < 4) return "**** **** **** ****";
        String last4 = encrypted.substring(encrypted.length() - 4);
        return "**** **** **** " + last4;
    }
}
