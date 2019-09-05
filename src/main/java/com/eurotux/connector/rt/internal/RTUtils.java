package com.eurotux.connector.rt.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mule.extension.http.api.error.HttpError;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import javax.annotation.Nullable;
import java.io.IOException;

import static com.eurotux.connector.rt.internal.error.RTError.EXECUTION;
import static java.nio.charset.StandardCharsets.UTF_8;

public class RTUtils {

    public static ObjectMapper mapper = new ObjectMapper();


    public static HttpError getHttpErrorFromResponse(HttpResponse response) {
        return HttpError.getErrorByCode(response.getStatusCode()).orElse(null);
    }

    public static Exception getThrowableFromResponse(HttpResponse response, @Nullable String message) {
        String _message = IOUtils.toString(response.getEntity().getContent(), UTF_8);
        if (_message != null && message != null) {
            _message = _message.concat("\n").concat(message);
        } else {
            _message = "";
        }
        throw new ModuleException(_message, HttpError.getErrorByCode(response.getStatusCode()).orElse(null));
    }

    public static String jsonResponseToString(HttpResponse response) {
        try {
            return mapper.readTree(response.getEntity().getContent()).toString();
        } catch (IOException e) {
            throw new ModuleException(EXECUTION, e);
        }
    }

    public static JsonNode jsonResponseToJsonNode(HttpResponse response) {
        try {
            return mapper.readTree(response.getEntity().getContent());
        } catch (IOException e) {
            throw new ModuleException(EXECUTION, e);
        }
    }

}
