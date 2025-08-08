package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardFilter;
import com.example.bankcards.dto.PagedResponse;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.BalanceDto;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
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
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "User endpoints to manage own cards and transfers")
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
     * Возвращает список карт текущего пользователя. Для обычного пользователя
     * метод вернёт только его карты (проверка выполняется в сервисе), для ADMIN — разрешено запрашивать любые userId.
     */
    public ResponseEntity<PagedResponse<CardDto>> listMyCards(@PathVariable Long userId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size,
                                                              @Valid CardFilter filter) {
        return ResponseEntity.ok(cardService.listUserCards(userId, filter, page, size));
    }

    @PostMapping("/cards/{cardId}/block")
    @Operation(summary = "Request block own card")
    @PreAuthorize("hasRole('USER')")
    /**
     * Отправляет запрос на блокировку собственной карты. Сейчас блокировка происходит сразу.
     */
    public ResponseEntity<CardDto> requestBlock(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.requestBlock(cardId));
    }

    @PostMapping("/cards/{cardId}/unblock")
    @Operation(summary = "Request unblock own card")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card unblocked (status set to ACTIVE)"),
            @ApiResponse(responseCode = "400", description = "Cannot activate expired card")
    })
    @PreAuthorize("hasRole('USER')")
    /**
     * Отправляет запрос на разблокировку собственной карты. Для карт со статусом EXPIRED вернёт 400.
     */
    public ResponseEntity<CardDto> requestUnblock(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.requestUnblock(cardId));
    }

    @GetMapping("/cards/{cardId}/balance")
    @Operation(summary = "Get balance of own card")
    @PreAuthorize("hasRole('USER')")
    /**
     * Возвращает текущий баланс карты. Доступ только владельцу карты (ADMIN имеет доступ ко всем картам).
     */
    public ResponseEntity<BalanceDto> getBalance(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getBalance(cardId));
    }

    @PostMapping("/users/{userId}/transfers")
    @Operation(summary = "Transfer money between own cards")
    @PreAuthorize("hasRole('USER')")
    /**
     * Выполняет перевод средств между собственными картами пользователя. Валидации и проверки на принадлежность
     * карт выполняются на уровне сервиса переводов.
     */
    public ResponseEntity<Void> transfer(@PathVariable Long userId,
                                         @Valid @RequestBody TransferRequest request) {
        transferService.transfer(userId, request);
        return ResponseEntity.ok().build();
    }
}
