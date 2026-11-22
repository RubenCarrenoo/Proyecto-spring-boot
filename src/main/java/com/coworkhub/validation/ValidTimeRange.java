package com.coworkhub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validación personalizada a nivel de clase: el inicio debe ser anterior al fin.
 * Se aplica a DTOs que implementan {@link TimeRange}.
 */
@Documented
@Constraint(validatedBy = ValidTimeRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTimeRange {

    String message() default "la hora de inicio debe ser anterior a la hora de fin";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
