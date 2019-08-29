package com.eurotux.connector.rt.internal.connection;


import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.client.auth.HttpAuthentication;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;


public final class RTConnection {

    private RTConfiguration config;

//    private HttpAuthentication authentication;

    private HttpClient httpClient;

    private Exception exception;

    private boolean connected;


    public RTConnection(HttpService httpService, RTConfiguration config) {
        this.config = config;
        initHttpClient(httpService);
    }

    public void initHttpClient(HttpService httpService) {
        HttpClientConfiguration.Builder httpClientBuilder = new HttpClientConfiguration.Builder();
        httpClientBuilder.setName("RT");

        TlsContextFactory tlsContextFactory = config.getTlsContextFactory();
        if (tlsContextFactory instanceof Initialisable) {
            try {
                ((Initialisable) tlsContextFactory).initialise();
            } catch (InitialisationException e) {
                exception = e;
                connected = false;
            }
        }

        httpClientBuilder.setTlsContextFactory(tlsContextFactory);
        httpClient = httpService.getClientFactory().create(httpClientBuilder.build());
        httpClient.start();
        connected = true;
//        authentication = HttpAuthentication.basic(config.getUsername(), config.getPassword()).build();
    }


    public void invalidate() {
        httpClient.stop();
    }

    public boolean isConnected() {
        return connected;
    }

    public Exception getException() {
        return exception;
    }

    public RTConfiguration getConfig() {
        return config;
    }

    public HttpResponse sendRequest(HttpRequest request) throws IOException, TimeoutException {
        return httpClient.send(request, 50000, false, null);
    }

    public CompletableFuture<HttpResponse> sendRequestAsync(HttpRequest request) {
        return httpClient.sendAsync(request, 50000, false, null);
    }
}
