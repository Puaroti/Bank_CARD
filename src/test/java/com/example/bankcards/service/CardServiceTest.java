package com.example.bankcards.service;

import com.example.bankcards.dto.CardUpdateStatusRequest;
import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.exception.ApiExceptions;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционный тест CardService, проверяющий бизнес-правила обновления статуса карты.
 */
@SpringBootTest
class CardServiceTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private EncryptionService encryptionService;

    /**
     * Бизнес-правило: истёкшую карту (EXPIRED) нельзя активировать.
     * Ожидания: при попытке обновить статус до ACTIVE выбрасывается BadRequestException.
     */
    @Test
    @Transactional
    @DisplayName("CardService: cannot activate EXPIRED card (blocked by business rule)")
    void cannotActivateExpiredCard() {
        // Arrange: create user
        UserEntity user = new UserEntity();
        user.setUsername("expired_owner");
        user.setPasswordHash("hash");
        user.setFullName("Иванов Иван Иванович");
        user.setFirstName("Иван");
        user.setLastName("Иванов");
        user.setPatronymic("Иванович");
        user.setRole("ADMIN"); // role value does not affect service method
        user = userRepository.save(user);

        // Arrange: create expired card
        CardEntity card = new CardEntity();
        String enc = encryptionService.encrypt("1111222233334444");
        card.setCardNumberEncrypted(enc);
        card.setOwner(user.getFullName());
        card.setExpiryDate(LocalDate.now().minusYears(1));
        card.setStatus(CardStatus.EXPIRED);
        card.setUser(user);
        card.setBalance(BigDecimal.ZERO);
        card = cardRepository.save(card);
        final Long cardId = card.getId();

        // Act: try to activate
        // Act + Assert
        assertThatThrownBy(() -> cardService.updateStatus(cardId, new CardUpdateStatusRequest(CardStatus.ACTIVE)))
                .isInstanceOf(ApiExceptions.BadRequestException.class)
                .hasMessageContaining("Cannot activate expired card");
    }
}
