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
import java.util.Iterator;
import java.util.List;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.RawMeasurement;
import org.hyperic.hq.measurement.ext.MonitorFactory;
import org.hyperic.hq.measurement.ext.MonitorInterface;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.monitor.MonitorCreateException;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerUtil;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.common.SystemException;
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
public class RawMeasurementManagerEJBImpl 
    extends SessionEJB
    implements SessionBean 
{
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
     * @ejb:interface-method
     */
    public RawMeasurement createMeasurement(Integer templateId,
                                            Integer instanceId,
                                            ConfigResponse config)
        throws MeasurementCreateException {
     
        try {
            MeasurementTemplate mt = 
                getMeasurementTemplateDAO().findById(templateId);
            String tmpl = mt.getTemplate();
            String dsn = translate(tmpl, config);

            return getRawMeasurementDAO().create(instanceId, mt, dsn);
        } catch (MetricInvalidException e) {
            throw new MeasurementCreateException("Invalid DSN generated", e);
        }
    }

    /**
     * Update a Measurement object based on new properties
     *
     * @ejb:interface-method
     */
    public void updateMeasurements(AppdefEntityID id,
                                   ConfigResponse config)
        throws MeasurementCreateException 
    {
        try {
            Collection mcol = 
                getRawMeasurementDAO().findByInstance(id.getType(),
                                                      id.getID());

            for (Iterator i = mcol.iterator(); i.hasNext();) {
                RawMeasurement rm = (RawMeasurement) i.next();
                String tmpl = rm.getTemplate().getTemplate();

                rm.setDsn(translate(tmpl, config));
            }
        } catch (MetricInvalidException e) {
            throw new MeasurementCreateException("Invalid DSN generated", e);
        }
    }

    private static final int SAMPLE_SIZE = 4;
    private String[] getTemplatesToCheck(AuthzSubject s,
                                         AppdefEntityID id) 
        throws AppdefEntityNotFoundException, PermissionException
    {
        MeasurementTemplateDAO dao = getMeasurementTemplateDAO();
        String mType = (new AppdefEntityValue(id, s)).getMonitorableType();
        List templates = dao.findDefaultsByMonitorableType(mType, id.getType());
        List dsnList = new ArrayList(SAMPLE_SIZE);
        int idx = 0;
        int availIdx = -1;
        MeasurementTemplate template;
        for (int i=0; i<templates.size(); i++) {

            template = (MeasurementTemplate)templates.get(i);

            if (template.getCategory().getName().
                equals(MeasurementConstants.CAT_AVAILABILITY) &&
                template.isDesignate()) {
                availIdx = idx;
            }

            // Need to get the raw measurements
            Collection args = template.getMeasurementArgs();
            MeasurementArg arg = (MeasurementArg)args.iterator().next();
            template = arg.getTemplateArg();

            if (idx == availIdx
                || (availIdx == -1 && idx < (SAMPLE_SIZE-1))
                || (availIdx != -1 && idx < SAMPLE_SIZE))
            {
                dsnList.add(template.getTemplate());
                // Increment only after we have successfully added DSN
                idx++;
                if (idx >= SAMPLE_SIZE) break;
            }
        }

        return (String[]) dsnList.toArray(new String[dsnList.size()]);
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
    public void checkConfiguration(AuthzSubject subject,
                                   AppdefEntityID entity, 
                                   ConfigResponse config)
        throws PermissionException, InvalidConfigException,
               AppdefEntityNotFoundException
    {
        String[] templates = getTemplatesToCheck(subject, entity);

        // there are no metric templates, just return
        if (templates.length == 0) {
            log.debug("No metrics to checkConfiguration for " + entity);
            return;
        } else {
            log.debug("Using " + templates.length +
                      " metrics to checkConfiguration for " + entity);
        }

        String[] dsns = new String[templates.length];
        for (int i = 0; i < dsns.length; i++) {
            dsns[i] = translate(templates[i], config);
        }

        try {
            getLiveMeasurementValues(entity, dsns);
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
        throws LiveMeasurementException, PermissionException 
    {
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
        throws LiveMeasurementException, PermissionException  
    {
        AppdefEntityID entity = null;
        String[] dsns = new String[mids.length];

        // Assume at this point, that all mids are for the same resource
        RawMeasurementDAO dao = getRawMeasurementDAO();
        for (int i = 0; i < mids.length; i++) {
            // First, find the raw measurement
            RawMeasurement rm = dao.findById(mids[i]);
            
            if (entity == null) {        
                int entityType =
                    rm.getTemplate().getMonitorableType().getAppdefType();
                entity = new AppdefEntityID(entityType,
                                            rm.getInstanceId().intValue());
            }
            
            dsns[i] = rm.getDsn();
        }
                                                
        return getLiveMeasurementValues(entity, dsns);
    }

    /**
     * Look up a RawMeasurement
     * @ejb:interface-method
     */
    public RawMeasurement findMeasurement(Integer tid, Integer instanceId) {
        return getRawMeasurementDAO().findByTemplateForInstance(tid, instanceId);
    }

    /**
     * Remove all measurements no longer associated with a resource.
     *
     * @ejb:interface-method
     */
    public int removeOrphanedMeasurements() {
        MetricDeleteCallback cb = 
            MeasurementStartupListener.getMetricDeleteCallbackObj();
        RawMeasurementDAO dao = getRawMeasurementDAO();

        List mids = dao.findOrphanedMeasurements();
        for (Iterator i=mids.iterator(); i.hasNext(); ) {
            RawMeasurement m = dao.get((Integer)i.next());
            
            cb.beforeMetricDelete(m);
            dao.remove(m);
        }

        return mids.size();
    }

    public static RawMeasurementManagerLocal getOne() {
        try {
            return RawMeasurementManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() {}
    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}
}
