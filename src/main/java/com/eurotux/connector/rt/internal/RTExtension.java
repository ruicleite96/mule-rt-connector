package com.eurotux.connector.rt.internal;

import com.eurotux.connector.rt.internal.connection.RTConnectionProvider;
import com.eurotux.connector.rt.internal.error.RTError;
import com.eurotux.connector.rt.internal.operations.RTOperations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;

import static org.mule.runtime.api.meta.Category.COMMUNITY;


@Xml(prefix = "rt")
@Extension(name = "Request Tracker", vendor = "Eurotux Informática, S.A.", category = COMMUNITY)
@ConnectionProviders({RTConnectionProvider.class})
@Operations({RTOperations.class})
@ErrorTypes(RTError.class)
public class RTExtension {

}
