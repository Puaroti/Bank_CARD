package com.example.bankcards.util;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.UserEntity;

import java.time.format.DateTimeFormatter;

/**
 * Утилитный класс для преобразования (маппинга) между сущностями доменной модели и DTO.
 * <p>
 * Содержит статические методы для:
 * <ul>
 *   <li>Преобразования {@link CardEntity} в {@link CardDto}</li>
 *   <li>Создания {@link CardEntity} из запроса {@link CardCreateRequest}</li>
 *   <li>Маскирования номера карты для безопасного отображения</li>
 * </ul>
 */
public final class MapperUtils {

    /**
     * Приватный конструктор предотвращает создание экземпляров утилитного класса.
     */
    private MapperUtils() {}

    /**
     * Преобразует сущность карты в DTO для отдачи наружу.
     * <p>
     * Номер карты в DTO представляется в замаскированном виде (последние 4 символа).
     *
     * @param entity сущность {@link CardEntity}; если {@code null}, возвращается {@code null}
     * @return собранный {@link CardDto} либо {@code null}
     */
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

    /**
     * Создает сущность карты {@link CardEntity} на основе запроса, уже зашифрованного номера и владельца.
     * <p>
     * По умолчанию новосозданная карта имеет статус {@link CardStatus#ACTIVE} и нулевой баланс.
     *
     * @param req            исходный запрос на создание карты
     * @param encryptedNumber уже зашифрованный номер карты (PAN)
     * @param user           пользователь-владелец карты
     * @return инициализированная сущность {@link CardEntity}
     */
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
    /**
     * Возвращает маску номера карты в формате «**** **** **** XXXX», где XXXX — последние 4 символа
     * переданной строки. Метод предназначен исключительно для безопасного отображения номера.
     * <p>
     * Примечание: в реальной системе последние 4 цифры обычно хранятся отдельно от шифротекста PAN.
     *
     * @param encrypted зашифрованное представление номера (или иная строка, из которой берутся последние 4 символа)
     * @return строка маскированного номера; если вход некорректен, возвращается «**** **** **** ****»
     */
    public static String maskCardNumber(String encrypted) {
        if (encrypted == null || encrypted.length() < 4) return "**** **** **** ****";
        String last4 = encrypted.substring(encrypted.length() - 4);
        return "**** **** **** " + last4;
    }
}
