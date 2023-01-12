package org.megoru.io;

import com.google.gson.Gson;

public class DefaultResponseTransformer<E> implements ResponseTransformer<E> {

    private final Class<E> aClass;
    private final Gson gson;

    public DefaultResponseTransformer(Class<E> aClass, Gson gson) {
        this.aClass = aClass;
        this.gson = gson;
    }

    @Override
    public E transform(String response) {
        return gson.fromJson(response, aClass);
    }
}
