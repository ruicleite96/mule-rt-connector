package com.eurotux.connector.rt.internal.error;

import com.google.common.collect.ImmutableSet;
import org.apache.http.HttpException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.extension.api.error.MuleErrors.CLIENT_SECURITY;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.getStatusByCode;

public enum RTError implements ErrorTypeDefinition<RTError> {

    TIMEOUT,

    BAD_REQUEST,

    UNAUTHORIZED(CLIENT_SECURITY),

    FORBIDDEN(CLIENT_SECURITY),

    NOT_FOUND,

    METHOD_NOT_ALLOWED,

    UNSUPPORTED_MEDIA_TYPE,

    TOO_MANY_REQUESTS,

    INTERNAL_SERVER_ERROR,

    SERVICE_UNAVAILABLE,

    UNKNOWN_ERROR;

    private ErrorTypeDefinition<? extends Enum<?>> parent;


    RTError(ErrorTypeDefinition<? extends Enum<?>> parent) {
        this.parent = parent;
    }

    RTError() {
    }

    @Override
    public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
        return ofNullable(parent);
    }

    public static RTError getErrorByCode(int statusCode) {
        RTError error = null;
        HttpConstants.HttpStatus status = getStatusByCode(statusCode);
        if (status != null) {
            try {
                error = RTError.valueOf(status.name());
            } catch (Throwable e) {
                // Do nothing
            }
        }
        return (error != null ? error : UNKNOWN_ERROR);
    }

    public static Set<ErrorTypeDefinition> getRTErrors() {
        return ImmutableSet.<ErrorTypeDefinition>builder()
                .add(TIMEOUT)
                .add(CLIENT_SECURITY)
                .add(BAD_REQUEST)
                .add(FORBIDDEN)
                .add(UNAUTHORIZED)
                .add(METHOD_NOT_ALLOWED)
                .add(TOO_MANY_REQUESTS)
                .add(NOT_FOUND)
                .add(UNSUPPORTED_MEDIA_TYPE)
                .add(INTERNAL_SERVER_ERROR)
                .add(SERVICE_UNAVAILABLE)
                .add(UNKNOWN_ERROR)
                .build();
    }

    public static Exception getThrowable(HttpResponse response, @Nullable String message) {
        String _message = IOUtils.toString(response.getEntity().getContent(), UTF_8);
        if (_message != null && message != null) {
            _message = _message.concat("\n").concat(message);
        } else {
            _message = "";
        }

        return new ModuleException(
                getErrorByCode(response.getStatusCode()),
                new HttpException(_message)
        );
    }
}
