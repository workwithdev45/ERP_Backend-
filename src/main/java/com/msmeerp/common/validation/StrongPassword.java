package com.msmeerp.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Minimum 10 characters, at least one lowercase, one uppercase, one digit, and one symbol (G9). */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {
    String message() default "Password must be at least 10 characters and include an uppercase letter, "
            + "a lowercase letter, a digit, and a symbol";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
