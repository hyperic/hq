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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.impl.SessionImpl;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerUtil;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;

/** The TemplateManagerEJB class is a stateless session bean that can be
 *  used to interact with Template EJB's
 *
 * @ejb:bean name="TemplateManager"
 *      jndi-name="ejb/measurement/TemplateManager"
 *      local-jndi-name="LocalTemplateManager"
 *      view-type="local"
 *      type="Stateless"
 *
 * @ejb:transaction type="REQUIRED"
 */
public class TemplateManagerEJBImpl extends SessionEJB implements SessionBean {
    private final Log log = LogFactory.getLog(TemplateManagerEJBImpl.class);

    /**
     * Get a MeasurementTemplate
     *
     * @ejb:interface-method
     */
    public MeasurementTemplate getTemplate(Integer id) {
        return getMeasurementTemplateDAO().findById(id);
    }

    /**
     * Look up measurement templates for an array of template IDs
     *
     * @ejb:interface-method
     */
    public List getTemplates(List ids) {
        Integer[] mtids = (Integer[]) ids.toArray(new Integer[ids.size()]);
        return getMeasurementTemplateDAO().findTemplates(mtids);
    }

    /**
     * Look up a measurement templates for an array of template IDs
     *
     * @throws FinderException if no measurement templates are found.
     * @return a MeasurementTemplate value
     * 
     * @ejb:interface-method
     */
    public List getTemplates(Integer[] ids, PageControl pc)
        throws TemplateNotFoundException {
        List mts = getMeasurementTemplateDAO().findTemplates(ids);

        if (ids.length != mts.size())
            throw new TemplateNotFoundException("Could not look up " +
                                                StringUtil.arrayToString(ids));

        if (pc.getSortorder() == PageControl.SORT_DESC)
            Collections.reverse(mts);

        return mts;
    }

    /**
     * Get all the templates.  Must be superuser to execute.
     *
     * @param pInfo must contain a sort field of type 
     *              {@link MeasurementTemplateSortField}
     * @param defaultOn If non-null, return templates with defaultOn == defaultOn
     * 
     * @return a list of {@link MeasurementTemplate}s
     * @ejb:interface-method
     */
    public List findTemplates(AuthzSubject user, PageInfo pInfo, 
                              Boolean defaultOn) 
        throws PermissionException
    {
        assertSuperUser(user);
        return getMeasurementTemplateDAO().findAllTemplates(pInfo, defaultOn);
    }
                                  
    /**
     * Get all templates for a given MonitorableType
     *
     * @param pInfo must contain a sort field of type 
     *              {@link MeasurementTemplateSortField}
     * @param defaultOn If non-null, return templates with defaultOn == defaultOn
     * 
     * @return a list of {@link MeasurementTemplate}s
     * @ejb:interface-method
     */
    public List findTemplatesByMonitorableType(AuthzSubject user, PageInfo pInfo,
                                               String type, Boolean defaultOn) 
        throws PermissionException
    {
        assertSuperUser(user);
        return getMeasurementTemplateDAO()
            .findTemplatesByMonitorableType(pInfo, type, defaultOn); 
    }
    
    private void assertSuperUser(AuthzSubject s) 
        throws PermissionException
    {
        boolean authorized = PermissionManagerFactory.getInstance() 
            .hasAdminPermission(s.getId());

        if (!authorized) {
            throw new PermissionException("Permission denied");
        }
    }

    /**
     * Look up a measurement templates for a monitorable type and
     * category.
     *
     * @return a MeasurementTemplate value
     * @ejb:interface-method
     */
    public List findTemplates(String type, String cat,
                              Integer[] excludeIds, PageControl pc) {
        MeasurementTemplateDAO dao = getMeasurementTemplateDAO();

        List templates;
        if (cat == null) {
            templates = dao.findTemplatesByMonitorableType(type);
        } else {
            templates = dao.findTemplatesByMonitorableTypeAndCategory(type,
                                                                      cat);
        }

        if (templates == null) {
            return new PageList();
        }

        // Handle excludes
        List includes;
        if (excludeIds == null) {
            includes = templates;
        } else {
            HashSet excludes = new HashSet(Arrays.asList(excludeIds));
            includes = new ArrayList();
            for (Iterator it = templates.iterator(); it.hasNext(); ) {
                MeasurementTemplate tmpl =
                    (MeasurementTemplate)it.next();
                if (!excludes.contains(tmpl.getId()))
                    includes.add(tmpl);
            }
        }
        
        pc = PageControl.initDefaults(pc, -1);
        if (pc.getSortorder() == PageControl.SORT_DESC)
            Collections.reverse(includes);

        return templates;
    }

    /**
     * Look up a measurement templates for a monitorable type and filtered
     * by categories and keyword.
     *
     * @return a MeasurementTemplate value
     * @ejb:interface-method
     */
    public List findTemplates(String type, long filters, String keyword) {
        MeasurementTemplateDAO dao = getMeasurementTemplateDAO();
        List mts;
            
        if ((filters & MeasurementConstants.FILTER_AVAIL) == 0 ||
            (filters & MeasurementConstants.FILTER_UTIL)  == 0 ||
            (filters & MeasurementConstants.FILTER_THRU)  == 0 ||
            (filters & MeasurementConstants.FILTER_PERF)  == 0) {
            mts = new ArrayList();
                
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
            return new PageList();
        }
    
        // Check filter types
        for (Iterator it = mts.iterator(); it.hasNext(); ) {
            MeasurementTemplate tmpl = (MeasurementTemplate) it.next();

            // First, keyword
            if (StringUtil.stringExists(tmpl.getName(), keyword)) {
                it.remove();
                continue;
            }

            switch (tmpl.getCollectionType()) {
            case MeasurementConstants.COLL_TYPE_DYNAMIC:
                if ((filters & MeasurementConstants.FILTER_DYN) == 0)
                    it.remove();
                break;
            case MeasurementConstants.COLL_TYPE_STATIC:
                if ((filters & MeasurementConstants.FILTER_STATIC) == 0)
                    it.remove();
                break;
            case MeasurementConstants.COLL_TYPE_TRENDSUP:
                if ((filters&MeasurementConstants.FILTER_TREND_UP) == 0)
                    it.remove();
                break;
            case MeasurementConstants.COLL_TYPE_TRENDSDOWN:
                if ((filters&MeasurementConstants.FILTER_TREND_DN) == 0)
                    it.remove();
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
     * @ejb:interface-method
     */
    public Integer[] findTemplateIds(String type) {
        List mts = getMeasurementTemplateDAO().
            findTemplatesByMonitorableType(type);

        if (mts == null) {
            return new Integer[0];
        }

        Integer[] ids = new Integer[mts.size()];
        Iterator it = mts.iterator();
        for (int i = 0; it.hasNext(); i++) {
            MeasurementTemplate tmpl =
                (MeasurementTemplate) it.next();
            ids[i] = tmpl.getId();
        }
        return ids;
    }

    /**
     * Remove a measurement template
     *
     * @ejb:interface-method
     */
    public void removeTemplate(AuthzSubjectValue subject, Integer tid) {
        MeasurementTemplate t = getMeasurementTemplateDAO().findById(tid);
        getMeasurementDAO().remove(t);
        getMeasurementTemplateDAO().remove(t);
    }

    /**
     * Get the Product Monitoring Configuration
     *
     * @return A String of HTML help.
     * @ejb:interface-method
     */
    public String getMonitoringHelp(AuthzSubjectValue subject,
                                    AppdefEntityValue entityVal,
                                    Map props)
        throws PluginNotFoundException, PermissionException,
               AppdefEntityNotFoundException
    {
        return this.getProductMan().getMonitoringHelp(subject, entityVal,
                                                      props);
    }
    
    /**
     * Update the default interval for a list of meas. templates
     * 
     * @subject - the subject
     * @param templIds - a list of integer template ids
     * @param interval - the interval of collection to set to
     * @ejb:interface-method 
     */
    public void updateTemplateDefaultInterval(AuthzSubject subject, 
                                              Integer[] templIds, 
                                              long interval) {
        HashSet toReschedule = new HashSet();
        MeasurementTemplateDAO templDao = getMeasurementTemplateDAO();
        MeasurementDAO dmDao = getMeasurementDAO();
               
        for (int i = 0; i < templIds.length; i++) {
            MeasurementTemplate template = templDao.findById(templIds[i]);

            if (interval != template.getDefaultInterval())
                template.setDefaultInterval(interval);

            if (!template.isDefaultOn())
                template.setDefaultOn(interval != 0);
            
            dmDao.updateIntervalToTemplateInterval(template);
            
            List appdefEntityIds = 
                dmDao.findAppdefEntityIdsByTemplate(template.getId());

            toReschedule.addAll(appdefEntityIds);
        }
        
        SRNManagerLocal srnManager = getSRNManager();
        SRNCache cache = SRNCache.getInstance();
        ScheduleRevNumDAO srnDao = getScheduleRevNumDAO();

        int count = 0;
        
        for (Iterator it = toReschedule.iterator(); it.hasNext();) {
            AppdefEntityID id = (AppdefEntityID)it.next();
            ScheduleRevNum srn = cache.get(id);
            if (srn != null) {
                srnManager.incrementSrn(id, Math.min(interval,
                        srn.getMinInterval()));

                if (++count % 100 == 0) {
                    srnDao.flushSession();                 
                }
            }
        }
        
        srnDao.flushSession();
    }
        
    /**
     * Make metrics disabled by default for a list of meas. templates
     * @param templIds - a list of integer template ids
     * @ejb:interface-method 
     */
    public void setTemplateEnabledByDefault(AuthzSubject subject, 
                                            Integer[] templIds, boolean on) 
    { 
        MeasurementDAO dmDao = getMeasurementDAO();
        MeasurementTemplateDAO tmpDao = getMeasurementTemplateDAO();
        SRNManagerLocal srnMan = getSRNManager();
        long current = System.currentTimeMillis();
        
        for (int i = 0; i < templIds.length; i++) {
            MeasurementTemplate template = tmpDao.findById(templIds[i]);

            template.setDefaultOn(on);

            List metrics = dmDao.findByTemplate(templIds[i]);
            for (Iterator it = metrics.iterator(); it.hasNext(); ) {
                Measurement dm = (Measurement)it.next();

                if (dm.isEnabled() == on || dm.getInterval() == 0)
                    continue;

                dm.setEnabled(on);
                dm.setMtime(current);

                AppdefEntityID aeid = new AppdefEntityID(dm.getAppdefType(),
                                                         dm.getInstanceId());
                ScheduleRevNum srn = srnMan.get(aeid);
                long minInterval = (srn == null) ? dm.getInterval() :
                                                   srn.getMinInterval();
                srnMan.incrementSrn(aeid, minInterval);
            }
        }
    }
    
    /**
     * Get the MonitorableType id, creating it if it does not exist.
     *
     * @todo: This should just return the pojo and be named getMonitorableType.
     *
     * @ejb:interface-method
     */
    public MonitorableType getMonitorableType(String pluginName, TypeInfo info) {
        MonitorableType t = getMonitorableTypeDAO().findByName(info.getName());
        
        if (t == null) {
            int a, e;
            e = info.getType();
            a = AppdefEntityConstants.entityInfoTypeToAppdefType(e);
            t = getMonitorableTypeDAO().create(info.getName(), a, pluginName);
        }
      
        return t;
    }

    /**
     * Update measurement templates for a given entity.  This still
     * needs some refactoring.
     *
     * @return A map of measurement info's that are new and will need
     * to be created.
     * @ejb:interface-method 
     */
    public Map updateTemplates(String pluginName, TypeInfo ownerEntity, 
                               MonitorableType monitorableType,
                               MeasurementInfo[] tmpls)
        throws CreateException, RemoveException 
    {
        MeasurementTemplateDAO dao = getMeasurementTemplateDAO();

        // Organize the templates first
        Map tmap = new HashMap();
        for (int i = 0; i < tmpls.length; i++) {
            tmap.put(tmpls[i].getAlias(), tmpls[i]);
        }
        
        Collection mts = dao.findRawByMonitorableType(monitorableType);
        
        for (Iterator i = mts.iterator(); i.hasNext();) {
            MeasurementTemplate mt = (MeasurementTemplate) i.next();
            
            // See if this is in the list
            MeasurementInfo info = (MeasurementInfo) tmap.remove(mt.getAlias());

            if (info == null) {
                getMeasurementDAO().remove(mt);
                dao.remove(mt);
            } else {
                dao.update(mt, pluginName, info);
            }
        }
        return tmap;
    }

    /**
     * Add new measurement templates for a plugin.
     *
     * This does a batch style insert, and expects a map of maps
     * indexed by the monitorable type id.
     *
     * @ejb:interface-method 
     */
    public void createTemplates(String pluginName, Map toAdd)
        throws CreateException {
        // Add the new templates
        PreparedStatement stmt;

        SessionFactoryImpl sessionFactory =
            (SessionFactoryImpl)Util.getSessionFactory();
        Session session =
            getMeasurementTemplateDAO().getSession();
        try {
            IdentifierGenerator tmplIdGenerator =
                sessionFactory
                .getEntityPersister(MeasurementTemplate.class.getName())
                .getIdentifierGenerator();

            Connection conn = session.connection();

            final String templatesql = "INSERT INTO EAM_MEASUREMENT_TEMPL " +
                "(id, name, alias, units, collection_type, default_on, " +
                "default_interval, designate, monitorable_type_id, " +
                "category_id, template, plugin, ctime, mtime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            long current = System.currentTimeMillis();

            // can assume this is called in a single thread
            // This is called at hq server startup
            HashMap cats = new HashMap();
            for (Iterator i = toAdd.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                MonitorableType monitorableType =
                    (MonitorableType) entry.getKey();
                Map newMetrics = (Map) entry.getValue();
                
                for (Iterator j = newMetrics.values().iterator(); j.hasNext();){
                    MeasurementInfo info = (MeasurementInfo)j.next();

                    Category cat =
                        (Category) cats.get(info.getCategory());
                    if (cat == null) {
                        cat = getCategoryDAO().findByName(info.getCategory());
                        if (cat == null) {
                            cat = getCategoryDAO().create(info.getCategory());
                        }
                        cats.put(info.getCategory(), cat);
                    }

                    int col = 1;
                    Integer rawid = (Integer)tmplIdGenerator.
                        generate((SessionImpl)session, new MeasurementTemplate());

                    stmt = conn.prepareStatement(templatesql);
                    stmt.setInt(col++, rawid.intValue());
                    stmt.setString(col++, info.getName());
                    stmt.setString(col++, info.getAlias());
                    stmt.setString(col++, info.getUnits());
                    stmt.setInt(col++, info.getCollectionType());
                    stmt.setBoolean(col++, info.isDefaultOn());
                    stmt.setLong(col++, info.getInterval());
                    stmt.setBoolean(col++, info.isIndicator());
                    stmt.setInt(col++, monitorableType.getId().intValue());
                    stmt.setInt(col++, cat.getId().intValue());
                    stmt.setString(col++, info.getTemplate());
                    stmt.setString(col++, pluginName);
                    stmt.setLong(col++, current);
                    stmt.setLong(col, current);
                    stmt.execute();
                    stmt.close();
                }
            }
        } catch (SQLException e) {
            this.log.error("Unable to add measurements for: " +
                           pluginName, e);
        } finally {
            session.disconnect();
        }
    }
 
    /** 
     * Set the measurement templates to be "designated" for a monitorable type.
     *
     * @ejb:interface-method
     */
    public void setDesignatedTemplates(String mType, Integer[] desigIds) {

        List derivedTemplates = getMeasurementTemplateDAO()
            .findDerivedByMonitorableType(mType);
            
        HashSet designates = new HashSet();
        designates.addAll(Arrays.asList(desigIds));
            
        for (Iterator i = derivedTemplates.iterator(); i.hasNext();) {
            MeasurementTemplate template = (MeasurementTemplate)i.next();

            // Never turn off Availability as an indicator
            if (template.getAlias().equalsIgnoreCase(
                    MeasurementConstants.CAT_AVAILABILITY))
                    continue;

            boolean designated = designates
                    .contains(template.getId());

            if (designated != template.isDesignate()) {
                template.setDesignate(designated);
            }
        }
    }

    public static TemplateManagerLocal getOne() {
        try {
            return TemplateManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException();
        }
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {}

    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}
}
