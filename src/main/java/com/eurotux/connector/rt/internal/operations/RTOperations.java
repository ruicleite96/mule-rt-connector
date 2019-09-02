package com.eurotux.connector.rt.internal.operations;

import com.eurotux.connector.rt.internal.RTRequestBuilderFactory.RTRequestBuilder;
import com.eurotux.connector.rt.internal.connection.RTConnection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zackehh.jackson.Jive;
import com.zackehh.jackson.stream.JiveCollectors;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;


public class RTOperations {

    private static Logger LOGGER = LoggerFactory.getLogger(RTOperations.class);
    private ObjectMapper mapper = new ObjectMapper();


    @DisplayName("Get Ticket Transactions")
    @MediaType(APPLICATION_JSON)
    public String getTicketTransactions(@Connection RTConnection connection, int ticketId, Map<String, String> fields,
                                        List<String> transactionTypes, boolean withTimeTakenFilter) throws Exception {
        ArrayNode body = mapper.createArrayNode();
        body.add(mapper.createObjectNode()
                .put("field", "ObjectId")
                .put("operator", "=")
                .put("value", String.valueOf(ticketId))
        );
        body.add(mapper.createObjectNode()
                .put("field", "ObjectType")
                .put("operator", "=")
                .put("value", "RT::Ticket")
        );
        transactionTypes.forEach(type -> body.add(mapper.createObjectNode()
                .put("field", "Type")
                .put("operator", "=")
                .put("value", type)
        ));

        ArrayNode transactions = retrieveTicketTransactions(connection, 1, body, fields, withTimeTakenFilter);
        return mapper.writeValueAsString(transactions);
    }

    private ArrayNode retrieveTicketTransactions(RTConnection connection, int page, JsonNode body,
                                                 Map<String, String> fields, boolean withTimeTakenFilter) throws Exception {
        RTRequestBuilder builder = connection.requestBuilderFactory
                .newRequest("transactions")
                .withParam("per_page", String.valueOf(100))
                .withParam("page", String.valueOf(page))
                .withBody(body);
        fields.forEach(builder::withParam);

        HttpResponse response = builder.sendSyncWithRetry(POST);

        ObjectNode data = (ObjectNode) mapper.readTree(response.getEntity().getContent());
        ArrayNode transactions = data.withArray("items");
        int count = data.get("count").asInt();

        if (withTimeTakenFilter) {
            transactions = Jive.stream(transactions)
                    .map(this::transformTransaction)
                    .filter(this::withTimeTaken)
                    .collect(JiveCollectors.toArrayNode());
        }
        if (count == 0) {
            return transactions;
        }

        transactions.addAll(retrieveTicketTransactions(connection, page + 1, body, fields, withTimeTakenFilter));
        return transactions;
    }

    private JsonNode transformTransaction(JsonNode transaction) {
        ObjectNode objectTransaction = (ObjectNode) transaction;
        JsonNode timeTaken = objectTransaction.get("TimeTaken");
        JsonNode internalTime = objectTransaction.get("CF.{tempo interno}");

        double timeTakenAsDouble = timeTaken.asDouble(-1.0);
        double internalTimeAsDouble = internalTime.asDouble(-1.0);

        // Is invalid
        if (timeTakenAsDouble < 0.0) {
            LOGGER.info(
                    "Transaction {}: Invalid TimeTaken {}",
                    objectTransaction.get("id").asText(), timeTaken.asText()
            );
        }
        if (internalTimeAsDouble < 0.0) {
            LOGGER.info(
                    "Transaction {}: Invalid CF.{tempo interno} {}",
                    objectTransaction.get("id").asText(), internalTime.asText()
            );
        }

        objectTransaction.set("TimeTaken", new DoubleNode(max(timeTakenAsDouble, 0.0)));
        objectTransaction.set("CF.{tempo interno}", new DoubleNode(max(internalTimeAsDouble, 0.0)));
        return objectTransaction;
    }

    private boolean withTimeTaken(JsonNode transaction) {
        JsonNode timeTaken = transaction.get("TimeTaken");
        JsonNode internalTime = transaction.get("CF.{tempo interno}");

        double timeTakenAsDouble = timeTaken.asDouble();
        double internalTimeAsDouble = internalTime.asDouble();

        if (timeTakenAsDouble == 0.0 && internalTimeAsDouble == 0.0) {
            LOGGER.info(
                    "Ignoring transaction {}. No time spent (TimeTaken: {}; Tempo interno: {})",
                    transaction.get("id").asText(), timeTakenAsDouble, internalTimeAsDouble
            );
            return false;
        }
        return true;
    }
}
