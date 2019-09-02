package com.eurotux.connector.rt.internal;

import org.mule.extension.http.api.error.HttpError;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import javax.annotation.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RTUtils {

    public static Exception getThrowable(HttpResponse response, @Nullable String message) {
        String _message = IOUtils.toString(response.getEntity().getContent(), UTF_8);
        if (_message != null && message != null) {
            _message = _message.concat("\n").concat(message);
        } else {
            _message = "";
        }

        return new ModuleException(_message, HttpError.getErrorByCode(response.getStatusCode()).get());
    }
}
