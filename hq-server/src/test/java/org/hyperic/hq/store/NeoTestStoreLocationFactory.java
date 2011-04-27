package org.hyperic.hq.store;

import java.io.File;
import java.util.Random;

import org.springframework.beans.factory.FactoryBean;

public class NeoTestStoreLocationFactory implements FactoryBean<String> {

    public String getObject() throws Exception {
        long suffix = getFileSuffix();
        while(new File("target/data/" + suffix).exists()) {
            suffix = getFileSuffix();
        }
        return "target/data/" + suffix;
    }
    
    private long getFileSuffix() {
        Random random = new Random(System.currentTimeMillis());
        return random.nextLong();
    }

    public Class<?> getObjectType() {
        return String.class;
    }

    public boolean isSingleton() {
        return true;
    }

}

