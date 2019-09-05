package com.eurotux.connector.rt.internal;

import com.eurotux.connector.rt.internal.operations.RTOperations;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.eurotux.connector.rt.internal.RTConstants.TICKET_FIELDS_PARAMS;
import static com.eurotux.connector.rt.internal.RTConstants.TRANSACTION_FIELDS_PARAMS;
import static com.eurotux.connector.rt.internal.RTUtils.jsonResponseToJsonNode;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;

public class RTClient {

    private static Logger LOGGER = LoggerFactory.getLogger(RTClient.class);

    private RTRequestBuilderFactory requestBuilderFactory;

    private HttpService httpService;

    private ObjectMapper mapper = new ObjectMapper();


    public RTClient(HttpService httpService, RTRequestBuilderFactory requestBuilderFactory) {
        this.httpService = httpService;
        this.requestBuilderFactory = requestBuilderFactory;
    }

    public void initHttpClient() throws ConnectionException {
        try {
            requestBuilderFactory.initHttpClient(httpService);
        } catch (InitialisationException e) {
            throw new ConnectionException(e);
        }
    }

    public void stopHttpClient() {
        this.requestBuilderFactory.stopHttpClient();
    }

    public boolean validate() {
        return requestBuilderFactory.validate();
    }

    public ObjectNode retrieveTicket(int ticketId) {
        HttpResponse response = requestBuilderFactory
                .newRequest(String.format("ticket/%d", ticketId))
                .withParams(TICKET_FIELDS_PARAMS)
                .sendSyncWithRetry(GET);
        return (ObjectNode) jsonResponseToJsonNode(response);
    }

    public ObjectNode retrieveTicketsPage(String ticketSQL, int page, int perPage) {
        HttpResponse response = requestBuilderFactory
                .newRequest("tickets")
                .withParam("query", ticketSQL)
                .withParam("page", String.valueOf(page))
                .withParam("per_page", String.valueOf(perPage))
                .withParams(TICKET_FIELDS_PARAMS)
                .sendSyncWithRetry(GET);
        return (ObjectNode) jsonResponseToJsonNode(response);
    }

    public ArrayNode retrieveTickets(String ticketSQL) {
        ArrayNode tickets = mapper.createArrayNode();
        int count = Integer.MAX_VALUE;
        int page = 0;
        while (count != 0) {
            ArrayNode _tickets = retrieveTicketsPage(ticketSQL, page++, 100)
                    .withArray("items");
            tickets.addAll(_tickets);
            count = _tickets.size();
        }
        return tickets;
    }

    public ObjectNode retrieveTicketHistoryPage(int ticketId, List<String> types, int page, int perPage) {
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
        types.forEach(type -> body.add(mapper.createObjectNode()
                .put("field", "Type")
                .put("operator", "=")
                .put("value", type)
        ));

        RTRequestBuilderFactory.RTRequestBuilder builder = requestBuilderFactory
                .newRequest("transactions")
                .withParams(TRANSACTION_FIELDS_PARAMS)
                .withParam("page", String.valueOf(page))
                .withParam("per_page", String.valueOf(perPage))
                .withBody(body);

        HttpResponse response = builder.sendSyncWithRetry(POST);
        return (ObjectNode) jsonResponseToJsonNode(response);
    }

    public ArrayNode retrieveTicketHistory(int ticketId, List<String> types) {
        ArrayNode transactions = mapper.createArrayNode();
        int count = Integer.MAX_VALUE;
        int page = 0;
        while (count != 0) {
            ArrayNode _transactions = retrieveTicketHistoryPage(ticketId, types, page++, 100)
                    .withArray("items");
            transactions.addAll(_transactions);
            count = _transactions.size();
        }
        return transactions;
    }
}
