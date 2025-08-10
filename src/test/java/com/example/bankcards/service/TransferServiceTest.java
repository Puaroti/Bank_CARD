package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.*;
import com.example.bankcards.exception.ApiExceptions;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.OperationHistoryRepository;
import com.example.bankcards.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты TransferService с использованием Mockito.
 * <p>
 * Проверяются сценарии отказа перевода (карта-источник/назначения заблокирована,
 * неактивные карты) и успешный перевод с корректным обновлением балансов,
 * историй операций и статуса перевода.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private OperationHistoryRepository historyRepository;
    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private TransferService transferService;

    private final Long userId = 1L;
    private CardEntity activeFrom;
    private CardEntity activeTo;

    /**
     * Подготавливает активные карты-участники перевода для успешных/ошибочных сценариев,
     * а также владельца (пользователя).
     */
    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setFullName("Ivanov Ivan Ivanovich");

        activeFrom = new CardEntity();
        activeFrom.setId(10L);
        activeFrom.setUser(user);
        activeFrom.setOwner(user.getFullName());
        activeFrom.setStatus(CardStatus.ACTIVE);
        activeFrom.setExpiryDate(LocalDate.now().plusYears(2));
        activeFrom.setBalance(new BigDecimal("200.00"));

        activeTo = new CardEntity();
        activeTo.setId(11L);
        activeTo.setUser(user);
        activeTo.setOwner(user.getFullName());
        activeTo.setStatus(CardStatus.ACTIVE);
        activeTo.setExpiryDate(LocalDate.now().plusYears(2));
        activeTo.setBalance(new BigDecimal("50.00"));
    }

    private void stubEncrypt(String from, String to) {
        when(encryptionService.encrypt(from)).thenReturn("enc_" + from);
        when(encryptionService.encrypt(to)).thenReturn("enc_" + to);
    }

    private void stubFindCards(CardEntity fromCard, CardEntity toCard, String from, String to) {
        when(cardRepository.findByCardNumberEncrypted("enc_" + from)).thenReturn(Optional.ofNullable(fromCard));
        when(cardRepository.findByCardNumberEncrypted("enc_" + to)).thenReturn(Optional.ofNullable(toCard));
    }

    /**
     * Перевод отклоняется, если карта-источник заблокирована.
     * Ожидания: бросается BadRequestException, операции/перевод не сохраняются.
     */
    @Test
    void transfer_fails_when_source_card_blocked() {
        String from = "4111111111111111";
        String to = "5555555555554444";
        stubEncrypt(from, to);

        CardEntity blockedFrom = cloneCard(activeFrom);
        blockedFrom.setStatus(CardStatus.BLOCKED);

        stubFindCards(blockedFrom, activeTo, from, to);

        TransferRequest req = new TransferRequest(from, to, new BigDecimal("10.00"));

        ApiExceptions.BadRequestException ex = assertThrows(ApiExceptions.BadRequestException.class,
                () -> transferService.transfer(userId, req));
        assertEquals("Source card is blocked", ex.getMessage());

        verifyNoInteractions(transferRepository);
        verify(historyRepository, never()).save(any());
    }

    /**
     * Перевод отклоняется, если карта-назначения заблокирована.
     * Ожидания: бросается BadRequestException, операции/перевод не сохраняются.
     */
    @Test
    void transfer_fails_when_target_card_blocked() {
        String from = "4111111111111111";
        String to = "5555555555554444";
        stubEncrypt(from, to);

        CardEntity blockedTo = cloneCard(activeTo);
        blockedTo.setStatus(CardStatus.BLOCKED);

        stubFindCards(activeFrom, blockedTo, from, to);

        TransferRequest req = new TransferRequest(from, to, new BigDecimal("10.00"));

        ApiExceptions.BadRequestException ex = assertThrows(ApiExceptions.BadRequestException.class,
                () -> transferService.transfer(userId, req));
        assertEquals("Target card is blocked", ex.getMessage());

        verifyNoInteractions(transferRepository);
        verify(historyRepository, never()).save(any());
    }

    /**
     * Перевод отклоняется, если любая из карт не в статусе ACTIVE (например, EXPIRED).
     * Ожидания: бросается BadRequestException, операции/перевод не сохраняются.
     */
    @Test
    void transfer_fails_when_any_card_not_active() {
        String from = "4111111111111111";
        String to = "5555555555554444";
        stubEncrypt(from, to);

        CardEntity expiredFrom = cloneCard(activeFrom);
        expiredFrom.setStatus(CardStatus.EXPIRED);

        stubFindCards(expiredFrom, activeTo, from, to);

        TransferRequest req = new TransferRequest(from, to, new BigDecimal("10.00"));

        ApiExceptions.BadRequestException ex = assertThrows(ApiExceptions.BadRequestException.class,
                () -> transferService.transfer(userId, req));
        assertEquals("Both cards must be ACTIVE", ex.getMessage());

        verifyNoInteractions(transferRepository);
        verify(historyRepository, never()).save(any());
    }

    /**
     * Успешный перевод обновляет балансы обеих карт, записывает две операции в историю,
     * сохраняет перевод со статусом SUCCESS и сохраняет обновлённые карты.
     */
    @Test
    void transfer_success_updates_balances_and_history() {
        String from = "4111111111111111";
        String to = "5555555555554444";
        stubEncrypt(from, to);
        stubFindCards(activeFrom, activeTo, from, to);

        // Stub save of transfer to set ID and return entity
        when(transferRepository.save(any(TransferEntity.class))).thenAnswer(invocation -> {
            TransferEntity t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(100L);
            return t;
        });

        TransferRequest req = new TransferRequest(from, to, new BigDecimal("25.00"));

        transferService.transfer(userId, req);

        // balances updated
        assertEquals(new BigDecimal("175.00"), activeFrom.getBalance());
        assertEquals(new BigDecimal("75.00"), activeTo.getBalance());

        // histories written
        verify(historyRepository, times(2)).save(any(OperationHistoryEntity.class));

        // final transfer status SUCCESS saved
        ArgumentCaptor<TransferEntity> captor = ArgumentCaptor.forClass(TransferEntity.class);
        verify(transferRepository, atLeastOnce()).save(captor.capture());
        boolean hasSuccess = captor.getAllValues().stream().anyMatch(t -> t.getStatus() == TransferStatus.SUCCESS);
        assertTrue(hasSuccess, "Final transfer status should be SUCCESS");

        // cards saved with new balances
        verify(cardRepository).save(activeFrom);
        verify(cardRepository).save(activeTo);
    }

    private static CardEntity cloneCard(CardEntity src) {
        CardEntity c = new CardEntity();
        c.setId(src.getId());
        c.setUser(src.getUser());
        c.setOwner(src.getOwner());
        c.setStatus(src.getStatus());
        c.setExpiryDate(src.getExpiryDate());
        c.setBalance(src.getBalance());
        return c;
    }
}
