/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.plugin.jboss7.objects;

public class Connector {

    private String protocol;
    private String scheme;
    private String bytesReceived;
    private String bytesSent;
    private String errorCount;
    private String maxTime;
    private String processingTime;
    private String requestCount;

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
     * @return the scheme
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * @param scheme the scheme to set
     */
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    /**
     * @return the bytesReceived
     */
    public String getBytesReceived() {
        return bytesReceived;
    }

    /**
     * @param bytesReceived the bytesReceived to set
     */
    public void setBytesReceived(String bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    /**
     * @return the bytesSent
     */
    public String getBytesSent() {
        return bytesSent;
    }

    /**
     * @param bytesSent the bytesSent to set
     */
    public void setBytesSent(String bytesSent) {
        this.bytesSent = bytesSent;
    }

    /**
     * @return the errorCount
     */
    public String getErrorCount() {
        return errorCount;
    }

    /**
     * @param errorCount the errorCount to set
     */
    public void setErrorCount(String errorCount) {
        this.errorCount = errorCount;
    }

    /**
     * @return the maxTime
     */
    public String getMaxTime() {
        return maxTime;
    }

    /**
     * @param maxTime the maxTime to set
     */
    public void setMaxTime(String maxTime) {
        this.maxTime = maxTime;
    }

    /**
     * @return the processingTime
     */
    public String getProcessingTime() {
        return processingTime;
    }

    /**
     * @param processingTime the processingTime to set
     */
    public void setProcessingTime(String processingTime) {
        this.processingTime = processingTime;
    }

    /**
     * @return the requestCount
     */
    public String getRequestCount() {
        return requestCount;
    }

    /**
     * @param requestCount the requestCount to set
     */
    public void setRequestCount(String requestCount) {
        this.requestCount = requestCount;
    }

    @Override
    public String toString() {
        return "Connector{" + "protocol=" + protocol + ", scheme=" + scheme + ", bytesReceived=" + bytesReceived + ", bytesSent=" + bytesSent + ", errorCount=" + errorCount + ", maxTime=" + maxTime + ", processingTime=" + processingTime + ", requestCount=" + requestCount + '}';
    }
}
