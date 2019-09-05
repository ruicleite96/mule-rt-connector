package com.eurotux.connector.rt.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public enum RTError implements ErrorTypeDefinition<RTError> {

    EXECUTION,
    RETRIES_EXCEEDED,
    TIMEOUT;

    private ErrorTypeDefinition parent;


    RTError(ErrorTypeDefinition parent) {
        this.parent = parent;
    }

    RTError() {
    }

    @Override
    public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
        return ofNullable(parent);
    }

}
