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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.hq.dao.RawMeasurementDAO;
import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.MeasurementTemplate;
import org.hyperic.hq.measurement.RawMeasurement;
import org.hyperic.hq.measurement.ext.MonitorFactory;
import org.hyperic.hq.measurement.ext.MonitorInterface;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.monitor.MonitorCreateException;
import org.hyperic.hq.measurement.shared.RawMeasurementValue;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** The RawMeasurementManagerEJB class is a stateless session bean that can be
 *  used to interact with RawMeasurement EJB's
 *
 * @ejb:bean name="RawMeasurementManager"
 *      jndi-name="ejb/measurement/RawMeasurementManager"
 *      local-jndi-name="LocalRawMeasurementManager"
 *      view-type="local"
 *      type="Stateless"
 *
 * @ejb:transaction type="REQUIRED"
 *
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public class RawMeasurementManagerEJBImpl extends SessionEJB
    implements SessionBean {
    private final Log log =
        LogFactory.getLog(RawMeasurementManagerEJBImpl.class);

    /**
     * Translate a template string into a DSN
     */
    private String translate(String tmpl, ConfigResponse config){
        try {
            return getMPM().translate(tmpl, config);
        } catch (org.hyperic.hq.product.PluginNotFoundException e) {
            return tmpl;
        }
    }

    /**
     * Create a Measurement object based on a template
     *
     * @return a RawMeasurement 
     * @ejb:interface-method
     */
    public RawMeasurementValue createMeasurement(Integer templateId,
                                                 Integer instanceId,
                                                 ConfigResponse config)
        throws MeasurementCreateException {
     
        try {
            MeasurementTemplate mt = 
                getMeasurementTemplateDAO().findById(templateId);
            String tmpl = mt.getTemplate();
            String dsn = translate(tmpl, config);

            RawMeasurement rm = getRawMeasurementDAO().create(instanceId, mt,
                                                              dsn);
            return rm.getRawMeasurementValue();
        } catch (MetricInvalidException e) {
            throw new MeasurementCreateException("Invalid DSN generated", e);
        }
    }

    /**
     * Update a Measurement object based on new properties
     *
     * @return a RawMeasurement 
     * @ejb:interface-method
     */
    public void updateMeasurements(AppdefEntityID id,
                                   ConfigResponse config)
        throws MeasurementCreateException {
        String tmpl = null;
        try {
            Collection mcol = 
                getRawMeasurementDAO().findByInstance(id.getType(),
                                                      id.getID());

            for (Iterator i = mcol.iterator(); i.hasNext();) {
                RawMeasurement rm = (RawMeasurement) i.next();

                // Translate the DSN
                tmpl = rm.getTemplate().getTemplate();
                rm.setDsn(translate(tmpl, config));
            }
        } catch (MetricInvalidException e) {
            throw new MeasurementCreateException("Invalid DSN generated", e);
        }
    }

    /**
     * Look up a raw measurement EJB
     *
     * @return a RawMeasurement value
     * @ejb:interface-method
     */
    public RawMeasurementValue getMeasurement(Integer mid) {
        RawMeasurement rm = getRawMeasurementDAO().findById(mid);
        return rm.getRawMeasurementValue();
    }

    /**
     * Check a configuration to see if it returns DSNs which the agent
     * can use to successfully monitor an entity.  This routine will
     * attempt to get live DSN values from the entity.  
     *
     * @param entity Entity to check the configuration for
     * @param config Configuration to check
     *
     * @ejb:interface-method
     */
    public void checkConfiguration(AuthzSubjectValue subject,
                                   AppdefEntityID entity, 
                                   ConfigResponse config)
        throws PermissionException, InvalidConfigException,
               AppdefEntityNotFoundException
    {
        ConfigCheckCache cache = ConfigCheckCache.instance();
        ConfigCheckCacheEntry entry
            = cache.getMetricsToCheck(subject, entity,
                                      getMeasurementTemplateDAO());

        // there are no metric templates, just return
        if (entry.isEmpty()) {
            log.debug("No metrics to checkConfiguration for " + entity);
            return;
        }
        else {
            log.debug("Using " + entry.dsns.length +
                      " metrics to checkConfiguration for " + entity);
        }
        // there was an error looking up the templates
        if (entry.exception != null) throw entry.exception;

        String[] dsns = new String[entry.dsns.length];
        for (int i=0; i<dsns.length; i++) {
            dsns[i] = translate(entry.dsns[i], config);
        }

        MetricValue[] mVals;
        try {
            mVals = this.getLiveMeasurementValues(entity, dsns);
        } catch(LiveMeasurementException exc){
            throw new InvalidConfigException("Invalid configuration: " +
                                             exc.getMessage(), exc);
        }
    }

    /**
     * Get live measurement values for a series of DSNs
     *
     * NOTE:  Since this routine allows callers to pass in arbitrary
     *        DSNs, the caller must do all the appropriate translation,
     *        etc.
     *
     * @param subject Subject who is performing the action
     * @param entity  Entity to get the measurement values from
     * @param dsns    Translated DSNs to fetch from the entity
     *
     * @return A list of MetricValue objects for each DSN passed
     */
    private MetricValue[] getLiveMeasurementValues(AppdefEntityID entity,
                                                   String[] dsns)
        throws LiveMeasurementException, PermissionException {
        try {
            MonitorInterface monitor;
            AgentValue aconn;
    
            aconn   = this.getAgentConnection(entity);
            monitor = 
                MonitorFactory.newInstance(aconn.getAgentType().getName());
    
            return monitor.getLiveValues(aconn, dsns);
        } catch(MonitorCreateException e){
            throw new LiveMeasurementException(e.getMessage(), e);
        } catch(MonitorAgentException e){
            throw new LiveMeasurementException(e.getMessage(), e);
        }
    }

    /**
     * Get the live measurement value
     * @param mID Raw measurement to get the value of
     * @ejb:interface-method
     */
    public MetricValue[] getLiveMeasurementValues(Integer[] mids)
        throws LiveMeasurementException, PermissionException  {
        AppdefEntityID entity = null;
        String[] dsns = new String[mids.length];

        // Assume at this point, that all mids are for the same resource
        for (int i = 0; i < mids.length; i++) {
            // First, find the raw measurement
            RawMeasurementValue rm = this.getMeasurement(mids[i]);
            
            if (entity == null) {        
                int entityType =
                    rm.getTemplate().getMonitorableType().getAppdefType();
                entity = new AppdefEntityID(entityType,
                                            rm.getInstanceId().intValue());
            }
            
            dsns[i] = rm.getDsn();
        }
                                                
        return this.getLiveMeasurementValues(entity, dsns);
    }

    /**
     * Look up a raw measurement EJB
     *
     * @return a RawMeasurement value
     * @ejb:interface-method
     */
    public RawMeasurementValue findMeasurement(String dsn, Integer id) {
        RawMeasurement rm = getRawMeasurementDAO().findByDsnForInstance(dsn, id);
        return rm.getRawMeasurementValue();
    }

    /**
     * Look up a raw measurement EJB
     *
     * @return a RawMeasurement value
     * @ejb:interface-method
     */
    public RawMeasurementValue findMeasurement(Integer tid, 
                                               Integer instanceId) {
        RawMeasurement rm = 
            getRawMeasurementDAO().findByTemplateForInstance(tid, instanceId);
        return rm.getRawMeasurementValue();
    }

    /**
     * Look up a list of raw measurement EJB
     *
     * @return a list of RawMeasurement value
     * @ejb:interface-method
     */
    public List findMeasurements(AppdefEntityID id) {
        ArrayList mlist = new ArrayList();
        
        Collection mcol = getRawMeasurementDAO().findByInstance(id.getType(),
                                                                id.getID());
        for (Iterator i = mcol.iterator(); i.hasNext();) {
            RawMeasurement rm = (RawMeasurement) i.next();
            mlist.add(rm.getRawMeasurementValue());
        }

        return mlist;
    }

    /**
     * Remove all measurements for an instance
     *
     * @ejb:interface-method
     */
    public void removeMeasurements(AppdefEntityID[] ids) {
        getRawMeasurementDAO().deleteByInstances(ids);
    }

    /**
     * @see javax.ejb.SessionBean#ejbCreate()
     * @ejb:create-method
     */
    public void ejbCreate() {}

    /**
     * @see javax.ejb.SessionBean#ejbPostCreate()
     */
    public void ejbPostCreate() {}

    /**
     * @see javax.ejb.SessionBean#ejbActivate()
     */
    public void ejbActivate() {}

    /**
     * @see javax.ejb.SessionBean#ejbPassivate()
     */
    public void ejbPassivate() {}

    /**
     * @see javax.ejb.SessionBean#ejbRemove()
     */
    public void ejbRemove() {
        this.ctx = null;
    }

    /**
     * @see javax.ejb.SessionBean#setSessionContext(SessionContext)
     */
    public void setSessionContext(SessionContext ctx)
        throws EJBException, RemoteException {
        this.ctx = ctx;
    }
}
