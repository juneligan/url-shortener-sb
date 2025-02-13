package com.auth.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MessageLengthValidator implements ConstraintValidator<ValidMessageLength, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.length() <= 255;
    }
}