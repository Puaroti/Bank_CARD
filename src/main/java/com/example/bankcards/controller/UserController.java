package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardFilter;
import com.example.bankcards.dto.PagedResponse;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.BalanceDto;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Пользовательский REST-контроллер.
 * Базовый путь: /api/user
 * <p>
 * Содержит операции для владельца карт: просмотр карт, запрос блокировки/разблокировки,
 * просмотр баланса и перевод средств между своими картами.
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "User endpoints to manage own cards and transfers")
@Slf4j
public class UserController {

    private final CardService cardService;
    private final TransferService transferService;

    public UserController(CardService cardService, TransferService transferService) {
        this.cardService = cardService;
        this.transferService = transferService;
    }

    @GetMapping("/users/{userId}/cards")
    @Operation(summary = "List current user's own cards with filters and pagination")
    @PreAuthorize("hasRole('USER')")
    /**
     * Возвращает список карт указанного пользователя с учётом правил доступа.
     * Пользователь с ролью USER получит только свои карты; ADMIN может запрашивать любые userId.
     *
     * @param userId идентификатор пользователя
     * @param page номер страницы (0-индексация)
     * @param size размер страницы
     * @param filter параметры фильтрации
     * @return PagedResponse с DTO карт
     */
    public ResponseEntity<PagedResponse<CardDto>> listMyCards(@PathVariable Long userId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size,
                                                              @Valid CardFilter filter) {
        log.info("[UserController] listMyCards userId={}, page={}, size={}, statusFilter={}, ownerContains={}",
                userId, page, size, filter != null ? filter.getStatus() : null, filter != null ? filter.getOwner() : null);
        PagedResponse<CardDto> response = cardService.listUserCards(userId, filter, page, size);
        log.debug("[UserController] listMyCards success userId={}, page={}, size={}, statusFilter={}, ownerContains={}, response={}",
                userId, page, size, filter != null ? filter.getStatus() : null, filter != null ? filter.getOwner() : null, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/cards")
    @Operation(summary = "Create a new card for current user")
    @PreAuthorize("hasRole('USER')")
    /**
     * Создаёт новую карту для указанного пользователя. Доступно владельцу (USER) для собственного userId.
     * Администратор использует отдельный эндпоинт в AdminController.
     *
     * @param userId идентификатор пользователя
     * @return 201 Created с DTO созданной карты
     */
    public ResponseEntity<CardDto> createMyCard(@PathVariable Long userId) {
        log.info("[UserController] createMyCard userId={}", userId);
        CardDto dto = cardService.createCard(userId);
        log.info("[UserController] createMyCard success userId={}, cardId={}", userId, dto.getId());
        return ResponseEntity.status(201).body(dto);
    }

    @PostMapping("/cards/{cardId}/block")
    @Operation(summary = "Request block own card")
    @PreAuthorize("hasRole('USER')")
    /**
     * Отправляет запрос на блокировку собственной карты.
     * В текущей реализации блокировка применяется немедленно.
     *
     * @param cardId идентификатор карты
     * @return DTO обновленной карты
     */
    public ResponseEntity<CardDto> requestBlock(@PathVariable Long cardId) {
        log.info("[UserController] requestBlock cardId={}", cardId);
        CardDto dto = cardService.requestBlock(cardId);
        log.info("[UserController] requestBlock success cardId={}, newStatus={}", cardId, dto.getStatus());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/cards/{cardId}/unblock")
    @Operation(summary = "Request unblock own card")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card unblocked (status set to ACTIVE)"),
            @ApiResponse(responseCode = "400", description = "Cannot activate expired card")
    })
    @PreAuthorize("hasRole('USER')")
    /**
     * Отправляет запрос на разблокировку собственной карты.
     * Попытка разблокировать карту со статусом EXPIRED завершится 400 Bad Request.
     *
     * @param cardId идентификатор карты
     * @return DTO обновленной карты
     */
    public ResponseEntity<CardDto> requestUnblock(@PathVariable Long cardId) {
        log.info("[UserController] requestUnblock cardId={}", cardId);
        CardDto dto = cardService.requestUnblock(cardId);
        log.info("[UserController] requestUnblock success cardId={}, newStatus={}", cardId, dto.getStatus());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/cards/{cardId}/balance")
    @Operation(summary = "Get balance of own card")
    @PreAuthorize("hasRole('USER')")
    /**
     * Возвращает текущий баланс карты.
     * Доступ разрешён только владельцу; ADMIN имеет доступ ко всем картам.
     *
     * @param cardId идентификатор карты
     * @return объект с текущим балансом
     */
    public ResponseEntity<BalanceDto> getBalance(@PathVariable Long cardId) {
        log.info("[UserController] getBalance cardId={}", cardId);
        BalanceDto dto = cardService.getBalance(cardId);
        log.debug("[UserController] getBalance cardId={}, balance={}", cardId, dto.balance());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/users/{userId}/transfers")
    @Operation(summary = "Transfer money between own cards")
    @PreAuthorize("hasRole('USER')")
    /**
     * Выполняет перевод средств между собственными картами пользователя.
     * Валидации и проверки принадлежности карт выполняются на уровне сервиса переводов.
     *
     * @param userId идентификатор пользователя
     * @param request детали перевода (карта-отправитель, карта-получатель, сумма)
     * @return 200 OK без тела при успехе
     */
    public ResponseEntity<Void> transfer(@PathVariable Long userId,
                                         @Valid @RequestBody TransferRequest request) {
        String fromMasked = request != null && request.fromCardNumber() != null && request.fromCardNumber().length() >= 4
                ? "****" + request.fromCardNumber().substring(request.fromCardNumber().length() - 4) : "(null)";
        String toMasked = request != null && request.toCardNumber() != null && request.toCardNumber().length() >= 4
                ? "****" + request.toCardNumber().substring(request.toCardNumber().length() - 4) : "(null)";
        log.info("[UserController] transfer userId={}, from={}, to={}, amount={}", userId, fromMasked, toMasked,
                request != null ? request.amount() : null);
        transferService.transfer(userId, request);
        log.info("[UserController] transfer success userId={}, from={}, to={}", userId, fromMasked, toMasked);
        return ResponseEntity.ok().build();
    }
}
