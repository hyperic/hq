/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.apache;

/**
 *
 * @author administrator
 */
public class ApacheVHost {

    private String name;
    private String ip;
    private String port;
    private String serverName;

    public ApacheVHost(String name, String ip, String port, String serverName) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        if (serverName.equals("")) {
            this.serverName = name+":"+port;
        } else {
            this.serverName = serverName;
        }
    }

    public String getIp() {
        return ip;
    }

    public String getName() {
        return name;
    }

    public String getPort() {
        return port;
    }

    public String getServerName() {
        return serverName;
    }

    @Override
    public String toString() {
        return "ApacheVHost{" + "name=" + name + ", ip=" + ip + ", port=" + port + ", serverName=" + serverName + '}';
    }
}
