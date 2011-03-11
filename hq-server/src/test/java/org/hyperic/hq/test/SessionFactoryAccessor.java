package org.hyperic.hq.test;

import org.hibernate.SessionFactory;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

public class SessionFactoryAccessor implements FactoryBean<SessionFactory> {

    @Autowired
    private LocalContainerEntityManagerFactoryBean entityManagerFactoryBean;

    public SessionFactory getObject() throws Exception {
        return ((EntityManagerFactoryImpl) entityManagerFactoryBean.getNativeEntityManagerFactory())
            .getSessionFactory();
    }

    public Class<?> getObjectType() {
        return SessionFactory.class;
    }

    public boolean isSingleton() {
        return true;
    }

}
