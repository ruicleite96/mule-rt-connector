package com.eurotux.connector.rt.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.http.api.HttpService;

import javax.inject.Inject;

@DisplayName("2 - Basic Auth Connection")
@Alias("basic-connection")
public class RTBasicConnectionProvider extends RTBaseConnectionProvider {

    @Inject
    private HttpService httpService;

    @Parameter
    private String username;

    @Parameter
    @Password
    private String password;


    @Override
    public RTConnection connect() throws ConnectionException {
        RTConnection connection = new RTConnection(httpService, getConfig(), username, password);
        connection.startClient();
        return connection;
    }
}
