/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.agent;

public class FileDataResult implements java.io.Serializable {

    private static final long serialVersionUID = -9153991732357001254L;

    private String fileName;
    private long sendBytes;
    private long sendTime;
    
    public FileDataResult(String fileName, long sendBytes, long sendTime) {
        this.fileName = fileName;
        this.sendBytes = sendBytes;
        this.sendTime = sendTime;
    }

    /**
     * The file that was sent
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Return the number of bytes sent
     */
    public long getSendBytes() {
        return this.sendBytes;
    }

    /**
     * Return the transfer time in milliseconds
     */
    public long getSendTime() {
        return this.sendTime;
    }

    /**
     * Return the transfer time in seconds
     */
    public double getSendTimeSeconds() {
        return (double)this.sendTime/1000;
    }
     
    /**
     * Return the transfer rate (in Kb/sec)
     */
    public double getTxRate() {
        return (double)(this.sendBytes/1024)/getSendTimeSeconds();
    }
}
