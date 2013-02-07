package org.hyperic.hq.api.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlEnum
public enum NotificationType {
    Create,
    Update,
    Delete;
}
