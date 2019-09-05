package com.eurotux.connector.rt.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;


@DisplayName("Basic Authentication Connection")
@Alias("basic-auth-connections")
public abstract class RTBaseConnectionProvider implements PoolingConnectionProvider<RTConnection> {

    @ParameterGroup(name = "Connection")
    private RTConfig config;


    @Override
    public void disconnect(RTConnection rtConnection) {
        rtConnection.invalidate();
    }

    @Override
    public ConnectionValidationResult validate(RTConnection rtConnection) {
        return rtConnection.getClient().validate() ?
                ConnectionValidationResult.success() :
                ConnectionValidationResult.failure("Connection Failed", new ConnectionException("Invalid Credentials"));
    }

    public RTConfig getConfig() {
        return config;
    }
}
