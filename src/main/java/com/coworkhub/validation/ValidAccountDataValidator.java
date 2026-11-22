package com.coworkhub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidAccountDataValidator implements ConstraintValidator<ValidAccountData, AccountData> {

    @Override
    public boolean isValid(AccountData value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        boolean hasUser = value.usuario() != null && !value.usuario().isBlank();
        boolean hasPassword = value.password() != null && !value.password().isBlank();
        return hasUser == hasPassword;
    }
}
