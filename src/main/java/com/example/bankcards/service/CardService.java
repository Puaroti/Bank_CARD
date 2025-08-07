package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.OperationHistoryEntity;
import com.example.bankcards.entity.OperationType;
import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.exception.ApiExceptions;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.OperationHistoryRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.MapperUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDate;
import java.util.Random;

/**
 * Сервис для работы с банковскими картами.
 */
@Service
@Tag(name = "Card Service", description = "Business logic for cards")
public class CardService {

    /**
     * Репозиторий для операций с сущностью карты.
     */
    private final CardRepository cardRepository;
    /**
     * Репозиторий для операций с пользователями.
     */
    private final UserRepository userRepository;
    private final OperationHistoryRepository historyRepository;
    /**
     * Сервис шифрования номеров карт.
     */
    private final EncryptionService encryptionService;

    public CardService(CardRepository cardRepository,
                       UserRepository userRepository,
                       EncryptionService encryptionService,
                       OperationHistoryRepository historyRepository) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.historyRepository = historyRepository;
    }

    @Transactional
    /**
     * Создает новую карту с автозаполнением данных на стороне бэкенда.
     *
     * Номер карты генерируется случайно (16 цифр), владелец подтягивается из профиля пользователя (ФИО),
     * срок действия устанавливается автоматически (через 4 года, первый день месяца).
     * Статус устанавливается ACTIVE, баланс — 0.
     *
     * @param userId идентификатор пользователя, которому принадлежит карта
     * @return DTO созданной карты
     * @throws com.example.bankcards.exception.ApiExceptions.NotFoundException если пользователь не найден
     */
    public CardDto createCard(Long userId) {
        return issueAutoCard(userId, null);
    }

    /**
     * Возвращает список карт пользователя с поддержкой пагинации и фильтров.
     *
     * @param userId идентификатор пользователя
     * @param filter необязательный фильтр (статус, владелец, строка поиска)
     * @param page номер страницы (0-индексация)
     * @param size размер страницы
     * @return страничный ответ с DTO карт
     */
    public PagedResponse<CardDto> listUserCards(Long userId, CardFilter filter, int page, int size) {
        // Enforce that regular USER can only view own cards; ADMIN can view any
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
                    throw new ApiExceptions.BadRequestException("Access denied to other user's cards");
                }
            }
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<CardEntity> paged = cardRepository.search(
                userId,
                filter != null ? filter.getStatus() : null,
                filter != null ? filter.getOwner() : null,
                filter != null ? filter.getQuery() : null,
                pageable);
        return new PagedResponse<>(
                paged.map(MapperUtils::toCardDto).getContent(),
                paged.getNumber(),
                paged.getSize(),
                paged.getTotalElements(),
                paged.getTotalPages()
        );
    }

    /**
     * Список всех карт (для админа) с фильтрами и пагинацией.
     */
    public PagedResponse<CardDto> listAllCards(CardFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CardEntity> paged = cardRepository.searchAll(
                filter != null ? filter.getStatus() : null,
                filter != null ? filter.getOwner() : null,
                filter != null ? filter.getQuery() : null,
                pageable);
        return new PagedResponse<>(
                paged.map(MapperUtils::toCardDto).getContent(),
                paged.getNumber(),
                paged.getSize(),
                paged.getTotalElements(),
                paged.getTotalPages()
        );
    }

    @Transactional
    /**
     * Запрос блокировки карты пользователем. Сейчас — немедленная блокировка,
     * чтобы не вводить новый статус в перечисление. Дальше можно заменить на PENDING-логику.
     */
    public CardDto requestBlock(Long cardId) {
        CardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Card not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            String username = auth != null ? auth.getName() : null;
            if (username == null) throw new ApiExceptions.BadRequestException("Not authenticated");
            Long currentUserId = userRepository.findByUsername(username)
                    .map(UserEntity::getId)
                    .orElseThrow(() -> new ApiExceptions.BadRequestException("Current user not found"));
            if (!card.getUser().getId().equals(currentUserId)) {
                throw new ApiExceptions.BadRequestException("Cannot request block for another user's card");
            }
        }

        if (card.getStatus() == CardStatus.BLOCKED) {
            return MapperUtils.toCardDto(card);
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new ApiExceptions.BadRequestException("Cannot block expired card");
        }

        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);

        OperationHistoryEntity hist = new OperationHistoryEntity();
        hist.setCard(card);
        hist.setOperationType(OperationType.BLOCK);
        hist.setAmount(java.math.BigDecimal.ZERO);
        hist.setDescription("Card blocked by user request");
        historyRepository.save(hist);

        return MapperUtils.toCardDto(card);
    }

    @Transactional
    /**
     * Запрос разблокировки карты пользователем. Сейчас — немедленная разблокировка
     * (установка статуса ACTIVE), если карта принадлежит текущему пользователю и
     * не является просроченной.
     */
    public CardDto requestUnblock(Long cardId) {
        CardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Card not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            String username = auth != null ? auth.getName() : null;
            if (username == null) throw new ApiExceptions.BadRequestException("Not authenticated");
            Long currentUserId = userRepository.findByUsername(username)
                    .map(UserEntity::getId)
                    .orElseThrow(() -> new ApiExceptions.BadRequestException("Current user not found"));
            if (!card.getUser().getId().equals(currentUserId)) {
                throw new ApiExceptions.BadRequestException("Cannot request unblock for another user's card");
            }
        }

        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new ApiExceptions.BadRequestException("Cannot activate expired card");
        }
        if (card.getStatus() == CardStatus.ACTIVE) {
            return MapperUtils.toCardDto(card);
        }

        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);

        OperationHistoryEntity hist = new OperationHistoryEntity();
        hist.setCard(card);
        hist.setOperationType(OperationType.UNBLOCK);
        hist.setAmount(java.math.BigDecimal.ZERO);
        hist.setDescription("Card unblocked by user request");
        historyRepository.save(hist);

        return MapperUtils.toCardDto(card);
    }

    @Transactional
    /**
     * Обновляет статус карты.
     *
     * @param cardId идентификатор карты
     * @param request запрос с новым статусом карты
     * @return DTO обновленной карты
     * @throws com.example.bankcards.exception.ApiExceptions.NotFoundException если карта не найдена
     * @throws com.example.bankcards.exception.ApiExceptions.BadRequestException если пытаемся активировать просроченную карту
     */
    public CardDto updateStatus(Long cardId, CardUpdateStatusRequest request) {
        CardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Card not found"));
        // Блокируем попытку активации просроченной карты
        if (card.getStatus() == CardStatus.EXPIRED && request.status() == CardStatus.ACTIVE) {
            throw new ApiExceptions.BadRequestException("Cannot activate expired card");
        }
        card.setStatus(request.status());
        return MapperUtils.toCardDto(cardRepository.save(card));
    }

    /**
     * Находит карту по зашифрованному номеру.
     *
     * @param encrypted зашифрованный номер карты
     * @return Optional с сущностью карты, если найдена
     */
    public Optional<CardEntity> findByEncrypted(String encrypted) {
        return cardRepository.findByCardNumberEncrypted(encrypted);
    }

    /** Возвращает баланс карты с проверкой доступа. */
    public com.example.bankcards.dto.BalanceDto getBalance(Long cardId) {
        CardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Card not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            String username = auth != null ? auth.getName() : null;
            if (username == null) throw new ApiExceptions.BadRequestException("Not authenticated");
            Long currentUserId = userRepository.findByUsername(username)
                    .map(UserEntity::getId)
                    .orElseThrow(() -> new ApiExceptions.BadRequestException("Current user not found"));
            if (!card.getUser().getId().equals(currentUserId)) {
                throw new ApiExceptions.BadRequestException("Access denied to other user's card balance");
            }
        }

        return new com.example.bankcards.dto.BalanceDto(card.getBalance());
    }

    @Transactional
    /**
     * Выпускает дополнительную (автоматическую) карту для пользователя.
     *
     * Генерирует случайный 16-значный номер, устанавливает срок действия на 4 года вперед
     * и владельца (полное имя пользователя или переданное значение), шифрует номер и сохраняет карту.
     *
     * @param userId идентификатор пользователя
     * @param ownerFullName ФИО владельца на карте (необязательно). Если не указано — берется из профиля пользователя
     * @return DTO выпущенной карты
     */
    public CardDto issueAutoCard(Long userId, String ownerFullName) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("User not found"));

        // Generate a 16-digit card number (simple random, for demo purposes)
        String cardNumber;
        String encrypted;
        LocalDate expiry = LocalDate.now().plusYears(4).withDayOfMonth(1);

        int attempts = 0;
        do {
            cardNumber = generateCardNumber();
            encrypted = encryptionService.encrypt(cardNumber);
            attempts++;
        } while (cardRepository.findByCardNumberEncrypted(encrypted).isPresent() && attempts < 10);
        if (cardRepository.findByCardNumberEncrypted(encrypted).isPresent()) {
            throw new ApiExceptions.ConflictException("Failed to generate unique card number, please retry");
        }

        CardEntity entity = new CardEntity();
        entity.setCardNumberEncrypted(encrypted);
        String owner = (ownerFullName == null || ownerFullName.isBlank()) ? user.getFullName() : ownerFullName;
        entity.setOwner(owner);
        entity.setExpiryDate(expiry);
        entity.setStatus(CardStatus.ACTIVE);
        entity.setUser(user);
        entity.setBalance(java.math.BigDecimal.ZERO);
        CardEntity saved = cardRepository.save(entity);
        return MapperUtils.toCardDto(saved);
    }

    /**
     * Генерирует случайный 16-значный номер карты (для демонстрационных целей).
     *
     *
     * @return строка из 16 цифр
     */
    private String generateCardNumber() {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(rnd.nextInt(10));
        }
        return sb.toString();
    }
}
