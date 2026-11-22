package com.coworkhub.exception;

/** Intento de eliminar un registro que otras entidades usan (RN-12). HTTP 409. */
public class ResourceInUseException extends BusinessRuleException {

    public ResourceInUseException(String message) {
        super(ErrorCodes.RN_12, message);
    }
}
