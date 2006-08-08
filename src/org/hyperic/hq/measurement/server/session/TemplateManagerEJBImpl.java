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
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.SRNCreateException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.server.mbean.SRNCache;
import org.hyperic.hq.measurement.shared.CategoryLocal;
import org.hyperic.hq.measurement.shared.CategoryLocalHome;
import org.hyperic.hq.measurement.shared.CategoryUtil;
import org.hyperic.hq.measurement.shared.DerivedMeasurementLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementLocalHome;
import org.hyperic.hq.measurement.shared.DerivedMeasurementUtil;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.MeasurementArgLocal;
import org.hyperic.hq.measurement.shared.MeasurementArgValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateLiteValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateLocal;
import org.hyperic.hq.measurement.shared.MeasurementTemplatePK;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.measurement.shared.MonitorableTypeLocal;
import org.hyperic.hq.measurement.shared.MonitorableTypeLocalHome;
import org.hyperic.hq.measurement.shared.MonitorableTypeUtil;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerUtil;
import org.hyperic.hq.measurement.shared.ScheduleRevNumValue;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.jdbc.IDGeneratorFactory;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;

/** The TemplateManagerEJB class is a stateless session bean that can be
 *  used to interact with Template EJB's
 * <p>
 *
 * </p>
 * @ejb:bean name="TemplateManager"
 *      jndi-name="ejb/measurement/TemplateManager"
 *      local-jndi-name="LocalTemplateManager"
 *      view-type="local"
 *      type="Stateless"
 *
 * @ejb:transaction type="REQUIRED"
 */
public class TemplateManagerEJBImpl extends SessionEJB implements SessionBean {
    private final Log log = LogFactory.getLog(
        "org.hyperic.hq.measurement.server.session.TemplateManagerEJBImpl");

    protected final String VALUE_PROCESSOR =
        PagerProcessor_measurement.class.getName();
        
    private Pager valuePager = null;

    MonitorableTypeLocalHome mtHome = null;
    private MonitorableTypeLocalHome getMTHome() {
        try {
            if (mtHome == null)
                mtHome = MonitorableTypeUtil.getLocalHome();
            return mtHome;
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }
    
    RawMeasurementManagerLocal rmMan = null;
    private RawMeasurementManagerLocal getRmMan() {
        try {
            if (rmMan == null)
                rmMan = RawMeasurementManagerUtil.getLocalHome().create();
            return rmMan;
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    ///////////////////////////////////////
    // operations

    /** <p>
     * Create a RawMeasurement Template
     * </p><p>
     *
     * @return a MeasurementTemplate ID
     * </p>
     * @ejb:interface-method
     */
    public MeasurementTemplateValue createTemplate(AuthzSubjectValue subject,
                                                   String name, String alias,
                                                   String type, String template)
        throws FinderException, CreateException {
        return createTemplate(subject, name, alias, type,
                              (String)null, template, 
                              MeasurementConstants.UNITS_NONE, 
                              MeasurementConstants.COLL_TYPE_DYNAMIC,
                              (MeasurementArgValue[])null);
    }

    /** <p>
     * Create a DerivedMeasurement Template
     * </p><p>
     *
     * @return a MeasurementTemplate ID
     * </p>
     * @ejb:interface-method
     */
    public MeasurementTemplateValue createTemplate(AuthzSubjectValue subject,
                                                   String name, String alias,
                                                   String type,
                                                   String catName,
                                                   String template,
                                                   String units,
                                                   int collectionType,
                                                   MeasurementArgValue[] args)
        throws CreateException {
        try {
            // Get the MonitorableType
            MonitorableTypeLocal t = this.getMTHome().findByName(type);

            // get the category
            CategoryLocal catLoc = null;
            if (catName!=null) {
                CategoryLocalHome cHome = CategoryUtil.getLocalHome();
                catLoc = cHome.findByName(catName);
            }

            ArrayList lis = null;

            if (args != null) {
                // First create the LineItems
                lis = new ArrayList();
                for (int i = 0; i < args.length; i++) {
                    MeasurementTemplatePK pk =
                        new MeasurementTemplatePK(args[i].getId());

                    MeasurementTemplateLocal arg =
                        getMtHome().findByPrimaryKey(pk);

                    // Create the line item
                    MeasurementArgLocal li =
                        getLiHome().create(new Integer(i+1), arg,
                                           args[i].getTicks(),
                                           args[i].getWeight(),
                                           args[i].getPrevious());
                    lis.add(li);
                }
            }

            // create
            MeasurementTemplateLiteValue lite =
                new MeasurementTemplateLiteValue(null, name, alias, units, 
                           collectionType, false, 
                           MeasurementConstants.INTERVAL_DEFAULT_MILLIS,
                           false, template, null, null);
            MeasurementTemplateLocal mt = getMtHome().create(lite, t, catLoc,
                                                             lis);
            return mt.getMeasurementTemplateValue();
        } catch (NamingException e) {
            log.debug("NamingException", e);
            throw new CreateException("NamingException: " + e);
        } catch (FinderException e) {
            log.debug("FinderException", e);
            throw new CreateException("FinderException: " + e);
        }
    }

    /** <p>
     * Get a measurement template
     * </p><p>
     *
     * @return a MeasurementTemplate value
     * </p>
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public MeasurementTemplateValue getTemplate(Integer tmplId)
        throws TemplateNotFoundException {
        try {
            MeasurementTemplateLocal mt =
                getMtHome().findByPrimaryKey(new MeasurementTemplatePK(tmplId));
            return mt.getMeasurementTemplateValue();
        } catch (FinderException e) {
            throw new TemplateNotFoundException(tmplId, e);
        }
    }

    /** <p>
     * Look up a measurement template (for testing)
     * </p><p>
     *
     * @return a MeasurementTemplate value
     * </p>
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public MeasurementTemplateValue findTemplate(String name, boolean derived) {
        try {
            Collection tmpls = getMtHome().findByName(name);
            for (Iterator it = tmpls.iterator(); it.hasNext(); ) {
                MeasurementTemplateLocal mt =
                    (MeasurementTemplateLocal) it.next();
                    
                if (derived) {
                    if (mt.getMeasurementArgs() != null &&
                        mt.getMeasurementArgs().size() > 0)
                        return mt.getMeasurementTemplateValue();
                }
                else {
                    if (mt.getMeasurementArgs() == null ||
                        mt.getMeasurementArgs().size() == 0)
                        return mt.getMeasurementTemplateValue();
                }
            }
        } catch (FinderException e) {
            // Not a problem
            log.debug("FinderException", e);
        }
    
        return null;
    }

    /** <p>
     * Look up a measurement templates for an array of template IDs
     * </p><p>
     *
     * @throws FinderException if no measurement templates are found.
     *
     * @return a MeasurementTemplate value
     * 
     * </p>
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList getTemplates(Integer[] ids, PageControl pc)
        throws TemplateNotFoundException {
        List mts = this.findTemplates(ids);
        
        if (ids.length != mts.size())
            throw new TemplateNotFoundException("Could not look up " + ids);

        if (pc.getSortorder() == PageControl.SORT_DESC)
            Collections.reverse(mts);

        return valuePager.seek(mts, pc);
    }

    /** <p>
     * Look up a measurement templates for an array of template IDs
     * </p><p>
     *
     * @return a list of MeasurementTemplate values
     * 
     * </p>
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public List findTemplates(Integer[] ids) {
        try {
            Collection locals = this.getMtHome().findByIds(ids);
            return valuePager.seek(locals, PageControl.PAGE_ALL);
        } catch (FinderException e) {
            return new ArrayList(0);
        }
    }

    /** <p>
     * Look up a measurement templates for a monitorable
     * and agent type.  All resources must have measurement templates.
     * otherwise, throw FinderException
     * </p><p>
     *
     * @throws FinderException if no measurement templates are found.
     *
     * @return a MeasurementTemplate value
     * 
     * </p>
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList findTemplates(String type, String cat,
                                  Integer[] excludeIds, PageControl pc) {
        try {
            List mts;
            if (cat == null || Arrays.binarySearch(
                MeasurementConstants.VALID_CATEGORIES, cat) < 0) {
                mts = this.getMtHome().findDerivedByMonitorableType(type);
            } else {
                mts = this.getMtHome().findDerivedByMonitorableTypeAndCategory(
                    type, cat);
            }

            // Create a HashSet of the excludes
            List includes;
            if (excludeIds == null) {
                includes = mts;
            }
            else {
                HashSet excludes = new HashSet(Arrays.asList(excludeIds));
                includes = new ArrayList();
                for (Iterator it = mts.iterator(); it.hasNext(); ) {
                    MeasurementTemplateLocal tmpl =
                        (MeasurementTemplateLocal) it.next();
                    if (!excludes.contains(tmpl.getId()))
                        includes.add(tmpl);
                }
            }
            
            // init defaults, not using sort column
            pc = PageControl.initDefaults(pc, -1);
            if (pc.getSortorder() == PageControl.SORT_DESC)
                Collections.reverse(includes);
            
            return valuePager.seek(includes, pc);
        }
        catch (FinderException e) {
            // Not a problem
            log.debug("No templates found for " + type, e);
            return new PageList();
        }
    }

    /** <p>
     * Look up a measurement templates for a monitorable type and filtered
     * by categories and keyword.
     * </p><p>
     *
     * @throws FinderException if no measurement templates are found.
     *
     * @return a MeasurementTemplate value
     * 
     * </p>
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public List findTemplates(String type, long filters, String keyword) {
        try {
            List mts;
            
            if ((filters & MeasurementConstants.FILTER_AVAIL) == 0 ||
                (filters & MeasurementConstants.FILTER_UTIL)  == 0 ||
                (filters & MeasurementConstants.FILTER_THRU)  == 0 ||
                (filters & MeasurementConstants.FILTER_PERF)  == 0) {
                mts = new ArrayList();
                
                // Go through each filter
                if ((filters & MeasurementConstants.FILTER_AVAIL) > 0) 
                    mts.addAll(this.getMtHome()
                        .findDerivedByMonitorableTypeAndCategory(
                            type, MeasurementConstants.CAT_AVAILABILITY));

                if ((filters & MeasurementConstants.FILTER_UTIL) > 0) 
                    mts.addAll(this.getMtHome()
                        .findDerivedByMonitorableTypeAndCategory(
                            type, MeasurementConstants.CAT_UTILIZATION));

                if ((filters & MeasurementConstants.FILTER_THRU) > 0) 
                    mts.addAll(this.getMtHome()
                        .findDerivedByMonitorableTypeAndCategory(
                            type, MeasurementConstants.CAT_THROUGHPUT));

                if ((filters & MeasurementConstants.FILTER_PERF) > 0) 
                    mts.addAll(this.getMtHome()
                        .findDerivedByMonitorableTypeAndCategory(
                            type, MeasurementConstants.CAT_PERFORMANCE));
            }
            else {
                mts = this.getMtHome().findDerivedByMonitorableType(type);
            }
    
            // Now check the other filter types
            for (Iterator it = mts.iterator(); it.hasNext(); ) {
                MeasurementTemplateLocal tmpl =
                    (MeasurementTemplateLocal) it.next();

                // First, keyword
                if (keyword != null && keyword.length() > 0) {
                    if (tmpl.getName().indexOf(keyword) < 0) {
                        it.remove();
                        continue;
                    }
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

            return valuePager.seek(mts, PageControl.PAGE_ALL);
        }
        catch (FinderException e) {
            // Not a problem
            log.debug("No templates found for " + type, e);
            return new PageList();
        }
    }

    /** <p>
     * Look up a measurement template IDs for a monitorable type.
     * </p><p>
     *
     * @return an array of ID values
     * 
     * </p>
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public Integer[] findTemplateIds(String type) {
        List mts;
        try {
            mts = this.getMtHome().findDerivedByMonitorableType(type);
        } catch (FinderException e) {
            // No templates found for this type
            return new Integer[0];
        }
        Integer[] ids = new Integer[mts.size()];
        Iterator it = mts.iterator();
        for (int i = 0; it.hasNext(); i++) {
            MeasurementTemplateLocal tmpl =
                (MeasurementTemplateLocal) it.next();
            ids[i] = tmpl.getId();
        }
        return ids;
    }

    /** <p>
     * Look up default measurement templates for a monitorable
     * and agent type.
     * </p><p>
     *
     * @return a MeasurementTemplate value
     * 
     * </p>
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public List findDefaultTemplates(String mtype, int atype) {
        ArrayList mtList = new ArrayList();
        try {
            Collection mts =
                getMtHome().findDefaultsByMonitorableType(mtype, atype);
            for (Iterator i = mts.iterator(); i.hasNext();) {
                MeasurementTemplateLocal mt =
                    (MeasurementTemplateLocal) i.next();
                mtList.add(mt.getMeasurementTemplateValue());
            }
        }
        catch (FinderException e) {
            // Not a problem
            log.debug("No default templates found for " + mtype, e);
        }
        return mtList;
    }

    /** <p>
     * Look up designated measurement templates for a monitorable
     * and agent type.
     * </p><p>
     *
     * @return a MeasurementTemplate value
     * 
     * </p>
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public List findDesignatedTemplates(String mtype, int atype) {
        ArrayList mtList = new ArrayList();
        try {
            Collection mts =
                getMtHome().findDesignatedByMonitorableType(mtype, atype);
            for (Iterator i = mts.iterator(); i.hasNext();) {
                MeasurementTemplateLocal mt =
                    (MeasurementTemplateLocal) i.next();
                mtList.add(mt.getMeasurementTemplateValue());
            }
        }
        catch (FinderException e) {
            // Not a problem
            log.debug("No default templates found for " + mtype, e);
        }
        return mtList;
    }

    /** List of all monitorable types
     * @return List of monitorable types
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public List findMonitorableTypes() {
        List retVal = new ArrayList();
        try {
            Collection types = this.getMTHome().findAll();
            for (Iterator i = types.iterator(); i.hasNext(); ) {
                MonitorableTypeLocal mtl = (MonitorableTypeLocal) i.next();
                retVal.add(mtl.getMonitorableTypeValue());
            }
        } catch (FinderException e) {
            // No problem
        }
        return retVal;
    }

    private void removeMeasurements(Integer tmplId) throws RemoveException {
        if (log.isDebugEnabled())
            log.debug("removeMeasurements() for " + tmplId);

        // Remove the measurement instances in a new transaction
        this.getRmMan().removeMeasurements(tmplId);
    }

    /** <p>
     * Remove a measurement template
     * </p><p>
     *
     * @return a MeasurementTemplate value
     * </p>
     * @ejb:interface-method
     */
    public void removeTemplate(AuthzSubjectValue subject, Integer tid)
        throws RemoveException {
        try {
            // Look up template
            MeasurementTemplateLocal t =
                getMtHome().findByPrimaryKey(new MeasurementTemplatePK(tid));
            t.remove();
        } catch (FinderException e) {
            // Not a problem
            log.debug("FinderException", e);
        }
    }

    /** <p>
     * Get the Product Monitoring Configuration
     * </p><p>
     *
     * @return A String of HTML help.
     * </p>
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
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
     * @param templIds - a list of integer template ids
     * @param interval - the interval of collection to set to
     * @ejb:interface-method 
     */
    public void updateTemplateDefaultInterval(Integer[] templIds, long interval) 
        throws TemplateNotFoundException {
        try {
            DerivedMeasurementLocalHome dmHome =
                DerivedMeasurementUtil.getLocalHome();
            
            long current = System.currentTimeMillis();

            // XXX this whole thing may need to be moved to directSQL...
            for (int i = 0; i < templIds.length; i++) {
                MeasurementTemplatePK pk =
                    new MeasurementTemplatePK(templIds[i]);
                MeasurementTemplateLocal ejb = getMtHome().findByPrimaryKey(pk);
                
                if (!ejb.getDefaultOn())
                    ejb.setDefaultOn(true);
                    
                if (interval != ejb.getDefaultInterval())
                    ejb.setDefaultInterval(interval);
                
                ejb.setMtime(current);
                
                List metrics = dmHome.findByTemplate(templIds[i]);
                DMValueCache cache = DMValueCache.getInstance();
                SRNCache srnCache = SRNCache.getInstance();
                for (Iterator it = metrics.iterator(); it.hasNext(); ) {
                    DerivedMeasurementLocal dm =
                        (DerivedMeasurementLocal) it.next();
                    
                    if (dm.getInterval() != interval)
                        dm.setInterval(interval);
                    
                    if (!dm.getEnabled())
                        dm.setEnabled(true);
                    
                    dm.setMtime(current);
                    
                    DerivedMeasurementValue dmval;
                    if ((dmval = cache.get(dm.getId())) != null) {
                        dmval.setInterval(interval);
                        dmval.setEnabled(true);
                        dmval.setMtime(current);
                    }
                    
                    AppdefEntityID aeid = new AppdefEntityID(dm.getAppdefType(),
                                                             dm.getInstanceId());
                    ScheduleRevNumValue srn = srnCache.getSRN(aeid);
                    if (srn != null) {  // Increment SRN only if not null
                        try {
                            srnCache.beginIncrementSRN(
                                aeid, Math.min(interval, srn.getMinInterval()));
                        } catch (SRNCreateException e) {
                            log.error("Should not be creating SRNs", e);
                        } finally {
                            srnCache.endIncrementSRN(aeid);
                        }
                    }
                }
            }
        } catch (FinderException e) {
            throw new TemplateNotFoundException(e.getMessage());
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * Make metrics disabled by default for a list of meas. templates
     * @param templIds - a list of integer template ids
     * @ejb:interface-method 
     */
    public void enableTemplateByDefault(Integer[] templIds, boolean on) 
        throws TemplateNotFoundException {
        try {
            DerivedMeasurementLocalHome dmHome =
                DerivedMeasurementUtil.getLocalHome();
            
            long current = System.currentTimeMillis();

            for (int i = 0; i < templIds.length; i++) {
                MeasurementTemplatePK pk =
                    new MeasurementTemplatePK(templIds[i]);
                MeasurementTemplateLocal ejb = getMtHome().findByPrimaryKey(pk);
                ejb.setDefaultOn(on);
                ejb.setMtime(current);

                List metrics = dmHome.findByTemplate(templIds[i]);
                DMValueCache cache = DMValueCache.getInstance();
                SRNCache srnCache = SRNCache.getInstance();
                for (Iterator it = metrics.iterator(); it.hasNext(); ) {
                    DerivedMeasurementLocal dm =
                        (DerivedMeasurementLocal) it.next();
                    
                    if (dm.getEnabled() == on)
                        continue;
                    
                    dm.setEnabled(on);
                    dm.setMtime(current);
                    
                    DerivedMeasurementValue dmval;
                    if ((dmval = cache.get(dm.getId())) != null) {
                        dmval.setEnabled(on);
                        dmval.setMtime(current);
                    }
                    
                    AppdefEntityID aeid = new AppdefEntityID(dm.getAppdefType(),
                                                             dm.getInstanceId());
                    ScheduleRevNumValue srn = srnCache.getSRN(aeid);
                    try {
                        long minInterval = (srn == null) ? dmval.getInterval() :
                                                           srn.getMinInterval();
                        srnCache.beginIncrementSRN(aeid, minInterval);
                    } catch (SRNCreateException e) {
                        log.error("Should not be creating SRNs", e);
                    } finally {
                        srnCache.endIncrementSRN(aeid);
                    }
                }
            }
        } catch (FinderException e) {
            throw new TemplateNotFoundException(e.getMessage());
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * Get the MonitorableType id, creating it if it does not exist
     *
     * @ejb:interface-method
     */
    public Integer getMonitorableTypeId(String pluginName, TypeInfo info)
        throws CreateException {
        MonitorableTypeLocal t;

        try {
            t = this.getMTHome().findByName(info.getName());
        } catch (FinderException exc) {
            int a, e;

            // Create it
            e = info.getType();
            a = AppdefEntityConstants.entityInfoTypeToAppdefType(e);
            t = getMTHome().create(info.getName(), a, pluginName);
        }
      
        return t.getId();
    }

    /**
     * Update measurement templates for a given entity.  This still
     * needs some refactoring.
     *
     * @return A map of measurement info's that are new and will need
     * to be created.
     * @ejb:interface-method 
     */
    public Map updateTemplates(String pluginName, 
                               TypeInfo ownerEntity,
                               Integer monitorableTypeId,
                               MeasurementInfo[] tmpls)
        throws CreateException, RemoveException {
        // Make sure we have this monitorable type

        // Organize the templates first
        HashMap tmap = new HashMap();
        for (int i = 0; i < tmpls.length; i++) {
            tmap.put(tmpls[i].getAlias(), tmpls[i]);
        }
        
        // See if the templates already exist
        try {
            if (log.isDebugEnabled())
                log.debug("updateTemplates() fetch templates for " +pluginName);
            
            Collection mts =
                getMtHome().findRawByMonitorableType(monitorableTypeId);

            ArrayList toDelete = new ArrayList();
            
            long current = System.currentTimeMillis();
            for (Iterator i = mts.iterator(); i.hasNext();) {
                MeasurementTemplateLocal mt =
                    (MeasurementTemplateLocal) i.next();

                // See if this is in the list
                MeasurementInfo info =
                    (MeasurementInfo) tmap.remove(mt.getAlias());

                if (info == null) {
                    // Remove the templates dependent on this
                    Collection ts =
                        getMtHome().findByMeasurementArg(mt.getId());

                    if (log.isDebugEnabled())
                        log.debug("updateTemplates() removing " + ts.size() +
                                  " dependent templates for " + mt.getId());
                    
                    for (Iterator it = ts.iterator(); it.hasNext();) {
                        MeasurementTemplateLocal dm =
                            (MeasurementTemplateLocal)it.next();
                        
                        removeMeasurements(dm.getId());
                        toDelete.add(dm);
                    }

                    // Now remove this template
                    if (log.isDebugEnabled())
                        log.debug("updateTemplates() removing raw template " +
                                  mt.getId());
                    
                    removeMeasurements(mt.getId());
                    toDelete.add(mt);
                }
                else {
                    // Make sure everything is correct
                    if (log.isDebugEnabled())
                        log.debug("updateTemplates() updating raw metric " +
                                  mt.getId());
                    
                    mt.setTemplate(info.getTemplate());
                    mt.setCollectionType(info.getCollectionType());
                    mt.setPlugin(pluginName);

                    // Check category
                    CategoryLocal cat = null;
                    String category = info.getCategory();
                    if (!category.equals(mt.getCategory().getName())) {
                        // Load the category with the correct name and set it
                        try {
                            cat = getCaHome().findByName(category);
                        } catch (FinderException e) {
                            cat = getCaHome().create(category);
                        }
                        mt.setCategory(cat);
                    }

                    // Set the following in the derived
                    List dmts = this.getMtHome().findByArgAndTemplate(
                        mt.getId(), MeasurementConstants.TEMPL_IDENTITY);
                    
                    // We should theoretically only get one
                    if (dmts.size() > 0) {
                        MeasurementTemplateLocal dmt =
                            (MeasurementTemplateLocal) dmts.get(0);
                                      
                        if (log.isDebugEnabled())
                            log.debug("updateTemplates() updating derived " +
                                      "metric " + dmt.getId());

                        dmt.setAlias(info.getAlias());
                        dmt.setDesignate(info.isIndicator());
                        dmt.setUnits(info.getUnits());
                        dmt.setCollectionType(info.getCollectionType());

                        // Only change the intervals if user has not changed 
                        // them
                        if (dmt.getMtime() <= dmt.getCtime()) {
                            dmt.setDefaultOn(info.isDefaultOn());
                            dmt.setDefaultInterval(info.getInterval());
                            dmt.setCtime(current);
                            dmt.setMtime(current);
                        }

                        if (cat != null)
                            dmt.setCategory(cat);
                    }
                }
            }

            // Delete the old templates
            for (Iterator it = toDelete.iterator(); it.hasNext(); ) {
                MeasurementTemplateLocal mt =
                    (MeasurementTemplateLocal) it.next();
                mt.remove();
            }
        } catch (FinderException e) {
            // Not a problem
            log.debug("FinderException", e);
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
        // Add the new metrics
        Connection conn = null;
        PreparedStatement tStmt = null;
        PreparedStatement aStmt = null;
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), 
                                        DATASOURCE_NAME);

            final String templatesql = 
                "INSERT INTO EAM_MEASUREMENT_TEMPL " +
                "(id, name, alias, units, collection_type," +
                " default_on, default_interval, designate," +
                " monitorable_type_id, category_id, template," +
                " plugin, ctime, mtime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            final String argsql =
                "INSERT INTO EAM_MEASUREMENT_ARG " +
                "(id, measurement_template_id," +
                " measurement_template_arg_id," +
                " placement, ticks, weight, previous) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            long current = System.currentTimeMillis();
            
            HashMap cats = new HashMap();
            for (Iterator i = toAdd.keySet().iterator(); i.hasNext();) {
                Integer monitorableTypeId = (Integer)i.next();
                Map newMetrics = (Map)toAdd.get(monitorableTypeId);
                
                for (Iterator j = newMetrics.values().iterator(); j.hasNext();){
                    MeasurementInfo info = (MeasurementInfo)j.next();

                    // XXX: This sucks, consider removing this entity bean.
                    CategoryLocal cat =
                        (CategoryLocal) cats.get(info.getCategory());
                    if (cat == null) {
                        try {
                            cat = getCaHome().findByName(info.getCategory());
                        } catch (FinderException e) {
                            cat = getCaHome().create(info.getCategory());
                        }
                        cats.put(info.getCategory(), cat);
                    }
                    
                    // First create the raw measurement
                    int rawid = (int)IDGeneratorFactory.
                        getNextId("TemplateManagerEJBImpl",
                                  "EAM_MEASUREMENT_TEMPL_ID_SEQ",
                                  DATASOURCE_NAME);
                    int col = 1;
                    tStmt = conn.prepareStatement(templatesql);
                    tStmt.setInt(col++, rawid);
                    tStmt.setString(col++, info.getName());
                    tStmt.setString(col++, info.getAlias());
                    tStmt.setString(col++, info.getUnits());
                    tStmt.setInt(col++, info.getCollectionType());
                    tStmt.setBoolean(col++, false);
                    tStmt.setLong(col++, 0l);
                    tStmt.setBoolean(col++, false);
                    tStmt.setInt(col++, monitorableTypeId.intValue());
                    tStmt.setInt(col++, cat.getId().intValue());
                    tStmt.setString(col++, info.getTemplate());
                    tStmt.setString(col++, pluginName);
                    tStmt.setLong(col++, current);
                    tStmt.setLong(col++, current);
                    tStmt.execute();
                    tStmt.close();
                    
                    
                    // Next, create the derived measurement
                    int derivedid = (int)IDGeneratorFactory.
                        getNextId("TemplateManagerEJBImpl",
                                  "EAM_MEASUREMENT_TEMPL_ID_SEQ",
                                  DATASOURCE_NAME);
                    col = 1;
                    tStmt = conn.prepareStatement(templatesql);
                    tStmt.setInt(col++, derivedid);
                    tStmt.setString(col++, info.getName());
                    tStmt.setString(col++, info.getAlias());
                    tStmt.setString(col++, info.getUnits());
                    tStmt.setInt(col++, info.getCollectionType());
                    tStmt.setBoolean(col++, info.isDefaultOn());
                    tStmt.setLong(col++, info.getInterval());
                    tStmt.setBoolean(col++, info.isIndicator());
                    tStmt.setInt(col++, monitorableTypeId.intValue());
                    tStmt.setInt(col++, cat.getId().intValue());
                    tStmt.setString(col++, MeasurementConstants.TEMPL_IDENTITY);
                    tStmt.setString(col++, pluginName);
                    tStmt.setLong(col++, current);
                    tStmt.setLong(col++, current);
                    tStmt.execute();
                    tStmt.close();
                
                    // Lastly, create the line item
                    int aid = (int)IDGeneratorFactory.
                        getNextId("TemplateManagerEJBImpl",
                                  "EAM_MEASUREMENT_ARG_ID_SEQ",
                                  DATASOURCE_NAME);
                    col = 1;
                    aStmt = conn.prepareStatement(argsql);
                    aStmt.setInt(col++, aid);
                    aStmt.setInt(col++, derivedid);
                    aStmt.setInt(col++, rawid);
                    aStmt.setInt(col++, 1);
                    aStmt.setInt(col++, 0);
                    aStmt.setFloat(col++, 0f);
                    aStmt.setInt(col++, 0);
                    aStmt.execute();
                    aStmt.close();
                }
            }
        } catch (SQLException e) {
            this.log.error("Unable to add measurements for: " +
                           pluginName, e);
        } catch (NamingException e) {
            throw new SystemException("Naming error while adding " +
                                      "measurements", e);
        } catch (ConfigPropertyException e) {
            throw new CreateException("Unable to get sequence: " + e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, null, null);
        }
    }
 
    /** 
     * Set the measurement templates to be "designated" for a monitorable type.
     *
     * @ejb:interface-method
     */
    public void setDesignatedTemplates(String mType, Integer[] desigIds) {
        try {
            List derivedTemplates = 
                this.getMtHome().findDerivedByMonitorableType(mType);
            
            HashSet designates = new HashSet();
            designates.addAll(Arrays.asList(desigIds));
            
            for (Iterator i = derivedTemplates.iterator(); i.hasNext();) {
                MeasurementTemplateLocal template =
                    (MeasurementTemplateLocal) i.next();

                // Never turn off Availability as an indicator
                if (template.getAlias().equalsIgnoreCase(
                        MeasurementConstants.CAT_AVAILABILITY))
                    continue;

                boolean designated = designates
                    .contains(((MeasurementTemplatePK)
                         template.getPrimaryKey()).getId());

                if (designated != template.getDesignate()) {
                    template.setDesignate(designated);
                }
            }
        } catch(FinderException exc){
            log.debug("No designated templates found for " + mType, exc);
        }
    }

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.SessionBean#ejbCreate()
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {
        try {
            valuePager = Pager.getPager(VALUE_PROCESSOR);
        } catch ( Exception e ) {
            throw new CreateException("Could not create value pager:" + e);
        }
    }

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

} // end TemplateManager
