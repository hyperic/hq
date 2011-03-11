package org.hyperic.hibernate;

import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hyperic.hibernate.dialect.HQDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;

@Component
public class DialectAccessorImpl implements DialectAccessor {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    public HQDialect getHQDialect() {
        return (HQDialect) getDialect();
    }

    public Dialect getDialect() {
        return ((SessionFactoryImplementor) ((Session) EntityManagerFactoryUtils
            .getTransactionalEntityManager(entityManagerFactory).getDelegate()).getSessionFactory())
            .getDialect();
    }

}
