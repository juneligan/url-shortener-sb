package com.auth.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = RegisterRequestValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRegisterRequest {
    String message() default "Phone number is required if email, username, and password are not provided";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}