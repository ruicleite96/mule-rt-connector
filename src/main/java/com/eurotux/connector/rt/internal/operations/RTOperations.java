package com.eurotux.connector.rt.internal.operations;

import com.eurotux.connector.rt.internal.connection.RTConnection;
import com.eurotux.connector.rt.internal.error.RTError;
import com.eurotux.connector.rt.internal.error.RTErrorTypeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.map.SingletonMap;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.CREATED;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;


public class RTOperations {

    private ObjectMapper mapper = new ObjectMapper();

    private HttpRequestBuilder httpRequestBuilder = HttpRequest.builder();


    @DisplayName("Get Ticket Transactions")
    @MediaType(APPLICATION_JSON)
    @Throws(RTErrorTypeProvider.class)
    public InputStream getTicketTransactions(@Connection RTConnection connection,
                                             int ticketId,
                                             int per_page,
                                             int page,
                                             Map<String, String> fields) throws Exception {
        HttpRequestBuilder requestBuilder = httpRequestBuilder
                .method(POST)
                .uri(String.format("%s/transactions", connection.getConfig().getApiUrl()))
                .addHeader("Content-Type", "application/json")
                .addQueryParam("per_page", String.valueOf(per_page))
                .addQueryParam("page", String.valueOf(page));

        fields.forEach(requestBuilder::addQueryParam);

        List<Map<String, String>> body = new ArrayList<>();
        body.add(ImmutableMap.of(
                "field", "ObjectId",
                "operator", "=",
                "value", String.valueOf(ticketId)
        ));
        body.add(ImmutableMap.of(
                "field", "ObjectType",
                "operator", "=",
                "value", "RT::Ticket"
        ));
        body.add(ImmutableMap.of(
                "field", "Type",
                "operator", "=",
                "value", "Comment"
        ));
        body.add(ImmutableMap.of(
                "field", "Type",
                "operator", "=",
                "value", "Correspond"
        ));
        body.add(ImmutableMap.of(
                "field", "Type",
                "operator", "=",
                "value", "Set"
        ));

        HttpRequest request = requestBuilder
                .entity(new ByteArrayHttpEntity(mapper.writeValueAsBytes(project)))
                .build();
        HttpResponse response = connection.sendRequest(request);
        if (response.getStatusCode() != CREATED.getStatusCode()) {
            throw RTError.getThrowable(response, mapper.writeValueAsString(project));
        }
        return response.getEntity().getContent();
    }
}
