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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.zimbra.five;

import java.util.Arrays;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;

/**
 *
 * @author laullon
 */
public class ZimbraMeasurement extends SigarMeasurementPlugin {

    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue res = null;
        try {
            res = super.getValue(metric);
            getLog().debug("--->" + res + " (" + metric + ")");
        } catch (Exception e) {
            getLog().debug(e);
        }
        return res;
    }

    public MeasurementInfo[] getMeasurements(TypeInfo info) {
        getLog().debug("[getMeasurements] (" + info + ")");
        MeasurementInfo[] res = super.getMeasurements(info);
        getLog().debug("[getMeasurements] (" + Arrays.asList(res) + ")");
        return res;
    }
}
