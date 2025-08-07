package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.*;
import com.example.bankcards.exception.ApiExceptions;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.OperationHistoryRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

/**
 * Сервис для перевода средств между картами одного пользователя.
 */
@Service
public class TransferService {

    /** Репозиторий карт. */
    private final CardRepository cardRepository;
    /** Репозиторий перевода. */
    private final TransferRepository transferRepository;
    /** Репозиторий истории операций по картам. */
    private final OperationHistoryRepository historyRepository;
    /** Сервис шифрования для поиска карт по зашифрованному номеру. */
    private final EncryptionService encryptionService;
    /** Репозиторий пользователей. */
    private final UserRepository userRepository;

    /**
     * Конструктор сервиса переводов.
     *
     * @param cardRepository репозиторий карт
     * @param transferRepository репозиторий переводов
     * @param historyRepository репозиторий истории операций
     * @param encryptionService сервис шифрования
     */
    public TransferService(CardRepository cardRepository,
                           TransferRepository transferRepository,
                           OperationHistoryRepository historyRepository,
                           EncryptionService encryptionService,
                           UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.transferRepository = transferRepository;
        this.historyRepository = historyRepository;
        this.encryptionService = encryptionService;
        this.userRepository = userRepository;
    }

    @Transactional
    /**
     * Выполняет перевод средств между картами одного пользователя.
     *
     * Валидации:
     * - обе карты должны принадлежать пользователю userId;
     * - обе карты должны быть в статусе ACTIVE;
     * - на исходной карте достаточно средств для списания суммы amount.
     *
     * @param userId идентификатор пользователя
     * @param request параметры перевода (номера карт-источника и назначения, сумма)
     * @throws com.example.bankcards.exception.ApiExceptions.NotFoundException если одна из карт не найдена
     * @throws com.example.bankcards.exception.ApiExceptions.BadRequestException при нарушении правил перевода
     */
    public void transfer(Long userId, TransferRequest request) {
        // Only ADMIN can initiate transfers for arbitrary userId.
        // Regular USER must match path userId to their own id.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_ADMIN"));
            if (!isAdmin) {
                String username = auth.getName();
                Long currentUserId = userRepository.findByUsername(username)
                        .map(UserEntity::getId)
                        .orElseThrow(() -> new ApiExceptions.BadRequestException("Current user not found"));
                if (!currentUserId.equals(userId)) {
                    throw new ApiExceptions.BadRequestException("Access denied to initiate transfer for another user");
                }
            }
        }
        String encFrom = encryptionService.encrypt(request.fromCardNumber());
        String encTo = encryptionService.encrypt(request.toCardNumber());

        CardEntity from = cardRepository.findByCardNumberEncrypted(encFrom)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Source card not found"));
        CardEntity to = cardRepository.findByCardNumberEncrypted(encTo)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Target card not found"));

        if (!from.getUser().getId().equals(userId) || !to.getUser().getId().equals(userId)) {
            throw new ApiExceptions.BadRequestException("Transfer allowed only between user's own cards");
        }
        // Explicitly forbid operations if any of the cards is BLOCKED
        if (from.getStatus() == CardStatus.BLOCKED) {
            throw new ApiExceptions.BadRequestException("Source card is blocked");
        }
        if (to.getStatus() == CardStatus.BLOCKED) {
            throw new ApiExceptions.BadRequestException("Target card is blocked");
        }
        // General rule: both cards must be ACTIVE (EXPIRED or other non-ACTIVE statuses are not allowed)
        if (from.getStatus() != CardStatus.ACTIVE || to.getStatus() != CardStatus.ACTIVE) {
            throw new ApiExceptions.BadRequestException("Both cards must be ACTIVE");
        }
        BigDecimal amount = request.amount();
        if (from.getBalance().compareTo(amount) < 0) {
            throw new ApiExceptions.BadRequestException("Insufficient funds");
        }

        // Create transfer record
        TransferEntity transfer = new TransferEntity();
        transfer.setFromCard(from);
        transfer.setToCard(to);
        transfer.setAmount(amount);
        transfer.setStatus(TransferStatus.PENDING);
        transfer = transferRepository.save(transfer);

        // Update balances
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        cardRepository.save(from);
        cardRepository.save(to);

        // History entries
        OperationHistoryEntity out = new OperationHistoryEntity();
        out.setCard(from);
        out.setOperationType(OperationType.TRANSFER_OUT);
        out.setAmount(amount);
        out.setDescription("Transfer to card");
        historyRepository.save(out);

        OperationHistoryEntity in = new OperationHistoryEntity();
        in.setCard(to);
        in.setOperationType(OperationType.TRANSFER_IN);
        in.setAmount(amount);
        in.setDescription("Transfer from card");
        historyRepository.save(in);

        // Mark success
        transfer.setStatus(TransferStatus.SUCCESS);
        transferRepository.save(transfer);
    }
}
