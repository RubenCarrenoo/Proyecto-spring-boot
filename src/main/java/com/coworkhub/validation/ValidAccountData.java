package com.coworkhub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validación personalizada: al registrar un miembro la cuenta de usuario es opcional,
 * pero si se envía "usuario" también debe enviarse "password" (y viceversa).
 */
@Documented
@Constraint(validatedBy = ValidAccountDataValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAccountData {

    String message() default "usuario y password deben enviarse juntos para crear la cuenta del miembro";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
