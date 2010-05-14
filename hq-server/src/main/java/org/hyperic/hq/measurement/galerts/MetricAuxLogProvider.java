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
package org.hyperic.hq.measurement.galerts;

import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.AlertAuxLog;
import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.shared.GalertManager;
import org.hyperic.hq.measurement.server.session.MetricAuxLogPojo;
import org.hyperic.hq.measurement.shared.MetricAuxLogManager;

/**
 * Metric information used as aux data for galerts
 */
public class MetricAuxLogProvider
    extends AlertAuxLogProvider
{
    private static final String BUNDLE = 
        "org.hyperic.hq.measurement.Resources";
    
    private static final Log _log = 
        LogFactory.getLog(MetricAuxLogProvider.class);

    public static final MetricAuxLogProvider INSTANCE =  
        new MetricAuxLogProvider(0xdecafbad, "Auxillary Metric Data",
                                 "metric.auxLog");

    private MetricAuxLogProvider(int code, String desc, String localeProp) {
        super(code, desc, localeProp, ResourceBundle.getBundle(BUNDLE)); 
    }

    private GalertAuxLog findGAuxLog(int id) {
        return Bootstrap.getBean(GalertManager.class).findAuxLogById(new Integer(id));
    }

    public AlertAuxLog load(int auxLogId, long timestamp, String desc) {
        GalertAuxLog gAuxLog = findGAuxLog(auxLogId);
        MetricAuxLogPojo auxLog = 
            Bootstrap.getBean(MetricAuxLogManager.class).find(gAuxLog);
        
        return new MetricAuxLog(gAuxLog, auxLog);
    }

    public void save(int auxLogId, AlertAuxLog log) {
        MetricAuxLog logInfo = (MetricAuxLog)log;
        GalertAuxLog gAuxLog = findGAuxLog(auxLogId);
        
        Bootstrap.getBean(MetricAuxLogManager.class).create(gAuxLog, logInfo);
    }

    public void deleteAll(GalertDef def) {
        Bootstrap.getBean(MetricAuxLogManager.class).removeAll(def);
    }
}
