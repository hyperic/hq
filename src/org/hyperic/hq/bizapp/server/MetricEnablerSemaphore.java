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

package org.hyperic.hq.bizapp.server;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerUtil;
import org.hyperic.util.config.ConfigResponse;


/**
 *
 * Used by DefaultMetricsEnabler to ensure only one enabler at a time
 */
public class MetricEnablerSemaphore {
    private static MetricEnablerSemaphore singleton = null;
    
    private DerivedMeasurementManagerLocal dmMan = null;
    
    private MetricEnablerSemaphore() {
        try {
            this.dmMan = DerivedMeasurementManagerUtil.getLocalHome().create();
        } catch(Exception exc){
            throw new SystemException(exc);
        }        
    }
    
    public static MetricEnablerSemaphore getInstance() {
        if (singleton == null) {
            singleton = new MetricEnablerSemaphore();
        }
        return singleton;
    }
    
    public void enableDefaultMetrics(AuthzSubjectValue subject,
                                     AppdefEntityID id, String mtype,
                                     ConfigResponse confResp)
        throws TemplateNotFoundException, MeasurementCreateException,
               PermissionException {
        synchronized (this.dmMan) {
            this.dmMan.createDefaultMeasurements(subject, id, mtype, confResp);
        }
    }
}
