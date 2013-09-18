/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], VMWare, Inc.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.StringUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The TemplateManager can be used to interact with Templates
 */
@Service
@Transactional
public class TemplateManagerImpl implements TemplateManager {
    private final Log log = LogFactory.getLog(TemplateManagerImpl.class);
   
    private MeasurementDAO measurementDAO;
    private MeasurementTemplateDAO measurementTemplateDAO;
    private MonitorableTypeDAO monitorableTypeDAO;
    private SRNManager srnManager;

    private ZeventManager zeventManager;

    @Autowired
    public TemplateManagerImpl(MeasurementDAO measurementDAO,
                               MeasurementTemplateDAO measurementTemplateDAO,
                               MonitorableTypeDAO monitorableTypeDAO,
                               SRNManager srnManager, ZeventManager zeventManager) {
        this.measurementDAO = measurementDAO;
        this.measurementTemplateDAO = measurementTemplateDAO;
        this.monitorableTypeDAO = monitorableTypeDAO;
        this.srnManager = srnManager;
        this.zeventManager = zeventManager;
    }

    /**
     * Get a MeasurementTemplate
     */
    @Transactional(readOnly = true)
    public MeasurementTemplate getTemplate(Integer id) {
        return measurementTemplateDAO.get(id);
    }

    /**
     * Look up measurement templates for an array of template IDs
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> getTemplates(List<Integer> ids) {
        Integer[] mtids = ids.toArray(new Integer[ids.size()]);
        return measurementTemplateDAO.findTemplates(mtids);
    }

    /**
     * Look up a measurement templates for an array of template IDs
     * 
     * @throws TemplateNotFoundException if no measurement templates are found.
     * @return a MeasurementTemplate value
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> getTemplates(Integer[] ids, PageControl pc)
        throws TemplateNotFoundException {
        List<MeasurementTemplate> mts = measurementTemplateDAO.findTemplates(ids);

        if (ids.length != mts.size()) {
            throw new TemplateNotFoundException("Could not look up " +
                                                StringUtil.arrayToString(ids));
        }

        if (pc.getSortorder() == PageControl.SORT_DESC) {
            Collections.reverse(mts);
        }

        return mts;
    }

    /**
     * Get all the templates. Must be superuser to execute.
     * 
     * @param pInfo must contain a sort field of type
     *        {@link MeasurementTemplateSortField}
     * @param defaultOn If non-null, return templates with defaultOn ==
     *        defaultOn
     * 
     * @return a list of {@link MeasurementTemplate}s
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> findTemplates(AuthzSubject user, PageInfo pInfo,
                                                   Boolean defaultOn) throws PermissionException {
        assertSuperUser(user);
        return measurementTemplateDAO.findAllTemplates(pInfo, defaultOn);
    }

    /**
     * Get all templates for a given MonitorableType
     * 
     * @param pInfo must contain a sort field of type
     *        {@link MeasurementTemplateSortField}
     * @param defaultOn If non-null, return templates with defaultOn ==
     *        defaultOn
     * 
     * @return a list of {@link MeasurementTemplate}s
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> findTemplatesByMonitorableType(AuthzSubject user,
                                                                    PageInfo pInfo, String type,
                                                                    Boolean defaultOn)
        throws PermissionException {
        assertSuperUser(user);
        return measurementTemplateDAO.findTemplatesByMonitorableType(pInfo, type, defaultOn);
    }

    private void assertSuperUser(AuthzSubject s) throws PermissionException {
        boolean authorized = PermissionManagerFactory.getInstance().hasAdminPermission(s.getId());

        if (!authorized) {
            throw new PermissionException("Permission denied");
        }
    }

    /**
     * Look up a measurement templates for a monitorable type and category.
     * 
     * @return a MeasurementTemplate value
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> findTemplates(String type, String cat, Integer[] excludeIds,
                                                   PageControl pc) {
        List<MeasurementTemplate> templates;
        if (cat == null) {
            templates = measurementTemplateDAO.findTemplatesByMonitorableType(type);
        } else {
            templates = measurementTemplateDAO.findTemplatesByMonitorableTypeAndCategory(type, cat);
        }

        if (templates == null) {
            return new PageList<MeasurementTemplate>();
        }

        // Handle excludes
        List<MeasurementTemplate> includes;
        if (excludeIds == null) {
            includes = templates;
        } else {
            HashSet<Integer> excludes = new HashSet<Integer>(Arrays.asList(excludeIds));
            includes = new ArrayList<MeasurementTemplate>();
            for (MeasurementTemplate tmpl : templates) {
                if (!excludes.contains(tmpl.getId()))
                    includes.add(tmpl);
            }
        }

        pc = PageControl.initDefaults(pc, -1);
        if (pc.getSortorder() == PageControl.SORT_DESC) {
            Collections.reverse(includes);
        }

        return templates;
    }

    /**
     * Look up a measurement templates for a monitorable type and filtered by
     * categories and keyword.
     * 
     * @return a MeasurementTemplate value
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> findTemplates(String type, long filters, String keyword) {
        MeasurementTemplateDAO dao = measurementTemplateDAO;
        List<MeasurementTemplate> mts;

        if ((filters & MeasurementConstants.FILTER_AVAIL) == 0 ||
            (filters & MeasurementConstants.FILTER_UTIL) == 0 ||
            (filters & MeasurementConstants.FILTER_THRU) == 0 ||
            (filters & MeasurementConstants.FILTER_PERF) == 0) {
            mts = new ArrayList<MeasurementTemplate>();

            // Go through each filter
            if ((filters & MeasurementConstants.FILTER_AVAIL) > 0) {
                mts.addAll(dao.findTemplatesByMonitorableTypeAndCategory(type,
                    MeasurementConstants.CAT_AVAILABILITY));
            }
            if ((filters & MeasurementConstants.FILTER_UTIL) > 0) {
                mts.addAll(dao.findTemplatesByMonitorableTypeAndCategory(type,
                    MeasurementConstants.CAT_UTILIZATION));
            }
            if ((filters & MeasurementConstants.FILTER_THRU) > 0) {
                mts.addAll(dao.findTemplatesByMonitorableTypeAndCategory(type,
                    MeasurementConstants.CAT_THROUGHPUT));
            }
            if ((filters & MeasurementConstants.FILTER_PERF) > 0) {
                mts.addAll(dao.findTemplatesByMonitorableTypeAndCategory(type,
                    MeasurementConstants.CAT_PERFORMANCE));
            }
        } else {
            mts = dao.findTemplatesByMonitorableType(type);
        }

        if (mts == null) {
            return new PageList<MeasurementTemplate>();
        }

        // Check filter types
        for (Iterator<MeasurementTemplate> it = mts.iterator(); it.hasNext();) {
            MeasurementTemplate tmpl = it.next();

            // First, keyword
            if (StringUtil.stringDoesNotExist(tmpl.getName(), keyword)) {
                it.remove();
                continue;
            }

            switch (tmpl.getCollectionType()) {
                case MeasurementConstants.COLL_TYPE_DYNAMIC:
                    if ((filters & MeasurementConstants.FILTER_DYN) == 0) {
                        it.remove();
                    }
                    break;
                case MeasurementConstants.COLL_TYPE_STATIC:
                    if ((filters & MeasurementConstants.FILTER_STATIC) == 0) {
                        it.remove();
                    }
                    break;
                case MeasurementConstants.COLL_TYPE_TRENDSUP:
                    if ((filters & MeasurementConstants.FILTER_TREND_UP) == 0) {
                        it.remove();
                    }
                    break;
                case MeasurementConstants.COLL_TYPE_TRENDSDOWN:
                    if ((filters & MeasurementConstants.FILTER_TREND_DN) == 0) {
                        it.remove();
                    }
                    break;
                default:
                    break;
            }
        }

        return mts;
    }

    /**
     * Look up a measurement template IDs for a monitorable type.
     * 
     * @return an array of ID values
     */
    @Transactional(readOnly = true)
    public Integer[] findTemplateIds(String type) {
        List<MeasurementTemplate> mts = measurementTemplateDAO.findTemplatesByMonitorableType(type);

        if (mts == null) {
            return new Integer[0];
        }

        Integer[] ids = new Integer[mts.size()];
        Iterator<MeasurementTemplate> it = mts.iterator();
        for (int i = 0; it.hasNext(); i++) {
            MeasurementTemplate tmpl = it.next();
            ids[i] = tmpl.getId();
        }
        return ids;
    }

    @Transactional(readOnly = true)
    public List<MeasurementTemplate> findTemplatesByName(List<String> tmpNames) {
    	return measurementTemplateDAO.findTemplatesByName(tmpNames);
    }

    /**
     * Update the default interval for a list of meas. templates
     * 
     * @subject - the subject
     * @param templIds - a list of integer template ids
     * @param interval - the interval of collection to set to
     */
    public void updateTemplateDefaultInterval(AuthzSubject subject, Integer[] templIds,
                                              final long interval) {
        List<Integer> idsOfModifiedResources = new ArrayList<Integer>(templIds.length);

        final HashSet<AppdefEntityID> toReschedule = new HashSet<AppdefEntityID>();
        final Map<Integer, Collection<Measurement>> measTemplMap =
            measurementDAO.getMeasurementsByTemplateIds(templIds);
        for (int i = 0; i < templIds.length; i++) {
            final MeasurementTemplate template = measurementTemplateDAO.get(templIds[i]);
            if (template == null) {
                continue;
            }
            if (interval != template.getDefaultInterval()) {
                template.setDefaultInterval(interval);
            }
            if (!template.isDefaultOn()) {
                template.setDefaultOn(interval != 0);
            }
            final Collection<Measurement> measurements = measTemplMap.get(template.getId());
            if (measurements == null) {
                continue;
            }
            for (final Measurement m : measurements) {
                final Resource r = m.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                m.setEnabled(template.isDefaultOn());
                m.setInterval(template.getDefaultInterval());
                final AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(r);
                toReschedule.add(aeid);
                idsOfModifiedResources.add(r.getId());                
            }
        }
        srnManager.scheduleInBackground(toReschedule, true, true);
        enqueueZeventForManualMeasScheduleChange(idsOfModifiedResources);        
    }

    /**
     * Make metrics disabled by default for a list of meas. templates
     * @param templIds - a list of integer template ids
     */
    public void setTemplateEnabledByDefault(AuthzSubject subject, Integer[] templIds, boolean on) {

        long current = System.currentTimeMillis();
        List<Integer> idsOfModifiedResources = new ArrayList<Integer>(templIds.length);
        
        Map<AppdefEntityID, Long> aeids = new HashMap<AppdefEntityID, Long>();
        for (Integer templateId : templIds) {
            MeasurementTemplate template = measurementTemplateDAO.findById(templateId);

            template.setDefaultOn(on);

            List<Measurement> metrics = measurementDAO.findByTemplate(templateId);
            for (Measurement dm : metrics) {
                if (dm.isEnabled() == on) {
                    continue;
                }

                dm.setEnabled(on);
                dm.setMtime(current);

                if (dm.isEnabled() && dm.getInterval() == 0) {
                    dm.setInterval(template.getDefaultInterval());
                }

                final AppdefEntityID aeid = new AppdefEntityID(dm.getAppdefType(), dm
                    .getInstanceId());

                Long min = new Long(dm.getInterval());
                if (aeids.containsKey(aeid)) {
                    // Set the minimum interval
                    min = new Long(Math.min(((Long) aeids.get(aeid)).longValue(), min.longValue()));
                }
                aeids.put(aeid, min);
                
                idsOfModifiedResources.add(dm.getResource().getId());
            }
        }

        for (Map.Entry<AppdefEntityID, Long> entry : aeids.entrySet()) {
            AppdefEntityID aeid = entry.getKey();
            srnManager.incrementSrn(aeid);
        }
        
        enqueueZeventForManualMeasScheduleChange(idsOfModifiedResources);
    }

    /**
     * Enqueue a {@link MeasurementScheduleZevent} on the zevent queue
     * corresponding to a change in schedule for the resource's measurements
     * 
     * @param resourceIds Ids of the resources, whose measurements changed
     */
    private void enqueueZeventForManualMeasScheduleChange(List<Integer> resourceIds) {

        ManualMeasurementScheduleZevent event = new ManualMeasurementScheduleZevent(resourceIds);
        zeventManager.enqueueEventAfterCommit(event);
    }    
    
    @Transactional
    public MonitorableType createMonitorableType(String pluginName, TypeInfo info) {
        int e = info.getType();
        int a = entityInfoTypeToAppdefType(e);
        return monitorableTypeDAO.create(info.getName(), a, pluginName);
    }

    @Transactional(readOnly = true)
    public Map<String, MonitorableType> getMonitorableTypesByName(String pluginName) {
        return monitorableTypeDAO.findByPluginName(pluginName);
    }

    private int entityInfoTypeToAppdefType(int entityInfoType) {
        switch (entityInfoType) {
            case TypeInfo.TYPE_PLATFORM:
                return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
            case TypeInfo.TYPE_SERVER:
                return AppdefEntityConstants.APPDEF_TYPE_SERVER;
            case TypeInfo.TYPE_SERVICE:
                return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
            default:
                throw new IllegalArgumentException("Unknown TypeInfo type");
        }
    }

    /**
     * Update measurement templates for a given entity. This still needs some
     * refactoring.
     * 
     * @return A map of measurement info's that are new and will need to be
     *         created.
     */
    public Map<String, MeasurementInfo> updateTemplates(String pluginName, TypeInfo ownerEntity,
                                                        MonitorableType monitorableType,
                                                        MeasurementInfo[] tmpls) {
        // Organize the templates first
        Map<String, MeasurementInfo> tmap = new HashMap<String, MeasurementInfo>();
        for (int i = 0; i < tmpls.length; i++) {
            tmap.put(tmpls[i].getAlias(), tmpls[i]);
        }

        Collection<MeasurementTemplate> mts = measurementTemplateDAO
            .findRawByMonitorableType(monitorableType);

        for (MeasurementTemplate mt : mts) {
            // See if this is in the list
            MeasurementInfo info = tmap.remove(mt.getAlias());

            if (info == null) {
                measurementDAO.remove(mt);
                measurementTemplateDAO.remove(mt);
            } else {
                measurementTemplateDAO.update(mt, pluginName, info);
            }
        }
        return tmap;
    }

    /**
     * Add new measurement templates for a plugin.
     * 
     * This does a batch style insert
     */
    public void createTemplates(String pluginName,Map<MonitorableType,List<MonitorableMeasurementInfo>> toAdd) {
        measurementTemplateDAO.createTemplates(pluginName, toAdd);
    }

    /** 
     */
    public void setDesignated(MeasurementTemplate tmpl, boolean designated) {
        if (tmpl.isAvailability()) {
            return;
        }
        tmpl.setDesignate(designated);
    }

    /**
     * Set the measurement templates to be "designated" for a monitorable type.
     */
    public void setDesignatedTemplates(String mType, Integer[] desigIds) {

    	List<MeasurementTemplate> derivedTemplates = measurementTemplateDAO.findDerivedByMonitorableType(mType);
    	HashSet<Integer> designates = new HashSet<Integer>();
    	designates.addAll(Arrays.asList(desigIds));
    	
    	for (MeasurementTemplate template : derivedTemplates) {
    		
    		// Never turn off Availability as an indicator
    		if (template.isAvailability())
    			continue;
    		
    		boolean designated = designates.contains(template.getId());
    		if (designated != template.isDesignate())
    			template.setDesignate(designated);
        }
    }
}
