/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.session;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefCompatException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceClusterValue;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.VirtualManagerLocal;
import org.hyperic.hq.appdef.shared.VirtualManagerUtil;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.bizapp.shared.MeasurementBossLocal;
import org.hyperic.hq.bizapp.shared.MeasurementBossUtil;
import org.hyperic.hq.bizapp.shared.uibeans.AutogroupDisplaySummary;
import org.hyperic.hq.bizapp.shared.uibeans.ClusterDisplaySummary;
import org.hyperic.hq.bizapp.shared.uibeans.GroupMetricDisplaySummary;
import org.hyperic.hq.bizapp.shared.uibeans.MeasurementMetadataSummary;
import org.hyperic.hq.bizapp.shared.uibeans.MeasurementSummary;
import org.hyperic.hq.bizapp.shared.uibeans.MetricConfigSummary;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayValue;
import org.hyperic.hq.bizapp.shared.uibeans.ProblemMetricSummary;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceDisplaySummary;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceMetricDisplaySummary;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTypeDisplaySummary;
import org.hyperic.hq.bizapp.shared.uibeans.SingletonDisplaySummary;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.server.session.Baseline;
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.collection.IntHashMap;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;

/** BizApp interface to the Measurement subsystem
 * @ejb:bean name="MeasurementBoss"
 *      jndi-name="ejb/bizapp/MeasurementBoss"
 *      local-jndi-name="LocalMeasurementBoss"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */
public class MeasurementBossEJBImpl extends MetricSessionEJB
    implements SessionBean 
{
    protected static Log _log = LogFactory.getLog(MeasurementBossEJBImpl.class);

    private List findDesignatedMetrics(AuthzSubject subject, AppdefEntityID id,
                                       Set cats) {
        final List metrics;
        
        if (cats.size() == 1) {
            String cat = (String) cats.iterator().next();
            metrics =
                getMetricManager().findDesignatedMeasurements(subject, id, cat);
        }
        else {
            metrics = getMetricManager().findDesignatedMeasurements(id);
            
            // Now iterate through and throw out the metrics we don't need
            for (Iterator it = metrics.iterator(); it.hasNext(); ) {
                Measurement dm = (Measurement) it.next();
                if (!cats.contains(dm.getTemplate().getCategory().getName()))
                    it.remove();
            }
        }

        return metrics;
    }

    /**
     * Get Autogroup member ids
     * @ejb:interface-method
     */
    public AppdefEntityID[] getAutoGroupMemberIDs(AuthzSubject subject,
                                                  AppdefEntityID[] aids,
                                                  AppdefEntityTypeID ctype)
        throws AppdefEntityNotFoundException, PermissionException {
        
        final List members = getAGMemberIds(subject, aids, ctype);
        return (AppdefEntityID[])
            members.toArray(new AppdefEntityID[members.size()]);
    }

    /**
     * Update the default interval for a list of template ids
     * @ejb:interface-method
     */
    public void updateMetricDefaultInterval(int sessionId, Integer[] tids,
                                            long interval)
        throws SessionException
    {
        final AuthzSubject subject = manager.getSubject(sessionId);
        getTemplateManager().updateTemplateDefaultInterval(subject, tids, 
                                                           interval);
    }

    /**
     * Update the templates to be indicators or not
     * @ejb:interface-method
     */
    public void updateIndicatorMetrics(int sessionId, AppdefEntityTypeID aetid,
                                       Integer[] tids)
        throws TemplateNotFoundException, SessionTimeoutException,
               SessionNotFoundException {
        String typeName = aetid.getAppdefResourceType().getName();
        getTemplateManager().setDesignatedTemplates(typeName, tids);
    }

    /**
     * @ejb:interface-method
     * @return a PageList of MeasurementTemplateValue objects
     */
    public List findMeasurementTemplates(int sessionId,
                                         AppdefEntityTypeID typeId,
                                         String category,
                                         PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException {
        manager.authenticate(sessionId);
        String typeName = typeId.getAppdefResourceType().getName();
        return getTemplateManager().findTemplates(typeName, category,
                                                  new Integer[] {}, pc);
    }

    /**
     * @ejb:interface-method
     * @return a PageList of MeasurementTemplateValue objects based on entity
     */
    public List findMeasurementTemplates(int sessionId,
                                         AppdefEntityID aeid)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        AppdefEntityValue aev = new AppdefEntityValue(aeid, subject);
        
        String typeName = aev.getMonitorableType();
        return getTemplateManager().findTemplates(typeName, null,
                                                  new Integer[] {},
                                                  PageControl.PAGE_ALL);
    }

    /**
     * Retrieve list of measurement templates applicable to a monitorable type
     * 
     * @param mtype the monitorableType
     * @return a List of MeasurementTemplateValue objects
     * @ejb:interface-method
     */
    public List findMeasurementTemplates(int sessionId, String mtype,
                                         PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException {
        manager.authenticate(sessionId);
        return getTemplateManager().findTemplates(mtype, null, null, pc);
    }

    /** Retrieve list of measurement templates given specific IDs
     * @ejb:interface-method
     */
    public List findMeasurementTemplates(String user, Integer[] ids,
                                         PageControl pc)
        throws LoginException, ApplicationException, ConfigPropertyException {
        int sessionId = getAuthManager().getUnauthSessionId(user);
        return findMeasurementTemplates(sessionId, ids, pc);
    }

    /** Retrieve list of measurement templates given specific IDs
     * @return a List of MeasurementTemplateValue objects
     * @ejb:interface-method
     */
    public List findMeasurementTemplates(int sessionId, Integer[] ids,
                                             PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException,
               TemplateNotFoundException {
        manager.authenticate(sessionId);
        return getTemplateManager().getTemplates(ids, pc);
    }

    /** Retrieve a measurement template given specific ID
     * @ejb:interface-method
     */
    public MeasurementTemplate getMeasurementTemplate(int sessionId, Integer id)
        throws SessionNotFoundException, SessionTimeoutException,
               TemplateNotFoundException {
        manager.authenticate(sessionId);
        return getTemplateManager().getTemplate(id);
    }

    /**
     * Get the the availability metric template for the given autogroup
     * @return The availabililty metric template.
     * @ejb:interface-method
     */
    public MeasurementTemplate getAvailabilityMetricTemplate(
            int sessionId, AppdefEntityID aid, AppdefEntityTypeID ctype)
        throws SessionNotFoundException, SessionTimeoutException,
               MeasurementNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
    
        // Find the autogroup members
        List entIds = getAGMemberIds(subject, new AppdefEntityID[] { aid },
                                     ctype);
    
        for (Iterator it = entIds.iterator(); it.hasNext();) {
            AppdefEntityID aeId = (AppdefEntityID) it.next();
            try {
                return getAvailabilityMetricTemplate(sessionId, aeId);
            } catch (MeasurementNotFoundException e) {
                // continue;
            }
        }
    
        // Throw a MeasurementNotFoundException here
        throw new MeasurementNotFoundException(
            "Autogroup for : " + aid + " of type : " + ctype +
            " does not contain designated measurements");
    }

    /**
     * @param measCache Map<Integer, Measurement> may be null.
     * Integer represents the AppdefEntityID.getId()
     */
    private MeasurementTemplate getAvailabilityMetricTemplate(
        AuthzSubject subj, AppdefEntityID aeid, Map measCache)
        throws AppdefEntityNotFoundException,
               PermissionException,
               MeasurementNotFoundException {
        Measurement dm = null;
        if (aeid.isApplication()) {
            // Get the appointed front-end service
            AppdefEntityValue aeval = new AppdefEntityValue(aeid, subj);
            Application app = (Application) aeval.getResourcePOJO();
            
            Collection appSvcs = app.getAppServices();
            for (Iterator it = appSvcs.iterator(); it.hasNext(); ) {
                AppService appSvc = (AppService) it.next();
                // Let's try it
                if (appSvc.isIsGroup()) {
                    if (appSvc.getResourceGroup() == null)
                        continue;
                        
                    aeid = AppdefEntityID.newGroupID(
                        appSvc.getResourceGroup().getId());
                }
                else {
                    // Make sure this is a valid service
                    if (appSvc.getService() == null)
                        continue;
                    
                    // Get the metrics for the service                        
                    aeid = appSvc.getService().getEntityId();
                }
    
                dm = findAvailabilityMetric(subj, aeid, measCache);

                if (dm != null) {
                    break;
                }
            }
        }
        else if (aeid.isGroup())
        {
            List grpMembers = getResourceIds(subj, aeid, null);
            
            // Go through the group members and return the first measurement
            // that we find
            for (Iterator it = grpMembers.iterator(); it.hasNext(); ) {
                aeid = (AppdefEntityID) it.next();
                dm = findAvailabilityMetric(subj, aeid, measCache);
                if (dm != null) {
                    break;
                }
            }
        }
        else {
            dm = findAvailabilityMetric(subj, aeid, measCache);
        }
        
        if (dm != null)
            return dm.getTemplate();
        else
            throw new MeasurementNotFoundException(
                "Availability metric not found for " + aeid);
    }

    private Measurement findAvailabilityMetric(AuthzSubject subj,
                                               AppdefEntityID aeid,
                                               Map measCache) {
        Measurement dm = null;
        if (measCache != null) {
            Object obj = measCache.get(aeid.getId());

            // HHQ-2884: Short-term fix for groups
            // XXX Need to refactor. Not originally designed for groups
            if (obj instanceof List) {
                List list = (List) obj;                
                if (list.size() == 1) {
                    dm = (Measurement) list.get(0);
                }
            } else if (obj instanceof Measurement) {
                dm = (Measurement) obj;
            }
        }

        if (dm == null) {
            dm = findAvailabilityMetric(subj, aeid);
        }
        return dm;
    }
    
    /**
     * Get the the availability metric template for the given resource
     * @return template of availabililty metric
     * @ejb:interface-method
     */
    public MeasurementTemplate getAvailabilityMetricTemplate(
        int sessionId, AppdefEntityID aeid)
        throws MeasurementNotFoundException, SessionNotFoundException,
               SessionTimeoutException, AppdefEntityNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        return getAvailabilityMetricTemplate(subject, aeid, null);
    }

    /**
     * Get the the designated measurement template for the given resource
     * and corresponding category.
     * @return Array of Measurement IDs
     * @ejb:interface-method
     */
    public List getDesignatedTemplates(int sessionId, AppdefEntityID id,
                                       Set cats) 
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, PermissionException {
        List metrics;
        try {
            metrics = getDesignatedMetrics(sessionId, id, cats);
        } catch (MeasurementNotFoundException e) {
            return new ArrayList(0);
        }
        
        ArrayList tmpls = new ArrayList(metrics.size());
        for (Iterator it = metrics.iterator(); it.hasNext(); ) {
            Measurement dm = (Measurement) it.next();
            tmpls.add(dm.getTemplate());
        }
            
        return tmpls;
    }

    /**
     * Get the the designated measurement template for the autogroup given
     * a type and corresponding category.
     * @param ctype the AppdefEntityTypeID of the AG members
     * @return Array of Measuremnt ids
     * @ejb:interface-method
     */
    public List getAGDesignatedTemplates(int sessionId, AppdefEntityID[] aids, 
                                         AppdefEntityTypeID ctype, Set cats) 
        throws SessionNotFoundException, SessionTimeoutException,
               MeasurementNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        
        // Find the autogroup members
        List entIds = getAGMemberIds(subject, aids, ctype);
        
        for (Iterator it = entIds.iterator(); it.hasNext(); ) {
            AppdefEntityID aeId = (AppdefEntityID) it.next();
            List templs = getDesignatedTemplates(sessionId, aeId, cats);
            if (templs.size() > 0) {
                return templs;
            }
        }
    
        // Throw a MeasurementNotFoundException here
        throw new MeasurementNotFoundException(
            "Autogroup for : " + aids + " of type : " + ctype +
            " does not contain designated measurements");
    }

    /**
     * Create list of measurements for a resource
     * @param id the resource ID
     */
    private void createMeasurements(int sessionId, AppdefEntityID id,
                                    Integer[] tids, long interval)
        throws SessionTimeoutException, SessionNotFoundException,
               ConfigFetchException, EncodingException, PermissionException,
               TemplateNotFoundException, AppdefEntityNotFoundException,
               GroupNotCompatibleException, MeasurementCreateException,
               MeasurementNotFoundException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        if (id.isGroup()) {
            // Recursively do this for each of the group members
            List grpMembers = getResourceIds(subject, id, null);
        
            AppdefEntityID[] aeids = (AppdefEntityID[])
                grpMembers.toArray(new AppdefEntityID[grpMembers.size()]);
            getMetricManager().enableMeasurements(subject, aeids, tids,
                                                  interval);
        }
        else {
            ConfigResponse mergedCR = getConfigManager()
                .getMergedConfigResponse(subject, 
                                         ProductPlugin.TYPE_MEASUREMENT, 
                                         id, true);
            
            if (interval > 0) {
                long[] intervals = new long[tids.length];
                Arrays.fill(intervals, interval);
    
                getMetricManager().createMeasurements(subject, id, tids,
                                                      intervals, mergedCR);
            } else {
                getMetricManager().createMeasurements(subject, id, tids,
                                                      mergedCR);
            }
        }
    }

    /** Update the measurements - set the interval
     * @param id the resource ID
     * @param tids the array of template ID's
     * @param interval the new interval value
     * @ejb:interface-method
     */
    public void updateMeasurements(int sessionId, AppdefEntityID id,
                                   Integer[] tids, long interval)
        throws MeasurementNotFoundException,
               SessionTimeoutException, SessionNotFoundException,
               TemplateNotFoundException, AppdefEntityNotFoundException,
               GroupNotCompatibleException, MeasurementCreateException,
               ConfigFetchException, PermissionException, EncodingException {
        createMeasurements(sessionId, id, tids, interval);
    }
    
    /**
     * Update measurements for the members of an autogroup
     * @param parentid - the parent resource of the autogroup
     * @param ctype - the type of child resource
     * @param tids - template ids to update
     * @param interval - the interval to set
     * @ejb:interface-method
     */
    public void updateAGMeasurements(int sessionId, 
                                     AppdefEntityID parentid, 
                                     AppdefEntityTypeID ctype,
                                     Integer[] tids,
                                     long interval)
        throws MeasurementNotFoundException, SessionTimeoutException,
               SessionNotFoundException, TemplateNotFoundException,
               AppdefEntityNotFoundException, GroupNotCompatibleException,
               MeasurementCreateException, ConfigFetchException,
               PermissionException, EncodingException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        List kids = getAGMemberIds(subject, parentid, ctype);
        for(int i = 0; i < kids.size(); i++) {
            // Do create, because we want to create or update
            AppdefEntityID kid = (AppdefEntityID)kids.get(i);
            createMeasurements(sessionId, kid, tids, interval);
        }
    }

    /**
     * Disable all measurements for an instance
     * @param id the resource's ID
     * @ejb:interface-method
     */
    public void disableMeasurements(int sessionId, AppdefEntityID id)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        getMetricManager().disableMeasurements(subject, id);
    }

    /**
     * Disable all measurements for a resource
     * @param id the resource's ID
     * @param tids the array of measurement ID's
     * @ejb:interface-method
     */
    public void disableMeasurements(int sessionId, AppdefEntityID id,
                                    Integer[] tids)
        throws SessionException, RemoveException, AppdefEntityNotFoundException,
               GroupNotCompatibleException, PermissionException
    {
        final AuthzSubject subject = manager.getSubject(sessionId);

        MeasurementManagerLocal dmm = getMetricManager();
        if (id == null) {
            getTemplateManager().setTemplateEnabledByDefault(subject, tids, 
                                                             false);
        } else if (id.isGroup()) {
            // Recursively do this for each of the group members
            List grpMembers = getResourceIds(subject, id, null);
    
            for (Iterator it = grpMembers.iterator(); it.hasNext();) {
                dmm.disableMeasurements(subject, (AppdefEntityID) it.next(), 
                                        tids);
            }
        } else {
            dmm.disableMeasurements(subject, id, tids); 
        }
    }

    /**
     * Disable all measurements for a resource
     * @param tids the array of measurement ID's
     * @ejb:interface-method
     */
    public void disableAGMeasurements(int sessionId, AppdefEntityID parentId,
                                      AppdefEntityTypeID childType,
                                      Integer[] tids)
        throws SessionTimeoutException, SessionNotFoundException,
               RemoveException, AppdefEntityNotFoundException,
               GroupNotCompatibleException, PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
    
        List grpMembers = getAGMemberIds(subject, parentId, childType);
    
        for (Iterator it = grpMembers.iterator(); it.hasNext();) {
            getMetricManager().disableMeasurements(
                subject, (AppdefEntityID) it.next(), tids);
        }
    }

    /**
     * Get the the designated measurement for the given resource
     * and corresponding category.
     * @return Array of Measurement IDs
     */
    private List getDesignatedMetrics(int sessionId, AppdefEntityID id, Set cats) 
        throws SessionNotFoundException, SessionTimeoutException,
               MeasurementNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        if (cats.size() == 0) {
            throw new MeasurementNotFoundException("No categories specified");
        }
        
        AuthzSubject subj = manager.getSubject(sessionId);
        
        List metrics = null;
        if (id.isApplication()) {
            // Get the appointed front-end service
            AppdefEntityValue aeval = new AppdefEntityValue(id, subj);
            Application app = (Application) aeval.getResourcePOJO();
            
            Collection appSvcs = app.getAppServices();
            for (Iterator it = appSvcs.iterator(); it.hasNext(); ) {
                AppService appSvc = (AppService) it.next();
                // Let's try it
                if (appSvc.isIsGroup()) {
                    if (appSvc.getResourceGroup() == null)
                        continue;
                        
                    id = AppdefEntityID.newGroupID(appSvc.getResourceGroup()
                                                   .getId());
                }
                else {
                    // Make sure this is a valid service
                    if (appSvc.getService() == null)
                        continue;
                    
                    // Get the metrics for the service                        
                    id = appSvc.getService().getEntityId();
                }

                // Require an entry point
                if (appSvc.isEntryPoint()) {
                    // Recursively call with the entry point
                    try {
                        metrics = getDesignatedMetrics(sessionId, id, cats);
                        break;
                    } catch (MeasurementNotFoundException ignore) {
                        // No measurement to be used
                    } catch (PermissionException ignore) {
                        // Can't use this service, because user can't see it
                    }
                }
                else if (metrics == null &&
                         cats.contains(MeasurementConstants.CAT_AVAILABILITY)) {
                    Measurement dm = findAvailabilityMetric(subj, id);
                    
                    if (dm != null) {
                        metrics = Collections.singletonList(dm);
                    }
                }
            }
        }
        else if (id.isGroup()) {
            List grpMembers = getResourceIds(subj, id, null);
            
            // Go through the group members and return the first measurement
            // that we find
            for (Iterator it = grpMembers.iterator(); it.hasNext(); ) {
                metrics = findDesignatedMetrics(
                        subj, (AppdefEntityID) it.next(), cats);
    
                if (metrics != null && metrics.size() > 0)
                    break;
            }
        }
        else {
            metrics = findDesignatedMetrics(subj, id, cats);
        }
        
        // Make sure we have valid metrics
        if (metrics == null || metrics.size() == 0)
            throw new MeasurementNotFoundException(
                "Designated metric not found for " + id);
    
        // Now iterate through and throw out the metrics we don't need
        for (Iterator it = metrics.iterator(); it.hasNext(); ) {
            Measurement dm = (Measurement) it.next();
            if (!cats.contains(dm.getTemplate().getCategory().getName()))
                it.remove();
        }
    
        return metrics;
    }

    /**
     * Find a measurement using measurement id
     * @param id measurement id
     * @ejb:interface-method
     */
    public Measurement getMeasurement(int sessionID, Integer id)
        throws SessionTimeoutException, SessionNotFoundException,
               MeasurementNotFoundException {
        return getMetricManager().getMeasurement(id);
    }

    /**
     * Get the last metric values for the given template IDs.
     *
     * @param tids The template IDs to get
     * @ejb:interface-method
     */
    public MetricValue[] getLastMetricValue(int sessionId,
                                            AppdefEntityID aeid,
                                            Integer[] tids)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);

        List measurements = new ArrayList(tids.length);
        long interval = 0;
        for (int i = 0; i < tids.length; i++) {
            try {
                Measurement m = getMetricManager()
                    .findMeasurement(subject, tids[i], aeid);
                measurements.add(m);
                interval = Math.max(interval, m.getInterval());
            } catch (MeasurementNotFoundException e) {
                measurements.add(null);
            }
        }

        return getLastMetricValue(sessionId,  measurements, interval);
    }
    
    /**
     * Get the last metric data for the array of measurement ids.
     *
     * @param measurements The List of Measurements to get metrics for
     * @param interval The allowable time in ms to go back looking for data.
     * @ejb:interface-method
     */
    public MetricValue[] getLastMetricValue(int sessionId,
                                            List measurements,
                                            long interval)
    {
        MetricValue[] ret = new MetricValue[measurements.size()];
        long after =  System.currentTimeMillis() - (3 * interval);
        Map data = getDataMan().getLastDataPoints(measurements, after);

        for (int i = 0; i < measurements.size(); i++) {
            Measurement m = (Measurement)measurements.get(i);
            if (m != null && data.containsKey(m.getId())) {
                ret[i] = (MetricValue)data.get(m.getId());
            }
        }

        return ret;
    }

    /**
     * Get the last indicator metric values
     * @ejb:interface-method
     */
    public Map getLastIndicatorValues(Integer sessionId, AppdefEntityID aeid) {
        final MeasurementManagerLocal metricManager = getMetricManager();
        List metrics = metricManager.findDesignatedMeasurements(aeid);
        long interval = 0;
        for (Iterator it = metrics.iterator(); it.hasNext();) {
            Measurement m = (Measurement) it.next();
            if (m.getTemplate().getAlias().equalsIgnoreCase("Availability"))
                it.remove();
            else
                interval = Math.max(interval, m.getInterval());
        }
        
        Integer[] mids = new Integer[metrics.size()];
        int i = 0;
        for (Iterator it = metrics.iterator(); it.hasNext();) {
            Measurement m = (Measurement) it.next();
            mids[i++] = m.getId();
        }
        
        final long after =  System.currentTimeMillis() - (3 * interval);
        Map data = new HashMap();
        getDataMan().getCachedDataPoints(mids, data, after);
        
        Map ret = new HashMap(data.size());
        for (Iterator it = metrics.iterator(); it.hasNext();) {
            Measurement m = (Measurement) it.next();
            if (data.containsKey(m.getId())) {
                ret.put(m.getTemplate().getId(), data.get(m.getId()));
            }
        }

        return ret;
    }

    /*
     * Private function to find the group measurements
     */
    private PageList findGroupMeasurements(int sessionId, AppdefEntityID gid,
                                           String cat, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, GroupNotCompatibleException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        List grpMembers = getResourceIds(subject, gid, null);
        return findGroupMeasurements(sessionId, grpMembers, cat, pc);
    }
    
    /*
     * Private function to find the group measurements
     */
    private PageList findGroupMeasurements(int sessionId, List grpMembers,
                                           String cat, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, GroupNotCompatibleException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
    
        String mtype = null;
        IntHashMap summaryMap = new IntHashMap();
        for (Iterator it = grpMembers.iterator(); it.hasNext(); ) {
            AppdefEntityID id = (AppdefEntityID) it.next();
            // Get the monitorable type from the first ID
            if (mtype == null) {
                AppdefEntityValue aeVal = new AppdefEntityValue(id, subject);
                mtype = aeVal.getMonitorableType();
            }
    
            // Get the list of measurements for this resource
            List metrics = getMetricManager()
                .findMeasurements(subject, id, cat, PageControl.PAGE_ALL);
                
            // Iterate through the measurements to get the interval
            for (Iterator it2 = metrics.iterator(); it2.hasNext(); ) {
                Measurement m = (Measurement) it2.next();
    
                MeasurementTemplate tmpl = m.getTemplate();
                GroupMetricDisplaySummary gmds = (GroupMetricDisplaySummary)
                    summaryMap.get(tmpl.getId().intValue());
                if (gmds == null) {
                    gmds = new GroupMetricDisplaySummary(
                        tmpl.getId().intValue(), tmpl.getName(),
                        tmpl.getCategory().getName());
                    gmds.setInterval(m.getInterval());
                    
                    // Now put it into the map
                    summaryMap.put(tmpl.getId().intValue(), gmds);
                }
    
                // Increment the member count
                gmds.incrementMember();
                
                // Check the interval
                if (gmds.getInterval() != m.getInterval()) {
                    // Set it to 0, because the intervals don't agree
                    gmds.setInterval(0);
                }
            }
        }
    
        // No mtype == no group members
        if (mtype == null) {
            return new PageList();
        }
        
        List tmpls = getTemplateManager().findTemplates(mtype, cat, null, pc);
            
        PageList result = new PageList();
        for (Iterator it = tmpls.iterator(); it.hasNext(); ) {
            MeasurementTemplate tmpl =
                (MeasurementTemplate) it.next();
            GroupMetricDisplaySummary gmds = (GroupMetricDisplaySummary)
                summaryMap.get(tmpl.getId().intValue());
    
            if (gmds == null) {
                gmds = new GroupMetricDisplaySummary(
                    tmpl.getId().intValue(), tmpl.getName(),
                    tmpl.getCategory().getName());
            }
            
            // Set the total number
            gmds.setTotalMembers(grpMembers.size());
    
            result.add(gmds);
        }
        
        // Total size is equal to the total size of the templates
        result.setTotalSize(tmpls.size());
        
        return result;
    }

    /** Retrieve a Measurement for a specific instance
     *
     * @ejb:interface-method
     */
    public Measurement findMeasurement(int sessionId, Integer tid,
                                       AppdefEntityID id)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException, MeasurementNotFoundException,
               AppdefEntityNotFoundException {
        if (id.isGroup())
            return (Measurement)findMeasurements(sessionId, tid,
                                                 new AppdefEntityID[] { id }).get(0);

        final AuthzSubject subject = manager.getSubject(sessionId);
        return getMetricManager().findMeasurement(subject, tid, id);
    }

    /** Retrieve List of measurements for a specific instance
     * @return List of Measurement objects
     * @ejb:interface-method
     */
    public List findMeasurements(int sessionId, AppdefEntityID id,
                                 PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, GroupNotCompatibleException,
               PermissionException {
        if (id.isGroup())
            return findGroupMeasurements(sessionId, id, null, pc);

        if (id instanceof AppdefEntityTypeID)
            return new ArrayList(0);
        
        final AuthzSubject subject = manager.getSubject(sessionId);
        return getMetricManager().findMeasurements(subject, id, null, pc);
    }

    /**
     * Retrieve list of measurements for a specific template and entities
     * 
     * @param tid
     *            the template ID
     * @param entIds
     *            the array of entity IDs
     * @return a List of Measurement objects
     * @ejb:interface-method
     */
    public List findMeasurements(int sessionId, Integer tid,
                                 AppdefEntityID[] entIds)
        throws SessionTimeoutException, SessionNotFoundException,
               MeasurementNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        
        List ids = new ArrayList();
        
        // Form an array of ID's
        int type = entIds[0].getType();
        for (int i = 0; i < entIds.length; i++) {
            if (entIds[i].getType() != type)
                throw new MeasurementNotFoundException("Entity type != " +type);
            
            if (entIds[i].isGroup()) {
                AppdefEntityID[] memberIds =
                    getGroupMemberIDs(subject, entIds[i].getId());
                ids.addAll(Arrays.asList(memberIds));
            }
            else {
                ids.add(entIds[i].getId());
            }
        }
        return getMetricManager().findMeasurements(subject, tid, entIds);
    }

    /**
     * Get the enabled measurements for an auto group
     * @param parentId - the parent resource appdefEntityID
     * @param childType - the type of child in the autogroup
     * @return a PageList of Measurement objects
     * @ejb:interface-method
     */
    public List findEnabledAGMeasurements(int sessionId,
                                          AppdefEntityID parentId,
                                          AppdefEntityTypeID childType,
                                          String cat,
                                          PageControl pc)
    throws SessionNotFoundException, SessionTimeoutException,
           AppdefEntityNotFoundException, GroupNotCompatibleException,
           PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        return findGroupMeasurements(sessionId,
                                     getAGMemberIds(subject,parentId,childType), 
                                     cat, pc);
    }
    
    /** Retrieve list of measurements for a specific instance and category
     * @return a PageList of Measurement objects
     * @ejb:interface-method
     */
    public PageList findEnabledMeasurements(int sessionId, AppdefEntityID id,
                                            String cat, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, GroupNotCompatibleException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        if (id.isGroup())
            return findGroupMeasurements(sessionId, id, cat, pc);

        AppdefEntityValue aeVal = new AppdefEntityValue(id, subject);
        String mtype = aeVal.getMonitorableType();

        IntHashMap dmvs = new IntHashMap();
        List enabledMetrics =
            getMetricManager().findEnabledMeasurements(subject, id, cat);
        
        for (Iterator it = enabledMetrics.iterator(); it.hasNext(); ) {
            Measurement dmv = (Measurement) it.next();
            dmvs.put(dmv.getTemplate().getId().intValue(), dmv);
        }

        // Create MetricConfigSummary beans
        List tmpls = getTemplateManager().findTemplates(mtype, cat, null, pc);
        ArrayList beans = new ArrayList(tmpls.size());
        for (Iterator it = tmpls.iterator(); it.hasNext(); ) {
            MeasurementTemplate mtv = (MeasurementTemplate) it.next();
            MetricConfigSummary mcs =
                new MetricConfigSummary(mtv.getId().intValue(), mtv.getName(),
                                        cat);
            
            Measurement dmv =
                (Measurement) dmvs.get(mtv.getId().intValue());
            if (dmv != null) {
                mcs.setInterval(dmv.getInterval());
            }
            
            beans.add(mcs);
        }
        
        return new PageList(beans, tmpls.size());
    }

    /**
     * Dumps data for a specific measurement
     * @return a PageList of MetricValue objects
     * @ejb:interface-method
     */
    public PageList findMeasurementData(int sessionId, Measurement m,
                                        long begin, long end, PageControl pc) {
        return getDataMan().getHistoricalData(m, begin, end, pc);
    }

    /**
     * Dumps data for a specific measurement template for an instance based on
     * an interval
     * @param tid the template ID
     * @param aid the AppdefEntityID
     * @param begin the beginning of the time range
     * @param end the end of the time range
     * @param interval the time interval at which the data should be calculated
     * @param returnNulls whether or not nulls should be inserted for no data
     * @return a PageList of MetricValue objects
     * @ejb:interface-method
     */
    public PageList findMeasurementData(int sessionId, Integer tid,
                                        AppdefEntityID aid, long begin,
                                        long end, long interval,
                                        boolean returnNulls, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException,
               PermissionException, MeasurementNotFoundException {

        final AuthzSubject subject = manager.getSubject(sessionId);

        MeasurementTemplate tmpl = getTemplateManager().getTemplate(tid);
            
        List measurements = getMeasurementsForResource(subject, aid, tmpl);
        if (measurements == null || measurements.size() == 0) {
            throw new MeasurementNotFoundException(
                "There is no measurement for " + aid + " with template " + tid);
        }

        return getDataMan().getHistoricalData(measurements, begin, end,
                                              interval, tmpl.getCollectionType(),
                                              returnNulls, pc);
    }

    /**
     * Dumps data for a specific measurement template for an auto-group based on
     * an interval.
     *
     * @param tid the measurement template id
     * @param aid the entity id
     * @param ctype the auto-group child type
     * @param begin start of interval
     * @param end end of interval
     * @param interval the interval
     * @param returnNulls whether or not to return nulls
     * @return a PageList of MetricValue objects
     * @ejb:interface-method
     */
    public PageList findMeasurementData(int sessionId, Integer tid,
                                        AppdefEntityID aid,
                                        AppdefEntityTypeID ctype,
                                        long begin, long end, long interval,
                                        boolean returnNulls, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException,
               PermissionException, MeasurementNotFoundException {
        final AuthzSubject subject = manager.getSubject(sessionId);

        // Find the autogroup members
        List entIds = getAGMemberIds(subject, aid, ctype);
        
        return findMeasurementData(sessionId, tid, entIds, begin, end, interval,
                                   returnNulls, pc);
    }

    /**
     * Dumps data for a specific measurement template for an auto-group based on
     * an interval.
     *
     * @param tid the measurement template id
     * @param begin start of interval
     * @param end end of interval
     * @param interval the interval
     * @param returnNulls whether or not to return nulls
     *        associated with the platform
     * @return a PageList of MetricValue objects
     * @ejb:interface-method
     */
    public PageList findMeasurementData(int sessionId, Integer tid,
                                        List entIds,
                                        long begin, long end, long interval,
                                        boolean returnNulls, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
    
        MeasurementTemplate tmpl = getTemplateManager().getTemplate(tid);

        // Find the measurement IDs of the members in the autogroup for the
        // template
        AppdefEntityID[] ids =
            (AppdefEntityID[])entIds.toArray(new AppdefEntityID[entIds.size()]);
        
        List measurements =
            getMetricManager().findMeasurements(subject, tid, ids);
    
        return getDataMan().getHistoricalData(measurements, begin, end,
                                             interval, tmpl.getCollectionType(),
                                             returnNulls, pc);
    }

    /**
     * Dumps data for a specific measurement template for an instance based on
     * an interval
     * @param aid the AppdefEntityID
     * @param begin the beginning of the time range
     * @param end the end of the time range
     * @param interval the time interval at which the data should be calculated
     * @param returnNulls whether or not nulls should be inserted for no data
     * @return a PageList of MetricValue objects
     * @ejb:interface-method
     */
    public PageList findMeasurementData(String user, AppdefEntityID aid,
                                        MeasurementTemplate tmpl,
                                        long begin, long end, long interval,
                                        boolean returnNulls, PageControl pc)
        throws LoginException, ApplicationException, ConfigPropertyException {
        int sessionId = getAuthManager().getUnauthSessionId(user);
        return findMeasurementData(sessionId, aid, tmpl, begin, end, interval,
                                   returnNulls, pc); 
    }

    /** Dumps data for a specific measurement template for an instance based on
     * an interval
     * @param aid the AppdefEntityID
     * @param tmpl the complete MeasurementTemplate value object
     * @param begin the beginning of the time range
     * @param end the end of the time range
     * @param interval the time interval at which the data should be calculated
     * @param returnNulls whether or not nulls should be inserted for no data
     * @return a PageList of MetricValue objects
     * @ejb:interface-method
     */
    public PageList findMeasurementData(int sessionId, AppdefEntityID aid,
                                        MeasurementTemplate tmpl,
                                        long begin, long end, long interval,
                                        boolean returnNulls, PageControl pc)
    throws SessionNotFoundException, SessionTimeoutException,
           AppdefEntityNotFoundException, PermissionException,
           MeasurementNotFoundException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        final boolean debug = _log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        List measurements;
        if (aid.isApplication() && tmpl.isAvailability()) {
            // Special case for application availability
            _log.debug("BEGIN findMeasurementData()");
            AppdefEntityValue aeval = new AppdefEntityValue(aid, subject);
            // Get the flattened list of services
            if (debug) watch.markTimeBegin("getFlattenedServiceIds");
            AppdefEntityID[] serviceIds = aeval.getFlattenedServiceIds();
            if (debug) watch.markTimeEnd("getFlattenedServiceIds");
            if (debug) watch.markTimeBegin("findDesignatedMeasurements");
            Map midMap = getMetricManager()
                .findDesignatedMeasurements(subject, serviceIds,
                                            MeasurementConstants.CAT_AVAILABILITY);
            if (debug) watch.markTimeEnd("findDesignatedMeasurements");
            measurements = new ArrayList(midMap.values());
        } else {
            measurements = getMeasurementsForResource(subject, aid, tmpl);
            if (measurements == null || measurements.size() == 0) {
                throw new MeasurementNotFoundException(
                    "There is no measurement for " + aid + " with template " + 
                    tmpl.getId());
            }
        }
        if (debug) watch.markTimeBegin("getHistoricalData");
	    PageList rtn = getDataMan().getHistoricalData(
	        measurements, begin, end, interval, tmpl.getCollectionType(),
	        returnNulls, pc);
        if (debug) watch.markTimeEnd("getHistoricalData");
        if (debug) _log.debug(watch);
        return rtn;
    }

    /**
     * Dumps data for a specific measurement template for an auto-group based on
     * an interval.
     * @param ctype the auto-group child type
     * @param begin start of interval
     * @param end end of interval
     * @param interval the interval
     * @param returnNulls whether or not to return nulls
     * @return a PageList of MetricValue objects
     * @throws ConfigPropertyException
     * @throws ApplicationException
     * @throws LoginException
     * @ejb:interface-method
     */
    public PageList findAGMeasurementData(String user, AppdefEntityID[] aids,
                                          MeasurementTemplate tmpl,
                                          AppdefEntityTypeID ctype,
                                          long begin, long end, long interval,
                                          boolean returnNulls, PageControl pc)
        throws LoginException, ApplicationException, ConfigPropertyException {
        int sessionId = getAuthManager().getUnauthSessionId(user);
        return findAGMeasurementData(sessionId, aids, tmpl, ctype, begin, end,
                                     interval, returnNulls, pc);
    }

    /**
     * Dumps data for a specific measurement template for an auto-group based on
     * an interval.
     * @param ctype the auto-group child type
     * @param begin start of interval
     * @param end end of interval
     * @param interval the interval
     * @param returnNulls whether or not to return nulls
     * @return a PageList of MetricValue objects
     * @ejb:interface-method
     */
    public PageList findAGMeasurementData(int sessionId, AppdefEntityID[] aids,
                                          MeasurementTemplate tmpl,
                                          AppdefEntityTypeID ctype,
                                          long begin, long end, long interval,
                                          boolean returnNulls,
                                          PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException,
               PermissionException, MeasurementNotFoundException {
        final AuthzSubject subject = manager.getSubject(sessionId);
    
        // Find the autogroup members
        List entIds = getAGMemberIds(subject, aids, ctype);
    
        // Find the measurement IDs of the members in the autogroup for the
        // template
        AppdefEntityID[] ids =
            (AppdefEntityID[])entIds.toArray(new AppdefEntityID[entIds.size()]);

        List measurements =
            getMetricManager().findMeasurements(subject, tmpl.getId(), ids);

        return getDataMan().getHistoricalData(measurements, begin, end,
                                              interval, tmpl.getCollectionType(),
                                              returnNulls, pc);
    }

    /**
     * Returns metadata for particular measurement
     * @ejb:interface-method
     */
    public List findMetricMetadata(int sessionId, AppdefEntityID aid,
                                   AppdefEntityTypeID ctype, Integer tid)
        throws SessionNotFoundException, SessionTimeoutException,
               GroupNotCompatibleException,
               AppdefEntityNotFoundException, ApplicationNotFoundException,
               TemplateNotFoundException, PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        
        // Get the template
        getTemplateManager().getTemplate(tid);
        
        final List entities;

        if (aid.isGroup()) {
            List memberIds =
                GroupUtil.getCompatGroupMembers(subject, aid, null);
            entities = new ArrayList();
            for (Iterator iter = memberIds.iterator(); iter.hasNext();) {
                AppdefEntityID anId = (AppdefEntityID) iter.next();
                AppdefEntityValue aev = new AppdefEntityValue(anId, subject);
                entities.add(aev.getResourceValue());                
            }
        } else if (ctype != null) {
            // if a child type was specified, then the template must be
            // intended for the autogroup of children of that type
            AppdefEntityValue entVal = new AppdefEntityValue(aid, subject);
            switch (ctype.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    entities =
                        entVal.getAssociatedPlatforms(PageControl.PAGE_ALL);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    // Get server IDs
                    entities = entVal.getAssociatedServers(ctype.getId(),
                        PageControl.PAGE_ALL);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    // Get service IDs
                    entities = entVal.getAssociatedServices(ctype.getId(),
                        PageControl.PAGE_ALL);
                    break;
                default:
                    throw new IllegalArgumentException(
                        "Unable to determine autogroup members for appdef type: " +
                        aid.getType());
            }
        } else {
            AppdefEntityValue entVal = new AppdefEntityValue(aid, subject);
            entities = Collections.singletonList(entVal.getResourceValue());
        }
        
        AppdefEntityID[] aeids = new AppdefEntityID[entities.size()];
        IntHashMap resourceMap = new IntHashMap();
        Iterator iter = entities.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            AppdefResourceValue resource = (AppdefResourceValue) iter.next();
            aeids[i] = resource.getEntityId();
            resourceMap.put(aeids[i].getID(), resource);
        }

        // get measurement summaries, enriched with metadata
        // tastes good and it's good for you 
        List mds = new ArrayList();
        List mms = getMetricManager().findMeasurements(subject, tid, aeids);
        for (iter = mms.iterator(); iter.hasNext();) {
            Measurement mm = (Measurement) iter.next();
            Integer instanceId = mm.getInstanceId();
            AppdefResourceValue resource =
                (AppdefResourceValue)resourceMap.get(instanceId.intValue());
            
            // Fetch the last data point
            MetricValue mv = getDataMan().getLastHistoricalData(mm);
            
            MeasurementMetadataSummary summary =
                new MeasurementMetadataSummary(mm, mv, resource);
            mds.add(summary);                        
        }
        return mds;
    }

    /**
     * Method findMetrics.
     * 
     * When the entId is a server, return all of the metrics that
     * are instances of the measurement templates for the server's
     * type.  In this case, the MetricDisplaySummary's attributes to
     * show the number collecting doesn't make sense; 
     * showNumberCollecting should false for each bean.
     * <p>
     * When the entId is a platform, return all of the metrics that
     * are instances of the measurement templates for the platform's
     * type.  In this case, the MetricDisplaySummary's attributes to
     * show the number collecting doesn't make sense; 
     * showNumberCollecting should false for each bean.
     * </p>
     * <p>
     * When the entId is compatible group of servers or platforms, 
     * return all of the metrics for the type.  Each MetricDisplaySummary
     * actually represents the metrics summarized for all of the group
     * members (cumulative/averaged as appropriate), 
     * showNumberCollecting should be true and the numberCollecting
     * as well as the total number of members assigned in each bean.
     * </p>
     * 
     * @return Map keyed on the category (String), values are List's of 
     * MetricDisplaySummary beans
     * @see org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary
     * @ejb:interface-method
     */
    public MetricDisplaySummary findMetric(int sessionId, AppdefEntityID aeid,
                                           AppdefEntityTypeID ctype,
                                           Integer tid, long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, AppdefEntityNotFoundException,
               AppdefCompatException, MeasurementNotFoundException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        
        List resources = getResourceIds(subject, aeid, ctype);
    
        return findMetric(sessionId, resources, tid, begin, end);
    }

    /**
     * Method findMetrics.
     * 
     * When the entId is a server, return all of the metrics that
     * are instances of the measurement templates for the server's
     * type.  In this case, the MetricDisplaySummary's attributes to
     * show the number collecting doesn't make sense; 
     * showNumberCollecting should false for each bean.
     * <p>
     * When the entId is a platform, return all of the metrics that
     * are instances of the measurement templates for the platform's
     * type.  In this case, the MetricDisplaySummary's attributes to
     * show the number collecting doesn't make sense; 
     * showNumberCollecting should false for each bean.
     * </p>
     * <p>
     * When the entId is compatible group of servers or platforms, 
     * return all of the metrics for the type.  Each MetricDisplaySummary
     * actually represents the metrics summarized for all of the group
     * members (cumulative/averaged as appropriate), 
     * showNumberCollecting should be true and the numberCollecting
     * as well as the total number of members assigned in each bean.
     * </p>
     * 
     * @return Map keyed on the category (String), values are List's of 
     * MetricDisplaySummary beans
     * @see org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary
     * @ejb:interface-method
     */
    public MetricDisplaySummary findMetric(int sessionId, List resources,
                                           Integer tid, long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, AppdefEntityNotFoundException,
               AppdefCompatException, MeasurementNotFoundException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        
        // Just one metric
        final List mtids = Collections.singletonList(tid);
        
        // Look up the metric summaries of all associated resources
        Map results = getResourceMetrics(subject, resources, mtids, begin, end,
                                         null);
        
        // Should only be one
        if (_log.isDebugEnabled()) {
            _log.debug("getResourceMetrics() returned " + results.size());
        }
    
        if (results.size() > 0) {
            Iterator it = results.values().iterator();
            Collection coll = (Collection) it.next();
            it = coll.iterator();
            MetricDisplaySummary summary = (MetricDisplaySummary) it.next();
            
            return summary;
        }
        
        return null;
    }
    
    /**
     * Prunes from the list of passed-in AppdefEntityValue array those
     * resources that are not collecting the metric corresponding to
     * the given template id.
     * @param resources the resources
     * @param tid the metric template id
     * @return an array of resources
     * @ejb:interface-method
     */
    public AppdefResourceValue[] pruneResourcesNotCollecting(
        int sessionId, AppdefResourceValue[] resources, Integer tid)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, MeasurementNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        MeasurementTemplate tmpl = getTemplateManager().getTemplate(tid);

        List pruned = new ArrayList();
        for (int i=0; i<resources.length; ++i) {
            List measurements =
                getMeasurementsForResource(subject, resources[i].getEntityId(),
                                           tmpl);
            if (measurements.size() > 0) {
                pruned.add(resources[i]);
            }
        }
        AppdefResourceValue[] prunedResources =
            new AppdefResourceValue[pruned.size()];
        return (AppdefResourceValue[])pruned.toArray(prunedResources);
    }

    private List getMeasurementsForResource(AuthzSubject subject,
                                            AppdefEntityID aid,
                                            MeasurementTemplate tmpl)
        throws PermissionException, AppdefEntityNotFoundException,
               MeasurementNotFoundException {
        // Find the measurement ID based on entity type
        if (aid.isApplication()) {
            AppdefEntityValue aeval = new AppdefEntityValue(aid, subject);

            Application app = (Application) aeval.getResourcePOJO();
            Collection appSvcs = app.getAppServices();
        
            // If it's availability, then we'd have to get data for all svcs
            for (Iterator it = appSvcs.iterator(); it.hasNext(); ) {
                AppService appSvc = (AppService) it.next();
                // Metric is based on the entry point
                if (appSvc.isEntryPoint()) {
                    AppdefEntityID id;
                    
                    if (appSvc.isIsGroup()) {
                        id = AppdefEntityID.newGroupID(
                            appSvc.getResourceGroup().getId());
                    }
                    else {
                        id = appSvc.getService().getEntityId();
                    }
                    
                    try {
                        return getMeasurementsForResource(subject, id, tmpl);
                    } catch (MeasurementNotFoundException ignore) {
                    }
                }
            }
            
            throw new MeasurementNotFoundException("No entry point found: " +
                                                   aid);
        } else if (aid.isGroup()) {
            if (tmpl.isAvailability()) {
                // Find the group
                final ResourceGroup group = getResourceGroupManager()
                    .findResourceGroupById(subject, aid.getId());
                final Map mmap = getMetricManager()
                    .getAvailMeasurements(Collections.singleton(group));
                List metrics = (List) mmap.get(group.getResource().getId());
                for (Iterator it = metrics.iterator(); it.hasNext(); ) {
                    Measurement m =  (Measurement) it.next();
                    if (!m.getTemplate().equals(tmpl))
                        it.remove();
                }
                return metrics;
            }
            else {
                AppdefEntityID[] ids =
                    getGroupMemberIDs(subject, aid.getId());
            
                // Get the list of measurements
                return getMetricManager().findMeasurements(subject,
                                                           tmpl.getId(),
                                                           ids);
            }
        } else {
            AppdefEntityID[] aids = { aid };
            return getMetricManager().findMeasurements(subject, tmpl.getId(),
                                                       aids);
        }
    }

    private MetricDisplaySummary getMetricDisplaySummary(AuthzSubject subject,
                                                         MeasurementTemplate tmpl,
                                                         long begin,
                                                         long end,
                                                         double[] data,
                                                         AppdefEntityID id)
        throws MeasurementNotFoundException
    {
        // Get baseline values
        Measurement dmval = getMetricManager().findMeasurement(subject,
                                                               tmpl.getId(),
                                                               id);

        // Use previous function to set most values, including only 1 resource
        MetricDisplaySummary summary =
            getMetricDisplaySummary(tmpl, new Long(dmval.getInterval()),
                                    begin, end, data, 1);

        if (dmval.getBaseline() != null) {
            Baseline bval = dmval.getBaseline();
            if (bval.getMean() != null)
                summary.setMetric(MetricDisplayConstants.BASELINE_KEY,
                        new MetricDisplayValue(bval.getMean()));
            if (bval.getMaxExpectedVal() != null)
                summary.setMetric(MetricDisplayConstants.HIGH_RANGE_KEY,
                        new MetricDisplayValue(bval.getMaxExpectedVal()));
            if (bval.getMinExpectedVal() != null)
                summary.setMetric(MetricDisplayConstants.LOW_RANGE_KEY,
                        new MetricDisplayValue(bval.getMinExpectedVal()));
        }
        return summary;
    }

    /**
     * Method findResourceMetricSummary.
     * 
     * For metric comparisons, the ResourceMetricDisplaySummary beans
     * are returned as a map where the keys are the MeasurementTemplateValue 
     * (or MeasurementTemplateLiteValue?)  objects associated with the given
     * resource's types, the values are Lists of ResourceMetricDisplaySummary
     *
     * The context that the user will be populating the input resource list 
     * from should always be like resource types.  If for some reason that's 
     * not the case, this method will take a garbage in/garbage out 
     * approach (as opposed to enforcing like types) -- comparing apples 
     * and oranges may be performed but if the user ends up with measurement 
     * templates for which there is only one resource to compare, that should 
     * indicate some other problem i.e. the application is presenting 
     * dissimilar objects as available for comparison.
     * 
     * The list of resources can be any concrete AppdefResourceValue (i.e.
     * a platform, server or service), composite AppdefResourceValues
     * (i.e. applications, groups) are inappropriate for this signature.
     * 
     * Used for screen 0.3
     *
     * @param begin the commencement of the timeframe of interest
     * @param end the end of the timeframe of interest
     * @return Map of measure templates and resource metric lists
     * @ejb:interface-method
     */
    public Map findResourceMetricSummary(int sessionId,
                                         AppdefEntityID[] entIds,
                                         long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException, 
               AppdefEntityNotFoundException, MeasurementNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
    
        // Assuming that all AppdefEntityIDs of the same type, use the first one
        AppdefEntityValue rv = new AppdefEntityValue(entIds[0], subject);
        String monitorableType = rv.getMonitorableType();        
    
        // Get the template ID's for that type
        List tmpls = findMeasurementTemplates(sessionId, monitorableType,
                                              PageControl.PAGE_ALL);
    
        // Keep the templates in a map        
        IntHashMap tmplMap = new IntHashMap();
        Integer[] tids = new Integer[tmpls.size()];
        int idx = 0;
        for (Iterator i = tmpls.iterator(); i.hasNext(); ) {
            MeasurementTemplate tmpl = (MeasurementTemplate) i.next();
            final Integer tid = tmpl.getId();
            tmplMap.put(tid.intValue(), tmpl);
            tids[idx++] = tid;
        }
    
        IntHashMap templateMetrics = new IntHashMap();
        HashMap uniqueTemplates = new HashMap();
        // a temp cache to save rountrips to the db
        HashMap seen = new HashMap();      
        
        // Now, iterate through each AppdefEntityID
        ResourceManagerLocal resMan = getResourceManager();
        final DataManagerLocal dMan = DataManagerEJBImpl.getOne();
        for (int i = 0; i < entIds.length; i++) {            
            Integer[] eids = new Integer[] { entIds[i].getId() };
            // Now get the aggregate data, keyed by template ID's
            List measurements =
                getMetricManager().getMeasurements(tids, eids);
            Map datamap =
                dMan.getAggregateDataByTemplate(measurements, begin, end);
    
            // For each template, add a new summary
            for (Iterator it = datamap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                    
                Integer mtid = (Integer) entry.getKey();
                // Get the MeasurementTemplate
                MeasurementTemplate tmpl =
                    (MeasurementTemplate) tmplMap.get(mtid.intValue());
                        
                // Use the MeasurementTemplate id to get the array List
                List resSummaries =
                    (List) templateMetrics.get(mtid.intValue());
                if (resSummaries == null) // this key hasn't been seen yet
                    resSummaries = new ArrayList();
                    
                // Get the data
                double[] data = (double[]) entry.getValue(); 
                    
                Resource v;
                rv = new AppdefEntityValue(entIds[i], subject);
    
                if (seen.containsKey(entIds[i])) {
                    v = (Resource) seen.get(entIds[i]);
                } else {
                    v = resMan.findResource(entIds[i]);
                    seen.put(entIds[i], v);  // keep track of what we've seen
                }
                    
                MetricDisplaySummary mds =
                    getMetricDisplaySummary(subject, tmpl, begin, end,
                                            data, entIds[i]);
                    
                resSummaries.add(new ResourceMetricDisplaySummary(mds, v));
                templateMetrics.put(mtid.intValue(), resSummaries);
                uniqueTemplates.put(mtid, tmpl);
            }
        }
        // now take all of the unique lists and unique 
        // MeasurementTemplate's and merge them into the result
        HashMap result = new HashMap();
        for (Iterator iter = uniqueTemplates.keySet().iterator(); 
             iter.hasNext();) 
        {
            Integer mtid = (Integer) iter.next();
            result.put(uniqueTemplates.get(mtid),
                       templateMetrics.get(mtid.intValue()));
        }                
        return result;
    }

    /**
     * Return a MetricSummary bean for each of the metrics (template) for the
     * entities in the given time frame
     * @param begin the beginning time frame
     * @param end the ending time frame
     * @return a list of ResourceTypeDisplaySummary beans
     * @throws AppdefCompatException
     * @ejb:interface-method
     */
    public Map findMetrics(int sessionId, AppdefEntityID[] entIds,
                           long filters, String keyword, long begin, long end,
                           boolean showNoCollect)
        throws SessionTimeoutException, SessionNotFoundException,
            InvalidAppdefTypeException, PermissionException,
            AppdefEntityNotFoundException, AppdefCompatException {
        return super.findMetrics(sessionId, entIds, filters, keyword,
                                 begin, end, showNoCollect);
    }

    /**
     * Method findMetrics.
     * 
     * When the entId is a server, return all of the metrics that are instances
     * of the measurement templates for the server's type. In this case, the
     * MetricDisplaySummary's attributes to show the number collecting doesn't
     * make sense; showNumberCollecting should false for each bean.
     * <p>
     * When the entId is a platform, return all of the metrics that are
     * instances of the measurement templates for the platform's type. In this
     * case, the MetricDisplaySummary's attributes to show the number collecting
     * doesn't make sense; showNumberCollecting should false for each bean.
     * </p>
     * <p>
     * When the entId is compatible group of servers or platforms, return all of
     * the metrics for the type. Each MetricDisplaySummary actually represents
     * the metrics summarized for all of the group members (cumulative/averaged
     * as appropriate), showNumberCollecting should be true and the
     * numberCollecting as well as the total number of members assigned in each
     * bean.
     * </p>
     * 
     * @return Map keyed on the category (String), values are List's of
     *         MetricDisplaySummary beans
     * @throws AppdefCompatException
     * @see org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary
     * @ejb:interface-method
     */
    public Map findMetrics(int sessionId, AppdefEntityID entId, List mtids,
                           long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
            PermissionException, AppdefEntityNotFoundException,
            AppdefCompatException {
        return super.findMetrics(sessionId, entId, mtids, begin, end);
    }

    /** Return a MetricSummary bean for each of the servers of a specific type.
     * @param begin the beginning time frame
     * @param end the ending time frame
     * @return a list of ResourceTypeDisplaySummary beans
     * @throws AppdefCompatException
     * @ejb:interface-method
     */
    public Map findAGPlatformMetricsByType(int sessionId, 
                                           AppdefEntityTypeID platTypeId,
                                           long begin, long end,
                                           boolean showAll)
        throws SessionTimeoutException, SessionNotFoundException,
               InvalidAppdefTypeException, AppdefEntityNotFoundException,
               PermissionException, AppdefCompatException {
        return super.findAGPlatformMetricsByType(sessionId, platTypeId,
                                                 begin, end, showAll);
    }

    /**
     * Return a Metric summary bean for each of the services of a specific type
     * <p>
     * The map returned has keys for the measurement categories (see
     * MeasurementConstants) and values that are Lists of MetricDisplaySummary
     * beans.
     * </p>
     * <p>
     * This is used to access metrics for entity's internal and deployed
     * services. The metrics returned are only applicable from within the given
     * timeframe of interest.
     * </p>
     * <p>
     * Appropriate entities include
     * <ul>
     * <li>applications (2.1.2.2-3)
     * <li>servers (2.3.2.1-4 - internal/deplyed tabs)
     * <li>services (2.5.2.2 - internal/deplyed tabs)
     * </ul>
     *
     * @param begin the beginning time frame
     * @param end the ending time frame
     * @return a list of CurrentHealthDisplaySummary beans
     * @throws AppdefCompatException
     * @ejb:interface-method
     */
    public Map findAGMetricsByType(int sessionId, AppdefEntityID[] entIds,
                                   AppdefEntityTypeID typeId, long filters,
                                   String keyword, long begin, long end,
                                   boolean showAll)
        throws SessionTimeoutException, SessionNotFoundException,
            InvalidAppdefTypeException, PermissionException,
            AppdefEntityNotFoundException, AppdefCompatException {
        return super.findAGMetricsByType(sessionId, entIds, typeId, filters,
                                         keyword, begin, end, showAll);
    }
    
    /** Return a MeasurementSummary bean for the resource's associated resources
     * specified by type
     * @param entId the entity ID
     * @param appdefType the type (server, service, etc) of the specified resource type
     * @param typeId the specified resource type ID
     * @return a MeasurementSummary bean
     * @ejb:interface-method
     */
    public MeasurementSummary getSummarizedResourceAvailability(
        int sessionId, AppdefEntityID entId, int appdefType, Integer typeId)
        throws AppdefEntityNotFoundException, PermissionException,
               SessionNotFoundException, SessionTimeoutException,
               InvalidOptionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
    
        List resources;
        if (entId.isGroup()) {
            resources = getResourceIds(subject, entId, null);
        } else if (entId.isApplication()) {
            AppdefEntityValue aev = new AppdefEntityValue(entId, subject);
            resources = aev.getAssociatedServices(typeId, PageControl.PAGE_ALL);
            if (typeId != null) {
                for (Iterator i = resources.iterator(); i.hasNext();) {
                    AppdefResourceValue r = (AppdefResourceValue) i.next();
                    if (r instanceof ServiceClusterValue)
                        i.remove();
                }
            }
    
        } else {
            AppdefEntityValue aev = new AppdefEntityValue(entId, subject);
            switch (appdefType) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    resources = aev.getAssociatedPlatforms(PageControl.PAGE_ALL);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    resources = aev.getAssociatedServerIds(typeId);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    resources =
                        aev.getAssociatedServices(typeId, PageControl.PAGE_ALL);
                    break;
                default:
                    throw new InvalidOptionException(
                        "Requested type (" + appdefType +
                        ") is not a platform, server, or service");
            }
        }

        final AppdefEntityID[] resourceArray = toAppdefEntityIDArray(resources);
        final double[] data = getAvailability(subject, resourceArray);
    
        // Availability counts **this calls getLiveMeasurement
        int availCnt = 0;
        int unavailCnt = 0;
        int unknownCnt = 0;
        for (int i = 0; i < data.length; i++) {
            // switch
            if (MeasurementConstants.AVAIL_UP == data[i]) {
                // yes
                availCnt++;
            }
            else if (MeasurementConstants.AVAIL_UNKNOWN == data[i]) {
                // maybe so
                unknownCnt++;
            }
            else if ((MeasurementConstants.AVAIL_DOWN <= data[i] &&
                      MeasurementConstants.AVAIL_UP > data[i]) ||
                     MeasurementConstants.AVAIL_PAUSED == data[i]) {
                // no
                unavailCnt++;
            }
            else {
                // If for some reason we have availability data that is not
                // recognized as a valid state in MeasurementConstants.AVAIL_
                // log as much info as possible and mark it as UNKNOWN.
                unknownCnt++;
                _log.error("Resource " + resourceArray[i] + " is reporting " +
                           "an invalid availability state of " +
                           data[i] + " (measurement id=" +
                           findAvailabilityMetric(sessionId,
                                                  resourceArray[i]).getId() +
                           ")");
            }
        }
        
        return new MeasurementSummary(new Integer(availCnt),
                                      new Integer(unavailCnt),
                                      new Integer(unknownCnt));
    }

    /**
     * @return a List of ResourceTypeDisplaySummary's
     * @deprecated use POJO API instead
     */    
    private List getSummarizedResourceCurrentHealth(AuthzSubject subject, 
                                                AppdefResourceValue[] resources)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException 
    {
        final List summaries = new ArrayList();
    
        // Create Map of auto-group'd/singleton resources and a List of clusters
        // since their current health summarizations are not flattened        
    
        // auto-group'd entities are kept track of in here where keys are the
        // type id's and the values are lists of resources
        final HashMap resTypeMap = new HashMap(); 
        // to avoid looking up singleton/autogroup resources again later, we
        // keep them here where keys are AppdefEntityID's and values are the
        // AppdefResourceValues
        final HashMap resourcemap = new HashMap();
        final List appdefVals = Arrays.asList(resources);
        final Map tmp = getMetricManager().getAvailMeasurements(appdefVals);
        final Map midMap = getMidMap(getAeids(appdefVals), tmp);
        // keys are type id's and the values are AppdefResourceTypeValues
        for (int i = 0; i < resources.length; i++) {
            if (resources[i] instanceof ServiceClusterValue) {
                AppdefResourceValue resource = resources[i];
                AppdefEntityID aid = resource.getEntityId();
                AppdefEntityValue aeval = new AppdefEntityValue(aid, subject);
                AppdefGroupValue agval =
                    (AppdefGroupValue) aeval.getResourceValue();
                ClusterDisplaySummary cds = new ClusterDisplaySummary();
                    
                cds.setEntityId(resource.getEntityId());
                cds.setEntityName(agval.getName());
                int size = agval.getTotalSize();
                cds.setNumResources(new Integer(size));
                // Replace the IDs with all of the members
                List memberIds = getResourceIds(subject, aid, null);
                AppdefEntityID[] ids = (AppdefEntityID[])
                    memberIds.toArray(new AppdefEntityID[0]);
                setResourceTypeDisplaySummary(subject, cds,
                                              agval.getAppdefResourceTypeValue(),
                                              ids, midMap);
                summaries.add(cds);
            } else {
                // all of the non-clusters get organized in here
                resourcemap.put(resources[i].getEntityId(), resources[i]);
                AppdefResourceTypeValue type =
                    resources[i].getAppdefResourceTypeValue();
                Integer typeId = type.getId();
                List siblings = (List) resTypeMap.get(typeId);
                if (siblings == null) {
                    siblings = new ArrayList();
                    resTypeMap.put(typeId, siblings);               
                }            
                // Add resource to list
                siblings.add(resources[i].getEntityId());
            }
        }
        // first deal with the autogroubz and singletons (singletons
        // are just the degenerative case of an autogroup, why it's
        // its own type is... silly)
        for (Iterator it = resTypeMap.entrySet().iterator(); it.hasNext();){
            Map.Entry entry = (Map.Entry)it.next();
            Collection siblings = (Collection) entry.getValue();
            // Make sure we have valid IDs
            if (siblings == null || siblings.size() == 0)
                continue;                    
                
            ResourceTypeDisplaySummary summary;
            AppdefResourceValue resource;
            AppdefEntityID[] ids =
                (AppdefEntityID[])siblings.toArray(new AppdefEntityID[0]);                
            AppdefEntityID aid = ids[0];
            resource = (AppdefResourceValue)resourcemap.get(aid);
            // autogroup
            if (ids.length > 1) {
                summary = new AutogroupDisplaySummary();
                summary.setNumResources(new Integer(ids.length));
            } else {
                // singleton
                summary = new SingletonDisplaySummary(aid, resource.getName());
            }
            setResourceTypeDisplaySummary(subject, summary,
                                          resource.getAppdefResourceTypeValue(),
                                          ids, midMap);
            summaries.add(summary);
        }
    
        Collections.sort(summaries);
        return summaries;
    }

    /**
     * @return a List of ResourceTypeDisplaySummary's
     */    
    private List getSummarizedResourceCurrentHealth(AuthzSubject subject, 
                                                    Collection resources)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException {
        List summaries = new ArrayList();

        // auto-group'd entities are kept track of in here where keys are the
        // type id's and the values are lists of resources
        HashMap resTypeMap = new HashMap(); 
        
        // keys are type id's and the values are AppdefResources
        for (final Iterator it = resources.iterator(); it.hasNext(); ) {
            AppdefResource resource = (AppdefResource) it.next();
            AppdefResourceType type = resource.getAppdefResourceType();
            Integer typeId = type.getId();
            List siblings = (List) resTypeMap.get(typeId);
            if (siblings == null) {
                siblings = new ArrayList();
                resTypeMap.put(typeId, siblings);               
            }

            // Add resource to list
            siblings.add(resource);
        }
        final Map measCache = getMetricManager().getAvailMeasurements(resources);
        final Map midMap = getMidMap(getAeids(resources), measCache);
        
        // first deal with the autogroups and singletons (singletons
        // are just the degenerative case of an autogroup, why it's
        // its own type is... silly)
        for (Iterator it = resTypeMap.entrySet().iterator(); it.hasNext();) {
            final Map.Entry entry = (Map.Entry) it.next();
            final Collection siblings = (Collection) entry.getValue();
            // Make sure we have valid IDs
            if (siblings == null || siblings.size() == 0)
                continue;                    
                
            ResourceTypeDisplaySummary summary = null;

            AppdefResourceType type = null;
            AppdefEntityID[] ids = new AppdefEntityID[siblings.size()];
            int i = 0;
            for (Iterator sibIt = siblings.iterator(); sibIt.hasNext(); i++) {
                AppdefResource res = (AppdefResource) sibIt.next();
                ids[i] = res.getEntityId();
                
                if (type == null) {
                    type = res.getAppdefResourceType();
                    
                    if (sibIt.hasNext()) {
                        // autogroup
                        summary = new AutogroupDisplaySummary();
                        summary.setNumResources(new Integer(siblings.size()));
                    }
                    else {
                        // singleton
                        summary = new SingletonDisplaySummary(ids[i],
                                                              res.getName());
                    }
                }
            }
            setResourceTypeDisplaySummary(subject, summary,
                                          type.getAppdefResourceTypeValue(),
                                          ids, midMap);
            summaries.add(summary);
        }

        Collections.sort(summaries);
        return summaries;
    }

    private void setResourceTypeDisplaySummary(AuthzSubject subject,
                                      ResourceTypeDisplaySummary summary, 
                                      AppdefResourceTypeValue resType,
                                      AppdefEntityID[] ids, Map midMap) {
        // Now get each category of measurements
        long end = System.currentTimeMillis();
        
        summary.setResourceType(resType);
        
        StopWatch watch = new StopWatch(end);
        
        if (_log.isDebugEnabled())
            _log.debug("BEGIN setResourceTypeDisplaySummary");

        // Availability
        try {
            double[] data = getAvailability(subject, ids, midMap, null);
            if (data.length > 0) {
                double sum = 0;
                for (int i = 0; i < data.length; i++) {
                    sum += data[i];
                }
                summary.setAvailability(
                    new Double(sum / (double) data.length));
            }
        } catch (Exception e) {
            // No Availability data
            summary.setAvailability(
                new Double(MeasurementConstants.AVAIL_UNKNOWN));
        }

        if (_log.isDebugEnabled())
            _log.debug("END setResourceTypeDisplaySummary -- " +
                    watch.getElapsed() + " msec");
    }

    /**
     * Method findSummarizedServerCurrentHealth.
     *  <p>
     * Return a ResourceTypeDisplaySummary bean for each of the platform's
     * deployed servers.  Each bean represents a type of server and the
     * measurement data summarized for that type.
     * </p>
     * <p>
     * see screen 2.2.2
     * </p>
     * @return List of ResourceTypeDisplaySummary beans
     * @ejb:interface-method
     */
    public List findSummarizedServerCurrentHealth(int sessionId, 
                                                  AppdefEntityID entId)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
    
        // Get the associated servers        
        AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
        List servers = rv.getAssociatedServers(PageControl.PAGE_ALL);
        return getSummarizedResourceCurrentHealth(subject, 
            (AppdefResourceValue[])servers.toArray(new AppdefResourceValue[servers.size()]));
    }

    /**
     * Method findSummarizedServiceCurrentHealth.
     * <p>
     * This is used for the lists of service types for the Current Health 
     * view for
     * <ul> 
     * <li>applications (2.1.2)
     * <li>servers (2.3.2.1-4)
     * <li>services (2.5.2.2)
     * </ul>
     * </p>
     * <p>
     * If <code>internal</code> is <i>true</i>, only the <i>internal</i> 
     * services will be returned, the <i>deployed</i> ones if it's 
     * <i>false</i>.  If <code>internal</code> is <i>null</i>, then
     * both deployed <i>and</i> internal services will be returned.
     * </p>
     * 
     * @param entId the appdef entity with child services
     * @return List a list of ResourceTypeDisplaySummary beans
     * @ejb:interface-method
     */
    public List findSummarizedPlatformServiceCurrentHealth(int sessionId,
                                                           AppdefEntityID entId)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, AppdefEntityNotFoundException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        ServiceManagerLocal svcMgr = ServiceManagerEJBImpl.getOne();
        Collection services =
            svcMgr.getPlatformServices(subject, entId.getId());
        return getSummarizedResourceCurrentHealth(subject, services);
    }
    
    /**
     * @ejb:interface-method
     */
    public List findSummarizedServiceCurrentHealth(int sessionId,
                                                   AppdefEntityID entId)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, AppdefEntityNotFoundException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        // Get the associated services        
        AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
        List services= rv.getAssociatedServices(PageControl.PAGE_ALL);
        return getSummarizedResourceCurrentHealth(subject, 
            (AppdefResourceValue[]) services.toArray(
                new AppdefResourceValue[services.size()]));
    }

    /**
     * Method findGroupCurrentHealth.
     *  <p>
     * Return a ResourceDisplaySummary bean for each of the group's
     * member resources.  Each bean represents a resource and the
     * measurement data summarized for that type.
     * </p>
     * <p>
     * see screen 2.2.2
     * </p>
     * @return List of ResourceDisplaySummary beans
     * @ejb:interface-method
     */
    public List findGroupCurrentHealth(int sessionId, Integer id)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        final PlatformManagerLocal platMan = getPlatformManager();
        final ServerManagerLocal serverMan = getServerManager();
        
        // Find the group
        final ResourceGroupManagerLocal resGrpMgr = getResourceGroupManager();
        final ResourceGroup group =
            resGrpMgr.findResourceGroupById(subject, id);

        Map cats = new HashMap(2);
        cats.put(MeasurementConstants.CAT_AVAILABILITY, null);
        // XXX scottmf need to review this, perf is bad and metric
        // is not very useful
        //cats.put(MeasurementConstants.CAT_THROUGHPUT, null);

        // Look up metrics by group first
        MeasurementManagerLocal mman = getMetricManager();
        for (Iterator it = cats.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            List metrics =
                mman.findDesignatedMeasurements(subject, group,
                                                (String) entry.getKey());
            Map mmap = new HashMap(metrics.size());
            // Optimization for the fact that we can have multiple indicator
            // metrics for each category, only keep one
            for (Iterator mit = metrics.iterator(); mit.hasNext(); ) {
                Measurement m = (Measurement) mit.next();
                if (mmap.containsKey(m.getResource()))
                    continue;
                
                mmap.put(m.getResource(), m);
            }
            entry.setValue(mmap);
        }
        
        final StopWatch watch = new StopWatch();
        final PageList summaries = new PageList();
        watch.markTimeBegin("getAvailMeasurements");
        final Map measCache = getMetricManager()
                .getAvailMeasurements(Collections.singleton(group));
        watch.markTimeEnd("getAvailMeasurements");
        
        // Remap from list to map of metrics
        List metrics = (List) measCache.remove(group.getResource().getId());
        for (Iterator it = metrics.iterator(); it.hasNext(); ) {
            Measurement meas = (Measurement) it.next();
            measCache.put(meas.getResource().getId(),
                          Collections.singletonList(meas));
        }

        // Members are sorted
        final Collection members = resGrpMgr.getMembers(group);
        watch.markTimeBegin("getLastAvail");
        final Map availCache = getAvailManager().getLastAvail(
            members, measCache);
        watch.markTimeEnd("getLastAvail");
        for (Iterator it = members.iterator(); it.hasNext(); ) {
            Resource res = (Resource) it.next();
            AppdefEntityID aeid = new AppdefEntityID(res);
            ResourceDisplaySummary summary = new ResourceDisplaySummary();
        
            // Set the resource
            AppdefResourceValue parent = null;
            switch (aeid.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_SERVER :
                    watch.markTimeBegin("get platform from server");
                    Server server = serverMan.findServerById(aeid.getId());
                    parent = server.getPlatform().getPlatformValue();
                    watch.markTimeEnd("get platform from server");
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    watch.markTimeBegin("Set Resource Display for PSS Type");
                    setResourceDisplaySummaryValueForCategories(
                        subject, res, summary, cats, measCache, availCache);
                    summary.setMonitorable(Boolean.TRUE);
                    watch.markTimeEnd("Set Resource Display for PSS Type");
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                    watch.markTimeBegin("Group Type");
                    summary.setMonitorable(Boolean.TRUE);
                    // Set the availability now
                    summary.setAvailability(new Double(
                        getAvailability(subject, aeid, measCache, availCache)));
                    
                    try {
                        // Get the availability template
                        MeasurementTemplate tmpl = getAvailabilityMetricTemplate(
                            subject, aeid, measCache);
                        summary.setAvailTempl(tmpl.getId());
                    } catch (MeasurementNotFoundException e) {
                        // No availability metric, don't set it
                    }
                    watch.markTimeEnd("Group Type");
                    break;
                default:
                    throw new InvalidAppdefTypeException(
                        "entity type is not monitorable, id type: " +
                        aeid.getType());
            }            
            setResourceDisplaySummary(summary, res, aeid, parent);
            summaries.add(summary);
        }
        if (_log.isDebugEnabled()) {
            _log.debug("getGroupCurrentHealth: " + watch);
        }
        summaries.setTotalSize(summaries.size());
        return summaries;
    }

    /**
     * Return a ResourceDisplaySummary bean for each of the resource's
     * virtual resources.  Each bean represents a resource and the
     * measurement data summarized for that resource.
     * </p>
     * <p>
     * see screen 2.2.2
     * </p>
     * @return List of ResourceDisplaySummary beans
     * @ejb:interface-method
     */
    public List findVirtualsCurrentHealth(int sessionId,
                                         AppdefEntityID entId)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, GroupNotCompatibleException,
               PermissionException {
        
        final AuthzSubject subject = manager.getSubject(sessionId);

        try {
            VirtualManagerLocal vman =
                VirtualManagerUtil.getLocalHome().create();
            List resources =
                vman.findVirtualResourcesByPhysical(subject, entId);
            PageList resPageList = new PageList(resources, resources.size());
            return getResourcesCurrentHealth(subject, resPageList);
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    private void setResourceDisplaySummary(ResourceDisplaySummary rds, 
                                           AppdefEntityValue resource,
                                           AppdefResourceValue parentResource)
        throws AppdefEntityNotFoundException, PermissionException {
        rds.setEntityId(resource.getID());
        rds.setResourceName(resource.getName());
        rds.setResourceEntityTypeName(resource.getID().getTypeName());
        rds.setResourceTypeName(resource.getTypeName());
        if (parentResource == null) {
            rds.setHasParentResource(Boolean.FALSE);
        } else {
            rds.setParentResourceId(parentResource.getId());
            rds.setParentResourceName(parentResource.getName());
            rds.setParentResourceTypeId(
                new Integer(parentResource.getEntityId().getType()));
            rds.setHasParentResource(Boolean.TRUE);
        }
    }

    private void setResourceDisplaySummary(ResourceDisplaySummary rds, 
                                           Resource resource,
                                           AppdefEntityID aeid,
                                           AppdefResourceValue parentResource)
        throws AppdefEntityNotFoundException, PermissionException {
        rds.setEntityId(aeid);
        rds.setResourceName(resource.getName());
        rds.setResourceEntityTypeName(aeid.getTypeName());
        rds.setResourceTypeName(resource.getPrototype().getName());
        if (parentResource == null) {
            rds.setHasParentResource(Boolean.FALSE);
        } else {
            rds.setParentResourceId(parentResource.getId());
            rds.setParentResourceName(parentResource.getName());
            rds.setParentResourceTypeId(
                new Integer(parentResource.getEntityId().getType()));
            rds.setHasParentResource(Boolean.TRUE);
        }
    }

    private PageList getResourcesCurrentHealth(AuthzSubject subject,
                                               PageList resources)
        throws AppdefEntityNotFoundException, PermissionException {
        final ServerManagerLocal sMan = getServerManager();
        final boolean debug = _log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        PageList summaries = new PageList();
        if (debug) watch.markTimeBegin("getAvailMeasurements");
        final Map measCache = getMetricManager().getAvailMeasurements(resources);
        if (debug) watch.markTimeEnd("getAvailMeasurements");
        if (debug) watch.markTimeBegin("getLastAvail");
        final Map availCache = getAvailManager().getLastAvail(resources, measCache);
        if (debug) watch.markTimeEnd("getLastAvail");
        for (Iterator it = resources.iterator(); it.hasNext(); ) {
            try {
                Object o = it.next();
                AppdefEntityValue rv;
                AppdefEntityID aeid;
                if (o instanceof AppdefEntityValue) {
                    rv = (AppdefEntityValue) o;
                    aeid = rv.getID();
                }
                else if (o instanceof AppdefEntityID) {
                    aeid = (AppdefEntityID) o;
                    rv = new AppdefEntityValue(aeid, subject);
                }
                else {
                    AppdefResourceValue resource = (AppdefResourceValue) o;
                    aeid = resource.getEntityId();
                    rv = new AppdefEntityValue(aeid, subject);
                }
                ResourceDisplaySummary summary = new ResourceDisplaySummary();
                // Set the resource
                AppdefResourceValue parent = null;
                HashSet categories = new HashSet(4);
                switch (aeid.getType()) {
                    case AppdefEntityConstants.APPDEF_TYPE_SERVER :
                        if (debug) watch.markTimeBegin("getPlatform");
                        Server server = sMan.findServerById(rv.getID().getId());
                        parent = server.getPlatform().getAppdefResourceValue();
                        if (debug) watch.markTimeEnd("getPlatform");
                    case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    case AppdefEntityConstants.APPDEF_TYPE_SERVICE :
                        categories.add(MeasurementConstants.CAT_AVAILABILITY);
                        // XXX scottmf need to review this, perf is bad and metric
                        // is not very useful
                        //categories.add(MeasurementConstants.CAT_THROUGHPUT);
                        if (debug) watch.markTimeBegin("setResourceDisplaySummaryValueForCategory");
                        setResourceDisplaySummaryValueForCategory(
                            subject, aeid, summary, categories, measCache,
                            availCache);
                        if (debug) watch.markTimeEnd("setResourceDisplaySummaryValueForCategory");
                        summary.setMonitorable(Boolean.TRUE);
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                    case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                        if (debug) watch.markTimeBegin("Group Type");
                        summary.setMonitorable(Boolean.TRUE);
                        // Set the availability now
                        double avail = getAvailability(
                            subject, aeid, measCache, availCache);
                        summary.setAvailability(new Double(avail));
                        try {
                            // Get the availability template
                            MeasurementTemplate tmpl = getAvailabilityMetricTemplate(
                                subject, aeid, measCache);
                            summary.setAvailTempl(tmpl.getId());
                        } catch (MeasurementNotFoundException e) {
                            // No availability metric, don't set it
                        }
                        if (debug) watch.markTimeEnd("Group Type");
                        break;
                    default:
                        throw new InvalidAppdefTypeException(
                            "entity type is not monitorable, id type: " +
                            aeid.getType());
                }            
                if (debug) watch.markTimeBegin("setResourceDisplaySummary");
                setResourceDisplaySummary(summary, rv, parent);
                if (debug) watch.markTimeEnd("setResourceDisplaySummary");
                summaries.add(summary);
            } catch (AppdefEntityNotFoundException e) {
                _log.debug(e.getMessage(), e);
            }
        }
        if (debug) _log.debug("getResourcesCurrentHealth: " + watch);
        summaries.setTotalSize(resources.getTotalSize());
        return summaries;
    }

    /**
     * @param id the AppdefEntityID of the resource our ResourceDisplaySummary is for
     * @param summary a ResourceDisplaySummary
     * @param availCache Map<Integer, MetricValue>
     *  Integer => Measurement.getId(), may be null
     * @param measCache Map<Integer, List<Measurement>>
     *  Integer => Resource.getId(), may be null
     * @throws PermissionException
     * @throws AppdefEntityNotFoundException
     */
    private void setResourceDisplaySummaryValueForCategory(AuthzSubject subject,
            AppdefEntityID aeid, ResourceDisplaySummary summary,
            Set categories, Map measCache, Map availCache)
        throws AppdefEntityNotFoundException,
               PermissionException {
        // Maybe we're not doing anything
        if (categories.size() == 0) {
            return;
        }
        final Resource resource = getResourceManager().findResource(aeid);
        MetricValue mv = null;
        if (categories.remove(MeasurementConstants.CAT_AVAILABILITY)) {
            Measurement dm = null;
            // try to use prefetched caches
            if (measCache != null) {
                List list = (List)measCache.get(resource.getId());
                if (list == null) {
                    // nothing to do
                } else if (list.size() == 1) {
                    dm = (Measurement)list.get(0);
                    mv = (MetricValue)availCache.get(dm.getId());
                    if (mv != null) {
                        summary.setAvailability(mv.getObjectValue());
                        summary.setAvailTempl(dm.getTemplate().getId());
                    }
                }
            }
            // check if prefetched caches didn't contain values
            dm = (dm == null) ? findAvailabilityMetric(subject, aeid) : dm;
            if (dm != null && mv == null) {
                summary.setAvailability(new Double(
                    getAvailability(subject, aeid, measCache, availCache)));
                summary.setAvailTempl(dm.getTemplate().getId());
            }
        }
        if (categories.size() == 0) {
            return;
        }
        List measurements = findDesignatedMetrics(subject, aeid, categories);
        // Optimization for the fact that we can have multiple indicator
        // metrics for each category
        HashSet done = new HashSet();
        for (Iterator it = measurements.iterator(); it.hasNext(); ) {
            Measurement m = (Measurement) it.next();
            final MeasurementTemplate templ = m.getTemplate();
            if (!templ.isDesignate()) {
                continue;
            }
            final String category = templ.getCategory().getName();
            if (done.contains(category)) {
                continue;
            }
            // XXX scottmf need to review this, perf is bad and metrics
            // are not very useful
            mv = new MetricValue();
            // XXX mv = getDataMan().getLastHistoricalData(m);
            if (mv == null) {
                continue;
            }
            Double theValue = mv.getObjectValue();
            if (category.equals(MeasurementConstants.CAT_THROUGHPUT)) {
                summary.setThroughput(theValue);
                summary.setThroughputUnits(templ.getUnits());
                summary.setThroughputTempl(templ.getId());
            } else if (category.equals(MeasurementConstants.CAT_PERFORMANCE)) {
                summary.setPerformance(theValue);
                summary.setPerformanceUnits(templ.getUnits());
                summary.setPerformTempl(templ.getId());
            }

            done.add(category);
        }
    }

    /**
     * @param id the AppdefEntityID of the resource our ResourceDisplaySummary is for
     * @param summary a ResourceDisplaySummary
     * @param availCache Map<Integer, MetricValue>
     *  Integer => Measurement.getId(), may be null
     * @param measCache Map<Integer, List<Measurement>>
     *  Integer => Resource.getId(), may be null
     * @throws PermissionException
     * @throws AppdefEntityNotFoundException
     */
    private void setResourceDisplaySummaryValueForCategories(
                                            AuthzSubject subject, Resource res,
                                            ResourceDisplaySummary summary,
                                            Map categories, Map measCache,
                                            Map availCache)
        throws AppdefEntityNotFoundException, PermissionException {        
        StopWatch watch = new StopWatch();
        List resMetrics = new ArrayList(categories.size());
        
        // First find the measurements
        for (Iterator it = categories.values().iterator(); it.hasNext(); ) {
            Map metrics = (Map) it.next();
            if (metrics.containsKey(res)) {
                resMetrics.add((Measurement) metrics.remove(res));
            }
        }

        final AppdefEntityID id = new AppdefEntityID(res);
        for (Iterator it = resMetrics.iterator(); it.hasNext(); ) {
            final Measurement m = (Measurement) it.next();
            final MeasurementTemplate templ = m.getTemplate();
            final String category = templ.getCategory().getName();
    
            if (category.equals(MeasurementConstants.CAT_AVAILABILITY)) {
                watch.markTimeBegin("getAvailability");
                MetricValue mv = null;;
                if (availCache != null) {
                    Integer mid = m.getId();
                    mv = (MetricValue)availCache.get(mid);
                }
                Double val = (mv == null) ?
                    new Double(
                        getAvailability(subject, id, measCache, availCache)) :
                    mv.getObjectValue();
                summary.setAvailability(val);
                summary.setAvailTempl(templ.getId());
                watch.markTimeEnd("getAvailability");
                continue;
            }
            
            // XXX scottmf, need to investigate this for perf
            //MetricValue mv = getDataMan().getLastHistoricalData(m);
            MetricValue mv = new MetricValue();
            if (mv == null) {
                continue;
            }
            Double theValue = mv.getObjectValue();
    
            if (category.equals(MeasurementConstants.CAT_THROUGHPUT)) {
                summary.setThroughput(theValue);
                summary.setThroughputUnits(templ.getUnits());
                summary.setThroughputTempl(templ.getId());
            } else if (category.equals(MeasurementConstants.CAT_PERFORMANCE)) {
                summary.setPerformance(theValue);
                summary.setPerformanceUnits(templ.getUnits());
                summary.setPerformTempl(templ.getId());
            }
        }
        if (_log.isDebugEnabled()) {
            _log.debug("setResourceDisplaySummaryValueForCategories: " + watch);
        }
    }

    /**
     * Method findResourcesCurrentHealth.
     * 
     * The size of the list of ResourceDisplaySummary beans returned will
     * be equivalent to the size of the entity ID's passed in.  Called by RSS
     * feed so it does not require valid session ID.
     * 
     * @throws ApplicationException if user is not found
     * @throws LoginException if user account has been disabled
     * @return PageList of ResourceDisplaySummary beans
     * @ejb:interface-method
     */
    public List findResourcesCurrentHealth(String user, AppdefEntityID[] entIds)
        throws LoginException, ApplicationException, PermissionException,
               AppdefEntityNotFoundException, SessionNotFoundException,
               SessionTimeoutException {
        int sessionId = getAuthManager().getUnauthSessionId(user);
        return findResourcesCurrentHealth(sessionId, entIds);
    }
    
    /**
     * Method findResourcesCurrentHealth.
     * 
     * The size of the list of ResourceDisplaySummary beans returned will
     * be equivalent to the size of the entity ID's passed in.
     * 
     * @return PageList of ResourceDisplaySummary beans
     * @ejb:interface-method
     */
    public List findResourcesCurrentHealth(int sessionId, 
                                           AppdefEntityID[] entIds)
        throws AppdefEntityNotFoundException, PermissionException,
               SessionNotFoundException, SessionTimeoutException {
        final AuthzSubject subject = manager.getSubject(sessionId);

        Log timingLog = LogFactory.getLog("DASHBOARD-TIMING");
        StopWatch timer = new StopWatch();

        PageList resources = new PageList(Arrays.asList(entIds),
                                          entIds.length);
        timingLog.trace("findResourceCurrentHealth(2) - timing [" +
                        timer.toString()+"]");

        return getResourcesCurrentHealth(subject, resources);
    }

    /**
     * Find the current health of the entity's host(s)
     * 
     * @return PageList of ResourceDisplaySummary beans
     * @ejb:interface-method
     */
    public List findHostsCurrentHealth(int sessionId, AppdefEntityID entId,
                                       PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException, AppdefEntityNotFoundException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
        int entType = entId.getType();

        if (entId.isGroup()) {
            entType = rv.getAppdefResourceType().getAppdefType();
        }
        
        PageList hosts;
        
        switch (entType) {
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            hosts = rv.getAssociatedPlatforms(pc);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            hosts = rv.getAssociatedServers(pc);
            
            // Go through and see if we are looking at virtual servers
            ArrayList virtIds = new ArrayList(hosts.size());
            for (Iterator it = hosts.iterator(); it.hasNext(); ) {
                ServerValue sv = (ServerValue) it.next();
                if (!sv.getServerType().getVirtual())
                    break;
                
                virtIds.add(sv.getEntityId());
            }
            
            if (virtIds.size() > 0)
                hosts = getPlatformManager().getPlatformsByServers(subject,
                                                                   virtIds);
            break;
        default:
            return new PageList();
        }
        
        // Return a paged list of current health        
        return getResourcesCurrentHealth(subject, hosts);
    }
    
    /**
     * Method findPlatformsCurrentHealth.
     * 
     * The population of the list of ResourceDisplaySummary beans returned will
     * vary depending on the entId's type.
     * 
     * When the entId is a server, the returned list should have just one
     * ResourceDisplaySummary with a PlatformValue in it, the one that 
     * represents the host that the server resides on.
     * 
     * When the entId is a compatible group of platforms, the returned list 
     * will have as many elements as there are individual PlatformValue's to
     * represent all of the hosts.
     * 
     * @return PageList of ResourceDisplaySummary beans
     * @ejb:interface-method
     */
    public PageList findPlatformsCurrentHealth(int sessionId, 
                                               AppdefEntityID entId,
                                               PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
        PageList platforms = rv.getAssociatedPlatforms(pc);
        
        // Return a paged list of current health        
        return getResourcesCurrentHealth(subject, platforms);
    }

    /**
     * Method findAGPlatformsCurrentHealthByType
     *  
     * For autogroup of platforms.
     * 
     * If the entId is a platform, the deployed servers view shows
     * the current health of servers.
     *
     * @return a list of ResourceDisplaySummary beans
     * @ejb:interface-method
     */
    public List findAGPlatformsCurrentHealthByType(int sessionId,
                                                   Integer platTypeId)
        throws SessionTimeoutException, SessionNotFoundException,
               InvalidAppdefTypeException, PermissionException,
               AppdefEntityNotFoundException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        PlatformManagerLocal platMan = getPlatformManager();
        PlatformType pt = platMan.findPlatformType(platTypeId);
        List platforms = platMan.getPlatformsByType(subject, pt.getName());
        
        // Need list of AppdefEntityValues
        PageList aevs = new PageList();
        for (Iterator it = platforms.iterator(); it.hasNext(); ) {
            aevs.add(new AppdefEntityValue(subject, ((Platform) it.next())));
        }
        
        // Return a paged list of current health
        return getResourcesCurrentHealth(subject, aevs);
    }

    /**
     * Method findServersCurrentHealth
     *  
     * For the screens that rely on this API, the entId is either an 
     * application, a service or a group.
     * 
     * The population of the list varies with the type of appdef entity input.
     * 
     * This is used for all of the application monitoring screens; they all 
     * show a list with current health data for each server that 
     * participates in supplying services for an application.  
     * So if the entity is an application, the list is populated with servers 
     * that host the services on which the application relies.  The
     * timeframe is not used in this context, the list of servers is always
     * the current list.  The timeframe shall still be sent but it will be
     * bounded be the current time and current time - default time
     * window.
     * (see 2.1.2 - 2.1.2.1-3)
     * 
     * If the entId is a platform, the deployed servers view shows
     * the current health of servers in the timeframe that the metrics
     * are shown for.  So if the entity is application, expect to populate
     * the list based on the presence of metrics in the timeframe of 
     * interest.
     * (see 2.2.2.3, it shows deployed servers... I'll give you a dollar if
     * you can come up with a reason why we'd want internal servers.
     * We aren't managing cron or syslog, dude.) 
     * 
     * This is also used for a services' current health page in which case
     * the appdef entity is a service.
     * 
     * @param entId the platform's or application's ID
     * @return a list of ResourceDisplaySummary beans
     * @ejb:interface-method
     */
    public PageList findServersCurrentHealth(int sessionId,
                                             AppdefEntityID entId,
                                             PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException,
               InvalidAppdefTypeException, AppdefEntityNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        
        switch (entId.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                break;
            default:
                throw new InvalidAppdefTypeException(
                    "entityID is not valid type, id type: " + entId.getType());
        }
        
        AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
        PageList servers = rv.getAssociatedServers(pc);
        
        // Return a paged list of current health        
        final StopWatch watch = new StopWatch();
        final boolean debug = _log.isDebugEnabled();
        if (debug) watch.markTimeBegin("getResourcesCurrentHealth");
        PageList rtn = getResourcesCurrentHealth(subject, servers);
        if (debug) watch.markTimeEnd("getResourcesCurrentHealth");
        if (debug) _log.debug(watch);
        return rtn;
    }

    /**
     * Method findServersCurrentHealth
     *  
     * For platform's autogroup of servers.
     * 
     * If the entId is a platform, the deployed servers view shows
     * the current health of servers.
     *
     * @return a list of ResourceDisplaySummary beans
     * @ejb:interface-method
     */
    public List findAGServersCurrentHealthByType(int sessionId,
                                                 AppdefEntityID[] entIds,
                                                 Integer serverTypeId)
        throws SessionTimeoutException, SessionNotFoundException,
               InvalidAppdefTypeException, AppdefEntityNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        PageList servers = new PageList();
        PageControl pc = PageControl.PAGE_ALL;
        
        for (int i = 0; i < entIds.length; i++) {
            AppdefEntityID entId = entIds[i];
            if (entId.getType() != AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
                throw new InvalidAppdefTypeException(
                    "findServersCurrentHealthByType() only allows Platforms, " +
                    "id type: " + entId.getType());
            }
        
            AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
            servers.addAll(rv.getAssociatedServers(serverTypeId, pc));
        }
        
        // Return a paged list of current health        
        return getResourcesCurrentHealth(subject, servers);
    }

    /**
     * Return a ResourceDisplaySummary bean for each of the resource's services.
     * The only applicable resource is currently a compatible group (of
     * services...)
     * @return a list of ResourceDisplaySummary beans
     * @ejb:interface-method
     */
    public List findAGServicesCurrentHealthByType(int sessionId,
                                                  AppdefEntityID[] entIds,
                                                  Integer serviceTypeId)
        throws SessionTimeoutException, SessionNotFoundException,
               InvalidAppdefTypeException, AppdefEntityNotFoundException,
               PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        PageList services = new PageList();
        
        for (int i = 0; i < entIds.length; i++){
            AppdefEntityID entId = entIds[i];
            AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
            services.addAll(rv.getAssociatedServices(serviceTypeId,
                                                     PageControl.PAGE_ALL));
        }
    
        // Return a paged list of current health        
        return getResourcesCurrentHealth(subject, services);
    }
    
	 /**
     * Get Availability measurement for a given entitiy
     * @ejb:interface-method
     */
    public double getAvailability(AuthzSubject subj, AppdefEntityID id)
        throws AppdefEntityNotFoundException,
               PermissionException {
        if (id == null) {
            return MeasurementConstants.AVAIL_UNKNOWN;
        }
        final Map measCache =
            getMetricManager().getAvailMeasurements(Collections.singleton(id));
        Map availCache = null;
        if (id.isApplication()) {
            List members =
                getApplicationManager().getApplicationResources(subj, id.getId());
            availCache = getAvailManager().getLastAvail(members, measCache);
        }
        return getAvailability(subj, id, measCache, availCache);
    }

    /**
     * @param measCache optional cache of <Integer, List<Measurement>>
     *  Integer => Resource.getId(), may be null
     * @param availCache optional cache of <Integer, MetricValue>
     *  Integer => Measurement.getId(), may be null
     */
    private double getAvailability(AuthzSubject subject, AppdefEntityID id,
                                   Map measCache, Map availCache)
        throws AppdefEntityNotFoundException,
               PermissionException {
        StopWatch watch = new StopWatch();
        final boolean debug = _log.isDebugEnabled();
        if (debug) _log.debug("BEGIN getAvailability() id=" + id);
        try {
            if (id.isGroup()) {
                return getGroupAvailability(
                    subject, id.getId(), measCache, availCache);
            } else if (id.isApplication()) {
                AppdefEntityValue appVal = new AppdefEntityValue(id, subject);
                if (debug) watch.markTimeBegin("getFlattenedServiceIds");
                AppdefEntityID[] services = appVal.getFlattenedServiceIds();
                if (debug) watch.markTimeEnd("getFlattenedServiceIds");
                if (debug) watch.markTimeBegin("getAggregateAvailability");
                double rtn = getAggregateAvailability(
                    subject, services, measCache, availCache);
                if (debug) watch.markTimeEnd("getAggregateAvailability");
                return rtn;
            }
            AppdefEntityID[] ids = new AppdefEntityID[] { id };
            return getAvailability(
                subject, ids, getMidMap(ids, measCache), availCache)[0];
        } finally {
            if (debug) _log.debug("END getAvailability() id=" + id + " -- " + watch);
        }
    }

    /** Get the availability of the resource
     * @param id the Appdef entity ID
     * @ejb:interface-method
     */
    public double getAvailability(int sessionId, AppdefEntityID id)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
        return getAvailability(subject, id);
    }

    /** Get the availability of autogroup resources
     * @return a MetricValue for the availability
     * @ejb:interface-method
     */
    public double getAGAvailability(int sessionId, AppdefEntityID[] aids,
                                    AppdefEntityTypeID ctype)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = manager.getSubject(sessionId);
    
        StopWatch watch = new StopWatch();
        _log.debug("BEGIN getAGAvailability()");
    
        List appdefIds = getAGMemberIds(subject, aids, ctype);
        
        double ret = getAggregateAvailability(
            subject, toAppdefEntityIDArray(appdefIds), null, null);
        _log.debug("END getAGAvailability() -- " + watch.getElapsed() + " msec");
        return ret;
    }

    /**
     * Returns a list of problem metrics for an autogroup, return a summarized
     * list of UI beans
     * @throws SessionTimeoutException
     * @throws SessionNotFoundException
     * @throws AppdefEntityNotFoundException
     * @throws PermissionException
     * @throws InvalidAppdefTypeException
     * @throws AppdefCompatException
     * @ejb:interface-method
     */
    public List findAllMetrics(int sessionId, AppdefEntityID aeid,
                                   AppdefEntityTypeID ctype,
                                   long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException,
               AppdefCompatException, InvalidAppdefTypeException {
        return super.findAllMetrics(sessionId, aeid, ctype, begin, end);
    }

    /**
     * Returns a list of problem metrics for a resource, and the selected
     * children and hosts of that resource.  Return a summarized
     * list of UI beans
     * @ejb:interface-method
     */
    public List findAllMetrics(int sessionId, AppdefEntityID aeid,
                                   AppdefEntityID[] hosts,
                                   AppdefEntityTypeID[] children,
                                   AppdefEntityID[] members,
                                   long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException,
               AppdefCompatException, InvalidAppdefTypeException
    {
        List singlesList = new ArrayList();
        
        if (aeid != null && members == null)
            singlesList.add(aeid);
        
        // Next add its hosts
        if (hosts != null)
            singlesList.addAll(Arrays.asList(hosts));
    
        ArrayList result = new ArrayList();
        
        // Go through the singles list first
        for (Iterator it = singlesList.iterator(); it.hasNext(); ) {
            AppdefEntityID entity = (AppdefEntityID) it.next();
            result.addAll(findAllMetrics(sessionId, entity, begin, end));
        }                
    
        if (members != null) {
            // AutoGroups and groups pass their entities in the entities
            // parameter
            List metrics = findAllMetrics(sessionId, members, begin, end);
            for (Iterator it = metrics.iterator(); it.hasNext(); ) {
                ProblemMetricSummary metric =
                    (ProblemMetricSummary) it.next();
                if (aeid.isGroup())
                    metric.setSingleAppdefKey(aeid.getAppdefKey());
                else
                    metric.setMultipleAppdefKey(children[0].getAppdefKey());
            }
            
            result.addAll(metrics);
        }
        else if (children != null) {
            // Go through the children list next
            for (int i = 0; i < children.length; i++) {
                result.addAll(findAllMetrics(sessionId, aeid, children[i],
                                             begin, end));
            }
        }
        
        return result;
    }

    /**
     * Returns a list of problem metrics for a resource, and the selected
     * children and hosts of that resource.  Return a summarized
     * list of UI beans
     * @ejb:interface-method
     */
    public List findAllMetrics(int sessionId, AppdefEntityID aeid,
                                   AppdefEntityID[] hosts,
                                   AppdefEntityTypeID[] children,
                                   long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException,
               AppdefCompatException, InvalidAppdefTypeException
    {
        List singlesList = new ArrayList();
        
        if (aeid != null)
            singlesList.add(aeid);
        
        // Next add its hosts
        if (hosts != null)
            singlesList.addAll(Arrays.asList(hosts));
    
        ArrayList result = new ArrayList();
        
        if (aeid == null || (aeid.isGroup() && hosts != null)) {
            // AutoGroups and groups pass their entities in the hosts
            // parameter
            List metrics = findAllMetrics(sessionId, hosts, begin, end);
            
            for (Iterator it = metrics.iterator(); it.hasNext(); ) {
                ProblemMetricSummary metric =
                    (ProblemMetricSummary) it.next();
                if (aeid != null)
                    metric.setSingleAppdefKey(aeid.getAppdefKey());
                else
                    metric.setMultipleAppdefKey(children[0].getAppdefKey());
            }
            
            result.addAll(metrics);
        } else {
            // Go through the singles list first
            for (Iterator it = singlesList.iterator(); it.hasNext(); ) {
                AppdefEntityID entity = (AppdefEntityID) it.next();
                result.addAll(findAllMetrics(sessionId, entity,
                                             begin, end));
            }                
            
            // Go through the children list next
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    result.addAll(findAllMetrics(sessionId, aeid, children[i],
                                                 begin, end));
                }
            }
        }

        return result;
    }

    /**
     * Get the availability metric for a given resource
     * @ejb:interface-method
     */
    public Measurement findAvailabilityMetric(int sessionId,
                                                     AppdefEntityID id)
        throws SessionTimeoutException, SessionNotFoundException
    {
        final AuthzSubject subject = manager.getSubject(sessionId);
        return findAvailabilityMetric(subject, id);
    }
    
    public static MeasurementBossLocal getOne() {
        try {
            return MeasurementBossUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate()
        throws CreateException {
    }

    public void ejbActivate()
        throws EJBException, RemoteException {
    }

    public void ejbPassivate()
        throws EJBException, RemoteException {
    }

    public void ejbRemove()
        throws EJBException, RemoteException {
        ctx = null;
    }

    public void setSessionContext(SessionContext ctx)
        throws EJBException, RemoteException {
        this.ctx = ctx;
    }

    public SessionContext getSessionContext() {
        return ctx;
    }
}
