package com.servicetransitcalculator.exception;

/**
 * Represents an error response with a message, typically used for conveying error details to clients.
 */
/**
 * Represents an error response with a message and optional details,
 * typically used for conveying error details to clients.
 */
public class ErrorResponse {
    private String message;
    private String details;

    public ErrorResponse() {}

    public ErrorResponse(String message, String details) {
        this.message = message;
        this.details = details;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}