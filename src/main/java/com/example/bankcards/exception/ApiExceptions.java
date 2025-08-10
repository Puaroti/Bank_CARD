package com.example.bankcards.exception;

/**
 * Common runtime exceptions used by the application/service layers.
 * <p>
 * Эти исключения предназначены для генерации контролируемых ошибок домена/приложения,
 * которые далее транслируются в корректные HTTP-ответы через {@code GlobalExceptionHandler}.
 * Типичные соответствия:
 * <ul>
 *   <li>{@link ApiExceptions.NotFoundException} → HTTP 404 Not Found</li>
 *   <li>{@link ApiExceptions.BadRequestException} → HTTP 400 Bad Request</li>
 *   <li>{@link ApiExceptions.ConflictException} → HTTP 409 Conflict</li>
 * </ul>
 * Исключения являются unchecked (наследуются от {@link RuntimeException}) и могут
 * пробрасываться вверх без явного объявления.
 */
public class ApiExceptions {
    /**
     * Thrown when a requested resource cannot be found.
     * <p>
     * Примеры: карта/пользователь/перевод не найдены по идентификатору.
     * Как правило, маппится на HTTP 404 Not Found глобальным обработчиком.
     */
    public static class NotFoundException extends RuntimeException {
        /**
         * Creates a NotFoundException with a descriptive message.
         * @param message описание отсутствующего ресурса или причины
         */
        public NotFoundException(String message) { super(message); }
    }
    /**
     * Thrown when the request is syntactically correct but violates business rules
     * or fails validation.
     * <p>
     * Примеры: попытка разблокировать EXPIRED карту, доступ к чужой карте,
     * некорректные параметры перевода и т.д. Обычно соответствует HTTP 400 Bad Request.
     */
    public static class BadRequestException extends RuntimeException {
        /**
         * Creates a BadRequestException with a descriptive message.
         * @param message описание ошибки валидации/бизнес-правила
         */
        public BadRequestException(String message) { super(message); }
    }
    /**
     * Thrown when an action conflicts with the current state of the resource.
     * <p>
     * Примеры: гонки обновлений, повторные операции, нарушающие уникальность/состояние.
     * Обычно соответствует HTTP 409 Conflict.
     */
    public static class ConflictException extends RuntimeException {
        /**
         * Creates a ConflictException with a descriptive message.
         * @param message описание конфликта состояния
         */
        public ConflictException(String message) { super(message); }
    }
}
