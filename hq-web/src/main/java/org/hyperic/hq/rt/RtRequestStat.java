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

package org.hyperic.hq.rt;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.product.RtStat;

public class RtRequestStat extends PersistedObject
{
    // Fields
    private String ipaddr;
    private double min;
    private double max;
    private double total;
    private Integer count;
    private long beginTime;
    private long endTime;
    private Integer svctype;
    private RtSvcReq svcReqId;
    private Set rtStatErrors;

    // Constructors
    public RtRequestStat() {
    }

    public String getIpaddr() {
        return this.ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    public Double getMin() {
        return new Double(this.min);
    }

    public void setMin(double min) {
        this.min = min;
    }

    public void setMin(Double min) {
        setMin(min.doubleValue());
    }

    public Double getMax() {
        return new Double(this.max);
    }

    public void setMax(double max) {
        this.max = max;
    }

    public void setMax(Double max) {
        setMax(max.doubleValue());
    }

    public Double getTotal() {
        return new Double(this.total);
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public void setTotal(Double average) {
        setTotal(average.doubleValue());
    }

    public Integer getCount() {
        return this.count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Long getBeginTime() {
        return new Long(this.beginTime);
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public void setBeginTime(Long begin) {
        setBeginTime(begin.longValue());
    }

    public Long getEndTime() {
        return new Long(this.endTime);
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setEndTime(Long end) {
        setEndTime(end.longValue());
    }

    public Integer getSvcType() {
        return this.svctype;
    }

    public void setSvcType(Integer svctype) {
        this.svctype = svctype;
    }

    public RtSvcReq getRtSvcReq() {
        return this.svcReqId;
    }

    public void setRtSvcReq(RtSvcReq svcReqId) {
        this.svcReqId = svcReqId;
    }

    public Set getRtStatErrors() {
        return rtStatErrors;
    }

    public void setRtStatErrors(Set s) {
        rtStatErrors = s;
    }

    public RtStat getRequestStat() {
        RtStat rs = new RtStat();

        rs.setSvcType(this.getSvcType().intValue());
        rs.setBegin(this.getBeginTime().longValue());
        rs.setEnd(this.getEndTime().longValue());
        rs.setSvcID(this.getRtSvcReq().getSvcID());
        rs.setUrl(this.getRtSvcReq().getUrl());
        rs.setCount(this.getCount().intValue());
        rs.setMin(this.getMin().doubleValue());
        rs.setMax(this.getMax().doubleValue());
        rs.setTotal(this.getTotal().doubleValue());

        Set s = this.getRtStatErrors();
        if (s != null) {
            Hashtable h = new Hashtable();
            for (Iterator i = s.iterator(); i.hasNext(); ) {
                RtStatError err = (RtStatError)i.next();
                h.put(err.getRtErrorCode().getCode() , err.getErrorCount());
            }
            rs.setStatus(h);
        }
        return rs;
    }
}


