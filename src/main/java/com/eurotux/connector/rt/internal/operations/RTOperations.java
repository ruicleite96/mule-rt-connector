package com.eurotux.connector.rt.internal.operations;

import com.eurotux.connector.rt.internal.connection.RTConnection;
import com.eurotux.connector.rt.internal.error.RTErrorTypeProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zackehh.jackson.Jive;
import com.zackehh.jackson.stream.JiveCollectors;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;


public class RTOperations {

    private static Logger LOGGER = LoggerFactory.getLogger(RTOperations.class);


    /**
     * This operation returns information about a Ticket
     *
     * @param connection The connection
     * @param ticketId   The Ticket Id
     * @return Information about a Ticket
     */
    @MediaType(APPLICATION_JSON)
    @Throws({RTErrorTypeProvider.class})
    @OutputJsonType(schema = "metadata/schemas/ticket-schema.json")
    public String retrieveTicket(@Connection RTConnection connection, int ticketId) {
        Preconditions.checkArgument(ticketId > 0, "Ticket ID must be a positive integer.");
        return connection.getClient().retrieveTicket(ticketId).toString();
    }

    /**
     * This operation returns a Ticket list according to a search criteria, with pagination
     *
     * @param connection The connection
     * @param ticketSQL  The search criteria
     * @param page       The page
     * @param perPage    Number of Tickets per page
     * @return Ticket list
     */
    @MediaType(APPLICATION_JSON)
    @Throws({RTErrorTypeProvider.class})
    @OutputJsonType(schema = "metadata/schemas/ticket-page-schema.json")
    public String searchTickets(@Connection RTConnection connection, String ticketSQL, int page, int perPage) {
        Preconditions.checkArgument(!ticketSQL.isEmpty(), "Search String is required");
        return connection.getClient().retrieveTicketsPage(ticketSQL, page, perPage).toString();
    }

    /**
     * This operation returns the history of a Ticket (without pagination)
     *
     * @param connection        The connection
     * @param ticketId          The Ticket Id
     * @param types             Types of Transactions to Retrieve
     * @param onlyWithTimeTaken Option to return only Transactions that have Time Taken associated
     * @return Transaction list
     */
    @MediaType(APPLICATION_JSON)
    @Throws({RTErrorTypeProvider.class})
    //@OutputJsonType(schema = "metadata/schemas/transaction-array-schema.json")
    public String retrieveAllTicketHistory(@Connection RTConnection connection, int ticketId, List<String> types,
                                           boolean onlyWithTimeTaken) {
        int count = Integer.MAX_VALUE;
        int page = 1;
        ArrayNode transactions = connection.getClient()
                .retrieveTicketHistory(ticketId, types);
        Stream<ObjectNode> transactionStream = Jive.stream(transactions)
                .map(transaction -> transformTime((ObjectNode) transaction));
        if (onlyWithTimeTaken) {
            transactionStream = transactionStream
                    .filter(this::withTimeTaken);
        }
        return transactionStream
                .collect(JiveCollectors.toArrayNode())
                .toString();
    }

    /**
     * This operation returns the history of a Ticket (with pagination)
     *
     * @param connection        The connection
     * @param ticketId          The Ticket Id
     * @param types             Types of Transactions to Retrieve
     * @param onlyWithTimeTaken Option to return only Transactions that have Time Taken associated
     * @return Transaction list
     */
    @MediaType(APPLICATION_JSON)
    @Throws({RTErrorTypeProvider.class})
    @OutputJsonType(schema = "metadata/schemas/transaction-page-schema.json")
    public String retrieveTicketHistory(@Connection RTConnection connection, int ticketId, List<String> types,
                                        boolean onlyWithTimeTaken, int page, int perPage) {
        ArrayNode transactions = connection.getClient()
                .retrieveTicketHistoryPage(ticketId, types, page, perPage)
                .withArray("items");
        Stream<ObjectNode> transactionStream = Jive.stream(transactions)
                .map(transaction -> transformTime((ObjectNode) transaction));
        if (onlyWithTimeTaken) {
            transactionStream = transactionStream
                    .filter(this::withTimeTaken);
        }
        return transactionStream
                .collect(JiveCollectors.toArrayNode())
                .toString();
    }

    private ObjectNode transformTime(ObjectNode transaction) {
        LOGGER.info(transaction.toString());
        JsonNode timeTaken = transaction.get("TimeTaken");
        JsonNode internalTime = transaction.get("CF.{tempo interno}");

        double timeTakenAsDouble = timeTaken.asDouble(-1.0);
        double internalTimeAsDouble = internalTime.asDouble(-1.0);

        // Is invalid
        if (timeTakenAsDouble < 0.0) {
            LOGGER.info(
                    "Transaction {}: Invalid TimeTaken {}",
                    transaction.get("id").asText(), timeTaken.asText()
            );
        }
        if (internalTimeAsDouble < 0.0) {
            LOGGER.info(
                    "Transaction {}: Invalid CF.{tempo interno} {}",
                    transaction.get("id").asText(), internalTime.asText()
            );
        }
        transaction.set("TimeTaken", new DoubleNode(max(timeTakenAsDouble, 0.0)));
        transaction.set("CF.{tempo interno}", new DoubleNode(max(internalTimeAsDouble, 0.0)));
        return transaction;
    }

    private boolean withTimeTaken(ObjectNode transaction) {
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
