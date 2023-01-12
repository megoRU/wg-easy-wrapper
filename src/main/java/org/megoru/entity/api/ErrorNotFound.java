package org.megoru.entity.api;

public class ErrorNotFound {

    private String error;
    private String stack;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getStack() {
        return stack;
    }

    public void setStack(String stack) {
        this.stack = stack;
    }
}
