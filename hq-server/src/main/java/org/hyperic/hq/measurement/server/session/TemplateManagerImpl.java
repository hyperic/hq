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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityNotFoundException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.data.CategoryRepository;
import org.hyperic.hq.measurement.data.MeasurementRepository;
import org.hyperic.hq.measurement.data.MeasurementTemplateRepository;
import org.hyperic.hq.measurement.data.MonitorableTypeRepository;
import org.hyperic.hq.measurement.data.ScheduleRevNumRepository;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.TypeInfo;
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
   
    private MeasurementRepository measurementRepository;
    private MeasurementTemplateRepository measurementTemplateRepository;
    private MonitorableTypeRepository monitorableTypeRepository;
    private ScheduleRevNumRepository scheduleRevNumRepository;
    private SRNManager srnManager;
    private SRNCache srnCache;
    private CategoryRepository categoryRepository;

    @Autowired
    public TemplateManagerImpl(MeasurementRepository measurementRepository,
                               MeasurementTemplateRepository measurementTemplateRepository,
                               MonitorableTypeRepository monitorableTypeRepository,
                               ScheduleRevNumRepository scheduleRevNumRepository, SRNManager srnManager,
                               SRNCache srnCache, CategoryRepository categoryRepository) {
        this.measurementRepository = measurementRepository;
        this.measurementTemplateRepository = measurementTemplateRepository;
        this.monitorableTypeRepository = monitorableTypeRepository;
        this.scheduleRevNumRepository = scheduleRevNumRepository;
        this.srnManager = srnManager;
        this.srnCache = srnCache;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Get a MeasurementTemplate
     */
    @Transactional(readOnly = true)
    public MeasurementTemplate getTemplate(Integer id) {
        return measurementTemplateRepository.findOne(id);
    }

    /**
     * Look up measurement templates for an array of template IDs
     */
    @Transactional(readOnly = true)
    public List<MeasurementTemplate> getTemplates(List<Integer> ids) {
        return measurementTemplateRepository.findByIds(ids);
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
        List<MeasurementTemplate> mts = measurementTemplateRepository.findByIds(Arrays.asList(ids));

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
    public List<MeasurementTemplate> findTemplatesByMonitorableType(AuthzSubject user,String type)
        throws PermissionException {
        assertSuperUser(user);
        return measurementTemplateRepository.findByMonitorableTypeOrderByName(type);
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
            templates = measurementTemplateRepository.findByMonitorableTypeOrderByName(type);
        } else {
            templates = measurementTemplateRepository.findByMonitorableTypeNameAndCategoryNameOrderByNameAsc(type, cat);
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
        List<MeasurementTemplate> mts;

        if ((filters & MeasurementConstants.FILTER_AVAIL) == 0 ||
            (filters & MeasurementConstants.FILTER_UTIL) == 0 ||
            (filters & MeasurementConstants.FILTER_THRU) == 0 ||
            (filters & MeasurementConstants.FILTER_PERF) == 0) {
            mts = new ArrayList<MeasurementTemplate>();

            // Go through each filter
            if ((filters & MeasurementConstants.FILTER_AVAIL) > 0) {
                mts.addAll(measurementTemplateRepository.findByMonitorableTypeNameAndCategoryNameOrderByNameAsc(type,
                    MeasurementConstants.CAT_AVAILABILITY));
            }
            if ((filters & MeasurementConstants.FILTER_UTIL) > 0) {
                mts.addAll(measurementTemplateRepository.findByMonitorableTypeNameAndCategoryNameOrderByNameAsc(type,
                    MeasurementConstants.CAT_UTILIZATION));
            }
            if ((filters & MeasurementConstants.FILTER_THRU) > 0) {
                mts.addAll(measurementTemplateRepository.findByMonitorableTypeNameAndCategoryNameOrderByNameAsc(type,
                    MeasurementConstants.CAT_THROUGHPUT));
            }
            if ((filters & MeasurementConstants.FILTER_PERF) > 0) {
                mts.addAll(measurementTemplateRepository.findByMonitorableTypeNameAndCategoryNameOrderByNameAsc(type,
                    MeasurementConstants.CAT_PERFORMANCE));
            }
        } else {
            mts = measurementTemplateRepository.findByMonitorableTypeOrderByName(type);
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
        List<MeasurementTemplate> mts = measurementTemplateRepository.findByMonitorableTypeOrderByName(type);

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

    /**
     * Update the default interval for a list of meas. templates
     * 
     * @subject - the subject
     * @param templIds - a list of integer template ids
     * @param interval - the interval of collection to set to
     */
    public void updateTemplateDefaultInterval(AuthzSubject subject, Integer[] templIds,
                                              long interval) {
        HashSet<AppdefEntityID> toReschedule = new HashSet<AppdefEntityID>();
        for (int i = 0; i < templIds.length; i++) {
            MeasurementTemplate template = measurementTemplateRepository.findOne(templIds[i]);
            if(template == null) {
                throw new EntityNotFoundException("MeasurementTemplate with id: " + templIds[i] + " not found");
            }

            if (interval != template.getDefaultInterval()) {
                template.setDefaultInterval(interval);
            }

            if (!template.isDefaultOn()) {
                template.setDefaultOn(interval != 0);
            }
            final List<Measurement> measurements = measurementRepository.findByTemplate(template.getId());
            for (Measurement m : measurements) {
                m.setEnabled(template.isDefaultOn());
                m.setInterval(template.getDefaultInterval());
            }

            List<Resource> resources = measurementRepository.findMeasurementResourcesByTemplate(template.getId());
            List<AppdefEntityID> appdefEntityIds = new ArrayList<AppdefEntityID>();
            for(Resource resource: resources) {
                appdefEntityIds.add(AppdefUtil.newAppdefEntityId(resource));
            }
            toReschedule.addAll(appdefEntityIds);
        }

        int count = 0;
        for (AppdefEntityID id : toReschedule) {
            ScheduleRevNum srn = srnCache.get(id);
            if (srn != null) {
                srnManager.incrementSrn(id, Math.min(interval, srn.getMinInterval()));
                if (++count % 100 == 0) {
                    scheduleRevNumRepository.flush();
                }
            }
        }

        scheduleRevNumRepository.flush();
    }

    /**
     * Make metrics disabled by default for a list of meas. templates
     * @param templIds - a list of integer template ids
     */
    public void setTemplateEnabledByDefault(AuthzSubject subject, Integer[] templIds, boolean on) {

        long current = System.currentTimeMillis();

        Map<AppdefEntityID, Long> aeids = new HashMap<AppdefEntityID, Long>();
        for (Integer templateId : templIds) {
            MeasurementTemplate template = measurementTemplateRepository.findOne(templateId);
            if(template == null) {
                throw new EntityNotFoundException("MeasurementTemplate with id: " + templateId + " not found");
            }
            template.setDefaultOn(on);

            List<Measurement> metrics = measurementRepository.findByTemplate(templateId);
            for (Measurement dm : metrics) {
                if (dm.isEnabled() == on) {
                    continue;
                }

                dm.setEnabled(on);
                dm.setMtime(current);

                if (dm.isEnabled() && dm.getInterval() == 0) {
                    dm.setInterval(template.getDefaultInterval());
                }

                final AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(dm
                    .getResource());

                Long min = new Long(dm.getInterval());
                if (aeids.containsKey(aeid)) {
                    // Set the minimum interval
                    min = new Long(Math.min(((Long) aeids.get(aeid)).longValue(), min.longValue()));
                }
                aeids.put(aeid, min);
            }
        }

        for (Map.Entry<AppdefEntityID, Long> entry : aeids.entrySet()) {
            AppdefEntityID aeid = entry.getKey();
            ScheduleRevNum srn = srnManager.get(aeid);
            srnManager.incrementSrn(aeid, (srn == null) ? entry.getValue().longValue() : srn
                .getMinInterval());
        }
    }

    @Transactional
    public MonitorableType createMonitorableType(String pluginName, TypeInfo info) {
        MonitorableType type =  new MonitorableType(info.getName(),pluginName);
        return monitorableTypeRepository.save(type);
    }

    @Transactional
    public MonitorableType createMonitorableType(String pluginName, ResourceType resourceType) {
        MonitorableType type =  new MonitorableType(resourceType.getName(),pluginName);
        return monitorableTypeRepository.save(type);
    }
    
    @Transactional(readOnly = true)
    public Map<String, MonitorableType> getMonitorableTypesByName(String pluginName) {
        Map<String,MonitorableType> namesToTypes = new HashMap<String,MonitorableType>();
        List<MonitorableType> types = monitorableTypeRepository.findByPluginName(pluginName);
        for(MonitorableType type: types) {
            namesToTypes.put(type.getName(), type);
        }
        return namesToTypes;
    }

    /**
     * Update measurement templates for a given entity. This still needs some
     * refactoring.
     * 
     * @return A map of measurement info's that are new and will need to be
     *         created.
     */
    public Map<String, MeasurementInfo> updateTemplates(String pluginName,
                                                        MonitorableType monitorableType,
                                                        MeasurementInfo[] tmpls) {
        // Organize the templates first
        Map<String, MeasurementInfo> tmap = new HashMap<String, MeasurementInfo>();
        for (int i = 0; i < tmpls.length; i++) {
            tmap.put(tmpls[i].getAlias(), tmpls[i]);
        }

        Collection<MeasurementTemplate> mts = measurementTemplateRepository
            .findByMonitorableType(monitorableType);

        for (MeasurementTemplate mt : mts) {
            // See if this is in the list
            MeasurementInfo info = tmap.remove(mt.getAlias());
           
            if (info == null) {
                List<Measurement> measurements = measurementRepository.findByTemplate(mt.getId());
                for(Measurement measurement: measurements) {
                    measurementRepository.delete(measurement);
                }
                measurementTemplateRepository.delete(mt);
            } else {
                update(mt, pluginName, info);
            }
        }
        return tmap;
    }
    
    private void update(MeasurementTemplate mt, String pluginName, MeasurementInfo info) {
        // Load category
        Category cat;
        if (info.getCategory() != null) {
            if (!mt.getCategory().getName().equals(info.getCategory())) {

                cat = categoryRepository.findByName(info.getCategory());
                if (cat == null) {
                    cat = new Category(info.getCategory());
                    categoryRepository.save(cat);
                }
            } else {
                cat = mt.getCategory();
            }
        } else {
            throw new IllegalArgumentException("Category is null");
        }

        // Update the MeasurementTemplate
        mt.setTemplate(info.getTemplate());
        mt.setCollectionType(info.getCollectionType());
        mt.setPlugin(pluginName);
        mt.setCategory(cat);

        // Don't reset indicator, defaultOn or interval if it's been
        // changed
        if (mt.getMtime() == mt.getCtime()) {
            mt.setDesignate(info.isIndicator());
            mt.setDefaultOn(info.isDefaultOn());
            mt.setDefaultInterval(info.getInterval());
        }

        measurementTemplateRepository.save(mt);
    }
    
    /**
     * Add new measurement templates for a plugin.
     * 
     * This does a batch style insert
     */
    public void createTemplates(String pluginName,Map<MonitorableType,List<MonitorableMeasurementInfo>> toAdd) {
        measurementTemplateRepository.createTemplates(pluginName, toAdd);
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
        List<MeasurementTemplate> derivedTemplates = measurementTemplateRepository
            .findByMonitorableTypeOrderByName(mType);

        HashSet<Integer> designates = new HashSet<Integer>();
        designates.addAll(Arrays.asList(desigIds));

        for (MeasurementTemplate template : derivedTemplates) {
            // Never turn off Availability as an indicator
            if (template.isAvailability()) {
                continue;
            }

            boolean designated = designates.contains(template.getId());

            if (designated != template.isDesignate()) {
                template.setDesignate(designated);
            }
        }
    }
}
