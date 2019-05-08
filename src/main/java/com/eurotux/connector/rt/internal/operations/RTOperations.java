package com.eurotux.connector.rt.internal.operations;

import com.eurotux.connector.rt.internal.connection.RTConnection;
import com.eurotux.connector.rt.internal.error.RTError;
import com.eurotux.connector.rt.internal.error.RTErrorTypeProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;


public class RTOperations {

    private Logger logger = LoggerFactory.getLogger(RTOperations.class);
    private ObjectMapper mapper = new ObjectMapper();
    private TypeReference hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
    };

    private HttpRequestBuilder httpRequestBuilder = HttpRequest.builder();


    @DisplayName("Get Ticket Transactions")
    @MediaType(APPLICATION_JSON)
    @Throws(RTErrorTypeProvider.class)
    public String getAllTicketTransactions(@Connection RTConnection connection,
                                           int ticketId,
                                           Map<String, String> fields,
                                           List<String> transactionTypes,
                                           boolean withTimeTaken) throws Exception {

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

        transactionTypes.forEach(type -> body.add(ImmutableMap.of(
                "field", "Type",
                "operator", "=",
                "value", type
        )));

        List<Map<String, Object>> transactions = getTicketTransactions(connection, 1, body, fields, withTimeTaken);

        return mapper.writeValueAsString(transactions);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getTicketTransactions(RTConnection connection,
                                                            int page,
                                                            List<Map<String, String>> body,
                                                            Map<String, String> fields,
                                                            boolean withTimeTaken) throws Exception {
        HttpRequestBuilder requestBuilder = httpRequestBuilder
                .method(POST)
                .uri(String.format("%s/transactions", connection.getConfig().getApiUrl()))
                .addHeader("Content-Type", "application/json")
                .addQueryParam("per_page", String.valueOf(100))
                .addQueryParam("page", String.valueOf(page));

        fields.forEach(requestBuilder::addQueryParam);

        HttpRequest request = requestBuilder
                .entity(new ByteArrayHttpEntity(mapper.writeValueAsBytes(body)))
                .build();
        HttpResponse response = connection.sendRequest(request);

        if (response.getStatusCode() != OK.getStatusCode()) {
            throw RTError.getThrowable(response, null);
        }

        Map<String, Object> data = mapper.readValue(response.getEntity().getContent(), hashMapTypeReference);

        List<Map<String, Object>> transactions = (List<Map<String, Object>>) data.get("items");
        Integer count = (Integer) data.get("count");

        if (count == 0) {
            return new ArrayList<>();
        }

        if (withTimeTaken) {
            transactions = transactions.stream().filter(this::withTimeTaken).collect(toList());
        }

        transactions.addAll(getTicketTransactions(connection, page + 1, body, fields, withTimeTaken));
        return transactions;
    }

    private boolean withTimeTaken(Map<String, Object> transaction) {
        Object timeTakenObj = transaction.get("TimeTaken");
        Object internalTimeObj = transaction.get("CF.{tempo interno}");

        double timeTaken;
        double internalTime;

        try {
            timeTaken = Double.parseDouble(timeTakenObj.toString());
        } catch (NumberFormatException e) {
            logger.info("Ignoring transaction {}. Invalid TimeTaken {}", transaction.get("id").toString(), timeTakenObj.toString());
            return false;
        }

        try {
            internalTime = Double.parseDouble(internalTimeObj.toString());
        } catch (NumberFormatException e) {
            logger.info("Ignoring transaction {}. Invalid CF.{tempo interno} {}", transaction.get("id").toString(), internalTimeObj.toString());
            return false;
        }

        if (timeTaken == 0.0 && internalTime == 0.0) {
            logger.info("Ignoring transaction {}. No time spent (TimeTaken: {}; Tempo interno: {})",
                    transaction.get("id").toString(), timeTaken, internalTime);
            return false;
        }

        return true;
    }
}
