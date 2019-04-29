package com.eurotux.connector.rt.internal.connection;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.http.api.HttpService;

import javax.inject.Inject;


@DisplayName("Basic Authentication Connection")
@Alias("basic-auth-connections")
public class RTConnectionProvider implements CachedConnectionProvider<RTConnection> {

    @ParameterGroup(name = "Connection")
    RTConfiguration config;

    @Inject
    HttpService httpService;


    @Override
    public RTConnection connect() {
        return new RTConnection(httpService, config);
    }

    @Override
    public void disconnect(RTConnection rtConnection) {
        rtConnection.invalidate();
    }

    @Override
    public ConnectionValidationResult validate(RTConnection rtConnection) {
        return rtConnection.isConnected() ?
                ConnectionValidationResult.success() :
                ConnectionValidationResult.failure("Connection Failed", rtConnection.getException());
    }
}
