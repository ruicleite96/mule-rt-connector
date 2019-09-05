package com.eurotux.connector.rt;

import org.junit.Assert;
import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.event.Event;

public class RTTestCases extends MuleArtifactFunctionalTestCase {

    @Override
    protected String getConfigFile() {
        return "test-mule-config.xml";
    }

    @Test
    public void testRetrieveTicket() {
        try {
            Event event = flowRunner("retrieveTicketFlow").run();
            Object payloadValue = event.getMessage()
                    .getPayload()
                    .getValue();
            Assert.assertNotNull(payloadValue);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testSearchTickets() {
        try {
            Event event = flowRunner("searchTicketsFlow").run();
            Object payloadValue = event.getMessage()
                    .getPayload()
                    .getValue();
            Assert.assertNotNull(payloadValue);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testRetrieveTicketHistory() {
        try {
            Event event = flowRunner("retrieveTicketHistoryFlow").run();
            Object payloadValue = event.getMessage()
                    .getPayload()
                    .getValue();
            Assert.assertNotNull(payloadValue);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
