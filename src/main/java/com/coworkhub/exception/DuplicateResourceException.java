package com.coworkhub.exception;

/** Violación de unicidad (RN-11). HTTP 409. */
public class DuplicateResourceException extends BusinessRuleException {

    public DuplicateResourceException(String message) {
        super(ErrorCodes.RN_11, message);
    }
}
