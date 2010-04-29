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

package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.measurement.galerts.MetricAuxLog;
import org.hyperic.hq.measurement.shared.MetricAuxLogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 
 */
@Service
@Transactional
public class MetricAuxLogManagerImpl implements MetricAuxLogManager, ApplicationListener<MetricsDeleteRequestedEvent> {
    private static final int CHUNKSIZE = 500;

    private MetricAuxLogDAO metricAuxLogDAO;

    @Autowired
    public MetricAuxLogManagerImpl(MetricAuxLogDAO metricAuxLogDAO) {
        this.metricAuxLogDAO = metricAuxLogDAO;
    }

    /**
     * 
     */
    public MetricAuxLogPojo create(GalertAuxLog log, MetricAuxLog logInfo) {
        MetricAuxLogPojo metricLog = new MetricAuxLogPojo(log, logInfo, log.getAlert().getAlertDef());

        metricAuxLogDAO.save(metricLog);
        return metricLog;
    }

    /**
     * 
     */
    public void removeAll(GalertDef def) {
        metricAuxLogDAO.removeAll(def);
    }

    /**
     * 
     */
    @Transactional(readOnly=true)
    public MetricAuxLogPojo find(GalertAuxLog log) {
        return metricAuxLogDAO.find(log);
    }

    /**
     * Callback, invoked when metrics are deleted. Since we still want to keep
     * the measurement around, we delete the value from the metric_aux_log and
     * transform the entry in the galert_aux_log to a regular entry.
     * 
     * 
     */
    public void onApplicationEvent(MetricsDeleteRequestedEvent event) {
        Collection<Integer> mids = event.getMetricIds();
        if (mids != null) {

            List<Integer> asList = (mids instanceof List<?> ? (List<Integer>) mids : new ArrayList<Integer>(mids));

            for (int i = 0; i < asList.size(); i += CHUNKSIZE) {
                int end = Math.min(i + CHUNKSIZE, asList.size());
                metricAuxLogDAO.resetAuxType(asList.subList(i, end));
                metricAuxLogDAO.deleteByMetricIds(asList.subList(i, end));
            }
        }
        
    }
}
