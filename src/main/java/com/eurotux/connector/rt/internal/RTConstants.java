package com.eurotux.connector.rt.internal;

import java.util.HashMap;
import java.util.Map;

public class RTConstants {

    private static final String QUEUE_FIELDS = "Name,Description,Created,LastUpdated,Disabled";

    private static final String USER_FIELDS = "Name,RealName,EmailAddress,LastUpdated,Created";

    private static final String TICKET_FIELDSs = "EffectiveId,Resolved,Created,Priority,Due,TimeEstimated,Status,Queue," +
            "Started,Starts,TimeWorked,LastUpdated,Subject,Creator,Owner,LastUpdatedBy";

    private static final String TRANSACTION_FIELDS = "TimeTaken,Creator,Type,Created,CF.{tempo interno}";

    public static final Map<String, String> TICKET_FIELDS_PARAMS = new HashMap<String, String>() {
        {
            put("fields", TICKET_FIELDSs);
            put("fields[Queue]", QUEUE_FIELDS);
            put("fields[Creator]", USER_FIELDS);
            put("fields[Owner]", USER_FIELDS);
        }
    };

    public static final Map<String, String> TRANSACTION_FIELDS_PARAMS = new HashMap<String, String>() {
        {
            put("fields", TRANSACTION_FIELDS);
            put("fields[Creator]", USER_FIELDS);
        }
    };

}
