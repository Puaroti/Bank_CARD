package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardFilter;
import com.example.bankcards.dto.PagedResponse;
import com.example.bankcards.dto.UserSummaryDto;
import com.example.bankcards.dto.UserWithCardCountDto;
import com.example.bankcards.dto.CardUpdateStatusRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.exception.ApiExceptions;

/**
 * Административный REST-контроллер.
 * Маршрутная префикс-группа: /api/admin
 * <p>
 * Содержит операции для администрирования пользователей и их карт: поиск/список пользователей,
 * просмотр и изменение статусов карт, выпуск карт, удаление пользователей и т.п.
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Admin-only management endpoints")
@Slf4j
public class AdminController {

    private final CardService cardService;
    private final UserRepository userRepository;

    public AdminController(CardService cardService, UserRepository userRepository) {
        this.cardService = cardService;
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    @Operation(summary = "List all users with their card counts (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    /**
     * Возвращает список всех пользователей с количеством их карт.
     * Доступно только для роли ADMIN.
     *
     * @return 200 OK со списком UserWithCardCountDto
     */
    public ResponseEntity<java.util.List<UserWithCardCountDto>> listUsersWithCardCounts() {
        log.info("[AdminController] listUsersWithCardCounts");
        return ResponseEntity.ok(userRepository.findAllWithCardCounts());
    }

    
    @GetMapping("/cards")
    @Operation(summary = "List all cards (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    /**
     * Возвращает страницу всех карт с фильтрами и пагинацией.
     * Доступно только для роли ADMIN.
     *
     * @param page номер страницы (0-индексация)
     * @param size размер страницы
     * @param filter фильтры (статус, владелец, строка поиска)
     * @return 200 OK с PagedResponse<CardDto>
     */
    public ResponseEntity<PagedResponse<CardDto>> listAll(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @Valid CardFilter filter) {
        log.info("[AdminController] listAllCards page={}, size={}, statusFilter={}, ownerContains={}",
                page, size, filter != null ? filter.getStatus() : null, filter != null ? filter.getOwner() : null);
        return ResponseEntity.ok(cardService.listAllCards(filter, page, size));
    }

    @PatchMapping("/cards/{cardId}/status")
    @Operation(summary = "Update card status by cardId (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    /**
     * Универсальное админ-обновление статуса карты.
     * Например, ACTIVE/BLOCKED/EXPIRED (в рамках бизнес-правил сервиса).
     */
    public ResponseEntity<CardDto> adminUpdateCardStatus(@PathVariable Long cardId,
                                                         @Valid @RequestBody CardUpdateStatusRequest request) {
        log.info("[AdminController] updateCardStatus cardId={}, newStatus={}", cardId, request != null ? request.status() : null);
        CardDto dto = cardService.updateStatus(cardId, request);
        log.info("[AdminController] updateCardStatus success cardId={}, newStatus={}", cardId, dto.getStatus());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/cards/{cardId}/block")
    @Operation(summary = "Block user's card by cardId (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    /**
     * Блокирует карту пользователя по идентификатору.
     * Доступно только для роли ADMIN.
     *
     * @param cardId идентификатор карты
     * @return 200 OK с DTO заблокированной карты
     */
    public ResponseEntity<CardDto> adminBlockCard(@PathVariable Long cardId) {
        log.info("[AdminController] blockCard cardId={}", cardId);
        CardDto dto = cardService.requestBlock(cardId);
        log.info("[AdminController] blockCard success cardId={}, newStatus={}", cardId, dto.getStatus());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/cards/{cardId}/unblock")
    @Operation(summary = "Unblock user's card by cardId (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card unblocked (status set to ACTIVE)"),
            @ApiResponse(responseCode = "400", description = "Cannot activate expired card")
    })
    @PreAuthorize("hasRole('ADMIN')")
    /**
     * Разблокирует карту пользователя (устанавливает статус ACTIVE).
     * Доступно только для роли ADMIN.
     * Примечание: попытка разблокировать карту со статусом EXPIRED вернёт 400 Bad Request.
     *
     * @param cardId идентификатор карты
     * @return 200 OK с DTO обновленной карты
     */
    public ResponseEntity<CardDto> adminUnblockCard(@PathVariable Long cardId) {
        log.info("[AdminController] unblockCard cardId={}", cardId);
        CardDto dto = cardService.updateStatus(cardId, new CardUpdateStatusRequest(CardStatus.ACTIVE));
        log.info("[AdminController] unblockCard success cardId={}, newStatus={}", cardId, dto.getStatus());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/users/search")
    @Operation(summary = "Find user by username (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    /**
     * Ищет пользователя по имени (username).
     * Доступно только для роли ADMIN.
     *
     * @param username имя пользователя
     * @return 200 OK с краткой информацией о пользователе или 404, если не найден
     */
    public ResponseEntity<UserSummaryDto> findUserByUsername(@RequestParam String username) {
        log.info("[AdminController] findUserByUsername username={}", username);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("User not found"));
        UserSummaryDto dto = new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getFirstName(),
                user.getLastName(),
                user.getPatronymic(),
                user.getRole()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user by id (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    /**
        * Удаляет пользователя по идентификатору.
        * Доступно только для роли ADMIN.
        *
        * @param userId идентификатор пользователя
        * @return 204 No Content при успешном удалении, 404 если не найден
        */
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.warn("[AdminController] deleteUser userId={}", userId);
        if (!userRepository.existsById(userId)) {
            throw new ApiExceptions.NotFoundException("User not found");
        }
        userRepository.deleteById(userId);
        return ResponseEntity.noContent().build();
    }

    /** Тело запроса при выпуске карты админом (опционально указать owner). */
    record AdminCreateCardRequest(String owner) {}

    @PostMapping("/users/{userId}/cards")
    @Operation(summary = "Create a card for user (admin only). If owner is provided in body, it will be used as printed name")
    @PreAuthorize("hasRole('ADMIN')")
    /**
     * Выпускает новую карту для пользователя. По умолчанию ФИО владельца берётся из профиля пользователя.
     * Можно опционально передать owner в теле запроса, чтобы напечатать на карте другое имя (например, доп.карта).
     * Доступно только для роли ADMIN.
     *
     * @param userId идентификатор пользователя
     * @return 201 Created с DTO созданной карты
     */
    public ResponseEntity<CardDto> adminCreateUserCard(@PathVariable Long userId,
                                                       @RequestBody(required = false) AdminCreateCardRequest req) {
        String owner = (req != null) ? req.owner() : null;
        log.info("[AdminController] createUserCard userId={}, ownerProvided={}", userId, owner != null);
        CardDto dto = cardService.issueAutoCard(userId, owner);
        log.info("[AdminController] createUserCard success userId={}, cardId={}", userId, dto.getId());
        return ResponseEntity.status(201).body(dto);
    }
}
