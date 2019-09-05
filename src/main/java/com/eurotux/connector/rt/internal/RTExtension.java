package com.eurotux.connector.rt.internal;

import com.eurotux.connector.rt.internal.connection.RTBasicConnectionProvider;
import com.eurotux.connector.rt.internal.connection.RTTokenConnectionProvider;
import com.eurotux.connector.rt.internal.error.RTError;
import com.eurotux.connector.rt.internal.operations.RTOperations;
import com.eurotux.connector.rt.internal.sources.UpdatedTicketsListener;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;

import static org.mule.runtime.api.meta.Category.COMMUNITY;


@Xml(prefix = "rt")
@Extension(name = "Request Tracker", vendor = "Eurotux Informática, S.A.", category = COMMUNITY)
@ConnectionProviders({RTBasicConnectionProvider.class, RTTokenConnectionProvider.class})
@Operations({RTOperations.class})
@Sources({UpdatedTicketsListener.class})
@ErrorTypes(RTError.class)
public class RTExtension {

}
