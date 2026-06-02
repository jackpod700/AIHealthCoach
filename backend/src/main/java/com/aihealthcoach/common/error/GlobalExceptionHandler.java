package com.aihealthcoach.common.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.aihealthcoach.meal.exception.MealErrorCode;
import com.aihealthcoach.meal.exception.MealException;
import com.aihealthcoach.user.exception.UserErrorCode;
import com.aihealthcoach.user.exception.UserException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(UserException exception) {
        UserErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.name(), errorCode.getMessage()));
    }

    @ExceptionHandler(MealException.class)
    public ResponseEntity<ErrorResponse> handleMealException(MealException exception) {
        MealErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.name(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "Request validation failed."));
    }
}
