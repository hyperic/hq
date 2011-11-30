package org.hyperic.hq.plugin.jboss7.objects;

import java.util.Map;

public class WebSubsystem {
    private Map<String,Connector> connector;

    public Map<String,Connector> getConector() {
        return connector;
    }

    public void setConector(Map<String,Connector> conector) {
        this.connector = conector;
    }

    @Override
    public String toString() {
        return "WebSubsystem{" + "connector=" + connector + '}';
    } 
}
