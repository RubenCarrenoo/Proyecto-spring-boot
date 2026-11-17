package com.coworkhub.exception;

/**
 * Incumplimiento de una regla de negocio. Se responde con HTTP 409 y el código de la regla (RN-xx).
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
