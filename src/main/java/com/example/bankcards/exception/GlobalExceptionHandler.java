package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений REST-слоя.
 * <p>
 * Централизует маппинг исключений на HTTP-коды и форматы ответов, чтобы API
 * возвращал предсказуемые и единообразные ошибки. Бизнес-исключения см. в
 * {@link ApiExceptions}.
 * <p>
 * Форматы ответов:
 * <ul>
 *   <li>Валидация: {@code 400 Bad Request} + map поле→сообщение</li>
 *   <li>NotFound: {@code 404 Not Found} + {"error": message}</li>
 *   <li>BadRequest: {@code 400 Bad Request} + {"error": message}</li>
 *   <li>AccessDenied: {@code 403 Forbidden} + {"error": "Access denied"}</li>
 *   <li>Unauthorized: {@code 401 Unauthorized} + {"error": "Unauthorized"}</li>
 *   <li>Прочее: {@code 500 Internal Server Error} + {"error": "Internal server error"}</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обрабатывает ошибки валидации аргументов контроллера.
     * Возвращает 400 и карту ошибок вида field→message.
     *
     * @param ex исключение {@link MethodArgumentNotValidException}
     * @return 400 Bad Request с подробностями по полям
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Обрабатывает доменное отсутствие ресурса.
     * Возвращает 404 с сообщением.
     *
     * @param ex {@link ApiExceptions.NotFoundException}
     * @return 404 Not Found с {"error": message}
     */
    @ExceptionHandler(ApiExceptions.NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ApiExceptions.NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Обрабатывает бизнес-ошибки/валидацию на уровне сервиса/приложения.
     * Возвращает 400 с сообщением.
     *
     * @param ex {@link ApiExceptions.BadRequestException}
     * @return 400 Bad Request с {"error": message}
     */
    @ExceptionHandler(ApiExceptions.BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(ApiExceptions.BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Обрабатывает запрет доступа (авторизация ок, но прав недостаточно).
     * Возвращает 403.
     *
     * @param ex {@link AuthorizationDeniedException} или {@link AccessDeniedException}
     * @return 403 Forbidden с {"error": "Access denied"}
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<Map<String, String>> handleAccessDenied(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
    }

    /**
     * Обрабатывает ошибки аутентификации (нет/невалидный токен и т.п.).
     * Возвращает 401.
     *
     * @param ex {@link AuthenticationException} или {@link AuthenticationServiceException}
     * @return 401 Unauthorized с {"error": "Unauthorized"}
     */
    @ExceptionHandler({AuthenticationException.class, AuthenticationServiceException.class})
    public ResponseEntity<Map<String, String>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
    }

    /**
     * Запасной обработчик неожиданных ошибок.
     * Возвращает 500 без подробностей реализации.
     *
     * @param ex любое необработанное исключение
     * @return 500 Internal Server Error с общим сообщением
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal server error"));
    }
}
