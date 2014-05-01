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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import org.hyperic.hq.appdef.shared.AppdefCompatException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceClusterValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.VirtualManager;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceRelation;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
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
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.management.shared.MeasurementInstruction;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.ext.ProblemMetricInfo;
import org.hyperic.hq.measurement.server.session.Baseline;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.ProblemMetricManager;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.Transformer;
import org.hyperic.util.collection.IntHashMap;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BizApp interface to the Measurement subsystem
 */
@Service
@Transactional
public class MeasurementBossImpl implements MeasurementBoss {
    private static final double AVAIL_WARN = MeasurementConstants.AVAIL_WARN;
    private static final double AVAIL_DOWN = MeasurementConstants.AVAIL_DOWN;
    private static final double AVAIL_PAUSED = MeasurementConstants.AVAIL_PAUSED;
    private static final double AVAIL_POWERED_OFF = MeasurementConstants.AVAIL_POWERED_OFF;
    private static final double AVAIL_UNKNOWN = MeasurementConstants.AVAIL_UNKNOWN;
    private static final double AVAIL_UP = MeasurementConstants.AVAIL_UP;
    private final Log log = LogFactory.getLog(MeasurementBossImpl.class);

    private final SessionManager sessionManager;
    private final AuthBoss authBoss;
    private final MeasurementManager measurementManager;
    private final TemplateManager templateManager;
    private final AvailabilityManager availabilityManager;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final PlatformManager platformManager;
    private final ResourceManager resourceManager;
    private final ResourceGroupManager resourceGroupManager;
    private final ServerManager serverManager;
    private final ServiceManager serviceManager;
    private final VirtualManager virtualManager;
    private final ApplicationManager applicationManager;
    private final ProblemMetricManager problemMetricManager;

    @Autowired
    public MeasurementBossImpl(SessionManager sessionManager, AuthBoss authBoss,
                               MeasurementManager measurementManager,
                               TemplateManager templateManager,
                               AvailabilityManager availabilityManager, DataManager dataManager,
                               ConfigManager configManager, PlatformManager platformManager,
                               ResourceManager resourceManager,
                               ResourceGroupManager resourceGroupManager,
                               ServerManager serverManager, ServiceManager serviceManager,
                               VirtualManager virtualManager, ApplicationManager applicationManager, 
                               ProblemMetricManager problemMetricManager) {
        this.sessionManager = sessionManager;
        this.authBoss = authBoss;
        this.measurementManager = measurementManager;
        this.templateManager = templateManager;
        this.availabilityManager = availabilityManager;
        this.dataManager = dataManager;
        this.configManager = configManager;
        this.platformManager = platformManager;
        this.resourceManager = resourceManager;
        this.resourceGroupManager = resourceGroupManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.virtualManager = virtualManager;
        this.applicationManager = applicationManager;
        this.problemMetricManager = problemMetricManager;
    }
    
    @Transactional(readOnly = true)
    public double getAvailabilityAverage(AppdefEntityID[] aeids, long begin, long end){
        
        final boolean debug = log.isDebugEnabled();
        
        Collection<Resource> resources = new ArrayList<Resource>();
        for(AppdefEntityID aeid:aeids){
            Resource resource = resourceManager.findResource(aeid);
            if (resource == null || resource.isInAsyncDeleteState()){
                continue;
            }
            resources.add(resource);
        }
        Map<Integer, List<Measurement>> measurements = measurementManager.getAvailMeasurements(resources);
        
        final List<Integer> mids = new ArrayList<Integer>();
        for(List<Measurement> measurementList : measurements.values()){
            if(measurementList == null){
                continue;
            }
            for (Measurement measurement: measurementList){
                if(measurement == null) {
                    continue;
                }
                mids.add(measurement.getId());
            }
        }
        
        Map<Integer, double[]> availData = 
            availabilityManager.getAggregateDataByTemplate(mids.toArray(new Integer[mids.size()]),  begin, end);
        //will get multi entries for mix group, so we need to average it    
        double sum = 0;
        // Fix HHQ-5678, HQ-4598: Ignore from AVAIL_UNKNOWN 
        int size = availData.size();
        double availabilityAverage = 0;

        for(Integer availDataKey : availData.keySet()){
            // Ignore from availData higher than 100%
            if (availData.get(availDataKey)[MeasurementConstants.IND_AVG] > MeasurementConstants.AVAIL_UP){
                sum += MeasurementConstants.AVAIL_UP;
            }else {
                // Collect the availData
                sum += availData.get(availDataKey)[MeasurementConstants.IND_AVG]; 
            }
        }
        
        if(size != 0){
            availabilityAverage = sum/size;
        }
        if (debug) log.debug("sum = [" + sum + "], size = [" + size + "], availablity average = [" + availabilityAverage + "]");
        
        return availabilityAverage;
    }
    
    @Transactional(readOnly = true)
    public double getAGAvailabilityAverage(int sessionId, AppdefEntityID aid, AppdefEntityTypeID ctype, 
                                           long begin, long end) throws AppdefEntityNotFoundException, PermissionException, SessionNotFoundException, SessionTimeoutException{
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        // Find the autogroup members
        List<AppdefEntityID> entIds = getAGMemberIds(subject,aid,ctype);
        return getAvailabilityAverage(entIds.toArray(new AppdefEntityID[entIds.size()]),begin,end);
    }
    
    private List<Measurement> findDesignatedMetrics(AuthzSubject subject, AppdefEntityID id,
                                                    Set<String> cats) {
        final List<Measurement> metrics;

        if (cats.size() == 1) {
            String cat = cats.iterator().next();
            metrics = measurementManager.findDesignatedMeasurements(subject, id, cat);
        } else {
            metrics = measurementManager.findDesignatedMeasurements(id);

            // Now iterate through and throw out the metrics we don't need
            for (Iterator<Measurement> it = metrics.iterator(); it.hasNext();) {
                Measurement dm = it.next();
                if (!cats.contains(dm.getTemplate().getCategory().getName()))
                    it.remove();
            }
        }

        return metrics;
    }

    /**
     * Get Autogroup member ids
     * 
     */
    @Transactional(readOnly = true)
    public AppdefEntityID[] getAutoGroupMemberIDs(AuthzSubject subject, AppdefEntityID[] aids,
                                                  AppdefEntityTypeID ctype)
        throws AppdefEntityNotFoundException, PermissionException {

        final List<AppdefEntityID> members = getAGMemberIds(subject, aids, ctype);
        return members.toArray(new AppdefEntityID[members.size()]);
    }

    /**
     * Update the default interval for a list of template ids
     * 
     */
    public void updateMetricDefaultInterval(int sessionId, Integer[] tids, long interval)
        throws SessionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        templateManager.updateTemplateDefaultInterval(subject, tids, interval);
    }

    /**
     * Update the templates to be indicators or not
     */
    public void updateIndicatorMetrics(int sessionId, AppdefEntityTypeID aetid, Integer[] tids)
    throws TemplateNotFoundException, SessionTimeoutException, SessionNotFoundException {
        String typeName = aetid.getAppdefResourceType().getName();
        templateManager.setDesignatedTemplates(typeName, tids);
    }

    /**
     * 
     * @return a PageList of MeasurementTemplateValue objects
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> findMeasurementTemplates(int sessionId,
                                                              AppdefEntityTypeID typeId,
                                                              String category, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException {
        sessionManager.authenticate(sessionId);
        String typeName = typeId.getAppdefResourceType().getName();
        return templateManager.findTemplates(typeName, category, new Integer[] {}, pc);
    }

    /**
     * 
     * @return a PageList of MeasurementTemplateValue objects based on entity
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> findMeasurementTemplates(int sessionId, AppdefEntityID aeid)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
        PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        AppdefEntityValue aev = new AppdefEntityValue(aeid, subject);

        String typeName = aev.getMonitorableType();
        return templateManager
            .findTemplates(typeName, null, new Integer[] {}, PageControl.PAGE_ALL);
    }

    /**
     * Retrieve list of measurement templates applicable to a monitorable type
     * 
     * @param mtype the monitorableType
     * @return a List of MeasurementTemplateValue objects
     * 
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> findMeasurementTemplates(int sessionId, String mtype,
                                                              PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException {
        sessionManager.authenticate(sessionId);
        return templateManager.findTemplates(mtype, null, null, pc);
    }

    /**
     * Retrieve list of measurement templates given specific IDs
     * 
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> findMeasurementTemplates(String user, Integer[] ids,
                                                              PageControl pc)
        throws LoginException, ApplicationException, ConfigPropertyException {
        int sessionId = authBoss.getUnauthSessionId(user);
        return findMeasurementTemplates(sessionId, ids, pc);
    }

    /**
     * Retrieve list of measurement templates given specific IDs
     * @return a List of MeasurementTemplateValue objects
     * 
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> findMeasurementTemplates(int sessionId, Integer[] ids,
                                                              PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, TemplateNotFoundException {
        sessionManager.authenticate(sessionId);
        return templateManager.getTemplates(ids, pc);
    }

    /**
     * Retrieve a measurement template given specific ID
     * 
     */
    @Transactional(readOnly = true)
    public MeasurementTemplate getMeasurementTemplate(int sessionId, Integer id)
        throws SessionNotFoundException, SessionTimeoutException, TemplateNotFoundException {
        sessionManager.authenticate(sessionId);
        return templateManager.getTemplate(id);
    }

    /**
     * Get the the availability metric template for the given autogroup
     * @return The availabililty metric template.
     * 
     */
    @Transactional(readOnly = true)
    public MeasurementTemplate getAvailabilityMetricTemplate(int sessionId, AppdefEntityID aid,
                                                             AppdefEntityTypeID ctype)
        throws SessionNotFoundException, SessionTimeoutException, MeasurementNotFoundException,
        AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        // Find the autogroup members
        List<AppdefEntityID> entIds = getAGMemberIds(subject, aid, ctype);

        for (AppdefEntityID aeId : entIds) {

            try {
                return getAvailabilityMetricTemplate(sessionId, aeId);
            } catch (MeasurementNotFoundException e) {
                // continue;
            }
        }

        // Throw a MeasurementNotFoundException here
        throw new MeasurementNotFoundException("Autogroup for : " + aid + " of type : " + ctype +
                                               " does not contain designated measurements");
    }

    /**
     * @param measCache Map<Integer, Measurement> may be null. Integer
     *        represents the AppdefEntityID.getId()
     */
    @Transactional(readOnly = true)
    private MeasurementTemplate getAvailabilityMetricTemplate(
                                                              AuthzSubject subj,
                                                              AppdefEntityID aeid,
                                                              Map<Integer, List<Measurement>> measCache)
        throws AppdefEntityNotFoundException, PermissionException, MeasurementNotFoundException {
        Measurement dm = null;
        if (aeid.isApplication()) {
            // Get the appointed front-end service
            AppdefEntityValue aeval = new AppdefEntityValue(aeid, subj);
            Application app = (Application) aeval.getResourcePOJO();

            Collection<AppService> appSvcs = app.getAppServices();
            for (AppService appSvc : appSvcs) {

                // Let's try it
                if (appSvc.isIsGroup()) {
                    if (appSvc.getResourceGroup() == null) {
                        continue;
                    }

                    aeid = AppdefEntityID.newGroupID(appSvc.getResourceGroup().getId());
                } else {
                    // Make sure this is a valid service
                    if (appSvc.getService() == null) {
                        continue;
                    }

                    // Get the metrics for the service
                    aeid = appSvc.getService().getEntityId();
                }

                dm = findAvailabilityMetric(subj, aeid, measCache);

                if (dm != null) {
                    break;
                }
            }
        } else if (aeid.isGroup()) {
            List<AppdefEntityID> grpMembers = getResourceIds(subj, aeid, null);

            // Go through the group members and return the first measurement
            // that we find
            for (Iterator<AppdefEntityID> it = grpMembers.iterator(); it.hasNext();) {
                aeid = it.next();
                dm = findAvailabilityMetric(subj, aeid, measCache);
                if (dm != null) {
                    break;
                }
            }
        } else {
            dm = findAvailabilityMetric(subj, aeid, measCache);
        }

        if (dm != null) {
            return dm.getTemplate();
        } else {
            throw new MeasurementNotFoundException("Availability metric not found for " + aeid);
        }
    }

    private Measurement findAvailabilityMetric(AuthzSubject subj, AppdefEntityID aeid,
                                               Map<Integer, List<Measurement>> measCache) {
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
            dm = measurementManager.getAvailabilityMeasurement(subj, aeid);
        }
        return dm;
    }

    /**
     * Get the the availability metric template for the given resource
     * @return template of availabililty metric
     * 
     */
    @Transactional(readOnly = true)
    public MeasurementTemplate getAvailabilityMetricTemplate(int sessionId, AppdefEntityID aeid)
        throws MeasurementNotFoundException, SessionNotFoundException, SessionTimeoutException,
        AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        return getAvailabilityMetricTemplate(subject, aeid, null);
    }

    /**
     * Get the the designated measurement template for the given resource and
     * corresponding category.
     * @return Array of Measurement IDs
     * 
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> getDesignatedTemplates(int sessionId, AppdefEntityID id,
                                                            Set<String> cats)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        PermissionException {
        List<Measurement> metrics;
        try {
            metrics = getDesignatedMetrics(sessionId, id, cats);
        } catch (MeasurementNotFoundException e) {
            return new ArrayList<MeasurementTemplate>(0);
        }

        ArrayList<MeasurementTemplate> tmpls = new ArrayList<MeasurementTemplate>(metrics.size());
        for (Measurement dm : metrics) {

            tmpls.add(dm.getTemplate());
        }

        return tmpls;
    }

    /**
     * Get the the designated measurement template for the autogroup given a
     * type and corresponding category.
     * @param ctype the AppdefEntityTypeID of the AG members
     * @return Array of Measuremnt ids
     * 
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> getAGDesignatedTemplates(int sessionId, AppdefEntityID[] aids,
                                                              AppdefEntityTypeID ctype,
                                                              Set<String> cats)
        throws SessionNotFoundException, SessionTimeoutException, MeasurementNotFoundException,
        AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        // Find the autogroup members
        List<AppdefEntityID> entIds = getAGMemberIds(subject, aids, ctype);

        for (AppdefEntityID aeId : entIds) {

            List<MeasurementTemplate> templs = getDesignatedTemplates(sessionId, aeId, cats);
            if (templs.size() > 0) {
                return templs;
            }
        }

        // Throw a MeasurementNotFoundException here
        throw new MeasurementNotFoundException("Autogroup for : " + aids + " of type : " + ctype +
                                               " does not contain designated measurements");
    }

    /**
     * Create list of measurements for a resource
     * @param id the resource ID
     */
    private void createMeasurements(int sessionId, AppdefEntityID id, Integer[] tids, long interval)
        throws SessionTimeoutException, SessionNotFoundException, ConfigFetchException,
        EncodingException, PermissionException, TemplateNotFoundException,
        AppdefEntityNotFoundException, GroupNotCompatibleException, MeasurementCreateException,
        MeasurementNotFoundException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        if (id.isGroup()) {
            // Recursively do this for each of the group members
            List<AppdefEntityID> grpMembers = getResourceIds(subject, id, null);

            AppdefEntityID[] aeids = grpMembers.toArray(new AppdefEntityID[grpMembers.size()]);
            measurementManager.enableMeasurements(subject, aeids, tids, interval);
        } else {
            ConfigResponse mergedCR = configManager.getMergedConfigResponse(subject,
                ProductPlugin.TYPE_MEASUREMENT, id, true);

            if (interval > 0) {
                long[] intervals = new long[tids.length];
                Arrays.fill(intervals, interval);

                measurementManager.createMeasurements(subject, id, tids, intervals, mergedCR);
            } else {
                measurementManager.createMeasurements(subject, id, tids, mergedCR);
            }
        }
    }
    
    public void createMeasurements(AuthzSubject subject, Resource resource, Collection<MeasurementInstruction> measurementInstructions) 
            throws SessionTimeoutException, SessionNotFoundException, ConfigFetchException,
            EncodingException, PermissionException, TemplateNotFoundException,
            AppdefEntityNotFoundException, MeasurementCreateException    {
        AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(resource);
        ConfigResponse mergedCR = configManager.getMergedConfigResponse(subject,
                ProductPlugin.TYPE_MEASUREMENT, aeid , true);
                measurementManager.createOrUpdateOrDeleteMeasurements(subject,
                        resource, aeid, measurementInstructions, mergedCR);
    }

    /**
     * Update the measurements - set the interval
     * @param id the resource ID
     * @param tids the array of template ID's
     * @param interval the new interval value
     * 
     */
    public void updateMeasurements(int sessionId, AppdefEntityID id, Integer[] tids, long interval)
        throws MeasurementNotFoundException, SessionTimeoutException, SessionNotFoundException,
        TemplateNotFoundException, AppdefEntityNotFoundException, GroupNotCompatibleException,
        MeasurementCreateException, ConfigFetchException, PermissionException, EncodingException {
        createMeasurements(sessionId, id, tids, interval);
    }

    /**
     * Update measurements for the members of an autogroup
     * @param parentid - the parent resource of the autogroup
     * @param ctype - the type of child resource
     * @param tids - template ids to update
     * @param interval - the interval to set
     * 
     */
    public void updateAGMeasurements(int sessionId, AppdefEntityID parentid,
                                     AppdefEntityTypeID ctype, Integer[] tids, long interval)
        throws MeasurementNotFoundException, SessionTimeoutException, SessionNotFoundException,
        TemplateNotFoundException, AppdefEntityNotFoundException, GroupNotCompatibleException,
        MeasurementCreateException, ConfigFetchException, PermissionException, EncodingException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        List<AppdefEntityID> kids = getAGMemberIds(subject, parentid, ctype);
        for (int i = 0; i < kids.size(); i++) {
            // Do create, because we want to create or update
            AppdefEntityID kid = kids.get(i);
            createMeasurements(sessionId, kid, tids, interval);
        }
    }

    /**
     * Disable all measurements for an instance
     * @param id the resource's ID
     * 
     */
    public void disableMeasurements(int sessionId, AppdefEntityID id)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        measurementManager.disableMeasurements(subject, id);
    }

    /**
     * Disable all measurements for a resource
     * @param id the resource's ID
     * @param tids the array of measurement ID's
     * 
     */
    public void disableMeasurements(int sessionId, AppdefEntityID id, Integer[] tids)
        throws SessionException, AppdefEntityNotFoundException, GroupNotCompatibleException,
        PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        if (id == null) {
            templateManager.setTemplateEnabledByDefault(subject, tids, false);
        } else if (id.isGroup()) {
            // Recursively do this for each of the group members
            List<AppdefEntityID> grpMembers = getResourceIds(subject, id, null);

            for (Iterator<AppdefEntityID> it = grpMembers.iterator(); it.hasNext();) {
                measurementManager.disableMeasurements(subject, it.next(), tids);
            }
        } else {
            measurementManager.disableMeasurements(subject, id, tids);
        }
    }

    /**
     * Disable all measurements for a resource
     * @param tids the array of measurement ID's
     * 
     */
    public void disableAGMeasurements(int sessionId, AppdefEntityID parentId,
                                      AppdefEntityTypeID childType, Integer[] tids)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        List<AppdefEntityID> grpMembers = getAGMemberIds(subject, parentId, childType);

        for (Iterator<AppdefEntityID> it = grpMembers.iterator(); it.hasNext();) {
            measurementManager.disableMeasurements(subject, it.next(), tids);
        }
    }

    /**
     * Get the the designated measurement for the given resource and
     * corresponding category.
     * @return Array of Measurement IDs
     */
    private List<Measurement> getDesignatedMetrics(int sessionId, AppdefEntityID id,
                                                   Set<String> cats)
        throws SessionNotFoundException, SessionTimeoutException, MeasurementNotFoundException,
        AppdefEntityNotFoundException, PermissionException {
        if (cats.size() == 0) {
            throw new MeasurementNotFoundException("No categories specified");
        }

        AuthzSubject subj = sessionManager.getSubject(sessionId);

        List<Measurement> metrics = null;
        if (id.isApplication()) {
            // Get the appointed front-end service
            AppdefEntityValue aeval = new AppdefEntityValue(id, subj);
            Application app = (Application) aeval.getResourcePOJO();

            Collection<AppService> appSvcs = app.getAppServices();
            for (AppService appSvc : appSvcs) {

                // Let's try it
                if (appSvc.isIsGroup()) {
                    if (appSvc.getResourceGroup() == null) {
                        continue;
                    }

                    id = AppdefEntityID.newGroupID(appSvc.getResourceGroup().getId());
                } else {
                    // Make sure this is a valid service
                    if (appSvc.getService() == null) {
                        continue;
                    }

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
                } else if (metrics == null && cats.contains(MeasurementConstants.CAT_AVAILABILITY)) {
                    Measurement dm = measurementManager.getAvailabilityMeasurement(subj, id);

                    if (dm != null) {
                        metrics = Collections.singletonList(dm);
                    }
                }
            }
        } else if (id.isGroup()) {
            List<AppdefEntityID> grpMembers = getResourceIds(subj, id, null);

            // Go through the group members and return the first measurement
            // that we find
            for (Iterator<AppdefEntityID> it = grpMembers.iterator(); it.hasNext();) {
                metrics = findDesignatedMetrics(subj, it.next(), cats);

                if (metrics != null && metrics.size() > 0) {
                    break;
                }
            }
        } else {
            metrics = findDesignatedMetrics(subj, id, cats);
        }

        // Make sure we have valid metrics
        if (metrics == null || metrics.size() == 0) {
            throw new MeasurementNotFoundException("Designated metric not found for " + id);
        }

        // Now iterate through and throw out the metrics we don't need
        for (Iterator<Measurement> it = metrics.iterator(); it.hasNext();) {
            Measurement dm = it.next();
            if (!cats.contains(dm.getTemplate().getCategory().getName())) {
                it.remove();
            }
        }

        return metrics;
    }

    /**
     * Find a measurement using measurement id
     * @param id measurement id
     * 
     */
    @Transactional(readOnly = true)
    public Measurement getMeasurement(int sessionID, Integer id) throws SessionTimeoutException,
        SessionNotFoundException, MeasurementNotFoundException {
        return measurementManager.getMeasurement(id);
    }

    /**
     * Get the last metric values for the given template IDs.
     * 
     * @param tids The template IDs to get
     */
    @Transactional(readOnly = true)
    public MetricValue[] getLastMetricValue(int sessionId, AppdefEntityID aeid, Integer[] tids)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        List<Measurement> measurements = new ArrayList<Measurement>(tids.length);
        long interval = 0;
        for (int i = 0; i < tids.length; i++) {
            try {
                Measurement m = measurementManager.findMeasurement(subject, tids[i], aeid);
                measurements.add(m);
                interval = Math.max(interval, m.getInterval());
            } catch (MeasurementNotFoundException e) {
                measurements.add(null);
            }
        }
        List<Integer> mids = new Transformer<Measurement, Integer>() {
            @Override
            public Integer transform(Measurement m) {
                return m.getId();
            }
        }.transform(measurements);
        return getLastMetricValue(sessionId, mids, interval);
    }

    /**
     * Get the last metric data for the array of measurement ids.
     * 
     * @param measurements The List of Measurements to get metrics for
     * @param interval The allowable time in ms to go back looking for data.
     * 
     */
    @Transactional(readOnly = true)
    public MetricValue[] getLastMetricValue(int sessionId, List<Integer> measurements, long interval) {
        MetricValue[] ret = new MetricValue[measurements.size()];
        long after = System.currentTimeMillis() - (3 * interval);
        Map<Integer, MetricValue> data = dataManager.getLastDataPoints(measurements, after);
        int i=0;
        for (final Integer mid : measurements) {
            if (mid != null && data.containsKey(mid)) {
                ret[i++] = data.get(mid);
            }
        }
        return ret;
    }

    /**
     * Get the last indicator metric values
     * 
     */
    @Transactional(readOnly = true)
    public Map<Integer, MetricValue> getLastIndicatorValues(Integer sessionId, AppdefEntityID aeid) {

        List<Measurement> metrics = measurementManager.findDesignatedMeasurements(aeid);
        long interval = 0;
        for (Iterator<Measurement> it = metrics.iterator(); it.hasNext();) {
            Measurement m = it.next();
            if (m.getTemplate().getAlias().equalsIgnoreCase("Availability")) {
                it.remove();
            } else {
                interval = Math.max(interval, m.getInterval());
            }
        }

        Collection<Integer> mids = new ArrayList<Integer>(metrics.size());
        for (final Measurement m : metrics) {
            mids.add(m.getId());
        }

        final long after = System.currentTimeMillis() - (3 * interval);
        Map<Integer, MetricValue> data = new HashMap<Integer, MetricValue>();
        dataManager.getCachedDataPoints(mids, data, after);

        Map<Integer, MetricValue> ret = new HashMap<Integer, MetricValue>(data.size());
        for (Measurement m : metrics) {

            if (data.containsKey(m.getId())) {
                ret.put(m.getTemplate().getId(), data.get(m.getId()));
            }
        }

        return ret;
    }

    /*
     * Private function to find the group measurements
     */
    @Transactional(readOnly = true)
    private PageList<MetricConfigSummary> findGroupMeasurements(int sessionId, AppdefEntityID gid,
                                                                String cat, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        List<AppdefEntityID> grpMembers = getResourceIds(subject, gid, null);
        return findGroupMeasurements(sessionId, grpMembers, cat, pc);
    }

    /*
     * Private function to find the group measurements
     */
    @Transactional(readOnly = true)
    private PageList<MetricConfigSummary> findGroupMeasurements(int sessionId,
                                                                List<AppdefEntityID> grpMembers,
                                                                String cat, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        String mtype = null;
        IntHashMap summaryMap = new IntHashMap();
        for (AppdefEntityID id : grpMembers) {

            // Get the monitorable type from the first ID
            if (mtype == null) {
                AppdefEntityValue aeVal = new AppdefEntityValue(id, subject);
                mtype = aeVal.getMonitorableType();
            }

            // Get the list of measurements for this resource
            List<Measurement> metrics = measurementManager.findMeasurements(subject, id, cat,
                PageControl.PAGE_ALL);

            // Iterate through the measurements to get the interval
            for (Measurement m : metrics) {

                MeasurementTemplate tmpl = m.getTemplate();
                GroupMetricDisplaySummary gmds = (GroupMetricDisplaySummary) summaryMap.get(tmpl
                    .getId().intValue());
                if (gmds == null) {
                    gmds = new GroupMetricDisplaySummary(tmpl.getId().intValue(), tmpl.getName(),
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
            return new PageList<MetricConfigSummary>();
        }

        List<MeasurementTemplate> tmpls = templateManager.findTemplates(mtype, cat, null, pc);

        PageList<MetricConfigSummary> result = new PageList<MetricConfigSummary>();
        for (MeasurementTemplate tmpl : tmpls) {

            GroupMetricDisplaySummary gmds = (GroupMetricDisplaySummary) summaryMap.get(tmpl
                .getId().intValue());

            if (gmds == null) {
                gmds = new GroupMetricDisplaySummary(tmpl.getId().intValue(), tmpl.getName(), tmpl
                    .getCategory().getName());
            }

            // Set the total number
            gmds.setTotalMembers(grpMembers.size());

            result.add(gmds);
        }

        // Total size is equal to the total size of the templates
        result.setTotalSize(tmpls.size());

        return result;
    }

    /**
     * Retrieve a Measurement for a specific instance
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Measurement findMeasurement(int sessionId, Integer tid, AppdefEntityID id)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException,
        MeasurementNotFoundException, AppdefEntityNotFoundException {
        if (id.isGroup())
            return findMeasurements(sessionId, tid, new AppdefEntityID[] { id }).get(
                0);

        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        return measurementManager.findMeasurement(subject, tid, id);
    }

    /**
     * Retrieve List of measurements for a specific instance
     * @return List of Measurement objects
     * 
     */
    @Transactional(readOnly = true)
    public List findMeasurements(int sessionId, AppdefEntityID id, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, PermissionException {
        if (id.isGroup())
            return findGroupMeasurements(sessionId, id, null, pc);

        if (id instanceof AppdefEntityTypeID)
            return new ArrayList<Measurement>(0);

        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        return measurementManager.findMeasurements(subject, id, null, pc);
    }

    /**
     * Retrieve list of measurements for a specific template and entities
     * 
     * @param tid the template ID
     * @param entIds the array of entity IDs
     * @return a List of Measurement objects
     * 
     */
    @Transactional(readOnly = true)
    public List<Measurement> findMeasurements(int sessionId, Integer tid, AppdefEntityID[] entIds)
        throws SessionTimeoutException, SessionNotFoundException, MeasurementNotFoundException,
        AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        List ids = new ArrayList();

        // Form an array of ID's
        int type = entIds[0].getType();
        for (int i = 0; i < entIds.length; i++) {
            if (entIds[i].getType() != type)
                throw new MeasurementNotFoundException("Entity type != " + type);

            if (entIds[i].isGroup()) {
                AppdefEntityID[] memberIds = getGroupMemberIDs(subject, entIds[i].getId());
                ids.addAll(Arrays.asList(memberIds));
            } else {
                ids.add(entIds[i].getId());
            }
        }
        return measurementManager.findMeasurements(subject, tid, entIds);
    }

    /**
     * Get the enabled measurements for an auto group
     * @param parentId - the parent resource appdefEntityID
     * @param childType - the type of child in the autogroup
     * @return a PageList of Measurement objects
     * 
     */
    @Transactional(readOnly = true)
    public List<MetricConfigSummary> findEnabledAGMeasurements(int sessionId,
                                                               AppdefEntityID parentId,
                                                               AppdefEntityTypeID childType,
                                                               String cat, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        return findGroupMeasurements(sessionId, getAGMemberIds(subject, parentId, childType), cat,
            pc);
    }

    /**
     * Retrieve list of measurements for a specific instance and category
     * @return a PageList of Measurement objects
     * 
     */
    @Transactional(readOnly = true)
    public PageList<MetricConfigSummary> findEnabledMeasurements(int sessionId, AppdefEntityID id,
                                                                 String cat, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        if (id.isGroup())
            return findGroupMeasurements(sessionId, id, cat, pc);

        AppdefEntityValue aeVal = new AppdefEntityValue(id, subject);
        String mtype = aeVal.getMonitorableType();

        IntHashMap dmvs = new IntHashMap();
        List<Measurement> enabledMetrics = measurementManager.findEnabledMeasurements(subject, id,
            cat);

        for (Measurement dmv : enabledMetrics) {

            dmvs.put(dmv.getTemplate().getId().intValue(), dmv);
        }

        // Create MetricConfigSummary beans
        List<MeasurementTemplate> tmpls = templateManager.findTemplates(mtype, cat, null, pc);
        ArrayList<MetricConfigSummary> beans = new ArrayList<MetricConfigSummary>(tmpls.size());
        for (MeasurementTemplate mtv : tmpls) {

            MetricConfigSummary mcs = new MetricConfigSummary(mtv.getId().intValue(),
                mtv.getName(), cat);

            Measurement dmv = (Measurement) dmvs.get(mtv.getId().intValue());
            if (dmv != null) {
                mcs.setInterval(dmv.getInterval());
            }

            beans.add(mcs);
        }

        return new PageList<MetricConfigSummary>(beans, tmpls.size());
    }

    /**
     * Dumps data for a specific measurement
     * @return a PageList of MetricValue objects
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> findMeasurementData(int sessionId, Measurement m,
                                                            long begin, long end, PageControl pc) {
        return dataManager.getHistoricalData(m, begin, end, pc);
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
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> findMeasurementData(int sessionId, Integer tid,
                                                            AppdefEntityID aid, long begin,
                                                            long end, long interval,
                                                            boolean returnNulls, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        PermissionException, MeasurementNotFoundException {

        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        MeasurementTemplate tmpl = templateManager.getTemplate(tid);

        List<Measurement> measurements = getMeasurementsForResource(subject, aid, tmpl);
        if (measurements == null || measurements.size() == 0) {
            throw new MeasurementNotFoundException("There is no measurement for " + aid +
                                                   " with template " + tid);
        }

        return dataManager.getHistoricalData(measurements, begin, end, interval, tmpl
            .getCollectionType(), returnNulls, pc);
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
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> findMeasurementData(int sessionId, Integer tid,
                                                            AppdefEntityID aid,
                                                            AppdefEntityTypeID ctype, long begin,
                                                            long end, long interval,
                                                            boolean returnNulls, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        PermissionException, MeasurementNotFoundException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        // Find the autogroup members
        List<AppdefEntityID> entIds = getAGMemberIds(subject, aid, ctype);

        return findMeasurementData(sessionId, tid, entIds, begin, end, interval, returnNulls, pc);
    }

    /**
     * Dumps data for a specific measurement template for an auto-group based on
     * an interval.
     * 
     * @param tid the measurement template id
     * @param begin start of interval
     * @param end end of interval
     * @param interval the interval
     * @param returnNulls whether or not to return nulls associated with the
     *        platform
     * @return a PageList of MetricValue objects
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> findMeasurementData(int sessionId, Integer tid,
                                                            List<AppdefEntityID> entIds,
                                                            long begin, long end, long interval,
                                                            boolean returnNulls, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        MeasurementTemplate tmpl = templateManager.getTemplate(tid);

        // Find the measurement IDs of the members in the autogroup for the
        // template
        AppdefEntityID[] ids = entIds.toArray(new AppdefEntityID[entIds.size()]);

        List<Measurement> measurements = measurementManager.findMeasurements(subject, tid, ids);

        return dataManager.getHistoricalData(measurements, begin, end, interval, tmpl
            .getCollectionType(), returnNulls, pc);
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
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> findMeasurementData(String user, AppdefEntityID aid,
                                                            MeasurementTemplate tmpl, long begin,
                                                            long end, long interval,
                                                            boolean returnNulls, PageControl pc)
        throws LoginException, ApplicationException, ConfigPropertyException {
        int sessionId = authBoss.getUnauthSessionId(user);
        return findMeasurementData(sessionId, aid, tmpl, begin, end, interval, returnNulls, pc);
    }

    /**
     * Dumps data for a specific measurement template for an instance based on
     * an interval
     * @param aid the AppdefEntityID
     * @param tmpl the complete MeasurementTemplate value object
     * @param begin the beginning of the time range
     * @param end the end of the time range
     * @param interval the time interval at which the data should be calculated
     * @param returnNulls whether or not nulls should be inserted for no data
     * @return a PageList of MetricValue objects
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> findMeasurementData(int sessionId, AppdefEntityID aid,
                                                            MeasurementTemplate tmpl, long begin,
                                                            long end, long interval,
                                                            boolean returnNulls, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        PermissionException, MeasurementNotFoundException {

        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();

        List<Measurement> measurements;

        if (aid.isApplication() && tmpl.isAvailability()) {
            // Special case for application availability
            log.debug("BEGIN findMeasurementData()");

            AppdefEntityValue aeval = new AppdefEntityValue(aid, subject);

            // Get the flattened list of services
            if (debug)
                watch.markTimeBegin("getFlattenedServiceIds");
            AppdefEntityID[] serviceIds = aeval.getFlattenedServiceIds();

            if (debug)
                watch.markTimeEnd("getFlattenedServiceIds");
            if (debug)
                watch.markTimeBegin("findDesignatedMeasurements");
            Map<AppdefEntityID, Measurement> midMap = measurementManager
                .findDesignatedMeasurements(subject, serviceIds,
                    MeasurementConstants.CAT_AVAILABILITY);
            if (debug)
                watch.markTimeEnd("findDesignatedMeasurements");
            measurements = new ArrayList<Measurement>(midMap.values());
        } else {
            measurements = getMeasurementsForResource(subject, aid, tmpl);
            if (measurements == null || measurements.size() == 0) {
                throw new MeasurementNotFoundException("There is no measurement for " + aid +
                                                       " with template " + tmpl.getId());
            }
        }

        if (debug)
            watch.markTimeBegin("getHistoricalData");
        PageList<HighLowMetricValue> rtn = dataManager.getHistoricalData(measurements, begin, end,
            interval, tmpl.getCollectionType(), returnNulls, pc);
        if (debug)
            watch.markTimeEnd("getHistoricalData");
        if (debug)
            log.debug(watch);
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
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> findAGMeasurementData(String user, AppdefEntityID[] aids,
                                                              MeasurementTemplate tmpl,
                                                              AppdefEntityTypeID ctype, long begin,
                                                              long end, long interval,
                                                              boolean returnNulls, PageControl pc)
        throws LoginException, ApplicationException, ConfigPropertyException {
        int sessionId = authBoss.getUnauthSessionId(user);
        return findAGMeasurementData(sessionId, aids, tmpl, ctype, begin, end, interval,
            returnNulls, pc);
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
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> findAGMeasurementData(int sessionId, AppdefEntityID[] aids,
                                                              MeasurementTemplate tmpl,
                                                              AppdefEntityTypeID ctype, long begin,
                                                              long end, long interval,
                                                              boolean returnNulls, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        PermissionException, MeasurementNotFoundException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        // Find the autogroup members
        List<AppdefEntityID> entIds = getAGMemberIds(subject, aids, ctype);

        // Find the measurement IDs of the members in the autogroup for the
        // template
        AppdefEntityID[] ids = entIds.toArray(new AppdefEntityID[entIds.size()]);

        List<Measurement> measurements = measurementManager.findMeasurements(subject, tmpl.getId(),
            ids);

        return dataManager.getHistoricalData(measurements, begin, end, interval, tmpl
            .getCollectionType(), returnNulls, pc);
    }

    /**
     * Returns metadata for particular measurement
     * 
     */
    @Transactional(readOnly = true)
    public List<MeasurementMetadataSummary> findMetricMetadata(int sessionId, AppdefEntityID aid,
                                                               AppdefEntityTypeID ctype, Integer tid)
        throws SessionNotFoundException, SessionTimeoutException, GroupNotCompatibleException,
        AppdefEntityNotFoundException, ApplicationNotFoundException, TemplateNotFoundException,
        PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        // Get the template
        templateManager.getTemplate(tid);

        final List<AppdefResourceValue> entities;

        if (aid.isGroup()) {
            List<AppdefEntityID> memberIds = GroupUtil.getCompatGroupMembers(subject, aid, null);
            entities = new ArrayList<AppdefResourceValue>();
            for (AppdefEntityID anId : memberIds) {

                AppdefEntityValue aev = new AppdefEntityValue(anId, subject);
                entities.add(aev.getResourceValue());
            }
        } else if (ctype != null) {
            // if a child type was specified, then the template must be
            // intended for the autogroup of children of that type
            AppdefEntityValue entVal = new AppdefEntityValue(aid, subject);
            switch (ctype.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    entities = entVal.getAssociatedPlatforms(PageControl.PAGE_ALL);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    // Get server IDs
                    entities = entVal.getAssociatedServers(ctype.getId(), PageControl.PAGE_ALL);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    // Get service IDs
                    entities = entVal.getAssociatedServices(ctype.getId(), PageControl.PAGE_ALL);
                    break;
                default:
                    throw new IllegalArgumentException(
                        "Unable to determine autogroup members for appdef type: " + aid.getType());
            }
        } else {
            AppdefEntityValue entVal = new AppdefEntityValue(aid, subject);
            entities = Collections.singletonList(entVal.getResourceValue());
        }

        AppdefEntityID[] aeids = new AppdefEntityID[entities.size()];
        IntHashMap resourceMap = new IntHashMap();
        Iterator<AppdefResourceValue> iter = entities.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            AppdefResourceValue resource = iter.next();
            aeids[i] = resource.getEntityId();
            resourceMap.put(aeids[i].getID(), resource);
        }

        // get measurement summaries, enriched with metadata
        // tastes good and it's good for you
        List<MeasurementMetadataSummary> mds = new ArrayList<MeasurementMetadataSummary>();
        List<Measurement> mms = measurementManager.findMeasurements(subject, tid, aeids);
        for (Measurement mm : mms) {

            Integer instanceId = mm.getInstanceId();
            AppdefResourceValue resource = (AppdefResourceValue) resourceMap.get(instanceId
                .intValue());

            // Fetch the last data point
            MetricValue mv = dataManager.getLastHistoricalData(mm);

            MeasurementMetadataSummary summary = new MeasurementMetadataSummary(mm, mv, resource);
            mds.add(summary);
        }
        return mds;
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
     * @see org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary
     * 
     */
    @Transactional(readOnly = true)
    public MetricDisplaySummary findMetric(int sessionId, AppdefEntityID aeid,
                                           AppdefEntityTypeID ctype, Integer tid, long begin,
                                           long end) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, AppdefEntityNotFoundException,
        AppdefCompatException, MeasurementNotFoundException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        List<AppdefEntityID> resources = getResourceIds(subject, aeid, ctype);

        return findMetric(sessionId, resources, tid, begin, end);
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
     * @see org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary
     * 
     */
    @Transactional(readOnly = true)
    public MetricDisplaySummary findMetric(int sessionId, List resources, Integer tid, long begin,
                                           long end) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, AppdefEntityNotFoundException,
        AppdefCompatException, MeasurementNotFoundException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        // Just one metric
        final List<Integer> mtids = Collections.singletonList(tid);

        // Look up the metric summaries of all associated resources
        Map<String, Set<MetricDisplaySummary>> results = getResourceMetrics(subject, resources,
            mtids, begin, end, null);

        // Should only be one
        if (log.isDebugEnabled()) {
            log.debug("getResourceMetrics() returned " + results.size());
        }

        if (results.size() > 0) {
            Iterator<Set<MetricDisplaySummary>> it = results.values().iterator();
            Collection<MetricDisplaySummary> coll = it.next();
            Iterator<MetricDisplaySummary> itr = coll.iterator();
            MetricDisplaySummary summary = itr.next();

            return summary;
        }

        return null;
    }

    /**
     * Prunes from the list of passed-in AppdefEntityValue array those resources
     * that are not collecting the metric corresponding to the given template
     * id.
     * @param resources the resources
     * @param tid the metric template id
     * @return an array of resources
     * 
     */
    public AppdefResourceValue[] pruneResourcesNotCollecting(int sessionId,
                                                             AppdefResourceValue[] resources,
                                                             Integer tid)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        MeasurementNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        MeasurementTemplate tmpl = templateManager.getTemplate(tid);

        List<AppdefResourceValue> pruned = new ArrayList<AppdefResourceValue>();
        for (int i = 0; i < resources.length; ++i) {
            List<Measurement> measurements = getMeasurementsForResource(subject, resources[i]
                .getEntityId(), tmpl);
            if (measurements.size() > 0) {
                pruned.add(resources[i]);
            }
        }
        AppdefResourceValue[] prunedResources = new AppdefResourceValue[pruned.size()];
        return pruned.toArray(prunedResources);
    }

    private List<Measurement> getMeasurementsForResource(AuthzSubject subject, AppdefEntityID aid,
                                                         MeasurementTemplate tmpl)
        throws PermissionException, AppdefEntityNotFoundException, MeasurementNotFoundException {
        // Find the measurement ID based on entity type
        if (aid.isApplication()) {
            AppdefEntityValue aeval = new AppdefEntityValue(aid, subject);

            Application app = (Application) aeval.getResourcePOJO();
            Collection<AppService> appSvcs = app.getAppServices();

            // If it's availability, then we'd have to get data for all svcs
            for (AppService appSvc : appSvcs) {

                // Metric is based on the entry point
                if (appSvc.isEntryPoint()) {
                    AppdefEntityID id;

                    if (appSvc.isIsGroup()) {
                        id = AppdefEntityID.newGroupID(appSvc.getResourceGroup().getId());
                    } else {
                        id = appSvc.getService().getEntityId();
                    }

                    try {
                        return getMeasurementsForResource(subject, id, tmpl);
                    } catch (MeasurementNotFoundException ignore) {
                    }
                }
            }

            throw new MeasurementNotFoundException("No entry point found: " + aid);
        } else if (aid.isGroup()) {
            if (tmpl.isAvailability()) {
                final ResourceGroup group = resourceGroupManager.findResourceGroupById(aid.getId());
                final Map<Integer, List<Measurement>> mmap =
                    measurementManager.getAvailMeasurements(Collections.singleton(group));
                final List<Measurement> metrics = mmap.get(group.getResource().getId());
                for (final Iterator<Measurement> it = metrics.iterator(); it.hasNext();) {
                    Measurement m = it.next();
                    if (!m.getTemplate().equals(tmpl)) {
                        it.remove();
                    }
                }
                return metrics;
            } else {
                AppdefEntityID[] ids = getGroupMemberIDs(subject, aid.getId());

                // Get the list of measurements
                return measurementManager.findMeasurements(subject, tmpl.getId(), ids);
            }
        } else {
            AppdefEntityID[] aids = { aid };
            return measurementManager.findMeasurements(subject, tmpl.getId(), aids);
        }
    }

    /**
     * Get a List of AppdefEntityIDs for the given resource.
     * @param subject The user to use for searches.
     * @param aeid The entity in question.
     * @param ctype The entity type in question.
     * @return A List of AppdefEntityIDs for the given resource.
     */
    private List<AppdefEntityID> getResourceIds(AuthzSubject subject, AppdefEntityID aeid,
                                                AppdefEntityTypeID ctype)
        throws AppdefEntityNotFoundException, PermissionException {
        final List<AppdefEntityID> resources;
        if (ctype == null) {
            if (aeid.isGroup()) {

                final ResourceGroup group = resourceGroupManager.findResourceGroupById(subject,
                    aeid.getId());

                final Collection<Resource> members = resourceGroupManager.getMembers(group);
                resources = new ArrayList<AppdefEntityID>(members.size());
                for (Resource r : members) {

                    if (r == null || r.isInAsyncDeleteState()) {
                        continue;
                    }
                    resources.add(AppdefUtil.newAppdefEntityId(r));
                }
            } else {
                // Just one
                resources = Collections.singletonList(aeid);
            }
        } else {
            // Autogroup
            resources = getAGMemberIds(subject, aeid, ctype);
        }
        return resources;
    }

    private final List<Resource> getResources(AppdefEntityID[] ids) {
        final List<Resource> resources = new ArrayList<Resource>(ids.length);

        for (int i = 0; i < ids.length; i++) {
            final Resource r = resourceManager.findResource(ids[i]);
            if (r == null || r.isInAsyncDeleteState()) {
                continue;
            }
            resources.add(resourceManager.findResource(ids[i]));
        }
        return resources;
    }

    private AppdefEntityID[] getGroupMemberIDs(AuthzSubject subject, Integer gid)
        throws AppdefEntityNotFoundException, PermissionException {
        final List<AppdefEntityID> members = getResourceIds(subject, AppdefEntityID.newGroupID(gid), null);
        return members.toArray(new AppdefEntityID[members.size()]);
    }

    private MetricDisplaySummary getMetricDisplaySummary(AuthzSubject subject,
                                                         MeasurementTemplate tmpl, long begin,
                                                         long end, double[] data, AppdefEntityID id)
        throws MeasurementNotFoundException {
        // Get baseline values
        Measurement dmval = measurementManager.findMeasurement(subject, tmpl.getId(), id);

        // Use previous function to set most values, including only 1 resource
        MetricDisplaySummary summary = getMetricDisplaySummary(tmpl, new Long(dmval.getInterval()),
            begin, end, data, 1);

        if (dmval.getBaseline() != null) {
            Baseline bval = dmval.getBaseline();
            if (bval.getMean() != null)
                summary.setMetric(MetricDisplayConstants.BASELINE_KEY, new MetricDisplayValue(bval
                    .getMean()));
            if (bval.getMaxExpectedVal() != null)
                summary.setMetric(MetricDisplayConstants.HIGH_RANGE_KEY, new MetricDisplayValue(
                    bval.getMaxExpectedVal()));
            if (bval.getMinExpectedVal() != null)
                summary.setMetric(MetricDisplayConstants.LOW_RANGE_KEY, new MetricDisplayValue(bval
                    .getMinExpectedVal()));
        }
        return summary;
    }

    /**
     * Method findResourceMetricSummary.
     * 
     * For metric comparisons, the ResourceMetricDisplaySummary beans are
     * returned as a map where the keys are the MeasurementTemplateValue (or
     * MeasurementTemplateLiteValue?) objects associated with the given
     * resource's types, the values are Lists of ResourceMetricDisplaySummary
     * 
     * The context that the user will be populating the input resource list from
     * should always be like resource types. If for some reason that's not the
     * case, this method will take a garbage in/garbage out approach (as opposed
     * to enforcing like types) -- comparing apples and oranges may be performed
     * but if the user ends up with measurement templates for which there is
     * only one resource to compare, that should indicate some other problem
     * i.e. the application is presenting dissimilar objects as available for
     * comparison.
     * 
     * The list of resources can be any concrete AppdefResourceValue (i.e. a
     * platform, server or service), composite AppdefResourceValues (i.e.
     * applications, groups) are inappropriate for this signature.
     * 
     * Used for screen 0.3
     * 
     * @param begin the commencement of the timeframe of interest
     * @param end the end of the timeframe of interest
     * @return Map of measure templates and resource metric lists
     * 
     */
    @Transactional(readOnly = true)
    public Map<MeasurementTemplate, List<MetricDisplaySummary>> findResourceMetricSummary(
                                                                                          int sessionId,
                                                                                          AppdefEntityID[] entIds,
                                                                                          long begin,
                                                                                          long end)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
        MeasurementNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        // Assuming that all AppdefEntityIDs of the same type, use the first one
        AppdefEntityValue rv = new AppdefEntityValue(entIds[0], subject);
        String monitorableType = rv.getMonitorableType();

        // Get the template ID's for that type
        List<MeasurementTemplate> tmpls = findMeasurementTemplates(sessionId, monitorableType,
            PageControl.PAGE_ALL);

        // Keep the templates in a map
        IntHashMap tmplMap = new IntHashMap();
        Integer[] tids = new Integer[tmpls.size()];
        int idx = 0;
        for (Iterator<MeasurementTemplate> i = tmpls.iterator(); i.hasNext();) {
            MeasurementTemplate tmpl = i.next();
            final Integer tid = tmpl.getId();
            tmplMap.put(tid.intValue(), tmpl);
            tids[idx++] = tid;
        }

        IntHashMap templateMetrics = new IntHashMap();
        HashMap<Integer, MeasurementTemplate> uniqueTemplates = new HashMap<Integer, MeasurementTemplate>();
        // a temp cache to save rountrips to the db
        HashMap<AppdefEntityID, Resource> seen = new HashMap<AppdefEntityID, Resource>();

        // Now, iterate through each AppdefEntityID

        for (int i = 0; i < entIds.length; i++) {
            Integer[] eids = new Integer[] { entIds[i].getId() };
            // Now get the aggregate data, keyed by template ID's
            List<Measurement> measurements = measurementManager.getMeasurements(tids, eids);
            Map<Integer, double[]> datamap = dataManager.getAggregateDataByTemplate(measurements,
                begin, end);

            // For each template, add a new summary
            for (Map.Entry<Integer, double[]> entry : datamap.entrySet()) {

                Integer mtid = entry.getKey();
                // Get the MeasurementTemplate
                MeasurementTemplate tmpl = (MeasurementTemplate) tmplMap.get(mtid.intValue());

                // Use the MeasurementTemplate id to get the array List
                List<ResourceMetricDisplaySummary> resSummaries = (List<ResourceMetricDisplaySummary>) templateMetrics
                    .get(mtid.intValue());
                if (resSummaries == null) { // this key hasn't been seen yet
                    resSummaries = new ArrayList<ResourceMetricDisplaySummary>();
                }

                // Get the data
                double[] data = entry.getValue();

                Resource v;
                rv = new AppdefEntityValue(entIds[i], subject);

                if (seen.containsKey(entIds[i])) {
                    v = seen.get(entIds[i]);
                } else {
                    v = resourceManager.findResource(entIds[i]);
                    seen.put(entIds[i], v); // keep track of what we've seen
                }

                MetricDisplaySummary mds = getMetricDisplaySummary(subject, tmpl, begin, end, data,
                    entIds[i]);

                resSummaries.add(new ResourceMetricDisplaySummary(mds, v));
                templateMetrics.put(mtid.intValue(), resSummaries);
                uniqueTemplates.put(mtid, tmpl);
            }
        }
        // now take all of the unique lists and unique
        // MeasurementTemplate's and merge them into the result
        HashMap<MeasurementTemplate, List<MetricDisplaySummary>> result = new HashMap<MeasurementTemplate, List<MetricDisplaySummary>>();
        for (Iterator<Integer> iter = uniqueTemplates.keySet().iterator(); iter.hasNext();) {
            Integer mtid = iter.next();
            result.put(uniqueTemplates.get(mtid), (List<MetricDisplaySummary>) templateMetrics
                .get(mtid.intValue()));
        }
        return result;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Map<String, Set<MetricDisplaySummary>> findMetrics(int sessionId, AppdefEntityID entId,
                                                              long begin, long end, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException,
        PermissionException, AppdefEntityNotFoundException, AppdefCompatException {
        AppdefEntityID[] entIds = new AppdefEntityID[] { entId };
        return findMetrics(sessionId, entIds, MeasurementConstants.FILTER_NONE, null, begin, end,
            false);
    }

    /**
     * Return a MetricSummary bean for each of the metrics (template) for the
     * entities in the given time frame
     * @param begin the beginning time frame
     * @param end the ending time frame
     * @return a list of ResourceTypeDisplaySummary beans
     * @throws AppdefCompatException
     * 
     */
    @Transactional(readOnly = true)
    public Map<String, Set<MetricDisplaySummary>> findMetrics(int sessionId,
                                                              AppdefEntityID[] entIds,
                                                              long filters, String keyword,
                                                              long begin, long end,
                                                              boolean showNoCollect)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException,
        PermissionException, AppdefEntityNotFoundException, AppdefCompatException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        // Assume all entities are of the same type
        AppdefEntityValue rv = new AppdefEntityValue(entIds[0], subject);

        List<AppdefEntityID> entArr;
        switch (entIds[0].getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                entArr = Arrays.asList(entIds);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                try {
                    entArr = GroupUtil.getCompatGroupMembers(subject, entIds[0], null,
                        PageControl.PAGE_ALL);
                } catch (GroupNotCompatibleException e) {
                    throw new IllegalArgumentException(
                        "Metrics are not available for groups that " +
                            "are not compatible types: " + e.getMessage());
                }
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                // No metric support for applications
                return new HashMap<String, Set<MetricDisplaySummary>>(0);
            default:
                throw new InvalidAppdefTypeException("entityID is not valid type, id type: " +
                                                     entIds[0].getType());
        }

        String monitorableType = rv.getMonitorableType();

        // Look up the metric summaries of associated servers
        return getResourceMetrics(subject, entArr, monitorableType, filters, keyword, begin, end,
            showNoCollect);
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
     * 
     */
    @Transactional(readOnly = true)
    public Map<String, Set<MetricDisplaySummary>> findMetrics(int sessionId, AppdefEntityID entId,
                                                              List<Integer> mtids, long begin,
                                                              long end)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException,
        AppdefEntityNotFoundException, AppdefCompatException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        boolean bPlatforms = false, bServers = false, bServices = false;

        // Let's get the templates to see what resources to gather
        List<MeasurementTemplate> templates = templateManager.getTemplates(mtids);
        for (MeasurementTemplate templ : templates) {
            int type = templ.getMonitorableType().getAppdefType();
            bPlatforms |= type == AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
            bServers |= type == AppdefEntityConstants.APPDEF_TYPE_SERVER;
            bServices |= type == AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        }

        AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
        List platforms = null, servers = null, services = null;

        // Can't assume that all templates are only for given entity,
        // might be for associated resources, too.
        switch (rv.getID().getType()) {
            // Hierarchical, not actually missing "break" statements
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                // Get the platforms
                if (bPlatforms)
                    platforms = rv.getAssociatedPlatforms(PageControl.PAGE_ALL);
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                // Get the servers
                if (bServers)
                    servers = rv.getAssociatedServers(PageControl.PAGE_ALL);
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                // Get the services
                if (bServices)
                    services = rv.getAssociatedServices(PageControl.PAGE_ALL);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                // Does not matter what kind of group this is, just use platform
                try {
                    platforms = GroupUtil.getCompatGroupMembers(subject, entId, null,
                        PageControl.PAGE_ALL);
                } catch (GroupNotCompatibleException e) {
                    log.debug("Group not compatible");
                }
                break;
            default:
                break;
        }

        // Look up the metric summaries of all associated resources
        Map<String, Set<MetricDisplaySummary>> results = new HashMap<String, Set<MetricDisplaySummary>>();
        if (bPlatforms)
            results.putAll(getResourceMetrics(subject, platforms, mtids, begin, end, Boolean.TRUE));
        if (bServers)
            results.putAll(getResourceMetrics(subject, servers, mtids, begin, end, Boolean.TRUE));
        if (bServices)
            results.putAll(getResourceMetrics(subject, services, mtids, begin, end, Boolean.TRUE));
        return results;
    }

    private final AppdefEntityID[] getAeids(final Collection resources) {
        final AppdefEntityID[] aeids = new AppdefEntityID[resources.size()];
        int i = 0;
        for (final Iterator it = resources.iterator(); it.hasNext(); i++) {
            final Object o = it.next();
            AppdefEntityID aeid = null;
            if (o instanceof AppdefEntityValue) {
                final AppdefEntityValue rv = (AppdefEntityValue) o;
                aeid = rv.getID();
            } else if (o instanceof AppdefEntityID) {
                aeid = (AppdefEntityID) o;
            } else if (o instanceof AppdefResource) {
                final AppdefResource r = (AppdefResource) o;
                aeid = r.getEntityId();
            } else if (o instanceof Resource) {
                final Resource resource = (Resource) o;
                aeid = AppdefUtil.newAppdefEntityId(resource);
            } else if (o instanceof ResourceGroup) {
                final ResourceGroup grp = (ResourceGroup) o;
                final Resource resource = grp.getResource();
                aeid = AppdefUtil.newAppdefEntityId(resource);
            } else {
                final AppdefResourceValue r = (AppdefResourceValue) o;
                aeid = r.getEntityId();
            }
            aeids[i] = aeid;
        }
        return aeids;

    }

    /**
     * @param availCache {@link Map} of {@link Integer} to {@link MetricValue}
     *        Integer => Measurement.getId(), may be null.
     * 
     *        Given an array of AppdefEntityID's, disqulifies their aggregate
     *        availability (with the disqualifying status) for all of them if
     *        any are down or unknown, otherwise the aggregate is deemed
     *        available
     * 
     *        If there's nothing in the array, then aggregate is not populated.
     *        Ergo, the availability shall be disqualified as unknown i.e. the
     *        (?) representation
     */
    private double getAggregateAvailability(AuthzSubject subject, AppdefEntityID[] ids,
                                            Map<Resource, List<Measurement>> measCache,
                                            Map<Integer, MetricValue> availCache)
        throws AppdefEntityNotFoundException, PermissionException {
        if (ids.length == 0) {
            return MeasurementConstants.AVAIL_UNKNOWN;
        }

        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        double sum = 0;
        int count = 0;
        int unknownCount = 0;
        final Map<Resource, Measurement> midMap = getMidMap(ids, measCache);
        if (debug) watch.markTimeBegin("getAvailability");
        double[] avails = getAvailability(subject, ids, midMap, availCache);
        if (debug) watch.markTimeEnd("getAvailability");
        for (int i = 0; i < avails.length; i++) {
            if (avails[i] == MeasurementConstants.AVAIL_UNKNOWN) {
                unknownCount++;
            } else {
                sum += avails[i];
                count++;
            }
        }
        if (unknownCount == ids.length) {
            // All resources are unknown
            return MeasurementConstants.AVAIL_UNKNOWN;
        }
        if (debug)
            log.debug(watch);
        return sum / count;
    }

    private AppdefEntityID[] toAppdefEntityIDArray(List<AppdefEntityID> entities) {
        AppdefEntityID[] result = new AppdefEntityID[entities.size()];
        int idx = 0;
        for (Iterator<AppdefEntityID> iter = entities.iterator(); iter.hasNext();) {
            Object thisThing = iter.next();
            if (thisThing instanceof AppdefResourceValue) {
                result[idx++] = ((AppdefResourceValue) thisThing).getEntityId();
                continue;
            }
            result[idx++] = (AppdefEntityID) thisThing;
        }
        return result;
    }

    @Transactional(readOnly = true)
    public MetricDisplaySummary getMetricDisplaySummary(MeasurementTemplate tmpl, Long interval,
                                                        long begin, long end, double[] data,
                                                        int totalConfigured) {
        // Create a new metric summary bean
        MetricDisplaySummary summary = new MetricDisplaySummary();

        // Set the time range
        summary.setBeginTimeFrame(new Long(begin));
        summary.setEndTimeFrame(new Long(end));

        // Set the template info
        summary.setLabel(tmpl.getName());
        summary.setTemplateId(tmpl.getId());
        summary.setTemplateCat(tmpl.getCategory().getId());
        summary.setCategory(tmpl.getCategory().getName());
        summary.setUnits(tmpl.getUnits());
        summary.setCollectionType(new Integer(tmpl.getCollectionType()));
        summary.setDesignated(Boolean.valueOf(tmpl.isDesignate()));
        summary.setMetricSource(tmpl.getMonitorableType().getName());

        summary.setCollecting(interval != null);

        if (summary.getCollecting())
            summary.setInterval(interval.longValue());

        if (data == null)
            return summary;

        // Set the data values
        summary.setMetric(MetricDisplayConstants.MIN_KEY, new MetricDisplayValue(
            data[MeasurementConstants.IND_MIN]));
        summary.setMetric(MetricDisplayConstants.AVERAGE_KEY, new MetricDisplayValue(
            data[MeasurementConstants.IND_AVG]));
        summary.setMetric(MetricDisplayConstants.MAX_KEY, new MetricDisplayValue(
            data[MeasurementConstants.IND_MAX]));

        // Groups get sums, not last value
        if (totalConfigured == 1 ||
            tmpl.getCollectionType() == MeasurementConstants.COLL_TYPE_STATIC) {
            summary.setMetric(MetricDisplayConstants.LAST_KEY, new MetricDisplayValue(
                data[MeasurementConstants.IND_LAST_TIME]));
        } else {
            // Percentage metrics (including Availability) do not need to be
            // summed
            if (MeasurementConstants.UNITS_PERCENTAGE.equals(tmpl.getUnits()) || MeasurementConstants.UNITS_PERCENT.equals(tmpl.getUnits()) ) {
                summary.setMetric(MetricDisplayConstants.LAST_KEY, new MetricDisplayValue(
                    data[MeasurementConstants.IND_AVG]));
            } else {
                summary.setMetric(MetricDisplayConstants.LAST_KEY, new MetricDisplayValue(
                    data[MeasurementConstants.IND_AVG] * data[MeasurementConstants.IND_CFG_COUNT]));
            }
        }

        // Number configured
        summary.setAvailUp(new Integer((int) data[MeasurementConstants.IND_CFG_COUNT]));
        summary.setAvailUnknown(new Integer(totalConfigured));

        return summary;
    }

    /**
     * @param resources {@link List} of {@link Resource}
     * @return {@link Map} of {@link Integer} to {@link Measurement}. Integer =
     *         Resource.getId()
     */
    private final Map<Resource, Measurement> getMidMap(Collection<Resource> resources) {
        final List<AppdefEntityID> aeids = new ArrayList<AppdefEntityID>();
        for (final Iterator<Resource> it = resources.iterator(); it.hasNext();) {
            final Resource r = it.next();
            if (r == null || r.isInAsyncDeleteState()) {
                continue;
            }
            aeids.add(AppdefUtil.newAppdefEntityId(r));
        }
        final AppdefEntityID[] ids = aeids.toArray(new AppdefEntityID[0]);
        return getMidMap(ids, null);
    }

    /**
     * @return {@link Map} of {@link Integer} to {@link Measurement} Integer =
     *         Resource.getId()
     */
    private final Map<Resource, Measurement> getMidMap(AppdefEntityID[] ids, Map<Resource, List<Measurement>> measCache) {
        final Map<Resource, Measurement> rtn = new HashMap<Resource, Measurement>(ids.length);
        final List<Resource> toGet = new ArrayList<Resource>();
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        final List<AppdefEntityID> aeids = Arrays.asList(ids);
        final int size = aeids.size();
        for (AppdefEntityID id : aeids) {
            if (id == null) {
                continue;
            }
            if (debug) watch.markTimeBegin("findResource size=" + size);
            final Resource res = resourceManager.findResource(id);
            if (res == null || res.isInAsyncDeleteState()) {
                continue;
            }
            if (debug) watch.markTimeEnd("findResource size=" + size);
            List<Measurement> list;
            if (null != measCache && null != (list = measCache.get(res))) {
                if (list.size() > 1) {
                    log.warn("resourceId " + res.getId() + " has more than one availability measurement assigned to it");
                } else if (list.size() <= 0) {
                    continue;
                }
                final Measurement m = list.get(0);
                rtn.put(res, m);
            } else {
                toGet.add(res);
            }
        }
        if (debug) watch.markTimeBegin("getAvailMeasurements");
        final Map<Integer, List<Measurement>> measMap = measurementManager.getAvailMeasurements(toGet);
        if (debug) watch.markTimeEnd("getAvailMeasurements");
        for (final Map.Entry<Integer, List<Measurement>> entry : measMap.entrySet()) {
            final Integer id = entry.getKey();
            final Resource r = resourceManager.findResourceById(id);
            final List<Measurement> vals = entry.getValue();
            if (vals.size() == 0) {
                continue;
            }
            final Measurement m = vals.get(0);
            if (vals.size() > 1) {
                log.warn("resourceId " + r.getId() + " has more than one availability measurement assigned to it");
            }
            rtn.put(r, m);
        }
        if (debug) log.debug(watch);
        return rtn;
    }

    /**
     * @param midMap {@link Map} of {@link Integer} to {@link Measurement}
     *        Integer = Resource.getId()
     * @param availCache {@link Map} of {@link Integer} to {@link MetricValue}
     *        Integer = Measurement.getId()
     */
    private double[] getAvailability(final AuthzSubject subject, final AppdefEntityID[] ids,
                                     final Map<Resource, Measurement> midMap,
                                     final Map<Integer, MetricValue> availCache)
    throws ApplicationNotFoundException, AppdefEntityNotFoundException, PermissionException {
        final double[] result = new double[ids.length];
        Arrays.fill(result, MeasurementConstants.AVAIL_UNKNOWN);
        final Map<Integer, MetricValue> data = new HashMap<Integer, MetricValue>();
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        final Map<AppdefEntityID, Resource> prefetched = new HashMap<AppdefEntityID, Resource>();
        if (!midMap.isEmpty()) {
            final List<Integer> mids = new ArrayList<Integer>();
            for (final Resource r : midMap.keySet()) {
                final AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(r);
                prefetched.put(aeid, r);
                Measurement meas;
                if (null == midMap || null == (meas = midMap.get(r))) {
                    if (debug) watch.markTimeBegin("getAvailabilityMeasurement");
                    meas = measurementManager.getAvailabilityMeasurement(r);
                    if (debug) watch.markTimeEnd("getAvailabilityMeasurement");
                }
                if (meas == null) {
                    continue;
                }
                if (availCache != null) {
                    MetricValue mv = availCache.get(meas.getId());
                    if (mv != null) {
                        data.put(meas.getId(), mv);
                    } else {
                        mids.add(meas.getId());
                    }
                } else {
                    mids.add(meas.getId());
                }
            }
            if (debug) watch.markTimeBegin("getLastAvail");
            data.putAll(availabilityManager.getLastAvail(mids.toArray(new Integer[0])));
            if (debug) watch.markTimeEnd("getLastAvail");
        }
        for (int i = 0; i < ids.length; i++) {
            Resource r = prefetched.get(ids[i]);
            if (r == null) {
                r = resourceManager.findResource(ids[i]);
            }
            if (r == null || r.isInAsyncDeleteState()) {
                continue;
            }
            if (midMap.containsKey(r)) {
                Integer mid = midMap.get(r).getId();
                MetricValue mval = null;
                if (null != (mval = data.get(mid))) {
                    result[i] = mval.getValue();
                }
            } else {
                // cases for abstract resources whose availability are xor'd
                switch (ids[i].getType()) {
                    case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                        AppdefEntityValue appVal = new AppdefEntityValue(ids[i], subject);
                        if (debug) watch.markTimeBegin("getFlattenedServiceIds");
                        AppdefEntityID[] services = appVal.getFlattenedServiceIds();
                        if (debug) watch.markTimeEnd("getFlattenedServiceIds");
                        if (debug) watch.markTimeBegin("getAggregateAvailability");
                        result[i] = getAggregateAvailability(subject, services, null, availCache);
                        if (debug) watch.markTimeEnd("getAggregateAvailability");
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                        if (debug) watch.markTimeBegin("getGroupAvailability");
                        result[i] = getGroupAvailability(subject, ids[i].getId(), null, null);
                        if (debug) watch.markTimeEnd("getGroupAvailability");
                        break;
                    default:
                        break;
                }
            }
        }
        if (debug) log.debug(watch);
        return result;
    }

    /**
     * Given a group, disqualifies their aggregate availability (with the
     * disqualifying status) for all of them if any are down or unknown,
     * otherwise the aggregate is deemed available
     * 
     * If there's nothing in the array, then aggregate is not populated. Ergo,
     * the availability shall be disqualified as unknown i.e. the (?)
     * representation
     * 
     * @param measCache {@link Map} of {@link Resource.getId} to {@link List} of
     *        {@link Measurement}. May be null.
     * @param availCache {@link Map} of {@link Measurement.getId} to
     *        {@link MetricValue}. May be null.
     */
    private double getGroupAvailability(AuthzSubject subject, Integer gid, Map<Resource, List<Measurement>> measCache,
                                        Map<Integer, MetricValue> availCache)
    throws AppdefEntityNotFoundException, PermissionException {
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        final ResourceGroup group = resourceGroupManager.getGroupById(gid);
        if (group == null) {
            return MeasurementConstants.AVAIL_UNKNOWN;
        }
        final Resource groupResource = group.getResource();
        if (groupResource == null || groupResource.isInAsyncDeleteState()) {
            return MeasurementConstants.AVAIL_UNKNOWN;
        }
        if (measCache == null) {
            measCache = measurementManager.getAvailMeasurementsByResource(Collections.singleton(group));
        }
        // Allow for the maximum window based on collection interval
        if (debug) watch.markTimeBegin("getMembers");
        final Collection<Resource> members = resourceGroupManager.getMembers(group);
        if (debug) watch.markTimeEnd("getMembers");
        final List<AppdefEntityID> aeids = new ArrayList<AppdefEntityID>(members.size());
        for (final Resource r : members) {
            aeids.add(AppdefUtil.newAppdefEntityId(r));
        }
        final List<Measurement> metrics = measCache.get(groupResource);
        final Map<Resource, Measurement> midMap = new HashMap<Resource, Measurement>(metrics.size());
        for (final Measurement m : metrics) {
            final Resource r = m.getResource();
            midMap.put(r, m);
        }
        final double[] data = getAvailability(subject, aeids.toArray(new AppdefEntityID[0]), midMap, availCache);
        double rtn = getCalculatedGroupAvailability(data);
        log.debug(watch);
        return rtn;
    }

    private List<AppdefEntityID> getAGMemberIds(AuthzSubject subject, AppdefEntityID[] aids, AppdefEntityTypeID ctype)
    throws AppdefEntityNotFoundException, PermissionException {
        final List<AppdefEntityID> res = new ArrayList<AppdefEntityID>();
        final Resource proto = resourceManager.findResourcePrototype(ctype);
        if (proto == null) {
            log.warn("Unable to find prototype for ctype=[" + ctype + "]");
            return res;
        }
        final List<Resource> resources = new ArrayList<Resource>();
        for (final AppdefEntityID aeid : aids) {
            if (aeid.isApplication()) {
                AppdefEntityValue rv = new AppdefEntityValue(aeid, subject);
                Collection<AppdefResourceValue> services = rv.getAssociatedServices(ctype.getId(), PageControl.PAGE_ALL);
                for (AppdefResourceValue r : services) {
                    res.add(r.getEntityId());
                }
            } else {
                final Resource r = resourceManager.findResource(aeid);
                resources.add(r);
            }
        }
        final ResourceRelation containment = resourceManager.getContainmentRelation();
        final Collection<Resource> children =
            resourceManager.getDescendantResources(subject, resources, containment, proto, true);
        final List<AppdefEntityID> childAeids = new Transformer<Resource, AppdefEntityID>() {
            @Override
            public AppdefEntityID transform(Resource r) {
                return AppdefUtil.newAppdefEntityId(r);
            }
        }.transform(children);
        res.addAll(childAeids);
        return res;
    }

    /**
     * Return a MetricSummary bean for each of the servers of a specific type.
     * @param begin the beginning time frame
     * @param end the ending time frame
     * @return a list of ResourceTypeDisplaySummary beans
     * @throws AppdefCompatException
     * 
     */
    @Transactional(readOnly = true)
    public Map<String, Set<MetricDisplaySummary>> findAGPlatformMetricsByType(
                                                                              int sessionId,
                                                                              AppdefEntityTypeID platTypeId,
                                                                              long begin, long end,
                                                                              boolean showAll)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException,
        AppdefEntityNotFoundException, PermissionException, AppdefCompatException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        // Get the member IDs
        List<AppdefEntityID> platforms = getPlatformAG(subject, platTypeId);

        PlatformType platType = platformManager.findPlatformType(platTypeId.getId());

        return getResourceMetrics(subject, platforms, platType.getName(),
            MeasurementConstants.FILTER_NONE, null, begin, end, showAll);
    }

    private List<AppdefEntityID> getPlatformAG(AuthzSubject subject, AppdefEntityTypeID ctype)
        throws AppdefEntityNotFoundException, PermissionException {
        if (!ctype.isPlatform()) {
            throw new IllegalArgumentException(ctype.getType() + " is not a platform type");
        }
        Integer[] platIds = platformManager.getPlatformIds(subject, ctype.getId());
        List<AppdefEntityID> entIds = new ArrayList<AppdefEntityID>(platIds.length);
        for (int i = 0; i < platIds.length; i++) {
            entIds.add(AppdefEntityID.newPlatformID(platIds[i]));
        }
        return entIds;
    }

    /**
     * Fetch all metric summaries for specified resources
     * @param resources the list of resources
     * @param begin the beginning of time range
     * @param end the end of time range
     * @param showNoCollect TODO
     * @return Map where key = category, value = List of summary beans
     * @throws AppdefCompatException
     */
    private Map<String, Set<MetricDisplaySummary>> getResourceMetrics(AuthzSubject subject,
                                                                      List resources,
                                                                      String resourceType,
                                                                      long filters, String keyword,
                                                                      long begin, long end,
                                                                      boolean showNoCollect)
    throws AppdefCompatException {
        // Need to get the templates for this type
        List<MeasurementTemplate> tmpls = templateManager.findTemplates(resourceType, filters, keyword);

        // Look up the metric summaries of associated servers
        return getResourceMetrics(subject, resources, tmpls, begin, end, Boolean.valueOf(showNoCollect));
    }

    /**
     * Fetch the metric summaries for specified resources and templates
     * @param resources the list of resources
     * @param tmpls the list of measurement templates
     * @param begin the beginning of time range
     * @param end the end of time range
     * @param showNoCollect whether or not to include templates that have not
     *        collected data
     * @return Map where key = category, value = List of summary beans
     */
    private Map<String, Set<MetricDisplaySummary>> getResourceMetrics(AuthzSubject subject, List resources, List tmpls,
                                                                      long begin, long end, Boolean showNoCollect)
    throws AppdefCompatException {
        List<MeasurementTemplate> templates;
        Integer[] tids;
        // Create Map of all resources
        final int size = MeasurementConstants.VALID_CATEGORIES.length;
        HashMap<String, Set<MetricDisplaySummary>> resmap = new HashMap<String, Set<MetricDisplaySummary>>(size);
        if (tmpls.size() == 0 || resources.size() == 0) {
            return resmap;
        }
        if (tmpls.get(0) instanceof MeasurementTemplate) {
            templates = tmpls;
            tids = new Integer[templates.size()];
            for (int i = 0; i < templates.size(); i++) {
                MeasurementTemplate t = templates.get(i);
                tids[i] = t.getId();
            }
        } else {
            // If templates are just ID's, we have to look them up
            tids = (Integer[]) tmpls.toArray(new Integer[tmpls.size()]);
            try {
                templates = templateManager.getTemplates(tids, PageControl.PAGE_ALL);
            } catch (TemplateNotFoundException e) {
                templates = new ArrayList<MeasurementTemplate>(0);
                // Well, if we don't find it, *shrug*
            }
        }

        // Create the EntityIds array and map of counts
        Integer[] eids = new Integer[resources.size()];
        AppdefEntityID[] aeids = new AppdefEntityID[resources.size()];
        Map<String, Integer> totalCounts = new HashMap<String, Integer>();
        Map<Integer, Collection<AppdefEntityID>> aeidsByType = new HashMap<Integer, Collection<AppdefEntityID>>();
        int i=0;
        for (Iterator<Object> it=resources.iterator(); it.hasNext(); i++) {
            // We understand two types
            AppdefEntityID aeid;
            Object resource = it.next();
            if (resource instanceof AppdefResourceValue) {
                AppdefResourceValue resVal = (AppdefResourceValue) resource;
                aeid = resVal.getEntityId();

                // Increase count
                String type = resVal.getAppdefResourceTypeValue().getName();
                int count = 0;
                if (totalCounts.containsKey(type)) {
                    count = totalCounts.get(type).intValue();
                }
                totalCounts.put(type, new Integer(++count));
            } else if (resource instanceof AppdefEntityID) {
                aeid = (AppdefEntityID) resource;
            } else {
                throw new AppdefCompatException("getResourceMetrics() does " +
                                                "not understand resource class: " +
                                                resource.getClass());
            }
            
            Collection<AppdefEntityID> tmp;
            if (null == (tmp = aeidsByType.get(aeid.getType()))) {
                tmp = new ArrayList<AppdefEntityID>();
                aeidsByType.put(aeid.getType(), tmp);
            }
            tmp.add(aeid);

            eids[i] = aeid.getId();
            aeids[i] = aeid;
        }

        // Now get the aggregate data, keyed by template ID's

        final List<Measurement> measurements = measurementManager.getEnabledMeasurements(tids, eids);
        final Map<Integer, double[]> datamap =
            dataManager.getAggregateDataByTemplate(measurements, begin, end);

        // Get the intervals, keyed by template ID's as well
        final Map<Integer, Long> intervals = (showNoCollect == null) ?
            new HashMap<Integer, Long>() :
            measurementManager.findMetricIntervals(subject, aeids, tids);
            
        final Map<Integer, ProblemMetricInfo> probmap = new HashMap<Integer, ProblemMetricInfo>();
        for (Integer aeidType : aeidsByType.keySet()) {
            probmap.putAll(problemMetricManager.getProblemsByTemplate(
                aeidType, eids, begin, end));
        }

        for (Iterator<MeasurementTemplate> it = templates.iterator(); it.hasNext();) {
            MeasurementTemplate tmpl = it.next();

            int total = eids.length;
            String type = tmpl.getMonitorableType().getName();
            if (totalCounts.containsKey(type)) {
                total = totalCounts.get(type).intValue();
            }

            double[] data = datamap.get(tmpl.getId());

            if (data == null && (showNoCollect == null || showNoCollect.equals(Boolean.FALSE))) {
                continue;
            }

            String category = tmpl.getCategory().getName();
            Set<MetricDisplaySummary> summaries = resmap.get(category);
            if (summaries == null) {
                summaries = new TreeSet<MetricDisplaySummary>();
                resmap.put(category, summaries);
            }

            Long interval = intervals.get(tmpl.getId());

            // Now create a MetricDisplaySummary and add it to the list
            MetricDisplaySummary summary =
                getMetricDisplaySummary(tmpl, interval, begin, end, data, total);
            
            if (data != null) {
                // See if there are problems, too
                if (probmap.containsKey(tmpl.getId())) {
                    ProblemMetricInfo pmi = probmap.get(tmpl.getId());
                    summary.setAlertCount(pmi.getAlertCount());
                    summary.setOobCount(pmi.getOobCount());
                }
            }
            summaries.add(summary);
        }

        return resmap;
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
     * 
     */
    @Transactional(readOnly = true)
    public Map<String, Set<MetricDisplaySummary>> findAGMetricsByType(int sessionId,
                                                                      AppdefEntityID[] entIds,
                                                                      AppdefEntityTypeID typeId,
                                                                      long filters, String keyword,
                                                                      long begin, long end,
                                                                      boolean showAll)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException,
        PermissionException, AppdefEntityNotFoundException, AppdefCompatException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        List group = new ArrayList();
        for (int i = 0; i < entIds.length; i++) {
            AppdefEntityValue rv = new AppdefEntityValue(entIds[i], subject);

            switch (typeId.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    // Get the associated servers
                    group.addAll(rv.getAssociatedServers(typeId.getId(), PageControl.PAGE_ALL));
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    // Get the associated services
                    group.addAll(rv.getAssociatedServices(typeId.getId(), PageControl.PAGE_ALL));
                    break;
                default:
                    break;
            }
        }

        // Need to get the templates for this type, using the first resource
        AppdefResourceValue resource = (AppdefResourceValue) group.get(0);
        String resourceType = resource.getAppdefResourceTypeValue().getName();

        // Look up the metric summaries of associated servers
        return getResourceMetrics(subject, group, resourceType, filters, keyword, begin, end,
            showAll);
    }

    /**
     * Return a MeasurementSummary bean for the resource's associated resources
     * specified by type
     * @param entId the entity ID
     * @param appdefType the type (server, service, etc) of the specified
     *        resource type
     * @param typeId the specified resource type ID
     * @return a MeasurementSummary bean
     * 
     */
    public MeasurementSummary getSummarizedResourceAvailability(int sessionId,
                                                                AppdefEntityID entId,
                                                                int appdefType, Integer typeId)
        throws AppdefEntityNotFoundException, PermissionException, SessionNotFoundException,
        SessionTimeoutException, InvalidOptionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        List resources;
        if (entId.isGroup()) {
            resources = getResourceIds(subject, entId, null);
        } else if (entId.isApplication()) {
            AppdefEntityValue aev = new AppdefEntityValue(entId, subject);
            resources = aev.getAssociatedServices(typeId, PageControl.PAGE_ALL);
            if (typeId != null) {
                for (Iterator<AppdefResourceValue> i = resources.iterator(); i.hasNext();) {
                    AppdefResourceValue r = i.next();
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
                    resources = aev.getAssociatedServices(typeId, PageControl.PAGE_ALL);
                    break;
                default:
                    throw new InvalidOptionException("Requested type (" + appdefType +
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
            } else if (MeasurementConstants.AVAIL_UNKNOWN == data[i]) {
                // maybe so
                unknownCnt++;
            } else if ((AVAIL_DOWN <= data[i] && AVAIL_UP > data[i]) || AVAIL_PAUSED == data[i] ||
                       AVAIL_POWERED_OFF == data[i]) {
                // no
                unavailCnt++;
            } else {
                // If for some reason we have availability data that is not
                // recognized as a valid state in MeasurementConstants.AVAIL_
                // log as much info as possible and mark it as UNKNOWN.
                unknownCnt++;
                log.error("Resource " + resourceArray[i] + " is reporting " +
                          "an invalid availability state of " + data[i] + " (measurement id=" +
                          findAvailabilityMetric(sessionId, resourceArray[i]).getId() + ")");
            }
        }

        return new MeasurementSummary(new Integer(availCnt), new Integer(unavailCnt), new Integer(
            unknownCnt));
    }

    /**
     * @return a List of ResourceTypeDisplaySummary's
     * @deprecated use POJO API instead
     */
    @Deprecated
    private List<ResourceTypeDisplaySummary> getSummarizedResourceCurrentHealth(AuthzSubject subject,
                                                                                AppdefResourceValue[] resources)
    throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException {
        final List<ResourceTypeDisplaySummary> summaries = new ArrayList<ResourceTypeDisplaySummary>();

        // Create Map of auto-group'd/singleton resources and a List of clusters
        // since their current health summarizations are not flattened

        // auto-group'd entities are kept track of in here where keys are the
        // type id's and the values are lists of resources
        final HashMap<Integer, List<AppdefEntityID>> resTypeMap = new HashMap<Integer, List<AppdefEntityID>>();
        // to avoid looking up singleton/autogroup resources again later, we
        // keep them here where keys are AppdefEntityID's and values are the
        // AppdefResourceValues
        final HashMap<AppdefEntityID, AppdefResourceValue> resourcemap = new HashMap<AppdefEntityID, AppdefResourceValue>();
        final List<AppdefResourceValue> appdefVals = Arrays.asList(resources);
        final Map<Resource, List<Measurement>> tmp = measurementManager.getAvailMeasurementsByResource(appdefVals);
        final Map<Resource, Measurement> midMap = getMidMap(getAeids(appdefVals), tmp);
        // keys are type id's and the values are AppdefResourceTypeValues
        for (int i = 0; i < resources.length; i++) {
            if (resources[i] instanceof ServiceClusterValue) {
                AppdefResourceValue resource = resources[i];
                AppdefEntityID aid = resource.getEntityId();
                AppdefEntityValue aeval = new AppdefEntityValue(aid, subject);
                AppdefGroupValue agval = (AppdefGroupValue) aeval.getResourceValue();
                ClusterDisplaySummary cds = new ClusterDisplaySummary();
                cds.setEntityId(resource.getEntityId());
                cds.setEntityName(agval.getName());
                int size = agval.getTotalSize();
                cds.setNumResources(new Integer(size));
                // Replace the IDs with all of the members
                List<AppdefEntityID> memberIds = getResourceIds(subject, aid, null);
                AppdefEntityID[] ids = memberIds.toArray(new AppdefEntityID[0]);
                setResourceTypeDisplaySummary(subject, cds, agval.getAppdefResourceTypeValue(), ids, midMap);
                summaries.add(cds);
            } else {
                // all of the non-clusters get organized in here
                resourcemap.put(resources[i].getEntityId(), resources[i]);
                AppdefResourceTypeValue type = resources[i].getAppdefResourceTypeValue();
                Integer typeId = type.getId();
                List<AppdefEntityID> siblings = resTypeMap.get(typeId);
                if (siblings == null) {
                    siblings = new ArrayList<AppdefEntityID>();
                    resTypeMap.put(typeId, siblings);
                }
                // Add resource to list
                siblings.add(resources[i].getEntityId());
            }
        }
        // first deal with the autogroubz and singletons (singletons
        // are just the degenerative case of an autogroup, why it's
        // its own type is... silly)
        for (Map.Entry<Integer, List<AppdefEntityID>> entry : resTypeMap.entrySet()) {
            Collection<AppdefEntityID> siblings = entry.getValue();
            // Make sure we have valid IDs
            if (siblings == null || siblings.size() == 0) {
                continue;
            }
            ResourceTypeDisplaySummary summary;
            AppdefEntityID[] ids = siblings.toArray(new AppdefEntityID[0]);
            AppdefEntityID aid = ids[0];
            AppdefResourceValue resource = resourcemap.get(aid);
            // autogroup
            if (ids.length > 1) {
                summary = new AutogroupDisplaySummary();
                summary.setNumResources(new Integer(ids.length));
            } else {
                // singleton
                summary = new SingletonDisplaySummary(aid, resource.getName());
            }
            setResourceTypeDisplaySummary(subject, summary, resource.getAppdefResourceTypeValue(), ids, midMap);
            summaries.add(summary);
        }
        Collections.sort(summaries);
        return summaries;
    }

    /**
     * @return a List of ResourceTypeDisplaySummary's
     */
    private List<ResourceTypeDisplaySummary> getSummarizedResourceCurrentHealth(AuthzSubject subject,
                                                                                Collection<AppdefResource> resources)
    throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException {
        List<ResourceTypeDisplaySummary> summaries = new ArrayList<ResourceTypeDisplaySummary>();
        // auto-group'd entities are kept track of in here where keys are the
        // type id's and the values are lists of resources
        HashMap<Integer, List<AppdefResource>> resTypeMap = new HashMap<Integer, List<AppdefResource>>();
        // keys are type id's and the values are AppdefResources
        for (AppdefResource resource : resources) {
            AppdefResourceType type = resource.getAppdefResourceType();
            Integer typeId = type.getId();
            List<AppdefResource> siblings = resTypeMap.get(typeId);
            if (siblings == null) {
                siblings = new ArrayList<AppdefResource>();
                resTypeMap.put(typeId, siblings);
            }
            // Add resource to list
            siblings.add(resource);
        }
        final Map<Resource, List<Measurement>> measCache = measurementManager.getAvailMeasurementsByResource(resources);
        final Map<Resource, Measurement> midMap = getMidMap(getAeids(resources), measCache);
        // first deal with the autogroups and singletons (singletons
        // are just the degenerative case of an autogroup, why it's
        // its own type is... silly)
        for (Map.Entry<Integer, List<AppdefResource>> entry : resTypeMap.entrySet()) {
            final Collection<AppdefResource> siblings = entry.getValue();
            // Make sure we have valid IDs
            if (siblings == null || siblings.size() == 0) {
                continue;
            }
            ResourceTypeDisplaySummary summary = null;
            AppdefResourceType type = null;
            AppdefEntityID[] ids = new AppdefEntityID[siblings.size()];
            int i = 0;
            for (Iterator<AppdefResource> sibIt = siblings.iterator(); sibIt.hasNext(); i++) {
                AppdefResource res = sibIt.next();
                ids[i] = res.getEntityId();
                if (type == null) {
                    type = res.getAppdefResourceType();
                    if (sibIt.hasNext()) {
                        // autogroup
                        summary = new AutogroupDisplaySummary();
                        summary.setNumResources(new Integer(siblings.size()));
                    } else {
                        // singleton
                        summary = new SingletonDisplaySummary(ids[i], res.getName());
                    }
                }
            }
            setResourceTypeDisplaySummary(subject, summary, type.getAppdefResourceTypeValue(), ids, midMap);
            summaries.add(summary);
        }
        Collections.sort(summaries);
        return summaries;
    }

    /**
     * summary logic is not aggregated, instead it is calcuted with the
     * following logic:
     * 
     * Red only (regardless of any Gray) = Red Red + Green (regardless of any
     * Gray) = Yellow Gray only (regardless of any Green) = Gray Green only =
     * Green
     */
    private void setResourceTypeDisplaySummary(AuthzSubject subject,
                                               ResourceTypeDisplaySummary summary,
                                               AppdefResourceTypeValue resType,
                                               AppdefEntityID[] ids,
                                               Map<Resource, Measurement> midMap) {
        // Now get each category of measurements
        long end = System.currentTimeMillis();
        summary.setResourceType(resType);
        StopWatch watch = new StopWatch(end);
        if (log.isDebugEnabled()) {
            log.debug("BEGIN setResourceTypeDisplaySummary");
        }
        // Availability
        try {
           double[] data = getAvailability(subject, ids, midMap, null);
           summary.setAvailability(getCalculatedGroupAvailability(data));
        } catch (AppdefEntityNotFoundException e) {
            log.debug(e, e);
            summary.setAvailability(new Double(MeasurementConstants.AVAIL_UNKNOWN));
        } catch (PermissionException e) {
            log.debug(e, e);
            summary.setAvailability(new Double(MeasurementConstants.AVAIL_UNKNOWN));
        }
        if (log.isDebugEnabled()) {
            log.debug("END setResourceTypeDisplaySummary -- " + watch.getElapsed() + " msec");
        }
    }

    protected double getCalculatedGroupAvailability(double[] data) {
        boolean hasDownValues = false;
        boolean hasUpValues = false;
        boolean hasUnknownValues = false;
        boolean hasWarnValues = false;
        boolean hasPausedValues = false;
        boolean hasOffValues = false;
        double result = MeasurementConstants.AVAIL_UNKNOWN;

        if (data.length > 0) {
            double sum = 0;

            for (int ii = 0; ii < data.length; ii++) {
                double val = data[ii];

                if (val == MeasurementConstants.AVAIL_DOWN) {
                    hasDownValues = true;
                } else if (val == MeasurementConstants.AVAIL_UP) {
                    hasUpValues = true;
                } else if (val == MeasurementConstants.AVAIL_UNKNOWN) {
                    hasUnknownValues = true;
                } else if (val == MeasurementConstants.AVAIL_WARN) {
                    hasWarnValues = true;
                } else if (val == MeasurementConstants.AVAIL_PAUSED) {
                    hasPausedValues = true;
                } else if (val == MeasurementConstants.AVAIL_POWERED_OFF) {
                    hasOffValues = true;
                }

                sum += val;
            }

            result = getSummaryValue(hasDownValues, hasUpValues, hasUnknownValues, hasWarnValues,
                hasPausedValues, hasOffValues);
        }

        return result;
    }

    /**
     * Here is the table for these states evaluated in this order: All Red = Red,
     * All Green = Green, All Yellow = Yellow, All Grey = Grey, All Orange = Orange,
     * All Black = Black, Yellow + Anything else = Yellow, Red + Green = Yellow,
     * Red + Anything else = Red, Green + Grey + Anything else = Grey, Green +
     * Anything else = Green
     * 
     * Color mapping: Red = Down, Green = Up, Yellow = Warn, Grey = Unknown, Orange
     * = Paused (VM)/Maintenance, Black = Powered Off (VM)/Suspended
     */
    private Double getSummaryValue(boolean hasDownValues, boolean hasUpValues,
                                   boolean hasUnknownValues, boolean hasWarnValues,
                                   boolean hasPausedValues, boolean hasOffValues) {
        Double result = new Double(AVAIL_UNKNOWN);
        if (hasOffValues &&
            !(hasDownValues || hasUnknownValues || hasWarnValues || hasPausedValues || hasUpValues)) {
            // only has off values
            result = new Double(AVAIL_POWERED_OFF);
        } else if (hasPausedValues &&
                   !(hasDownValues || hasUnknownValues || hasWarnValues || hasOffValues || hasUpValues)) {
            // only has paused values
            result = new Double(AVAIL_PAUSED);
        } else if (hasUpValues && !(hasDownValues || hasUnknownValues || hasWarnValues)) {
            // Everything is up, no downs, no unknowns, no warns, we ignore the
            // paused and off == GREEN
            result = new Double(AVAIL_UP);
        } else if (hasWarnValues || (hasDownValues && hasUpValues)) {
            // There are all warns, or there are ups and downs, doesn't matter
            // about unknowns, paused, and off == YELLOW
            result = new Double(AVAIL_WARN);
        } else if (hasDownValues) {
            // There's a down, at this point nothing else matters == RED
            result = new Double(AVAIL_DOWN);
        } else if (hasUnknownValues) {
            // If we've gotten this far and it's an unknown == GREY
            result = new Double(AVAIL_UNKNOWN);
        }

        return result;
    }

    /**
     * Method findSummarizedServerCurrentHealth.
     * <p>
     * Return a ResourceTypeDisplaySummary bean for each of the platform's
     * deployed servers. Each bean represents a type of server and the
     * measurement data summarized for that type.
     * </p>
     * <p>
     * see screen 2.2.2
     * </p>
     * @return List of ResourceTypeDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceTypeDisplaySummary> findSummarizedServerCurrentHealth(int sessionId,
                                                                              AppdefEntityID entId)
    throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        // Get the associated servers
        AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
        List servers = rv.getAssociatedServers(PageControl.PAGE_ALL);
        return getSummarizedResourceCurrentHealth(subject, (AppdefResourceValue[]) servers
            .toArray(new AppdefResourceValue[servers.size()]));
    }

    /**
     * Method findSummarizedServiceCurrentHealth.
     * <p>
     * This is used for the lists of service types for the Current Health view
     * for
     * <ul>
     * <li>applications (2.1.2)
     * <li>servers (2.3.2.1-4)
     * <li>services (2.5.2.2)
     * </ul>
     * </p>
     * <p>
     * If <code>internal</code> is <i>true</i>, only the <i>internal</i>
     * services will be returned, the <i>deployed</i> ones if it's <i>false</i>.
     * If <code>internal</code> is <i>null</i>, then both deployed <i>and</i>
     * internal services will be returned.
     * </p>
     * 
     * @param entId the appdef entity with child services
     * @return List a list of ResourceTypeDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceTypeDisplaySummary> findSummarizedPlatformServiceCurrentHealth(int sessionId,
                                                                                       AppdefEntityID entId)
    throws SessionTimeoutException, SessionNotFoundException, PermissionException, AppdefEntityNotFoundException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        Collection<AppdefResource> services = serviceManager.getPlatformServices(subject, entId
            .getId());
        return getSummarizedResourceCurrentHealth(subject, services);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceTypeDisplaySummary> findSummarizedServiceCurrentHealth(int sessionId, AppdefEntityID entId)
    throws SessionTimeoutException, SessionNotFoundException, PermissionException, AppdefEntityNotFoundException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        // Get the associated services
        AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
        List services = rv.getAssociatedServices(PageControl.PAGE_ALL);
        return getSummarizedResourceCurrentHealth(subject, (AppdefResourceValue[]) services
            .toArray(new AppdefResourceValue[services.size()]));
    }

    /**
     * Method findGroupCurrentHealth.
     * <p>
     * Return a ResourceDisplaySummary bean for each of the group's member
     * resources. Each bean represents a resource and the measurement data
     * summarized for that type.
     * </p>
     * <p>
     * see screen 2.2.2
     * </p>
     * @return List of ResourceDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceDisplaySummary> findGroupCurrentHealth(int sessionId, Integer id)
    throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        final ResourceGroup group = resourceGroupManager.findResourceGroupById(subject, id);
        Map<String, Map<Resource, Measurement>> cats = new HashMap<String, Map<Resource, Measurement>>(2);
        cats.put(MeasurementConstants.CAT_AVAILABILITY, null);
        // XXX scottmf need to review this, perf is bad and metric
        // is not very useful
        // cats.put(MeasurementConstants.CAT_THROUGHPUT, null);
        // Look up metrics by group first
        for (Map.Entry<String, Map<Resource, Measurement>> entry : cats.entrySet()) {
            List<Measurement> metrics = measurementManager.findDesignatedMeasurements(subject, group, entry.getKey());
            Map<Resource, Measurement> mmap = new HashMap<Resource, Measurement>(metrics.size());
            // Optimization for the fact that we can have multiple indicator
            // metrics for each category, only keep one
            for (Measurement m : metrics) {
                if (mmap.containsKey(m.getResource())) {
                    continue;
                }
                mmap.put(m.getResource(), m);
            }
            entry.setValue(mmap);
        }

        final StopWatch watch = new StopWatch();
        final PageList<ResourceDisplaySummary> summaries = new PageList<ResourceDisplaySummary>();
        watch.markTimeBegin("getAvailMeasurements");
        final Map<Resource, List<Measurement>> measCache =
            measurementManager.getAvailMeasurementsByResource(Collections.singleton(group));
        final Map<Integer, List<Measurement>> measCacheById = new HashMap<Integer, List<Measurement>>();
        watch.markTimeEnd("getAvailMeasurements");
        // Remap from list to map of metrics
        List<Measurement> metrics = measCache.remove(group.getResource());
        for (Measurement meas : metrics) {
            measCache.put(meas.getResource(), Collections.singletonList(meas));
            measCacheById.put(meas.getResource().getId(), Collections.singletonList(meas));
        }
        // Members are sorted
        final Collection<Resource> members = resourceGroupManager.getMembers(group);
        watch.markTimeBegin("getLastAvail");
        final Map<Integer, MetricValue> availCache = availabilityManager.getLastAvail(members, measCacheById);
        watch.markTimeEnd("getLastAvail");
        for (Resource res : members) {
            AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(res);
            ResourceDisplaySummary summary = new ResourceDisplaySummary();
            // Set the resource
            AppdefResourceValue parent = null;
            switch (aeid.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    watch.markTimeBegin("get platform from server");
                    Server server = serverManager.getServerById(aeid.getId());
                    // HHQ-5648 - server may not exist due to delete
                    if (server == null) {
                        continue;
                    }
                    final Resource serverResource = server.getResource();
                    //[HQ-4052] Guys 16.10.2012 - platform might be null due to async. delete operation in progress 
                    if(serverResource != null && serverResource.isInAsyncDeleteState()) { 
                        log.warn("findGroupCurrentHealth() -->  skipping server " + server +
                                 " as server is in deletion process.") ;
                        continue ; 
                    }
                    parent = server.getPlatform().getPlatformValue();
                    watch.markTimeEnd("get platform from server");
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    watch.markTimeBegin("Set Resource Display for PSS Type");
                    setResourceDisplaySummaryValueForCategories(subject, res, summary, cats, measCache, availCache);
                    summary.setMonitorable(Boolean.TRUE);
                    watch.markTimeEnd("Set Resource Display for PSS Type");
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                    watch.markTimeBegin("Group Type");
                    summary.setMonitorable(Boolean.TRUE);
                    // Set the availability now
                    summary.setAvailability(new Double(getAvailability(subject, aeid, measCache, availCache)));
                    try {
                        // Get the availability template
                        MeasurementTemplate tmpl = getAvailabilityMetricTemplate(subject, aeid, measCacheById);
                        summary.setAvailTempl(tmpl.getId());
                    } catch (MeasurementNotFoundException e) {
                        // No availability metric, don't set it
                    }
                    watch.markTimeEnd("Group Type");
                    break;
                default:
                    throw new InvalidAppdefTypeException(
                        "entity type is not monitorable, id type: " + aeid.getType());
            }
            setResourceDisplaySummary(summary, res, aeid, parent);
            summaries.add(summary);
        }
        if (log.isDebugEnabled()) {
            log.debug("getGroupCurrentHealth: " + watch);
        }
        summaries.setTotalSize(summaries.size());
        return summaries;
    }

    /**
     * Return a ResourceDisplaySummary bean for each of the resource's virtual
     * resources. Each bean represents a resource and the measurement data
     * summarized for that resource. </p>
     * <p>
     * see screen 2.2.2
     * </p>
     * @return List of ResourceDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceDisplaySummary> findVirtualsCurrentHealth(int sessionId,
                                                                  AppdefEntityID entId)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, PermissionException {

        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        List<AppdefResourceValue> resources = virtualManager.findVirtualResourcesByPhysical(
            subject, entId);
        PageList resPageList = new PageList(resources, resources.size());
        return getResourcesCurrentHealth(subject, resPageList);

    }

    private void setResourceDisplaySummary(ResourceDisplaySummary rds, AppdefEntityID aeid,
                                           AppdefResourceValue parentResource)
    throws AppdefEntityNotFoundException, PermissionException {
        Resource resource = resourceManager.findResource(aeid);
        rds.setEntityId(aeid);
        rds.setResourceName(resource.getName()); 
        rds.setResourceEntityTypeName(aeid.getTypeName());
        rds.setResourceTypeName(resource.getPrototype().getName());
        if (parentResource == null) {
            rds.setHasParentResource(Boolean.FALSE); 
        } else {
            rds.setParentResourceId(parentResource.getId());
            rds.setParentResourceName(parentResource.getName());
            rds.setParentResourceTypeId(new Integer(parentResource.getEntityId().getType()));
            rds.setHasParentResource(Boolean.TRUE);
        }
    }

    private void setResourceDisplaySummary(ResourceDisplaySummary rds, Resource resource,
                                           AppdefEntityID aeid, AppdefResourceValue parentResource)
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
            rds.setParentResourceTypeId(new Integer(parentResource.getEntityId().getType()));
            rds.setHasParentResource(Boolean.TRUE);
        }
    }

    private PageList<ResourceDisplaySummary> getResourcesCurrentHealth(AuthzSubject subject, Collection<?> resources)
    throws AppdefEntityNotFoundException, PermissionException {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        final Collection<ResourceDisplaySummary> summaries =
            new TreeSet<ResourceDisplaySummary>(new Comparator<ResourceDisplaySummary>() {
                public int compare(ResourceDisplaySummary o1, ResourceDisplaySummary o2) {
                    return o1.getResourceName().compareTo(o2.getResourceName());
                }
        });
        if (debug) watch.markTimeBegin("getAvailMeasurements");
        final Map<Resource, List<Measurement>> measCache = measurementManager.getAvailMeasurementsByResource(resources);
        final Map<Integer, List<Measurement>> measCacheById = new HashMap<Integer, List<Measurement>>();
        for (Map.Entry<Resource, List<Measurement>> entry : measCache.entrySet()) {
            measCacheById.put(entry.getKey().getId(), entry.getValue());
        }
        if (debug) watch.markTimeEnd("getAvailMeasurements");
        if (debug) watch.markTimeBegin("getLastAvail");
        final Map<Integer, MetricValue> availCache = availabilityManager.getLastAvail(resources, measCacheById);
        if (debug) watch.markTimeEnd("getLastAvail");
        for (final Iterator<?> it = resources.iterator(); it.hasNext();) {
            try {
                Object o = it.next();
                AppdefEntityID aeid;
                if (o instanceof AppdefEntityValue) {
                    AppdefEntityValue rv = (AppdefEntityValue) o;
                    aeid = rv.getID();
                } else if (o instanceof Resource) {
                    aeid = AppdefUtil.newAppdefEntityId((Resource) o);
                } else if (o instanceof AppdefEntityID) {
                    aeid = (AppdefEntityID) o;
                } else {
                    AppdefResourceValue resource = (AppdefResourceValue) o;
                    aeid = resource.getEntityId();
                }
                ResourceDisplaySummary summary = new ResourceDisplaySummary();
                // Set the resource
                AppdefResourceValue parent = null;
                HashSet<String> categories = new HashSet<String>(4);
                switch (aeid.getType()) {
                    case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                        if (debug) watch.markTimeBegin("getPlatform");
                        Server server = serverManager.findServerById(aeid.getId());
                        parent = server.getPlatform().getAppdefResourceValue();
                        if (debug) watch.markTimeEnd("getPlatform");
                    case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                        categories.add(MeasurementConstants.CAT_AVAILABILITY);
                        // XXX scottmf need to review this, perf is bad and metric
                        // is not very useful categories.add(MeasurementConstants.CAT_THROUGHPUT);
                        if (debug) watch.markTimeBegin("setResourceDisplaySummaryValueForCategory");
                        setResourceDisplaySummaryValueForCategory(subject, aeid, summary, categories, measCache, availCache);
                        if (debug) watch.markTimeEnd("setResourceDisplaySummaryValueForCategory");
                        summary.setMonitorable(Boolean.TRUE);
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                    case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                        if (debug) watch.markTimeBegin("Group Type");
                        summary.setMonitorable(Boolean.TRUE);
                        // Set the availability now
                        double avail = getAvailability(subject, aeid, measCache, availCache);
                        summary.setAvailability(new Double(avail));
                        try {
                            // Get the availability template
                            MeasurementTemplate tmpl = getAvailabilityMetricTemplate(subject, aeid, measCacheById);
                            summary.setAvailTempl(tmpl.getId());
                        } catch (MeasurementNotFoundException e) {
                            // No availability metric, don't set it
                        }
                        if (debug) watch.markTimeEnd("Group Type");
                        break;
                    default:
                        throw new InvalidAppdefTypeException(
                            "entity type is not monitorable, id type: " + aeid.getType());
                }
                if (debug) watch.markTimeBegin("setResourceDisplaySummary");
                setResourceDisplaySummary(summary, aeid, parent);
                if (debug) watch.markTimeEnd("setResourceDisplaySummary");
                summaries.add(summary);
            } catch (AppdefEntityNotFoundException e) {
                log.debug(e.getMessage(), e);
            }
        }
        if (debug) log.debug("getResourcesCurrentHealth: " + watch);
        final PageList<ResourceDisplaySummary> rtn = new PageList<ResourceDisplaySummary>(summaries, resources.size());
        return rtn;
    }

    /**
     * @param id the AppdefEntityID of the resource our ResourceDisplaySummary
     *        is for
     * @param summary a ResourceDisplaySummary
     * @param availCache Map<Integer, MetricValue> Integer =>
     *        Measurement.getId(), may be null
     * @param measCache Map<Integer, List<Measurement>> Integer =>
     *        Resource.getId(), may be null
     * @throws PermissionException
     * @throws AppdefEntityNotFoundException
     */
    private void setResourceDisplaySummaryValueForCategory(AuthzSubject subject, AppdefEntityID aeid,
                                                           ResourceDisplaySummary summary,
                                                           Set<String> categories,
                                                           Map<Resource, List<Measurement>> measCache,
                                                           Map<Integer, MetricValue> availCache)
        throws AppdefEntityNotFoundException, PermissionException {
        // Maybe we're not doing anything
        if (categories.size() == 0) {
            return;
        }
        final Resource resource = resourceManager.findResource(aeid);
        MetricValue mv = null;
        if (categories.remove(MeasurementConstants.CAT_AVAILABILITY)) {
            Measurement dm = null;
            // try to use prefetched caches
            if (measCache != null) {
                List<Measurement> list = measCache.get(resource);
                if (list == null) {
                    // nothing to do
                } else if (list.size() == 1) {
                    dm = list.get(0);
                    mv = availCache.get(dm.getId());
                    if (mv != null) {
                        summary.setAvailability(mv.getObjectValue());
                        summary.setAvailTempl(dm.getTemplate().getId());
                    }
                }
            }
            // check if prefetched caches didn't contain values
            dm = (dm == null) ? measurementManager.getAvailabilityMeasurement(subject, aeid) : dm;
            if (dm != null && mv == null) {
                summary.setAvailability(new Double(getAvailability(subject, aeid, measCache, availCache)));
                summary.setAvailTempl(dm.getTemplate().getId());
            }
        }
        if (categories.size() == 0) {
            return;
        }
        List<Measurement> measurements = findDesignatedMetrics(subject, aeid, categories);
        // Optimization for the fact that we can have multiple indicator
        // metrics for each category
        HashSet<String> done = new HashSet<String>();
        for (Measurement m : measurements) {

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
            // XXX mv = dataManager.getLastHistoricalData(m);
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
     * @param id the AppdefEntityID of the resource our ResourceDisplaySummary
     *        is for
     * @param summary a ResourceDisplaySummary
     * @param availCache Map<Integer, MetricValue> Integer =>
     *        Measurement.getId(), may be null
     * @param measCache Map<Integer, List<Measurement>> Integer =>
     *        Resource.getId(), may be null
     * @throws PermissionException
     * @throws AppdefEntityNotFoundException
     */
    private void setResourceDisplaySummaryValueForCategories(AuthzSubject subject, Resource res,
                                                             ResourceDisplaySummary summary,
                                                             Map<String, Map<Resource, Measurement>> categories,
                                                             Map<Resource, List<Measurement>> measCache,
                                                             Map<Integer, MetricValue> availCache)
        throws AppdefEntityNotFoundException, PermissionException {
        StopWatch watch = new StopWatch();
        List<Measurement> resMetrics = new ArrayList<Measurement>(categories.size());
        // First find the measurements
        for (Map<Resource, Measurement> metrics : categories.values()) {
            if (metrics.containsKey(res)) {
                resMetrics.add(metrics.remove(res));
            }
        }
        final AppdefEntityID id = AppdefUtil.newAppdefEntityId(res);
        for (Measurement m : resMetrics) {
            final MeasurementTemplate templ = m.getTemplate();
            final String category = templ.getCategory().getName();
            if (category.equals(MeasurementConstants.CAT_AVAILABILITY)) {
                watch.markTimeBegin("getAvailability");
                MetricValue mv = null;
                if (availCache != null) {
                    Integer mid = m.getId();
                    mv = availCache.get(mid);
                }
                Double val = (mv == null) ? new Double(getAvailability(subject, id, measCache, availCache)) : mv.getObjectValue();
                summary.setAvailability(val);
                summary.setAvailTempl(templ.getId());
                watch.markTimeEnd("getAvailability");
                continue;
            }
            // XXX scottmf, need to investigate this for perf
            // MetricValue mv = dataManager.getLastHistoricalData(m);
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
        if (log.isDebugEnabled()) {
            log.debug("setResourceDisplaySummaryValueForCategories: " + watch);
        }
    }

    /**
     * Method findResourcesCurrentHealth.
     * 
     * The size of the list of ResourceDisplaySummary beans returned will be
     * equivalent to the size of the entity ID's passed in. Called by RSS feed
     * so it does not require valid session ID.
     * 
     * @throws ApplicationException if user is not found
     * @throws LoginException if user account has been disabled
     * @return PageList of ResourceDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceDisplaySummary> findResourcesCurrentHealth(String user, AppdefEntityID[] entIds)
        throws LoginException, ApplicationException, PermissionException,
        AppdefEntityNotFoundException, SessionNotFoundException, SessionTimeoutException {
        int sessionId = authBoss.getUnauthSessionId(user);
        return findResourcesCurrentHealth(sessionId, entIds);
    }

    /**
     * Method findResourcesCurrentHealth.
     * 
     * The size of the list of ResourceDisplaySummary beans returned will be
     * equivalent to the size of the entity ID's passed in.
     * 
     * @return PageList of ResourceDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceDisplaySummary> findResourcesCurrentHealth(int sessionId, AppdefEntityID[] entIds)
        throws AppdefEntityNotFoundException, PermissionException, SessionNotFoundException,
        SessionTimeoutException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        Log timingLog = LogFactory.getLog("DASHBOARD-TIMING");
        StopWatch timer = new StopWatch();

        PageList resources = new PageList(Arrays.asList(entIds), entIds.length);
        timingLog.trace("findResourceCurrentHealth(2) - timing [" + timer.toString() + "]");

        return getResourcesCurrentHealth(subject, resources);
    }

    /**
     * Find the current health of the entity's host(s)
     * 
     * @return PageList of ResourceDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceDisplaySummary> findHostsCurrentHealth(int sessionId, AppdefEntityID aeid, PageControl pc)
    throws SessionNotFoundException, SessionTimeoutException, PermissionException, AppdefEntityNotFoundException {
        final AuthzSubject subj = sessionManager.getSubject(sessionId);
        final Resource resource = resourceManager.findResource(aeid);
        if (resource == null || resource.isInAsyncDeleteState()) {
            return Collections.emptyList();
        }
        final Collection<Resource> hosts =
            resourceManager.getParentResources(subj, resource, resourceManager.getContainmentRelation());
        return getResourcesCurrentHealth(subj, hosts);
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
     * When the entId is a compatible group of platforms, the returned list will
     * have as many elements as there are individual PlatformValue's to
     * represent all of the hosts.
     * 
     * @return PageList of ResourceDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ResourceDisplaySummary> findPlatformsCurrentHealth(int sessionId, AppdefEntityID entId,
                                                                       PageControl pc)
    throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
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
     * If the entId is a platform, the deployed servers view shows the current
     * health of servers.
     * 
     * @return a list of ResourceDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceDisplaySummary> findAGPlatformsCurrentHealthByType(int sessionId,
                                                                           Integer platTypeId)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException,
        PermissionException, AppdefEntityNotFoundException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        PlatformType pt = platformManager.findPlatformType(platTypeId);
        List<Platform> platforms = platformManager.getPlatformsByType(subject, pt.getName());

        // Need list of AppdefEntityValues
        PageList<AppdefEntityValue> aevs = new PageList<AppdefEntityValue>();
        for (Platform platform : platforms) {
            aevs.add(new AppdefEntityValue(subject, platform));
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
     * This is used for all of the application monitoring screens; they all show
     * a list with current health data for each server that participates in
     * supplying services for an application. So if the entity is an
     * application, the list is populated with servers that host the services on
     * which the application relies. The timeframe is not used in this context,
     * the list of servers is always the current list. The timeframe shall still
     * be sent but it will be bounded be the current time and current time -
     * default time window. (see 2.1.2 - 2.1.2.1-3)
     * 
     * If the entId is a platform, the deployed servers view shows the current
     * health of servers in the timeframe that the metrics are shown for. So if
     * the entity is application, expect to populate the list based on the
     * presence of metrics in the timeframe of interest. (see 2.2.2.3, it shows
     * deployed servers... I'll give you a dollar if you can come up with a
     * reason why we'd want internal servers. We aren't managing cron or syslog,
     * dude.)
     * 
     * This is also used for a services' current health page in which case the
     * appdef entity is a service.
     * 
     * @param entId the platform's or application's ID
     * @return a list of ResourceDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ResourceDisplaySummary> findServersCurrentHealth(int sessionId,
                                                                     AppdefEntityID entId,
                                                                     PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException,
        AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        switch (entId.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                break;
            default:
                throw new InvalidAppdefTypeException("entityID is not valid type, id:type=" + entId);
        }

        AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
        PageList servers = rv.getAssociatedServers(pc);

        // Return a paged list of current health
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        if (debug) watch.markTimeBegin("getResourcesCurrentHealth");
        PageList<ResourceDisplaySummary> rtn = getResourcesCurrentHealth(subject, servers);
        if (debug) watch.markTimeEnd("getResourcesCurrentHealth");
        if (debug) log.debug(watch);
        return rtn;
    }

    /**
     * Method findServersCurrentHealth
     * 
     * For platform's autogroup of servers.
     * 
     * If the entId is a platform, the deployed servers view shows the current health of servers.
     * 
     * @return a list of ResourceDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceDisplaySummary> findAGServersCurrentHealthByType(int sessionId, AppdefEntityID[] entIds,
                                                                         Integer serverTypeId)
    throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException, AppdefEntityNotFoundException,
           PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        final Collection<AppdefEntityID> aeids = Arrays.asList(entIds);
        final Collection<Resource> resources = new ArrayList<Resource>(entIds.length);
        for (final AppdefEntityID aeid : aeids) {
            if (aeid.getType() != AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
                throw new InvalidAppdefTypeException("findServersCurrentHealthByType() only allows Platforms, " +
                                                     "id:type=" + aeid);
            }
            final Resource resource = resourceManager.findResource(aeid);
            resources.add(resource);
        }
        final AppdefEntityTypeID appdefType = new AppdefEntityTypeID(AppdefEntityConstants.APPDEF_TYPE_SERVER, serverTypeId);
        final Resource prototype = resourceManager.findResourcePrototype(appdefType);
        final ResourceRelation relation = resourceManager.getContainmentRelation();
        final Collection<Resource> childResources =
            resourceManager.getDescendantResources(subject, resources, relation, prototype, true);
        return getResourcesCurrentHealth(subject, childResources);
    }

    /**
     * Return a ResourceDisplaySummary bean for each of the resource's services.
     * The only applicable resource is currently a compatible group (of
     * services...)
     * @return a list of ResourceDisplaySummary beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceDisplaySummary> findAGServicesCurrentHealthByType(int sessionId,
                                                                          AppdefEntityID[] entIds,
                                                                          Integer serviceTypeId)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException,
        AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        PageList services = new PageList();

        for (int i = 0; i < entIds.length; i++) {
            AppdefEntityID entId = entIds[i];
            AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
            services.addAll(rv.getAssociatedServices(serviceTypeId, PageControl.PAGE_ALL));
        }

        // Return a paged list of current health
        return getResourcesCurrentHealth(subject, services);
    }

    /**
     * Get Availability measurement for a given entitiy
     * 
     */
    @Transactional(readOnly = true)
    public double getAvailability(AuthzSubject subj, AppdefEntityID id)
        throws AppdefEntityNotFoundException, PermissionException {
        if (id == null) {
            return MeasurementConstants.AVAIL_UNKNOWN;
        }
        final Map<Resource, List<Measurement>> measCache =
            measurementManager.getAvailMeasurementsByResource(Collections.singletonList(id));
        final Map<Integer, List<Measurement>> measCacheById = new HashMap<Integer, List<Measurement>>();
        for (Map.Entry<Resource, List<Measurement>> entry : measCache.entrySet()) {
            measCacheById.put(entry.getKey().getId(), entry.getValue());
        }
        Map<Integer, MetricValue> availCache = null;
        if (id.isApplication()) {
            List<Resource> members = applicationManager.getApplicationResources(subj, id.getId());
            availCache = availabilityManager.getLastAvail(members, measCacheById);
        }
        return getAvailability(subj, id, measCache, availCache);
    }

    /**
     * @param measCache optional cache of <Integer, List<Measurement>> Integer
     *        => Resource.getId(), may be null
     * @param availCache optional cache of <Integer, MetricValue> Integer =>
     *        Measurement.getId(), may be null
     */
    @Transactional(readOnly = true)
    private double getAvailability(AuthzSubject subject, AppdefEntityID id, Map<Resource, List<Measurement>> measCache,
                                   Map<Integer, MetricValue> availCache)
    throws AppdefEntityNotFoundException, PermissionException {
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        if (debug) log.debug("BEGIN getAvailability() id=" + id);
        try {
            if (id.isGroup()) {
                if (debug) watch.markTimeBegin("getGroupAvailability");
                double rtn = getGroupAvailability(subject, id.getId(), measCache, availCache);
                if (debug) watch.markTimeEnd("getGroupAvailability");
                return rtn;
            } else if (id.isApplication()) {
                AppdefEntityValue appVal = new AppdefEntityValue(id, subject);
                if (debug) watch.markTimeBegin("getFlattenedServiceIds");
                AppdefEntityID[] services = appVal.getFlattenedServiceIds();
                if (debug) watch.markTimeEnd("getFlattenedServiceIds");
                if (debug) watch.markTimeBegin("getAggregateAvailability");
                double rtn = getAggregateAvailability(subject, services, measCache, availCache);
                if (debug) watch.markTimeEnd("getAggregateAvailability");
                return rtn;
            }
            AppdefEntityID[] ids = new AppdefEntityID[] { id };
            return getAvailability(subject, ids, getMidMap(ids, measCache), availCache)[0];
        } catch (RuntimeException e) {
            log.error(e,e);
            throw e;
        } finally {
            if (debug) log.debug("END getAvailability() id=" + id + " -- " + watch);
        }
    }

    /**
     * Get the availability of the resource
     * @param id the Appdef entity ID
     * 
     */
    @Transactional(readOnly = true)
    public double getAvailability(int sessionId, AppdefEntityID id)
    throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        return getAvailability(subject, id);
    }

    /**
     * Get the availability of autogroup resources
     * @return a MetricValue for the availability
     * 
     */
    @Transactional(readOnly = true)
    public double getAGAvailability(int sessionId, AppdefEntityID[] aids, AppdefEntityTypeID ctype)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
        PermissionException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);

        StopWatch watch = new StopWatch();
        log.debug("BEGIN getAGAvailability()");

        List<AppdefEntityID> appdefIds = getAGMemberIds(subject, aids, ctype);

        double ret = getAggregateAvailability(subject, toAppdefEntityIDArray(appdefIds), null, null);
        log.debug("END getAGAvailability() -- " + watch.getElapsed() + " msec");
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
     * 
     */
    @Transactional(readOnly = true)
    public List<ProblemMetricSummary> findAllMetrics(int sessionId, AppdefEntityID aeid,
                                                     AppdefEntityTypeID ctype, long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
        PermissionException, AppdefCompatException, InvalidAppdefTypeException {
        ArrayList<ProblemMetricSummary> result = new ArrayList<ProblemMetricSummary>();
        AppdefEntityID[] entIds = new AppdefEntityID[] { aeid };

        Map<String, Set<MetricDisplaySummary>> metrics = findAGMetricsByType(sessionId, entIds,
            ctype, MeasurementConstants.FILTER_NONE, null, begin, end, false);
        for (Collection<MetricDisplaySummary> metricColl : metrics.values()) {

            for (MetricDisplaySummary summary : metricColl) {

                ProblemMetricSummary pms = new ProblemMetricSummary(summary);
                pms.setMultipleAppdefKey(ctype.getAppdefKey());
                result.add(pms);
            }
        }
        return result;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<ProblemMetricSummary> findAllMetrics(int sessionId, AppdefEntityID aeid,
                                                     long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
        PermissionException, AppdefCompatException, InvalidAppdefTypeException {
        ArrayList<ProblemMetricSummary> result = new ArrayList<ProblemMetricSummary>();
        Map<String, Set<MetricDisplaySummary>> metrics = findMetrics(sessionId, aeid, begin, end,
            PageControl.PAGE_ALL);
        for (Collection<MetricDisplaySummary> metricColl : metrics.values()) {

            for (MetricDisplaySummary summary : metricColl) {

                ProblemMetricSummary pms = new ProblemMetricSummary(summary);
                pms.setSingleAppdefKey(aeid.getAppdefKey());
                result.add(pms);
            }
        }
        return result;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<ProblemMetricSummary> findAllMetrics(int sessionId, AppdefEntityID[] aeids,
                                                     long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
        PermissionException, AppdefCompatException, InvalidAppdefTypeException {
        ArrayList<ProblemMetricSummary> result = new ArrayList<ProblemMetricSummary>();
        Map<String, Set<MetricDisplaySummary>> metrics = findMetrics(sessionId, aeids,
            MeasurementConstants.FILTER_NONE, null, begin, end, false);
        for (Collection<MetricDisplaySummary> metricColl : metrics.values()) {

            for (MetricDisplaySummary summary : metricColl) {

                ProblemMetricSummary pms = new ProblemMetricSummary(summary);
                result.add(pms);
            }
        }
        return result;
    }

    /**
     * Returns a list of problem metrics for a resource, and the selected
     * children and hosts of that resource. Return a summarized list of UI beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ProblemMetricSummary> findAllMetrics(int sessionId, AppdefEntityID aeid, AppdefEntityID[] hosts,
                                                     AppdefEntityTypeID[] children, AppdefEntityID[] members,
                                                     long begin, long end)
    throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException,
           AppdefCompatException, InvalidAppdefTypeException {
        List<AppdefEntityID> singlesList = new ArrayList<AppdefEntityID>();

        if (aeid != null && members == null) {
            singlesList.add(aeid);
        }

        // Next add its hosts
        if (hosts != null) {
            singlesList.addAll(Arrays.asList(hosts));
        }

        ArrayList<ProblemMetricSummary> result = new ArrayList<ProblemMetricSummary>();

        // Go through the singles list first
        for (AppdefEntityID entity : singlesList) {

            result.addAll(findAllMetrics(sessionId, entity, begin, end));
        }

        if (members != null) {
            // AutoGroups and groups pass their entities in the entities
            // parameter
            List<ProblemMetricSummary> metrics = findAllMetrics(sessionId, members, begin, end);
            for (ProblemMetricSummary metric : metrics) {

                if (aeid.isGroup()) {
                    metric.setSingleAppdefKey(aeid.getAppdefKey());
                } else {
                    metric.setMultipleAppdefKey(children[0].getAppdefKey());
                }
            }

            result.addAll(metrics);
        } else if (children != null) {
            // Go through the children list next
            for (int i = 0; i < children.length; i++) {
                result.addAll(findAllMetrics(sessionId, aeid, children[i], begin, end));
            }
        }

        return result;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public double[] getAvailability(AuthzSubject subject, AppdefEntityID[] ids)
        throws AppdefEntityNotFoundException, PermissionException {
        // Allow for the maximum window based on collection interval
        return getAvailability(subject, ids, getMidMap(getResources(ids)), null);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<AppdefEntityID> getAGMemberIds(AuthzSubject subject, AppdefEntityID parentAid,
                                               AppdefEntityTypeID ctype)
        throws AppdefEntityNotFoundException, PermissionException {
        return getAGMemberIds(subject, new AppdefEntityID[] { parentAid }, ctype);
    }

    /**
     * Returns a list of problem metrics for a resource, and the selected
     * children and hosts of that resource. Return a summarized list of UI beans
     * 
     */
    @Transactional(readOnly = true)
    public List<ProblemMetricSummary> findAllMetrics(int sessionId, AppdefEntityID aeid, AppdefEntityID[] hosts,
                                                     AppdefEntityTypeID[] children, long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
               PermissionException, AppdefCompatException, InvalidAppdefTypeException {
        List<AppdefEntityID> singlesList = new ArrayList<AppdefEntityID>();

        if (aeid != null) {
            singlesList.add(aeid);
        }

        // Next add its hosts
        if (hosts != null) {
            singlesList.addAll(Arrays.asList(hosts));
        }

        ArrayList<ProblemMetricSummary> result = new ArrayList<ProblemMetricSummary>();

        if (aeid == null || (aeid.isGroup() && hosts != null)) {
            // AutoGroups and groups pass their entities in the hosts
            // parameter
            List<ProblemMetricSummary> metrics = findAllMetrics(sessionId, hosts, begin, end);

            for (ProblemMetricSummary metric : metrics) {

                if (aeid != null) {
                    metric.setSingleAppdefKey(aeid.getAppdefKey());
                } else {
                    metric.setMultipleAppdefKey(children[0].getAppdefKey());
                }
            }

            result.addAll(metrics);
        } else {
            // Go through the singles list first
            for (AppdefEntityID entity : singlesList) {
                result.addAll(findAllMetrics(sessionId, entity, begin, end));
            }

            // Go through the children list next
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    result.addAll(findAllMetrics(sessionId, aeid, children[i], begin, end));
                }
            }
        }

        return result;
    }

    /**
     * Get the availability metric for a given resource
     * 
     */
    @Transactional(readOnly = true)
    public Measurement findAvailabilityMetric(int sessionId, AppdefEntityID id)
        throws SessionTimeoutException, SessionNotFoundException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        return measurementManager.getAvailabilityMeasurement(subject, id);
    }

}
