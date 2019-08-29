package com.eurotux.connector.rt.internal.operations;

import com.eurotux.connector.rt.internal.connection.RTConnection;
import com.eurotux.connector.rt.internal.error.RTError;
import com.eurotux.connector.rt.internal.error.RTErrorTypeProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.zackehh.jackson.Jive;
import com.zackehh.jackson.stream.JiveCollectors;
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
import java.util.List;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;


public class RTOperations {

    private Logger logger = LoggerFactory.getLogger(RTOperations.class);
    private ObjectMapper mapper = new ObjectMapper();


    @DisplayName("Get Ticket Transactions")
    @MediaType(APPLICATION_JSON)
    @Throws(RTErrorTypeProvider.class)
    public String getTicketTransactions(@Connection RTConnection connection,
                                        int ticketId,
                                        Map<String, String> fields,
                                        List<String> transactionTypes,
                                        boolean withTimeTakenFilter) throws Exception {

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

        ArrayNode transactions = retrieveTicketTransactions(connection, 1, body, fields, withTimeTakenFilter);
        return mapper.writeValueAsString(transactions);
    }

    private ArrayNode retrieveTicketTransactions(RTConnection connection,
                                                 int page,
                                                 List<Map<String, String>> body,
                                                 Map<String, String> fields,
                                                 boolean withTimeTakenFilter) throws Exception {
        HttpRequestBuilder requestBuilder = HttpRequest.builder()
                .method(POST)
                .uri(String.format("%s/transactions", connection.getConfig().getApiUrl()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", String.format("token %s", connection.getConfig().getToken()))
                .addQueryParam("per_page", String.valueOf(100))
                .addQueryParam("page", String.valueOf(page));
        fields.forEach(requestBuilder::addQueryParam);

        HttpRequest request = requestBuilder
                .entity(new ByteArrayHttpEntity(mapper.writeValueAsBytes(body)))
                .build();
        HttpResponse response = connection.sendRequest(request);

        // HTTP/1.1 401 Authorization Required
        if (response.getStatusCode() === 401) {
            throw RTError.getThrowable(response, response.getReasonPhrase());
        }

        ObjectNode data = (ObjectNode) mapper.readTree(response.getEntity().getContent());
        ArrayNode transactions = data.withArray("items");
        int total = data.get("total").asInt();

        logger.info(mapper.writeValueAsString(transactions));

        if (transactions.size() == total) {
            return transactions;
        }
        if (withTimeTakenFilter) {
            transactions = Jive.stream(transactions).filter(this::withTimeTaken).collect(JiveCollectors.toArrayNode());
        }

        transactions.addAll(retrieveTicketTransactions(connection, page + 1, body, fields, withTimeTakenFilter));
        return transactions;
    }

    private boolean withTimeTaken(JsonNode transaction) {
        JsonNode timeTaken = transaction.get("TimeTaken");
        JsonNode internalTime = transaction.get("CF.{tempo interno}");

        double internalTimeAsDouble = 0.0;
        double timeTakenAsDouble = 0.0;

        logger.info("Transaction: {}", transaction.asText());

        if (timeTaken != null) {
            timeTakenAsDouble = timeTaken.asDouble(-1.0);
        }
        if (internalTime != null) {
            internalTimeAsDouble = internalTime.asDouble(-1.0);
        }

        // Is null or zero
        if (timeTakenAsDouble == 0.0 && internalTimeAsDouble == 0.0) {
            logger.info(
                    "Ignoring transaction {}. No time spent (TimeTaken: {}; Tempo interno: {})",
                    transaction.get("id").asText(), timeTakenAsDouble, internalTimeAsDouble
            );
            return false;
        }

        // Is invalid
        if (timeTakenAsDouble < 0.0) {
            logger.info(
                    "Ignoring transaction {}. Invalid TimeTaken {}",
                    transaction.get("id").asText(), timeTaken.asText()
            );
            return false;
        }
        if (internalTimeAsDouble < 0.0) {
            logger.info(
                    "Ignoring transaction {}. Invalid CF.{tempo interno} {}",
                    transaction.get("id").asText(), internalTime.asText()
            );
            return false;
        }

        return true;
    }
}
