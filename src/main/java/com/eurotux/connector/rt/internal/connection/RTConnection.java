package com.eurotux.connector.rt.internal.connection;

import com.eurotux.connector.rt.internal.RTRequestBuilderFactory;

public final class RTConnection {

    public RTRequestBuilderFactory requestBuilderFactory;

    public RTConfiguration config;


    public RTConnection(RTRequestBuilderFactory requestBuilderFactory, RTConfiguration config) {
        this.requestBuilderFactory = requestBuilderFactory;
        this.config = config;
    }

    public void invalidate() {
        requestBuilderFactory.stopHttpClient();
    }

}
