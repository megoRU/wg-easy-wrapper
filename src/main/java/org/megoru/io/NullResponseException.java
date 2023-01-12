package org.megoru.io;

public class NullResponseException extends Exception {

    public NullResponseException() {
        super("response is NULL");
    }
}
