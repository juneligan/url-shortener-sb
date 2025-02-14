package com.auth.user.service.model;

import com.auth.user.exception.ErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Data
public class GenericResponse<E> {
    private String message;
    private E data;
    private ErrorResponse error;

    public boolean hasError() {
        return error != null;
    }

    public ResponseEntity<GenericResponse<?>> toResponseEntity() {
        return ResponseEntity.status(error.getStatus()).body(this);
    }
}
