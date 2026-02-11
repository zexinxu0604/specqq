package com.specqq.chatbot.exception;

/**
 * Business Exception
 *
 * <p>Thrown when business logic validation fails or business rules are violated.</p>
 *
 * <p>This exception is caught by {@link GlobalExceptionHandler} and returned as a user-friendly error response.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public class BusinessException extends RuntimeException {

    /**
     * Constructs a new business exception with the specified detail message.
     *
     * @param message the detail message
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Constructs a new business exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
