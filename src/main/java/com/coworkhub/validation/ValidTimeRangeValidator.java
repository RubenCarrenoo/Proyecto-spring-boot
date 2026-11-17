package com.coworkhub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidTimeRangeValidator implements ConstraintValidator<ValidTimeRange, TimeRange<?>> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean isValid(TimeRange<?> value, ConstraintValidatorContext context) {
        if (value == null || value.rangeStart() == null || value.rangeEnd() == null) {
            return true; // de los nulos se encarga @NotNull
        }
        Comparable start = value.rangeStart();
        return start.compareTo(value.rangeEnd()) < 0;
    }
}
