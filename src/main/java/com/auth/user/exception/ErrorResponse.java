package com.auth.user.exception;

import com.auth.user.service.model.SbResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ErrorResponse implements SbResponse {
    private LocalDateTime timestamp;
    private Integer status;
    private String errorCode;
    private String error;
    private String message;
    private String path;
    private List<ErrorDetail> errors;
    // transient
    private String username; // for logging purposes

    public static ErrorResponse build(ErrorCode errorCode, Object... args) {
        return build(errorCode, null, args);
    }

    public static ErrorResponse build(ErrorCode errorCode, String username, Object... args) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus())
                .errorCode(errorCode.getCode())
                .error(errorCode.formatMessage(args))
                .username(username)
                .build();
    }
}