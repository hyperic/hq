/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
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
package org.hyperic.hq.plugin.vsphere;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

public class VCenterMeasurementPlugin extends MeasurementPlugin {
    
    @Override
    public MetricValue getValue(Metric metric)
    throws PluginException, MetricNotFoundException, MetricUnreachableException {
        VSphereUtil vim = null;
        try {
            final long start = System.currentTimeMillis();
            vim = VSphereUtil.getInstance(metric.getObjectProperties());
            if (!vim.isSessionValid()) {
                throw new MetricUnreachableException("Cannot validate connection properties");
            }
            if (metric.getAttributeName().equals("ConnectionValidationTime")) {
                return new MetricValue(System.currentTimeMillis()-start);
            }
        } catch (PluginException e) {
            if (metric.isAvail()) {
                return new MetricValue(Metric.AVAIL_DOWN);
            }
            throw new MetricUnreachableException(e.getMessage(),e);
        } finally {
            if (vim!= null) VSphereUtil.dispose(vim);
        }
        return new MetricValue(Metric.AVAIL_UP);
    }

}
