package org.hyperic.hq.api.model.config;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;


@XmlType(namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlEnum
public enum ServerConfigStatus {
    ACTIVE,
    CONNECTION_PROBLEM,
    CONFIGURED;
}
