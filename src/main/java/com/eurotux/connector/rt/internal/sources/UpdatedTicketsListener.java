package com.eurotux.connector.rt.internal.sources;

import com.eurotux.connector.rt.internal.connection.RTConnection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import javax.inject.Inject;
import java.time.LocalDate;

import static com.eurotux.connector.rt.internal.error.RTError.EXECUTION;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

@MediaType(APPLICATION_JSON)
public class UpdatedTicketsListener extends PollingSource<String, Void> {

    private static final Logger LOGGER = LogManager.getLogger(UpdatedTicketsListener.class);
    private static final String OBJECT_STORE_KEY = "TICKET_LISTENER_OS";
    private static final String OBJECT_STORE_POLL_DATE_KEY = "POLL_DATE";
    private static final ObjectStoreSettings OBJECT_STORE_SETTINGS = ObjectStoreSettings.unmanagedPersistent();

    @Parameter
    private String dateFrom;
    private LocalDate localDateFrom;

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
        LocalDate pollDate = getOSValue(OBJECT_STORE_POLL_DATE_KEY, localDateFrom);
        boolean today = pollDate.isEqual(LocalDate.now());
        LOGGER.info(String.format("Poll started. Poll date: %s", pollDate.format(ISO_LOCAL_DATE)));

        int total = 0;
        while (true) {
            ArrayNode tickets = connection.getClient()
                    .retrieveTickets(String.format("'LastUpdated'='%s'", pollDate.format(ISO_LOCAL_DATE)));
            total += tickets.size();
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
                    pollContext.accept(item -> {
                        item.setResult(Result.<String, Void>builder().output(ticket.toString()).build());
                    });
                }
                break;
            }
        }
        LOGGER.info(String.format("Poll stopped. %d tickets retrieved.", total));
        if (!today) {
            nextDay(pollDate);
        }
    }

    private LocalDate nextDay(LocalDate pollDate) {
        pollDate = pollDate.plusDays(1);
        setOSValue(OBJECT_STORE_POLL_DATE_KEY, pollDate);
        return pollDate;
    }

    @Override
    public void onRejectedItem(Result<String, Void> result, SourceCallbackContext sourceCallbackContext) {
        LOGGER.info("Item rejected " + result.getOutput());
    }

    private void setOSValue(String key, LocalDate value) {
        try {
            if (this.objectStore.contains(key)) {
                this.objectStore.remove(key);
            }
            this.objectStore.store(key, value);
        } catch (ObjectStoreException e) {
            throw new ModuleException(EXECUTION, e);
        }
    }

    private LocalDate getOSValue(String key, LocalDate defaultValue) {
        try {
            LocalDate value = defaultValue;
            if (this.objectStore.contains(key)) {
                value = this.objectStore.retrieve(key);
            }
            return value;
        } catch (ObjectStoreException e) {
            throw new ModuleException(EXECUTION, e);
        }
    }
}
