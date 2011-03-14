package org.hyperic.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hyperic.hibernate.dialect.HQDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DialectAccessorImpl implements DialectAccessor {

    @Autowired
    private SessionFactory sessionFactory;

    public HQDialect getHQDialect() {
        return (HQDialect) getDialect();
    }

    public Dialect getDialect() {
        return ((SessionFactoryImplementor) sessionFactory).getDialect();
    }

}
