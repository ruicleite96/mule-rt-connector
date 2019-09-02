package com.eurotux.connector.rt.internal.sources;

import com.eurotux.connector.rt.internal.RTRequestBuilderFactory.RTRequestBuilder;
import com.eurotux.connector.rt.internal.RTUtils;
import com.eurotux.connector.rt.internal.connection.RTConnection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;

@MediaType(APPLICATION_JSON)
public class UpdatedTicketsListener extends PollingSource<String, Void> {

    private static final Logger LOGGER = LogManager.getLogger(UpdatedTicketsListener.class);
    private static final String OBJECT_STORE_KEY = "TICKET_LISTENER_OS";
    private static final String OBJECT_STORE_POLL_DATE_KEY = "POLL_DATE";
    private static final ObjectStoreSettings OBJECT_STORE_SETTINGS = ObjectStoreSettings.unmanagedPersistent();

    private ObjectMapper mapper = new ObjectMapper();


    @Parameter
    private String dateFrom;
    private LocalDate localDateFrom;

    @Parameter
    @Optional(defaultValue = "20")
    private int limit;

    @Parameter
    private Map<String, String> fields;

    @Connection
    private ConnectionProvider<RTConnection> connectionProvider;
    private RTConnection connection;

    @Inject
    private ObjectStoreManager objectStoreManager;
    private ObjectStore<LocalDate> objectStore;


    @Override
    protected void doStart() throws MuleException {
        connection = connectionProvider.connect();
        objectStore = objectStoreManager.getOrCreateObjectStore(OBJECT_STORE_KEY, OBJECT_STORE_SETTINGS);
        localDateFrom = LocalDate.parse(dateFrom, ISO_LOCAL_DATE);
    }

    @Override
    protected void doStop() {
        connectionProvider.disconnect(connection);
    }

    @Override
    public void poll(PollContext<String, Void> pollContext) {
        if (pollContext.isSourceStopping()) {
            return;
        }

        try {
            LocalDate pollDate = getPollDate();
            boolean today = pollDate.isEqual(LocalDate.now());

            LOGGER.info(String.format("Poll started. Poll date: %s", pollDate.format(ISO_LOCAL_DATE)));

            while (true) {
                ArrayNode tickets = retrieveTickets(pollDate, 1);

                if (tickets.size() == 0) {
                    if (today) {
                        LOGGER.info("Nothing to do. 0 tickets.");
                    } else {
                        LOGGER.info(String.format("Skipping date %s. 0 tickets.", pollDate.format(ISO_LOCAL_DATE)));
                        pollDate = nextDay(pollDate);
                    }
                } else {
                    for (JsonNode ticket : tickets) {
                        if (pollContext.isSourceStopping()) {
                            break;
                        }
                        String ticketAsString = mapper.writeValueAsString(ticket);

                        pollContext.accept(item -> {
                            item.setResult(Result.<String, Void>builder().output(ticketAsString).build());
                        });
                    }
                    break;
                }
            }

            if (!today) {
                nextDay(pollDate);
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred: " + e.getMessage(), e);
        }

    }

    private LocalDate nextDay(LocalDate pollDate) {
        pollDate = pollDate.plusDays(1);
        setOSValue(OBJECT_STORE_POLL_DATE_KEY, pollDate);
        return pollDate;
    }

    private ArrayNode retrieveTickets(LocalDate pollDate, int page) throws Exception {
        RTRequestBuilder builder = connection.requestBuilderFactory
                .newRequest("tickets")
                .withParam("per_page", String.valueOf(limit))
                .withParam("page", String.valueOf(page))
                .withParam("query", String.format("'LastUpdated'='%s'", pollDate.format(ISO_LOCAL_DATE)));
        fields.forEach(builder::withParam);

        HttpResponse response = builder.sendSyncWithRetry(GET);

        if (response.getStatusCode() != OK.getStatusCode()) {
            LOGGER.error(String.format("An error occurred calling RT API. Status code: %s.", response.getStatusCode()));
            throw RTUtils.getThrowable(response, response.getReasonPhrase());
        }

        ObjectNode data = (ObjectNode) mapper.readTree(response.getEntity().getContent());
        ArrayNode tickets = data.withArray("items");

        if (tickets.size() != 0) {
            tickets.addAll(retrieveTickets(pollDate, page + 1));
        }

        return tickets;
    }

    @Override
    public void onRejectedItem(Result<String, Void> result, SourceCallbackContext sourceCallbackContext) {
        LOGGER.info("Item rejected " + result.getOutput());
    }

    private LocalDate getPollDate() throws ObjectStoreException {
        return getOSValue(OBJECT_STORE_POLL_DATE_KEY, localDateFrom);
    }

    private void setOSValue(String key, LocalDate value) {
        try {
            if (this.objectStore.contains(key)) {
                this.objectStore.remove(key);
            }
            this.objectStore.store(key, value);
            LOGGER.info(String.format("ObjectStore Store %s => %s", key, value.toString()));
        } catch (ObjectStoreException e) {
            e.printStackTrace();
        }
    }

    private LocalDate getOSValue(String key, LocalDate defaultValue) throws ObjectStoreException {
        LocalDate value = defaultValue;
        if (this.objectStore.contains(key)) {
            value = this.objectStore.retrieve(key);
        }
        LOGGER.info(String.format("ObjectStore Retrieve %s => %s", key, value.toString()));
        return value;
    }
}
