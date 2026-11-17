package com.coworkhub.exception;

/** El recurso solicitado no existe. HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " con id " + id + " no existe");
    }
}
