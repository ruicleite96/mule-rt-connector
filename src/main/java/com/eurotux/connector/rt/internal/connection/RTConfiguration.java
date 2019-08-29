package com.eurotux.connector.rt.internal.connection;

import jdk.nashorn.internal.objects.annotations.Property;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;

public class RTConfiguration {

    @Parameter
    private String apiUrl;

    @Parameter
    @Optional
    private String username;

    @Parameter
    @Optional
    @Password
    private String password;

    @Parameter
    private String token;

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

    public String getToken() {
        return token;
    }

    public TlsContextFactory getTlsContextFactory() {
        return tlsContextFactory;
    }

}