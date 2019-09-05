package com.eurotux.connector.rt.internal.connection;

import com.eurotux.connector.rt.internal.RTClient;
import com.eurotux.connector.rt.internal.RTRequestBuilderFactory;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.http.api.HttpService;

public final class RTConnection {

    private RTConfig config;

    private RTClient client;


    public RTConnection(HttpService httpService, RTConfig config, String token) {
        this.config = config;
        RTRequestBuilderFactory requestBuilderFactory = new RTRequestBuilderFactory(config, token);
        this.client = new RTClient(httpService, requestBuilderFactory);
    }

    public RTConnection(HttpService httpService, RTConfig config, String username, String password) {
        this.config = config;
        RTRequestBuilderFactory requestBuilderFactory = new RTRequestBuilderFactory(config, username, password);
        this.client = new RTClient(httpService, requestBuilderFactory);
    }

    public void startClient() throws ConnectionException {
        this.client.initHttpClient();
    }

    public void invalidate() {
        client.stopHttpClient();
    }

    public RTConfig getConfig() {
        return config;
    }

    public RTClient getClient() {
        return client;
    }
}
