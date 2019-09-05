package com.eurotux.connector.rt.internal.connection;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Example;

public class RTConfig {

    @Parameter
    @Example("https://suporte.eurotux.com/REST/2.0")
    private String apiUrl;

    @Parameter
    @Optional(defaultValue = "5")
    private int maxRetries;

    @Parameter
    @Optional
    private TlsContextFactory tlsContextFactory;


    public String getApiUrl() {
        return apiUrl;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public TlsContextFactory getTlsContextFactory() {
        return tlsContextFactory;
    }

}