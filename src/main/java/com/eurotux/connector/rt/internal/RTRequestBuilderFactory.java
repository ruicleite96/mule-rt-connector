package com.eurotux.connector.rt.internal;

import com.eurotux.connector.rt.internal.connection.RTConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.HttpConstants.Method;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.runtime.http.api.HttpHeaders.Names.CONTENT_TYPE;


public class RTRequestBuilderFactory {

    private static final Logger LOGGER = LogManager.getLogger(RTRequestBuilderFactory.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private RTConfiguration connectionConfig;

    private HttpClient httpClient;

    private int responseTimeout;


    public RTRequestBuilderFactory(RTConfiguration connectionConfig) {
        this(connectionConfig, 5000);
    }

    public RTRequestBuilderFactory(RTConfiguration connectionConfig, int responseTimeout) {
        this.connectionConfig = connectionConfig;
        this.responseTimeout = responseTimeout;
    }

    public void initHttpClient(HttpService httpService) throws InitialisationException {
        HttpClientConfiguration.Builder builder = new HttpClientConfiguration.Builder();
        builder.setName("rt");

        TlsContextFactory tlsContextFactory = connectionConfig.getTlsContextFactory();
        if (tlsContextFactory instanceof Initialisable) {
            ((Initialisable) tlsContextFactory).initialise();
        }
        builder.setTlsContextFactory(tlsContextFactory);

        httpClient = httpService.getClientFactory().create(builder.build());
        httpClient.start();
    }

    public void stopHttpClient() {
        this.httpClient.stop();
    }

    public boolean validate() throws IOException, TimeoutException {
        HttpResponse response = newRequest(String.format("user/%s", connectionConfig.getUsername()))
                .sendSyncWithRetry(GET);
        return response.getStatusCode() != 401;
    }

    public RTRequestBuilder newRequest(String endpoint) {
        return new RTRequestBuilder(endpoint);
    }


    public class RTRequestBuilder {

        private String uri;

        private MultiMap<String, String> queryParams = new MultiMap<>();

        private HttpRequestBuilder builder = HttpRequest.builder();


        public RTRequestBuilder(String endpoint) {
            this.uri = connectionConfig.getApiUrl() + "/" + endpoint;
            this.withHeader(AUTHORIZATION, String.format("token %s", connectionConfig.getToken()));
        }

        public RTRequestBuilder withBody(JsonNode body) throws IOException {
            this.builder.entity(new ByteArrayHttpEntity(mapper.writeValueAsBytes(body)));
            return this;
        }

        public RTRequestBuilder withBody(String body) {
            this.builder.entity(new ByteArrayHttpEntity(body.getBytes(UTF_8)));
            return this;
        }

        public RTRequestBuilder withParams(MultiMap<String, String> queryParams) {
            this.queryParams.putAll(queryParams);
            return this;
        }

        public RTRequestBuilder withParam(String key, String value) {
            this.queryParams.put(key, value);
            return this;
        }

        public RTRequestBuilder withHeader(String key, String value) {
            this.builder.addHeader(key, value);
            return this;
        }

        public RTRequestBuilder removeHeader(String key) {
            this.builder.removeHeader(key);
            return this;
        }

        public CompletableFuture<HttpResponse> sendAsync(Method method) {
            return httpClient.sendAsync(build(method), responseTimeout, true, null);
        }

        public HttpResponse sendSync(Method method) throws IOException, TimeoutException {
            return httpClient.send(build(method), responseTimeout, true, null);
        }

        public HttpResponse sendSyncWithRetry(Method method) throws IOException, TimeoutException {
            return sendSyncWithRetry(method, connectionConfig.getMaxRetries());
        }

        private HttpResponse sendSyncWithRetry(Method method, int retries) throws IOException, TimeoutException {
            HttpResponse httpResponse = sendSync(method);
            if (httpResponse.getStatusCode() == 401 && retries > 0) {
                return sendSyncWithRetry(method, retries - 1);
            } else {
                return httpResponse;
            }
        }

        private HttpRequest build(Method method) {
            if (!builder.getHeaderValue(CONTENT_TYPE).isPresent()) {
                this.withHeader(CONTENT_TYPE, "application/json");
            }
            return builder
                    .method(method)
                    .uri(uri)
                    .queryParams(queryParams)
                    .build();
        }

    }
}
