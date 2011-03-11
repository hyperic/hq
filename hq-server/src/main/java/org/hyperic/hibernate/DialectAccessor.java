package org.hyperic.hibernate;

import org.hibernate.dialect.Dialect;
import org.hyperic.hibernate.dialect.HQDialect;

public interface DialectAccessor {

    HQDialect getHQDialect();
    
    Dialect getDialect();

}
