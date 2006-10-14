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

package org.hyperic.hq.product;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import org.hyperic.util.encoding.Base64;

public class RtStat implements Serializable {

    public static final String NULL_IP = "0.0.0.0";
    public static final String NULL_URL = "?unknown-url?";

    private String ip = null;
    private String url = null;
    private long begin = Long.MAX_VALUE;
    private long end = Long.MIN_VALUE;
    private double min = Double.NaN;
    private double max = Double.NEGATIVE_INFINITY;
    private double total = 0;
    private int count = 0;
    private Hashtable status = null;
    private Integer svcID;
    private int svcType = RtPlugin.UNKNOWN;

    // Only useful for testing.  Will cause NPEs in a running server.  Do not
    // use this API unless it is for a test.
    public RtStat() {}

    public RtStat(Integer svcID) {
        this(svcID, RtPlugin.UNKNOWN);
    }

    public RtStat(Integer svcID, String ip) {
        this(svcID, RtPlugin.UNKNOWN, ip);
    }

    public RtStat(Integer svcID, int svcType) {
        this(svcID, svcType, null);
    }

    public RtStat(Integer svcID, int svcType, String ip) {
        this.ip = ip;
        this.svcID = svcID;
        this.svcType = svcType;
        status = new Hashtable();
    }

    public void recompute(String givenurl, Date date, double timetaken, 
                          Integer statcode) {
        if (url == null) {
            url = givenurl;
        }
        long finish = date.getTime() + (long)timetaken;
        long start = date.getTime();
        if (start < begin) {
            begin = start;
        }
        if (finish > end) {
            end = finish;
        }
        if (statcode.compareTo(new Integer(200)) >= 0 &&
            statcode.compareTo(new Integer(300)) < 0) {
            count++;
            total += timetaken;
            if ((timetaken < min || Double.isNaN(min)) && timetaken >= 0) {
                min = timetaken;
            }
            if (timetaken > max) {
                max = timetaken;
            }
        }
        else {
            if (status.get(statcode) == null) {
                status.put(statcode, new Integer(1));
            }
            else {
                Integer number = (Integer)status.get(statcode);
                status.put(statcode, new Integer(number.intValue() + 1));
            }
        }
    }

    public void recompute(RtStat rs) {
        if (rs == null) {
            return;
        }
        
        if (svcType == RtPlugin.UNKNOWN) {
            svcType = rs.svcType;
        }
        if (url == null) {
            url = rs.url;
        }
        if (ip == null) {
            ip = rs.ip;
        }
        
        if (rs.getBegin() < begin) {
            begin = rs.begin;
        }
        if (rs.getEnd() > end) {
            end = rs.end;
        }
        if (rs.count > 0) {
            count += rs.count;
            if (rs.min > 0) {
                total += rs.total;
            }
            if ((rs.min < min || Double.isNaN(min)) && rs.min >= 0) {
                min = rs.min;
            }
            if (rs.max > max) {
                max = rs.max;
            }
        }
        else {
            Enumeration en = rs.status.keys();
            
            while (en.hasMoreElements()) {
                Integer stat = (Integer)en.nextElement();
                Integer number = (Integer)status.get(stat);
                
                if (number == null) {
                    number = new Integer(0);
                }
                
                status.put(stat, new Integer(number.intValue() + 
                                    ((Integer)rs.status.get(stat)).intValue()));
            }
        }
    }

    /**
     * XXX: For 3.0 we should validate the IP address passed into setIp is 
     *      valid
     */
    public String getIp() { 
        if (ip == null || ip.length() == 0) {
            return NULL_IP;
        } else {
            return this.ip;
        }
    }
    public void   setIp(String ip) { this.ip = ip; }
    public void   resetIp() { this.ip = null; }

    public String getUrl() { return (url == null) ? NULL_URL : url; }
    public void   setUrl(String u) { url = u; }

    public long getBegin() { return begin; }
    public void setBegin(long d) { begin = d; }

    public long getEnd() { return end; }
    public void setEnd(long d) { end = d; }

    public double getMin() { return min; }
    public void   setMin(double min) { this.min = min; }
    
    public double getMax() { return max; }
    public void   setMax(double max) { this.max = max; }

    public double getTotal() { return total; }
    public void   setTotal(double total) { this.total = total; }

    public int  getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public Hashtable getStatus() { return status; }
    public void setStatus( Hashtable st ) { this.status = st; }

    public Integer getSvcID() { return svcID; }
    public void    setSvcID(Integer ID) { svcID = ID; }

    public int  getSvcType() { return svcType; }
    public void setSvcType(int st) { svcType = st; }

    public double getAverage() { return (count == 0) ? 0 : (total / count); }

    public boolean isEndUser() { return (svcType == RtPlugin.ENDUSER); }

    public String getIpUrlKey () { return getIp() + ":" + getUrl(); }

    public String toString () {
        StringBuffer sb = new StringBuffer ();
        sb.append("[RtStat")
            .append(" svcid=").append(svcID)
            .append(" ip=").append(getIp())
            .append(" url=").append(getUrl())
            .append(" min=").append(min)
            .append(" max=").append(max)
            .append(" count=").append(count)
            .append(" total=").append(total)
            .append(" begin=").append(begin)
            .append(" end=").append(end)
            .append(" status=").append(status)
            .append("]");
        return sb.toString();
    }

    public String encode() throws IOException {

        ByteArrayOutputStream bOs = null;
        DataOutputStream dOs = null;

        try {
            bOs = new ByteArrayOutputStream();
            dOs = new DataOutputStream(bOs);

            dOs.writeUTF(getIp());
            dOs.writeUTF(url);
            dOs.writeLong(begin);
            dOs.writeLong(end);
            dOs.writeDouble(min);
            dOs.writeDouble(max);
            dOs.writeDouble(total);
            dOs.writeInt(count);
            dOs.writeInt(svcType);
            dOs.writeInt(svcID.intValue());
            
            dOs.writeInt(status.size());

            Enumeration keys = status.keys();
            while (keys.hasMoreElements()) {
                Integer key = (Integer)keys.nextElement();
                int val = ((Integer)status.get(key)).intValue();
                dOs.writeInt(key.intValue());
                dOs.writeInt(val);
            }
            return Base64.encode(bOs.toByteArray());

        } finally {
            dOs.close();
            bOs.close();
        }
    }
          
    public static RtStat decode(String val) throws IOException {

        ByteArrayInputStream bIs = null;
        DataInputStream dIs = null;
        RtStat rs = new RtStat();
        
        try {
            bIs = new ByteArrayInputStream(Base64.decode(val));
            dIs = new DataInputStream(bIs);
    
            rs.ip      = dIs.readUTF();
            rs.url     = dIs.readUTF();
            rs.begin   = dIs.readLong();
            rs.end     = dIs.readLong();
            rs.min     = dIs.readDouble();
            rs.max     = dIs.readDouble();
            rs.total   = dIs.readDouble();
            rs.count   = dIs.readInt();
            rs.svcType = dIs.readInt();
            rs.svcID   = new Integer(dIs.readInt());
            
            int numEntries = dIs.readInt();
            rs.status = new Hashtable(numEntries);
            
            while (numEntries > 0) {
                Integer key = new Integer(dIs.readInt());
                Integer value = new Integer(dIs.readInt());
                
                rs.status.put(key, value);
                numEntries--;
            }
            return rs;

        } finally {
            dIs.close();
            bIs.close();
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof RtStat)) return false;
        RtStat o = (RtStat) obj;
        return (getIp().equals(o.getIp()) &&
                url.equals(o.url) && 
                begin == o.begin &&
                end == o.end &&
                (Double.isNaN(min) && Double.isNaN(o.min)) &&
                max == o.max &&
                total == o.total &&
                count == o.count &&
                svcType == o.svcType &&
                svcID.equals(o.svcID) &&
                status.equals(o.status));
    }
}

