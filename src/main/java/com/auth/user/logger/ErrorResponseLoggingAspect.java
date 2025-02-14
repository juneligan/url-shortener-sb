package com.auth.user.logger;

import com.auth.user.exception.ErrorResponse;
import com.auth.user.service.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

@Aspect
@Component
@Slf4j
public class ErrorResponseLoggingAspect {

    public static final String SEPARATOR = "\\.";

    @AfterReturning(
            pointcut = "execution(com.auth.user.service.model.GenericResponse *(..))",
            returning = "errorResponse"
    )
    public void logErrorResponse(JoinPoint joinPoint, GenericResponse<?> errorResponse) {
        if (errorResponse == null || !errorResponse.hasError()) {
            return;
        }// Get the full class name
        String fullClassName = joinPoint.getTarget().getClass().getName();

        // Convert to Spring-style abbreviated format
        String formattedClassName = abbreviatePackageName(fullClassName);

        // Get the method name
        String methodName = joinPoint.getSignature().getName();
        ErrorResponse error = errorResponse.getError();

        // Log the details
        log.error("[{}]{}.{}: {}", error.getUsername(), formattedClassName, methodName, error.getError());
        // sample log:
        // 2025-02-14T21:01:22.626+08:00 ERROR 35441 --- [url-shortener-sb]
        // [nio-8080-exec-1] c.a.u.a.ErrorResponseLoggingAspect
        // : [09912049206] c.a.u.s.UserService.registerUser: Phone number already in use
    }

    /**
     * Abbreviates package names in the format used by Spring Boot logs.
     * Example: "com.example.service.MyClass" → "c.e.service.MyClass"
     */
    private String abbreviatePackageName(String fullClassName) {
        String[] parts = fullClassName.split(SEPARATOR);
        StringJoiner joiner = new StringJoiner(".");

        for (int i = 0; i < parts.length - 1; i++) {
            joiner.add(String.valueOf(parts[i].charAt(0)));  // Use only the first letter of each package
        }

        joiner.add(parts[parts.length - 1]);  // Keep the class name fully
        return joiner.toString();
    }
}