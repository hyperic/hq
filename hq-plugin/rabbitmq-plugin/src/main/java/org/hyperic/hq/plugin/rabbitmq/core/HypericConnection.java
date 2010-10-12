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

import com.rabbitmq.client.Address;

import java.util.Map;

/**
 * ErlangConnection is a simple POJO reflecting the response <connectioninfoitem>
 * @author Helena Edelson
 */
public class HypericConnection {
 
    private String pid;
   
    private Address address;

    private Address peerAddress;

    private long octetsReceived;

    private long receiveCount;

    private long octetsSent;

    private long sendCount;

    private long pendingSends;

    private String state; // running

    private long channels;

    private String username;

    private String vhost;

    private long timeout;

    private long frameMax;

    private Map clientProperties;

    @Override
    public String toString() {
        return new StringBuilder("Connection[pid=").append(pid).append(" address=").append(address).append(" peerAddress=").append(peerAddress)
               .append(" octetsReceived=").append(octetsReceived).append(" receiveCount=").append(receiveCount).append(" octetsSent=").append(octetsSent)
                 .append(" sendCount=").append(sendCount).append(" pendingSends=").append(pendingSends).append(" state=").append(state)
                    .append(" channels=").append(channels).append(" username=").append(username).append(" vhost=").append(vhost).append(" timeout=")
                       .append(timeout).append(" frameMax=").append(frameMax).toString(); //.append(" clientProperties=").append(clientProperties)

    }

    public HypericConnection() {

    }
    
    public HypericConnection(String pid) {
        this.pid = pid;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(String host, int port) {
        this.address = new Address(host, port);
    }

    public Address getPeerAddress() {
        return peerAddress;
    }

    public void setPeerAddress(String host, int port) {
        this.peerAddress = new Address(host, port);
    }

    public long getOctetsReceived() {
        return octetsReceived;
    }

    public void setOctetsReceived(long octetsReceived) {
        this.octetsReceived = octetsReceived;
    }

    public long getReceiveCount() {
        return receiveCount;
    }

    public void setReceiveCount(long receiveCount) {
        this.receiveCount = receiveCount;
    }

    public long getOctetsSent() {
        return octetsSent;
    }

    public void setOctetsSent(long octetsSent) {
        this.octetsSent = octetsSent;
    }

    public long getSendCount() {
        return sendCount;
    }

    public void setSendCount(long sendCount) {
        this.sendCount = sendCount;
    }

    public long getPendingSends() {
        return pendingSends;
    }

    public void setPendingSends(long pendingSends) {
        this.pendingSends = pendingSends;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getChannels() {
        return channels;
    }

    public void setChannels(long channels) {
        this.channels = channels;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getFrameMax() {
        return frameMax;
    }

    public void setFrameMax(long frameMax) {
        this.frameMax = frameMax;
    }

    public Map getClientProperties() {
        return clientProperties;
    }

    public void setClientProperties(Map clientProperties) {
        this.clientProperties = clientProperties;
    }
}
