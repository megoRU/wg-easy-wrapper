package org.megoru.entity;

public class Errors {

    private int code;
    private String message;

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Errors code: " + code + "\nErrors message: " + message;
    }
}
