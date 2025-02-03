package com.auth.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {
    String message() default "Invalid phone number";
    String messagePlus63() default "Phone number must start with +63 and be 13 characters long";
    String message63() default "Phone number must start with 63 and be 12 characters long";
    String message0() default "Phone number must start with 0 and be 11 characters long";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}