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

package org.hyperic.hq.ui.servlet;

import java.rmi.RemoteException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.image.chart.AvailabilityReportChart;
import org.hyperic.image.chart.Chart;
import org.hyperic.image.chart.DataPointCollection;
import org.hyperic.util.config.InvalidOptionException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.MeasurementSummary;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.AvailabilityDataPoint;
import org.hyperic.hq.ui.beans.ChartDataBean;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

public class AvailabilityStoplightChartServlet extends ChartServlet {
    

    Log log = LogFactory.getLog(AvailabilityStoplightChartServlet.class.getName());

    /**
     * 
     */
    public AvailabilityStoplightChartServlet() {
        super();
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.ui.servlet.ChartServlet#createChart()
     */
    protected Chart createChart(HttpServletRequest request,
                                ChartDataBean dataBean) {
        return new AvailabilityReportChart();
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.ui.servlet.ChartServlet#plotData(javax.servlet.http.HttpServletRequest)
     */
    protected void plotData(HttpServletRequest request, Chart chart, ChartDataBean dataBean)
            throws ServletException {

        // the user
        int sessionId = RequestUtils.getSessionId(request).intValue();

        // the principal resource
        AppdefEntityID entId = RequestUtils.getEntityId(request);

        // the child resource type
        AppdefEntityTypeID ctype = RequestUtils.getChildResourceTypeId(request);

        MeasurementBoss boss = ContextUtils.getMeasurementBoss( getServletContext() );
        try {
            MeasurementSummary summary = boss.getSummarizedResourceAvailability(sessionId, 
                entId, ctype.getType(), ctype.getId());
            AvailabilityReportChart availChart = (AvailabilityReportChart) chart;
            DataPointCollection data = availChart.getDataPoints();
            data.clear();
            for (Iterator iter = summary.asList().iterator(); iter.hasNext();) {
                Integer avail = (Integer) iter.next();
                data.add(new AvailabilityDataPoint(avail));
            }
            
        } catch (AppdefEntityNotFoundException e) {
            log.error("failed: ", e);
        } catch (SessionTimeoutException e) {
            log.error("failed: ", e);
        } catch (SessionNotFoundException e) {
            log.error("failed: ", e);
        } catch (PermissionException e) {
            log.error("failed: ", e);
        } catch (InvalidOptionException e) {
            log.error("failed: ", e);
        } catch (RemoteException e) {
            log.error("failed: ", e);
        }                   
    }

}
