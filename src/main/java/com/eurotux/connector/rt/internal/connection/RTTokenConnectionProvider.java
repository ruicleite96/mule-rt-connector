package com.eurotux.connector.rt.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.http.api.HttpService;

import javax.inject.Inject;

@DisplayName("1 - Token Auth Connection")
@Alias("token-connection")
public class RTTokenConnectionProvider extends RTBaseConnectionProvider {

    @Parameter
    private String token;

    @Inject
    private HttpService httpService;


    @Override
    public RTConnection connect() throws ConnectionException {
        RTConnection connection = new RTConnection(httpService, getConfig(), token);
        connection.startClient();
        return connection;
    }

}
