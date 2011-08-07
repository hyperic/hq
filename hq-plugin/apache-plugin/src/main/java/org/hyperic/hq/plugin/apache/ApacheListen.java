/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.apache;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author administrator
 */
public class ApacheListen {

    private String port;
    private String ip;
    private String name;

    ApacheListen(String name, String ip, String port) {
        this.name=name;
        this.ip=ip;
        this.port=port;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }


    @Override
    public String toString() {
        return "ApacheListen{" + "port=" + port + ", ip=" + ip + ", name=" + name + '}';
    }
}
