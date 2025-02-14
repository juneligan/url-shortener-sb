package com.auth.user.exception;

import com.auth.user.service.model.GenericResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // error codes, status codes and messages
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND.value(), "User not found"),
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", HttpStatus.BAD_REQUEST.value(), "User already exists"),
    INVALID_OTP("INVALID_OTP", HttpStatus.BAD_REQUEST.value(), "Invalid OTP! not found or Expired"),
    OTP_EXPIRED("OTP_EXPIRED", HttpStatus.BAD_REQUEST.value(), "OTP expired"),
    OTP_ALREADY_SENT("OTP_ALREADY_SENT", HttpStatus.BAD_REQUEST.value(),
            "OTP already sent! wait for 2 minute to generate new OTP"),
    INVALID_PHONE_NUMBER("INVALID_PHONE_NUMBER", HttpStatus.BAD_REQUEST.value(), "Invalid phone number"),
    INVALID_PASSWORD("INVALID_PASSWORD", HttpStatus.BAD_REQUEST.value(), "Invalid password"),
    INVALID_USERNAME("INVALID_USERNAME", HttpStatus.BAD_REQUEST.value(), "Invalid username"),
    INVALID_EMAIL("INVALID_EMAIL", HttpStatus.BAD_REQUEST.value(), "Invalid email"),
    INVALID_ROLE("INVALID_ROLE", HttpStatus.BAD_REQUEST.value(), "Invalid role"),
    INVALID_REQUEST("INVALID_REQUEST", HttpStatus.BAD_REQUEST.value(), "Invalid request"),
    PHONE_NUMBER_IN_USE("PHONE_NUMBER_IN_USE", HttpStatus.BAD_REQUEST.value(), "Phone number already in use"),
    SMS_LIMIT_REACHED("SMS_LIMIT_REACHED", HttpStatus.TOO_MANY_REQUESTS.value(),
            "You have reached the maximum number of messages for this hour. Please try again later."),
    ;

    private final String code;
    private final int status;
    private final String message;

    public ErrorResponse toErrorResponse(String username) {
        return ErrorResponse.build(this, username);
    }

    public GenericResponse<?> toGenericResponse(String username) {
        return GenericResponse.builder().error(toErrorResponse(username)).build();
    }

    public String formatMessage(Object... args) {
        return String.format(message, args);
    }

}
