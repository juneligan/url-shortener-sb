package com.auth.user.validation;

import com.auth.user.service.model.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RegisterRequestValidator implements ConstraintValidator<ValidRegisterRequest, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        boolean isPhoneNumberEmpty = request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty();
        boolean areOtherFieldsFilled = request.getEmail() != null && !request.getEmail().isEmpty()
                && request.getUsername() != null && !request.getUsername().isEmpty()
                && request.getPassword() != null && !request.getPassword().isEmpty();

        if (isPhoneNumberEmpty && !areOtherFieldsFilled) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Phone number is required if email, username, and password are not provided")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}