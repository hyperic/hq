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

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.measurement.galerts.MetricAuxLog;
import org.hyperic.hq.measurement.shared.MetricAuxLogManagerLocal;
import org.hyperic.hq.measurement.shared.MetricAuxLogManagerUtil;
import org.hyperic.hq.measurement.server.session.MetricAuxLogPojo;

/**
 * @ejb:bean name="MetricAuxLogManager"
 *      jndi-name="ejb/common/MetricAuxLogManager"
 *      local-jndi-name="LocalMetricAuxLogManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class MetricAuxLogManagerEJBImpl 
    implements SessionBean
{
    public static MetricAuxLogManagerLocal getOne() {
        try {
            return MetricAuxLogManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    private MetricAuxLogDAO getDAO() {
        return new MetricAuxLogDAO(DAOFactory.getDAOFactory()); 
    }
    
    /**
     * @ejb:interface-method
     */
    public MetricAuxLogPojo create(GalertAuxLog log, MetricAuxLog logInfo) {  
        MetricAuxLogPojo metricLog = 
            new MetricAuxLogPojo(log, logInfo, log.getAlert().getAlertDef());
        
        getDAO().save(metricLog);
        return metricLog;
    }
    
    /**
     * @ejb:interface-method
     */
    public void removeAll(GalertDef def) {
        getDAO().removeAll(def);
    }

    /**
     * @ejb:interface-method
     */
    public MetricAuxLogPojo find(GalertAuxLog log) { 
        return getDAO().find(log);
    }

    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
    public void setSessionContext(SessionContext c) {}
}
