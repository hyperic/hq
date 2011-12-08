package org.hyperic.hq.plugin.jboss7.objects;

public class ServerInfo {

    private String name;
    private String version;

    @Override
    public String toString() {
        return "ServerInfo{" + "name=" + name + ", version=" + version + '}';
    }
}
