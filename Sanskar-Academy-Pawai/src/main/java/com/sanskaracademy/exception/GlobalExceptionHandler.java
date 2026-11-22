package com.sanskaracademy.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** messages written for the user (validation, "already submitted" ...) are shown as they are */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleUserMessage(RuntimeException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error-page";
    }

    /** anything else is logged for the developer and the user sees a neutral message */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, Model model) {
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;                       // let Spring Security answer (403 / login page)
        }
        log.error("Unexpected error", ex);
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        return "error/error-page";
    }
}
