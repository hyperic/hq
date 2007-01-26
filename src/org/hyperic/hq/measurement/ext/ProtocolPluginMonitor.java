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

package org.hyperic.hq.measurement.ext;

import java.util.Hashtable;

import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.server.session.RawMeasurement;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.MeasurementPluginManager;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.shared.ProductManagerLocal;
import org.hyperic.hq.product.shared.ProductManagerUtil;
import org.hyperic.util.schedule.Schedule;

public class ProtocolPluginMonitor
    extends ScheduledMonitor
{
    private static Schedule _monitorSchedule = new Schedule();
    private static Hashtable _measurementMeta = new Hashtable();
    private static MeasurementPluginManager _mpm  = null;

    public Schedule getSchedule() {
        return _monitorSchedule;
    }

    public Hashtable getScheduleMap() {
        return _measurementMeta;
    }

    public MetricValue[] getValues(RawMeasurement[] measurements)
    {
        MetricValue[] vals = 
            new MetricValue[measurements.length];

        try {
            if (_mpm == null) {
                ProductManagerLocal ppm =
                    ProductManagerUtil.getLocalHome().create();

                _mpm = (MeasurementPluginManager)
                    ppm.getPluginManager(ProductPlugin.TYPE_MEASUREMENT);
            }

            // Get value straight for now
            for (int i = 0; i < measurements.length; i++) {
                RawMeasurement rmv = measurements[i];

                try {
                    vals[i] = _mpm.getValue(rmv.getDsn());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // ppm.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return vals;
    }

    /** Get the live value
     * @param agent Agent (ignored) to use to get the values
     * @param dsns the DSNs that identify the values to fetch
     * @return an array of values for each requested DSN
     */
    public MetricValue[] getLiveValues(AgentValue agent, String[] dsns)
        throws MonitorAgentException, LiveMeasurementException 
    {
        try {
            MetricValue[] res;

            if (_mpm == null) {
                ProductManagerLocal ppm =
                    ProductManagerUtil.getLocalHome().create();

                _mpm = (MeasurementPluginManager)
                    ppm.getPluginManager(ProductPlugin.TYPE_MEASUREMENT);
            }

            res = new MetricValue[dsns.length];
            for(int i=0; i<dsns.length; i++){
                res[i] = _mpm.getValue(dsns[i]);
            }
            return res;
        } catch (PluginNotFoundException e) {
            throw new MonitorAgentException(e);
        } catch (PluginException e) {
            throw new MonitorAgentException(e);
        } catch (Exception e) {
            throw new LiveMeasurementException(e);
        }
    }    
}
