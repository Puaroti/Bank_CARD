package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST-контроллер для перевода средств между картами одного пользователя.
 * <p>
 * Функциональность:
 * <ul>
 *   <li>Создание перевода между собственными картами пользователя.</li>
 * </ul>
 * Безопасность:
 * <ul>
 *   <li>Доступ разрешен пользователям с ролями USER или ADMIN.</li>
 *   <li>Аутентификация осуществляется через глобальную конфигурацию безопасности.</li>
 * </ul>
 * Маршруты:
 * <ul>
 *   <li>POST /api/transfers/{userId} — выполнить перевод между картами пользователя.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Transfers", description = "Money transfer endpoints")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/{userId}")
    @Operation(summary = "Transfer money between user's own cards")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    /**
     * Выполняет перевод средств между картами одного и того же пользователя.
     * <p>
     * Требует аутентификации и наличия роли USER или ADMIN. Валидация входных данных
     * выполняется через Bean Validation.
     *
     * @param userId  идентификатор пользователя, для которого выполняется перевод
     * @param request тело запроса с деталями перевода (источник, назначение, сумма и пр.)
     * @return 200 OK в случае успешного выполнения операции без тела ответа
     */
    public ResponseEntity<Void> transfer(@PathVariable Long userId,
                                         @Valid @RequestBody TransferRequest request) {
        transferService.transfer(userId, request);
        return ResponseEntity.ok().build();
    }
}
