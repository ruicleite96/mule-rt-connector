package com.eurotux.connector.rt.internal.connection;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;

public class RTConfiguration {

    @Parameter
    private String apiUrl;

    @Parameter
    private String username;

    @Parameter
    @Password
    private String password;

    @Parameter
    @Optional
    private TlsContextFactory tlsContextFactory;


    public String getApiUrl() {
        return apiUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public TlsContextFactory getTlsContextFactory() {
        return tlsContextFactory;
    }
}