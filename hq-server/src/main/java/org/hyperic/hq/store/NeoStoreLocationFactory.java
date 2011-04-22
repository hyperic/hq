package org.hyperic.hq.store;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;

public class NeoStoreLocationFactory implements FactoryBean<String> {

    @Value("${neo4j.location}")
    private String neo4jLocation;

    public String getObject() throws Exception {
        return neo4jLocation;
    }

    public Class<?> getObjectType() {
        return String.class;
    }

    public boolean isSingleton() {
        return true;
    }

}
