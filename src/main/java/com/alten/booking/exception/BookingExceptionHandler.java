package com.alten.booking.exception;

import com.alten.booking.model.ApiError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class BookingExceptionHandler extends ResponseEntityExceptionHandler {


    @ExceptionHandler(value = {RoomBookedException.class})
    protected ResponseEntity<Object> handleRoomException(RuntimeException ex, WebRequest request) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, ex.getMessage());
        return handleExceptionInternal(ex, error, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = {InvalidRequestException.class})
    protected ResponseEntity<Object> handleRequestException(RuntimeException ex, WebRequest request) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, ex.getMessage());
        return handleExceptionInternal(ex, error, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

}
