package com.cagatayergunes.library.exception.handler;

public class OperationNotPermittedException extends RuntimeException{

    public OperationNotPermittedException(String message) {
        super(message);
    }
}
