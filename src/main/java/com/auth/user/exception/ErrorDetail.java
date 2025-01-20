package com.auth.user.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorDetail {
    private String field;
    private String message;
    private String code;
}