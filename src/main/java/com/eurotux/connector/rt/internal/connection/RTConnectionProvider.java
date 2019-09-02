package com.eurotux.connector.rt.internal.connection;

import com.eurotux.connector.rt.internal.RTRequestBuilderFactory;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.http.api.HttpService;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeoutException;


@DisplayName("Basic Authentication Connection")
@Alias("basic-auth-connections")
public class RTConnectionProvider implements CachedConnectionProvider<RTConnection> {

    @ParameterGroup(name = "Connection")
    private RTConfiguration config;

    @Inject
    private HttpService httpService;


    @Override
    public RTConnection connect() throws ConnectionException {
        RTRequestBuilderFactory requestBuilderFactory = new RTRequestBuilderFactory(config);
        try {
            requestBuilderFactory.initHttpClient(httpService);
        } catch (InitialisationException e) {
            throw new ConnectionException(e);
        }
        return new RTConnection(requestBuilderFactory, config);
    }

    @Override
    public void disconnect(RTConnection rtConnection) {
        rtConnection.invalidate();
    }

    @Override
    public ConnectionValidationResult validate(RTConnection rtConnection) {
        try {
            return rtConnection.requestBuilderFactory.validate() ?
                    ConnectionValidationResult.success() :
                    ConnectionValidationResult.failure("Connection Failed", new ConnectionException("Invalid Credentials"));
        } catch (IOException | TimeoutException e) {
            return ConnectionValidationResult.failure("Connection Failed", e);
        }
    }
}
