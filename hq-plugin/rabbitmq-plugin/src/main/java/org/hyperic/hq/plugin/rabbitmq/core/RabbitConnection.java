/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.rabbitmq.core;

import org.hyperic.hq.plugin.rabbitmq.collect.MetricConstants;
import org.hyperic.util.config.ConfigResponse;

/**
 * ErlangConnection is a simple POJO reflecting the response <connectioninfoitem>
 * @author Helena Edelson
 */
public class RabbitConnection implements RabbitObject {

    private String name;
    private long recvOct;
    private long recvCnt;
    private long sendOct;
    private long sendCnt;
    private long sendPend;
    private String state; // running
    private long channels;
    private String authMechanism;
    private String protocol;
    private String user;
    private int timeout;
    private boolean ssl;

    public RabbitConnection() {
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param pid the name to set
     */
    public void setName(String pid) {
        this.name = pid;
    }

    /**
     * @return the recvOct
     */
    public long getRecvOct() {
        return recvOct;
    }

    /**
     * @param recvOct the recvOct to set
     */
    public void setRecvOct(long recvOct) {
        this.recvOct = recvOct;
    }

    /**
     * @return the recvCnt
     */
    public long getRecvCnt() {
        return recvCnt;
    }

    /**
     * @param recvCnt the recvCnt to set
     */
    public void setRecvCnt(long recvCnt) {
        this.recvCnt = recvCnt;
    }

    /**
     * @return the sendOct
     */
    public long getSendOct() {
        return sendOct;
    }

    /**
     * @param sendOct the sendOct to set
     */
    public void setSendOct(long sendOct) {
        this.sendOct = sendOct;
    }

    /**
     * @return the sendCnt
     */
    public long getSendCnt() {
        return sendCnt;
    }

    /**
     * @param sendCnt the sendCnt to set
     */
    public void setSendCnt(long sendCnt) {
        this.sendCnt = sendCnt;
    }

    /**
     * @return the sendPend
     */
    public long getSendPend() {
        return sendPend;
    }

    /**
     * @param sendPend the sendPend to set
     */
    public void setSendPend(long sendPend) {
        this.sendPend = sendPend;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return the channels
     */
    public long getChannels() {
        return channels;
    }

    /**
     * @param channels the channels to set
     */
    public void setChannels(long channels) {
        this.channels = channels;
    }

    /**
     * @return the authMechanism
     */
    public String getAuthMechanism() {
        return authMechanism;
    }

    /**
     * @param authMechanism the authMechanism to set
     */
    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * @return the ssl
     */
    public boolean isSsl() {
        return ssl;
    }

    /**
     * @param ssl the ssl to set
     */
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    @Override
    public String toString() {
        return "RabbitConnection{name=" + name + ", recvOct=" + recvOct + " ,recvCnt=" + recvCnt + " ,sendOct=" + sendOct + " ,sendCnt=" + sendCnt + ", sendPend=" + sendPend + ", state=" + state + ", channels=" + channels + ", authMechanism=" + authMechanism + ", protocol=" + protocol + ", user=" + user + ", timeout=" + timeout + ", ssl=" + ssl + '}';
    }

    public String getServiceType() {
        return AMQPTypes.CONNECTION;
    }

    public String getServiceName() {
        return getServiceType() + " " + getName();
    }

    public ConfigResponse getProductConfig() {
        ConfigResponse c = new ConfigResponse();
        c.setValue(MetricConstants.CONNECTION, getName());
        return c;
    }

    public ConfigResponse getCustomProperties() {
        ConfigResponse c = new ConfigResponse();
        c.setValue("authMechanism", getAuthMechanism());
        c.setValue("protocol", getProtocol());
        c.setValue("user", getUser());
        c.setValue("timeout", getTimeout());
        c.setValue("ssl", isSsl());
        return c;
    }

    public boolean isDurable() {
        return false;
    }
}
