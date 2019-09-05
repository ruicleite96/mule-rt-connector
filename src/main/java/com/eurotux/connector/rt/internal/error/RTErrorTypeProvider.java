package com.eurotux.connector.rt.internal.error;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

import static com.eurotux.connector.rt.internal.error.RTError.*;

public class RTErrorTypeProvider implements ErrorTypeProvider {

    @Override
    public Set<ErrorTypeDefinition> getErrorTypes() {
        HashSet<ErrorTypeDefinition> errors = new HashSet<>();
        errors.add(EXECUTION);
        errors.add(RETRIES_EXCEEDED);
        errors.add(TIMEOUT);
        return errors;
    }
}
