package com.eurotux.connector.rt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.io.IOException;

public class RTTestCases {

    @Test
    public void executeTest() {
        String json = "{\"TimeTaken\": \"0m\", \"InternalTime\": \"\"}";
        ObjectMapper mapper = new ObjectMapper();
        try {
            ObjectNode objectNode = (ObjectNode) mapper.readTree(json);

            JsonNode timeTaken = objectNode.get("TimeTaken");
            JsonNode internalTime = objectNode.get("InternalTime");

            System.out.println(String.format(
                    "TimeTaken: %s; InternalTime: %s",
                    timeTaken.asDouble(),
                    internalTime.asDouble()
            ));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
