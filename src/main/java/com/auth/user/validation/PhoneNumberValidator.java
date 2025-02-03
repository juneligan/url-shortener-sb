package com.auth.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    private String messagePlus63;
    private String message63;
    private String message0;

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.messagePlus63 = constraintAnnotation.messagePlus63();
        this.message63 = constraintAnnotation.message63();
        this.message0 = constraintAnnotation.message0();
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null) {
            return false;
        }
        if (phoneNumber.startsWith("+63") && phoneNumber.length() == 13) {
            return true;
        }
        if (phoneNumber.startsWith("63") && phoneNumber.length() == 12) {
            return true;
        }
        if (phoneNumber.startsWith("0") && phoneNumber.length() == 11) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        if (phoneNumber.startsWith("+") && !phoneNumber.startsWith("+63")) {
            context.buildConstraintViolationWithTemplate(messagePlus63).addConstraintViolation();
        } else if (!phoneNumber.startsWith("63")) {
            context.buildConstraintViolationWithTemplate(message63).addConstraintViolation();
        } else if (!phoneNumber.startsWith("0")) {
            context.buildConstraintViolationWithTemplate(message0).addConstraintViolation();
        } else {
            context.buildConstraintViolationWithTemplate("Phone number must start with +63, 63, or 0").addConstraintViolation();
        }
        return false;
    }
}