/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.measurement.galerts;

import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.events.SimpleAlertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.measurement.server.session.MetricAuxLogPojo;
import org.hyperic.hq.measurement.server.session.Measurement;

/**
 * Used to create {@link MetricAuxLog} objects
 */
public class MetricAuxLog
    extends SimpleAlertAuxLog
{
    private Measurement _measurement;
    
    public MetricAuxLog(String desc, long timestamp, 
                        Measurement measurement)
    {
        super(desc, timestamp);
        _measurement = measurement;
    }
    
    MetricAuxLog(GalertAuxLog gAuxLog, MetricAuxLogPojo auxLog) { 
        this(gAuxLog.getDescription(), gAuxLog.getTimestamp(), 
             auxLog.getMetric());
    }

    public Measurement getMetric() {
        return _measurement;
    }

    public String getURL() {
        return "/resource/common/monitor/Visibility.do?m=" +
               _measurement.getTemplate().getId() +
               "&eid=" + _measurement.getEntityId().toString() +
               "&mode=chartSingleMetricSingleResource";
    }

    public AlertAuxLogProvider getProvider() {
        return MetricAuxLogProvider.INSTANCE;
    }
}
