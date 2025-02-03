package com.auth.user.validation;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PhoneNumberValidatorTest {

    private PhoneNumberValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    public void setUp() {
        validator = new PhoneNumberValidator();
        context = Mockito.mock(ConstraintValidatorContext.class);
        violationBuilder = Mockito.mock(ConstraintViolationBuilder.class);

        ValidPhoneNumber annotation = Mockito.mock(ValidPhoneNumber.class);
        Mockito.when(annotation.messagePlus63()).thenReturn("Phone number must start with +63 and be 13 characters long");
        Mockito.when(annotation.message63()).thenReturn("Phone number must start with 63 and be 12 characters long");
        Mockito.when(annotation.message0()).thenReturn("Phone number must start with 0 and be 11 characters long");
        validator.initialize(annotation);

        Mockito.when(context.buildConstraintViolationWithTemplate(Mockito.anyString())).thenReturn(violationBuilder);
        Mockito.when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    public void testValidPhoneNumberPlus63() {
        assertTrue(validator.isValid("+639123456789", context));
    }

    @Test
    public void testValidPhoneNumber63() {
        assertTrue(validator.isValid("639123456789", context));
    }

    @Test
    public void testValidPhoneNumber0() {
        assertTrue(validator.isValid("09123456789", context));
    }

    @Test
    public void testInvalidPhoneNumber() {
        assertFalse(validator.isValid("1234567890", context));
    }

    @Test
    public void testInvalidPhoneNumberLength() {
        assertFalse(validator.isValid("+63912345678", context));
        assertFalse(validator.isValid("63912345678", context));
        assertFalse(validator.isValid("0912345678", context));
    }
}