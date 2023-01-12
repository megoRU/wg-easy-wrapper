package org.megoru.io;

public interface ResponseTransformer<E> {

    E transform(String response);
}