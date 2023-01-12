package org.megoru.entity;

public class ErrorResponseToMany {

    private int statusCode;
    private String error;
    private String message;

    public int getStatusCode() {
        return statusCode;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}
