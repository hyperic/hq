/**
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;

/**
 * Collector that reports availability of the vCenter Server by attempting to
 * make a remote connection, as opposed to the usual process query. This allows
 * an agent to monitor the vCenter Server from a remote location, as all other
 * VSphere plugin monitoring is done via the vijava remote interface
 * @author jhickey
 * 
 */
public class VCenterServerCollector
    extends Collector {

    private final Log log = LogFactory.getLog(VCenterServerCollector.class);

    @Override
    public void collect() {
        //We only collect the Availability metric
        VSphereConnection conn = null;
        try {
            conn = VSphereConnection.getPooledInstance(getProperties());
            //We can make a connection - the vCenter Server is available
            setAvailability(true);
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.debug("Unable to make a connection to vCenter Server: " + e.getMessage(), e);
            }
            setAvailability(false);
        } finally {
            if (conn != null) {
                conn.release();
            }
        }  
    }

}
