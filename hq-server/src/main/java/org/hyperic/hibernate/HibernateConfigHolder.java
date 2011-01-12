package org.hyperic.hibernate;

import java.util.Iterator;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.mapping.Table;

public class HibernateConfigHolder extends HibernatePersistence {
    
    private Ejb3Configuration cfg;

    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
        this.cfg = new Ejb3Configuration();
        Ejb3Configuration configured = cfg.configure( info, properties );
        return configured != null ? configured.buildEntityManagerFactory() : null;
    }
    
    @SuppressWarnings("unchecked")
    public Iterator<Table> getTableMappings() {
        return cfg.getTableMappings();
    }
}
