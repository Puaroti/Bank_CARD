package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Контроллер для управления картами.
 * <p>
 * Админские операции по выпуску и изменению статусов карт, а также пользовательские запросы
 * на блокировку. Путь: /api/cards.
 */
@RestController
@RequestMapping("/api/cards")
@Deprecated
@Tag(name = "Cards (Deprecated)", description = "Deprecated endpoints. Use /api/admin/** and /api/user/** instead.")
public class CardController {

    /**
     * Сервис для работы с картами.
     */
    private final CardService cardService;
    private static final Logger log = LoggerFactory.getLogger(CardController.class);

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/{userId}")
    @Operation(summary = "[DEPRECATED] Create card for user",
            description = "Use POST /api/admin/users/{userId}/cards (optionally with {\"owner\":\"...\"})",
            responses = {
            @ApiResponse(responseCode = "200", description = "Card created",
                    content = @Content(schema = @Schema(implementation = CardDto.class)))
    })
    /**
     * Создает карту для указанного пользователя. Все критичные поля формируются на стороне сервера:
     * случайный 16-значный номер, ФИО владельца из профиля пользователя, дата окончания — автоматически.
     *
     * @param userId идентификатор пользователя
     * @return созданная карта в виде DTO
     */
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> create(@PathVariable Long userId) {
        log.warn("[DEPRECATED] POST /api/cards/{}/ -> use POST /api/admin/users/{}/cards", userId, userId);
        return ResponseEntity.ok(cardService.createCard(userId));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "[DEPRECATED] List user's cards",
            description = "Use GET /api/user/users/{userId}/cards")
    /**
     * Возвращает список карт пользователя с пагинацией и фильтрами.
     *
     * @param userId идентификатор пользователя
     * @param page номер страницы (по умолчанию 0)
     * @param size размер страницы (по умолчанию 20)
     * @param filter параметры фильтрации (необязательно)
     * @return страничный ответ с картами пользователя
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<PagedResponse<CardDto>> list(@PathVariable Long userId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       @Valid CardFilter filter) {
        log.warn("[DEPRECATED] GET /api/cards/{}/ -> use GET /api/user/users/{}/cards", userId, userId);
        return ResponseEntity.ok(cardService.listUserCards(userId, filter, page, size));
    }


    @PatchMapping("/{cardId}/status")
    @Operation(summary = "[DEPRECATED] Update card status",
            description = "Use PATCH /api/admin/cards/{cardId}/status")
    /**
     * Обновляет статус карты по идентификатору.
     *
     * @param cardId идентификатор карты
     * @param request запрос с новым статусом
     * @return обновленная карта в виде DTO
     */
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> updateStatus(@PathVariable Long cardId,
                                                @Valid @RequestBody CardUpdateStatusRequest request) {
        log.warn("[DEPRECATED] PATCH /api/cards/{}/status -> use PATCH /api/admin/cards/{}/status", cardId, cardId);
        return ResponseEntity.ok(cardService.updateStatus(cardId, request));
    }

    @PostMapping("/{cardId}/block-request")
    @Operation(summary = "[DEPRECATED] Request card block",
            description = "Use POST /api/user/cards/{cardId}/block (user) or /api/admin/cards/{cardId}/block (admin)",
            responses = @ApiResponse(responseCode = "200", description = "Card blocked",
                    content = @Content(schema = @Schema(implementation = CardDto.class))))
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CardDto> requestBlock(@PathVariable Long cardId) {
        log.warn("[DEPRECATED] POST /api/cards/{}/block-request -> use /api/user|admin paths", cardId);
        return ResponseEntity.ok(cardService.requestBlock(cardId));
    }

    /**
     * Тело запроса для выпуска дополнительной карты.
     *
     * @param owner имя владельца, печатаемое на карте
     */
    record AdditionalCardRequest(String owner) {}

    @PostMapping("/{userId}/additional")
    @Operation(summary = "[DEPRECATED] Issue an additional card for the user",
            description = "Use POST /api/admin/users/{userId}/cards with optional body {owner}",
            responses = @ApiResponse(responseCode = "200", description = "Additional card issued",
                    content = @Content(schema = @Schema(implementation = CardDto.class))))
    /**
     * Выпускает дополнительную карту для пользователя.
     *
     * @param userId идентификатор пользователя
     * @param request опциональное тело запроса с именем владельца, которое будет напечатано на карте
     * @return DTO выпущенной дополнительной карты
     */
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> issueAdditional(@PathVariable Long userId,
                                                   @RequestBody(required = false) AdditionalCardRequest request) {
        String owner = request != null ? request.owner() : null;
        log.warn("[DEPRECATED] POST /api/cards/{}/additional -> use POST /api/admin/users/{}/cards", userId, userId);
        return ResponseEntity.ok(cardService.issueAutoCard(userId, owner));
    }
}
