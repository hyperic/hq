/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.ui.util;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.LabelValueBean;
import org.hyperic.hq.appdef.shared.AIAppdefResourceValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ScanMethodState;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.ObjectNotFoundException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.Baseline;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformServiceDetector;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.platform.PlatformForm;
import org.hyperic.hq.ui.beans.AgentBean;
import org.hyperic.hq.ui.beans.ConfigValues;
import org.hyperic.hq.ui.exception.InvalidOptionValsFoundException;
import org.hyperic.snmp.SNMPClient;
import org.hyperic.util.StringUtil;
import org.hyperic.util.StringifiedException;
import org.hyperic.util.config.ArrayConfigOption;
import org.hyperic.util.config.BooleanConfigOption;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.DirArrayConfigOption;
import org.hyperic.util.config.EnumerationConfigOption;
import org.hyperic.util.config.HiddenConfigOption;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsFormat;

/* XXX: use reflection to combine some of these methods? */

/**
 * Utilities class that provides convenience methods for operating on
 * bizapp objects.
 */

public class BizappUtils {

    private static Log log = LogFactory.getLog(BizappUtils.class.getName());

    /**
     * Replace the word 'platform' in the input string with the correct
     * object type as specified by the entity ID.  
     * 
     * For instance, if the string is "events.config.platform", and id
     * specifies a server, the result will be "events.config.server"
     */
    public static String replacePlatform(String inStr, AppdefEntityID id) {
        if (id.isPlatform()) {
            return inStr;
        } else if (id.isServer()) {
            return StringUtil.replace(inStr, "platform", "server");
        } else if (id.isService()) {
            return StringUtil.replace(inStr, "platform", "service");
        } else if (id.isApplication()) {
            return StringUtil.replace(inStr, "platform", "application");
        } else if (id.isGroup()) {
            return StringUtil.replace(inStr, "platform", "group");
        } else {
            return StringUtil.replace(inStr, "platform.", "");
        }
     }
    
    /** A helper method used by setRuntimeAIMessage */
    private static String getServiceName(PageList serviceTypes, int index, 
                                         String stName) {
        String baseName
            = ((ServiceTypeValue) serviceTypes.get(index)).getName();
        return StringUtil.pluralize(StringUtil.removePrefix(baseName, stName));
    }

    /**
     * Return the full name of the subject.
     *
     * @param fname the subject's first name
     * @param lname the subject's last name
     */
    public static String makeSubjectFullName(String fname, String lname) {
        // XXX: what about Locales that display last name first?
    
        StringBuffer full = new StringBuffer();
        if (fname == null || fname.equals("")) {
            if (lname != null && ! lname.equals("")) {
                full.append(lname);
            }
        }
        else {
            if (lname == null || lname.equals("")) {
                full.append(fname);
            }
            else {
                full.append(fname);
                full.append(" ");
                full.append(lname);
            }
        }
    
        return full.toString();
    }

    public static PlatformTypeValue getPlatformTypeName(ServletContext ctx,
                                                        HttpServletRequest request,
                                                        String name)
        throws Exception {
    
        PlatformTypeValue ptValue;

        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        int sessionId = RequestUtils.getSessionIdInt(request);
        ptValue = appdefBoss.findPlatformTypeByName(sessionId, name); 

        return ptValue;
    }

    /**
     * filter on a list of AIAppdefResourceValue.  Either get the ignored
     * resources or non-ignored.
     * 
     * @param resources List of AIAppdefResources to filter
     */
    public static List filterAIResourcesByStatus(List resources, Integer status)
    {
        if (status == null || status.intValue() == -1)
            return resources;
            
        List resourceList = new PageList();

        Iterator sIterator = resources.iterator();
        while (sIterator.hasNext())
        {   
            AIAppdefResourceValue rValue = (AIAppdefResourceValue)sIterator.next();     
            if (rValue.getQueueStatus() == status.intValue() )
            {
                resourceList.add(rValue);        
            }
        
        }

        return resourceList;
    }

    /**
     * filter on a list of AIAppdefResourceValue by Server Type.
     */
    public static List filterAIResourcesByServerType(List resources, String name)
    {
        if (name == null || name.equals("") )
            return resources;
            
        List resourceList = new PageList();

        Iterator sIterator = resources.iterator();
        while (sIterator.hasNext())
        {   
            AIServerValue rValue = (AIServerValue)sIterator.next();     
            if (rValue.getServerTypeName().equals(name) )
            {
                resourceList.add(rValue);        
            }
        
        }

        return resourceList;
    }

    /**
     * When displaying the config options (both in the ViewXXX and EditXXX 
     * tiles), we display a message "Auto-Discover foo, bar, and other 
     * services?" next to the checkbox.  The "foo, bar, and other" part
     * is what gets generated here and stuck in the request attributes
     * as the Constants.SERVICE_TYPE_EXAMPLE_LIST attribute.
     */
    public static void setRuntimeAIMessage (int sessionId,
                                            HttpServletRequest request,
                                            ServerValue server,
                                            AppdefBoss appdefBoss) 
        throws SessionTimeoutException, SessionNotFoundException, 
               RemoteException {

        // Find a couple of sample services
        int serverTypeId = server.getServerType().getId().intValue();
        PageList serviceTypes
            = appdefBoss.findServiceTypesByServerType(sessionId, serverTypeId);
        String serviceNameList;
        int numServiceTypes = serviceTypes.size();
        String stName = server.getServerType().getName();
        
        if (numServiceTypes == 0) {
            // Should not really happen
            serviceNameList = "services";
            
        } else if (numServiceTypes == 1) {
            serviceNameList = getServiceName(serviceTypes, 0, stName);
            
        } else if (numServiceTypes == 2) {
            serviceNameList
                = getServiceName(serviceTypes, 0, stName)
                + " and "
                + getServiceName(serviceTypes, 1, stName);
        } else {
            serviceNameList 
                = getServiceName(serviceTypes, 0, stName)
                + ", "
                + getServiceName(serviceTypes, 1, stName)
                + ", and other services";
        }
        // System.err.println("serviceNameList---->" + serviceNameList);
        request.setAttribute(Constants.AI_SAMPLE_SERVICETYPE_LIST, 
                             serviceNameList);
    }
    
    /**
     * builds a list of ids of ai resources for resources which are not
     * ignored.
     */
    public static List buildAIResourceIds(AIAppdefResourceValue[] aiResources,
                                          boolean ignored) {
        List listServerIds = new ArrayList();
        
        for (int i = 0; i < aiResources.length; i++)
            if (aiResources[i].getIgnored() == ignored )
                listServerIds.add(aiResources[i].getId());
        
        return listServerIds;
    }

    /**
     * build a list of supported server types for ai subsystem
     */
    public static AppdefResourceTypeValue[] buildSupportedAIServerTypes
        (ServletContext ctx,
         HttpServletRequest request,
         String platType) throws Exception {

        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        AIBoss aiBoss = ContextUtils.getAIBoss(ctx);
        int sessionId = RequestUtils.getSessionIdInt(request);

        // Build support ai server types
        PlatformTypeValue ptv =
            appdefBoss.findPlatformTypeByName(sessionId, platType);
        List serverTypes =
            appdefBoss.findServerTypesByPlatformType(sessionId,
                                                     ptv.getId(),
                                                     PageControl.PAGE_ALL);
        List serverTypeVals = new ArrayList();
        for (Iterator i = serverTypes.iterator(); i.hasNext(); ) {
            ServerTypeValue stv = (ServerTypeValue)i.next();
            //XXX NewServerFormPrepareAction does similar, should there
            //be a generic method?
            if (stv.getVirtual()) {
                continue;
            }
            serverTypeVals.add(stv);
        }

        Map serverSigs = aiBoss.getServerSignatures(sessionId,
                                                    serverTypeVals);

        ServerTypeValue[] typeArray =
            (ServerTypeValue[])serverTypeVals.toArray(new ServerTypeValue[0]);
        List filteredServerTypes = BizappUtils.
            buildServerTypesFromServerSig(typeArray,
                                          serverSigs.values().iterator());

        AppdefResourceTypeValue[] sType =
            new AppdefResourceTypeValue[filteredServerTypes.size()];
        
        filteredServerTypes.toArray(sType);                                         

        return sType;
    }

    /**
     * build a list of server types extracted from the ai server list 
     */
    public static AppdefResourceTypeValue[] buildfilteredAIServerTypes(
            AppdefResourceTypeValue[] supportedResTypes,
            AIServerValue[] sValues) {
        List filteredServerTypes = new ArrayList();
        for (int i = 0; i < sValues.length; i++)
        {
            String sTypeName = sValues[i].getServerTypeName();
            AppdefResourceTypeValue appdefType = 
                    findResourceTypeValue(supportedResTypes, sTypeName);
           if (appdefType != null)
            filteredServerTypes.add(appdefType);
        }
        
        filteredServerTypes = sortAppdefResourceType(filteredServerTypes);
        AppdefResourceTypeValue[] sType = new AppdefResourceTypeValue[filteredServerTypes.size()];
        
        filteredServerTypes.toArray(sType);                                         
        return sType;
    }

    /**
     * use this comparator to sort the AppdefResourceTypeValue objects
     */
    private static Comparator COMPARE_NAME = new Comparator() {
        public int compare(Object obj1, Object obj2) {
            
            AppdefResourceTypeValue resType1 = (AppdefResourceTypeValue)obj1;
            AppdefResourceTypeValue resType2 = (AppdefResourceTypeValue)obj2;

            if (resType1 == null) {
                if (resType2 == null) return 0;
                else return Integer.MAX_VALUE;
            }
            if (resType2 == null) return Integer.MIN_VALUE;

            return resType1.getName().compareToIgnoreCase(resType2.getName());
        }
    };
    
    /**
     * use this class to sort pending AppdefResourceTypeValue objects
     * for groups.  case-sensitive sort.
     */
    private class AppdefResourceNameComparator implements Comparator
    {
        private PageControl pc = null;
        
        public AppdefResourceNameComparator(PageControl pc)
        {
            this.pc = pc;
        }
        
            /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object obj1, Object obj2) {
            
            AppdefResourceValue res1 = (AppdefResourceValue)obj1;
            AppdefResourceValue res2 = (AppdefResourceValue)obj2;
            
            // Case insensitive compare
            String name1 = res1.getName().toLowerCase();
            String name2 = res2.getName().toLowerCase();
            if (pc != null && pc.isDescending())
                return -(name1.compareTo(name2));
            else
                return name1.compareTo(name2);
        }

    }
    
    /**
     * use this class to sort the AppdefResourceTypeValue objects
     */
    private class AIResourceIdComparator implements Comparator
    {
            /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object obj1, Object obj2) {
            
            AIAppdefResourceValue res1 = (AIAppdefResourceValue)obj1;
            AIAppdefResourceValue res2 = (AIAppdefResourceValue)obj2;
            
            return res1.getId().compareTo(res2.getId());
        }

    }
    
    public static List sortAppdefResourceType(List resourceType)
    {
        List sortedList = new ArrayList();
        SortedSet sSet = new TreeSet(COMPARE_NAME);
        sSet.addAll(resourceType);
        CollectionUtils.addAll(sortedList, sSet.iterator());        
        return sortedList;    
    }
    
    public static List sortAIResource(List resource)
    {
        List sortedList = new ArrayList();
        SortedSet sSet =
            new TreeSet(new BizappUtils().new AIResourceIdComparator());
        sSet.addAll(resource);
        CollectionUtils.addAll(sortedList, sSet.iterator());        
        return sortedList;    
    }
    
    /**
     * builds a list of server types from ServerSignature objects
     */
    public static List buildServerTypesFromServerSig(AppdefResourceTypeValue[] sTypes, 
                                                     Iterator sigIterator) 
    {
        List serverTypes = new ArrayList();
        synchronized(serverTypes) {
            while (sigIterator.hasNext()) {
                ServerSignature st = (ServerSignature)sigIterator.next();

                AppdefResourceTypeValue stype = 
                        BizappUtils.findResourceTypeValue(sTypes, st.getServerTypeName());
                serverTypes.add(stype);
            }
        }
        Collections.sort(serverTypes, COMPARE_NAME);
        return serverTypes;
    }

    /**
     * find a ResourceTypeValue object from a list of ResourceTypeValue obects.
     *
     * @return a ResourceTypeValue or null
     */
    public static AppdefResourceTypeValue findResourceTypeValue(
        AppdefResourceTypeValue[] resourceTypes, String name) {
        for(int i = 0; i < resourceTypes.length; i++)
        {
            AppdefResourceTypeValue resourceType = resourceTypes[i];
            if (resourceType != null && resourceType.getName().equals(name) )
                return resourceType;
        }
        
        return null;
    }

    /**
     * This method builds a list of AppdefResourceValue objects 
     * from a list of AppdefEntityID.  
     * 
     * This function should be moved into bizapp layer if possible 
     * later.  I am leaving it until I have an api which provides 
     * similar functionality.
     * 
     * @return a list of AppdefResourceValue objects
     */
    public static List buildAppdefResources(int sessionId, AppdefBoss boss,
                                            AppdefEntityID[] entities) 
        throws ObjectNotFoundException, RemoteException,
               SessionTimeoutException, SessionNotFoundException,
               PermissionException 
    {
        if (entities == null)
            return new ArrayList();
        
        return boss.findByIds(sessionId, entities);            
    }
    
    /**
     * This method sorts list of AppdefResourceValue objects 
     * 
     * @return a list of AppdefResourceValue objects
     */
    public static List sortAppdefResource(List appdefList, PageControl pc) 
    {
        List sortedList = new ArrayList();
        SortedSet sSet =
            new TreeSet(new BizappUtils().new AppdefResourceNameComparator(pc));
        sSet.addAll(appdefList);
        CollectionUtils.addAll(sortedList, sSet.iterator());       
        
        // There are duplicated names, figure out where to insert them
        for (Iterator it = appdefList.iterator();
             sortedList.size() != appdefList.size() && it.hasNext(); ) {
            AppdefResourceValue res = (AppdefResourceValue) it.next();
            
            for (int i = 0; i < sortedList.size(); i++) {
                AppdefResourceValue sorted =
                    (AppdefResourceValue) sortedList.get(i);
                if (sorted.getEntityId().equals(res.getEntityId()))
                    break;
                
                // Either it's meant to go in between or the last
                if (res.getName().toLowerCase().compareTo(
                    sorted.getName().toLowerCase()) < 0 ||
                    i == sortedList.size() - 1) {
                    sortedList.add(i, res);
                    break;
                }
            }
        }
        
        return sortedList;    
            
    }

    /**
     * This method builds a list of AppdefEntityID objects from
     * [entityType]:[resourceTypeId] strings
     * 
     * @param entityIds list of [entityType]:[resourceTypeId] strings
     */
    public static List buildAppdefEntityIds(List entityIds)
    {
        List entities = new ArrayList();
        Iterator rIterator = entityIds.iterator();
        while (rIterator.hasNext())
        {
            AppdefEntityID entityId = new AppdefEntityID(
                        (String)rIterator.next());
            entities.add(entityId);
        }
                    
        return entities;
    }


    /**
     * builds the value objects in the form: [entity type id]:[resource type id]
     * 
     * @param resourceTypes
     * @return List
     * @throws InvalidAppdefTypeException
     */
    public static PageList buildAppdefOptionList(List resourceTypes,
                                                 boolean useHyphen)
        throws InvalidAppdefTypeException {
        PageList optionList = new PageList();
        
        if (resourceTypes == null)
            return optionList;
            
        Iterator aIterator = resourceTypes.iterator();
        optionList.setTotalSize(resourceTypes.size() );
        while (aIterator.hasNext())
        {
            AppdefResourceTypeValue sTypeVal = 
                            (AppdefResourceTypeValue)aIterator.next();
            
            HashMap map1 = new HashMap(2);
            map1.put("value", sTypeVal.getAppdefTypeKey() );
            if (useHyphen)
                map1.put("label", "- " + sTypeVal.getName());
            else
                map1.put("label", sTypeVal.getName());
                
            optionList.add(map1);
        }
        
        return optionList;
    }
    

    /**
     * Return the full name of the subject.
     *
     * @param subject the subject
     */
    public static String makeSubjectFullName(AuthzSubjectValue subject) {
        return makeSubjectFullName(subject.getFirstName(),
                                   subject.getLastName());
    }

    /**
     * build group types and its corresponding resource string 
     * respresentations from the ApplicationResources.properties file.
     *
     * @return a list 
     */
    public static List buildGroupTypes(HttpServletRequest request) 
    {
        
        List groupTypes = new ArrayList();

        HashMap map2 = new HashMap(2);
        map2.put("value", 
        new Integer(Constants.APPDEF_TYPE_GROUP_COMPAT));
        map2.put("label", RequestUtils.message(request, 
                                "resource.group.inventory.CompatibleClusterResources"));
        groupTypes.add(map2);

        HashMap map1 = new HashMap(2);
        map1.put("value", 
                new Integer(Constants.APPDEF_TYPE_GROUP_ADHOC));
        map1.put("label", RequestUtils.message(request, 
                                "resource.group.inventory.MixedResources"));
        groupTypes.add(map1);

        return groupTypes;
    }

    /**
     * builds a list of AppdefResourceValue objects from a list
     * of AppdefEntityID objects stored in the group.
     * @param group AppdefGroupValue which contains the list of resources
     * @param pc TODO
     * 
     * @return a list of AppdefResourceValue objects
     */
    public static List buildGroupResources(AppdefBoss boss, int sessionId,
                                           AppdefGroupValue group,
                                           PageControl pc) 
        throws ObjectNotFoundException, RemoteException, PermissionException,
               SessionTimeoutException, SessionNotFoundException 
    {
        List grpEntries = group.getAppdefGroupEntries();
        
        AppdefEntityID[] entities = new AppdefEntityID[grpEntries.size()];
        entities = (AppdefEntityID[]) grpEntries.toArray(entities);
                    
        return boss.findByIds(sessionId, entities, pc);
    }


    /**
     * Check in the permissions map to see if the user can administer HQ.
     * @return Whether or not the admin cam is contained in the type map.
     */
    public static boolean canAdminHQ(Integer sessionId,
                                     AuthzBoss boss) {
        try {
            return boss.hasAdminPermission(sessionId.intValue());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Return a <code>List</code> of <code>AuthzSubjectValue</code>
     * objects from a list that do <strong>not</strong> appear in
     * a list of matches.
     *
     * @param all the list to operate on
     * @param matches the list to grep out
     */
    public static List grepSubjects(List all, List matches) {
        if (all == null || matches == null) {
            return new ArrayList(0);
        }

        // build an index of role subjects
        HashSet index = new HashSet();
        Iterator mi = matches.iterator();
        while (mi.hasNext()) {
            Object m = mi.next();
            
            if (m instanceof AuthzSubjectValue)
                index.add(((AuthzSubjectValue) m).getId());
            else if (m instanceof AuthzSubject)
                index.add(((AuthzSubject) m).getId());
        }

        // find available subjects (those not in the index)
        ArrayList objects = new ArrayList();
        Iterator ai = all.iterator();
        while (ai.hasNext()) {
            AuthzSubjectValue obj = (AuthzSubjectValue) ai.next();
            if (!index.contains(obj.getId())) {
                objects.add(obj);
            }
        }

        return objects;
    }

    public static String getBaselineText(String baselineOption,
                                         Measurement m) {
        final String minText  = "Min Value";
        final String meanText = "Baseline Value";
        final String maxText  = "Max Value";

        if (null != m && null != m.getBaseline()) {
            Baseline b = m.getBaseline();
            if (baselineOption.equals(MeasurementConstants.BASELINE_OPT_MIN)) {
                if (null != b.getMaxExpectedVal()) {
                    FormattedNumber min = UnitsConvert.convert(
                        b.getMinExpectedVal().doubleValue(),
                        m.getTemplate().getUnits());
                    return min.toString() + " (" + minText + ')';
                }
                else
                    return minText;
            }
            
            if (baselineOption.equals(MeasurementConstants.BASELINE_OPT_MEAN)) {
                if (null != b.getMean()) {
                    FormattedNumber mean =
                        UnitsConvert.convert(b.getMean().doubleValue(),
                                             m.getTemplate().getUnits());
                    return mean.toString() + " (" + meanText + ')';
                }
                else
                    return meanText;
            }
            
            if (baselineOption.equals(MeasurementConstants.BASELINE_OPT_MAX)) {
                if (null != b.getMaxExpectedVal()) {
                    FormattedNumber max = UnitsConvert.convert(
                        b.getMaxExpectedVal().doubleValue(),
                        m.getTemplate().getUnits());
                    return max.toString() + " (" + maxText + ')';
                }
                else
                    return maxText;
            }
        }

        // this is the fall-through
        if (baselineOption.equals(MeasurementConstants.BASELINE_OPT_MIN))
            return minText;
        else if (baselineOption.equals(MeasurementConstants.BASELINE_OPT_MAX))
            return maxText;
        else
            return meanText;
    }

    /**
     * build a list of UI option using a list of ConfigOptions
     */
    public static List buildLoadConfigOptions(ConfigSchema config,
                                              ConfigResponse oldResponse) {
        return buildLoadConfigOptions("", config, oldResponse);
    }

    //XXX tmp hack.  AIPlatformValue does not have a ConfigResponse, if it did,
    //plugins could set this default themselves.
    private static String getDefaultConfigValue(ConfigOption option,
                                                ConfigResponse config) {
        String value = option.getDefault();
        String name = option.getName();
        String type = config.getValue(ProductPlugin.PROP_PLATFORM_TYPE);
        if (name.equals(SNMPClient.PROP_IP) ||
            name.equals(PlatformServiceDetector.PROP_IPADDRESS))
        {
            //dont want to override those that default to the loopback
            //address, such as apache and iplanet servers
            if (type != null && !PlatformDetector.isSupportedPlatform(type)) {
                value = config.getValue(ProductPlugin.PROP_PLATFORM_IP);
            }
        }
        if (value == null) {
            value = option.getDefault();
        }
        return value;
    }

    /**
     * build a list of UI option using a list of ConfigOptions
     */
    public static List buildLoadConfigOptions( String prefix,
                                               ConfigSchema config,
                                               ConfigResponse oldResponse)
    {
        List options = config.getOptions();
        List uiOptions = new ArrayList();
        int i,nOptions = options.size();
        //XXX set the defaults from the oldResponse and also 
        Set oldKeys = oldResponse.getKeys();
        if (prefix == null) prefix = "";
        i=0;
        while (i < nOptions) {
            ConfigOption option = (ConfigOption)options.get(i);
            
            ConfigValues configValue = new ConfigValues();
            configValue.setOption(prefix + option.getName());
            configValue.setPrefix(prefix);
            configValue.setValue(oldKeys.contains(option.getName())?
                    oldResponse.getValue(option.getName())  :
                    getDefaultConfigValue(option, oldResponse));
            configValue.setOptional(option.isOptional());
            configValue.setDescription(option.getDescription());

            if(option instanceof HiddenConfigOption) {
                // Skip hidden options
                i++;
                continue;
            }
        
            if(option instanceof StringConfigOption) {
                if(((StringConfigOption) option).isSecret()) {
                    
                    configValue.setIsSecret(true);
                }
            }

            if(option instanceof BooleanConfigOption) {
                configValue.setIsBoolean(true);
            }        
            else if(option instanceof EnumerationConfigOption) {
                List enumValues = ((EnumerationConfigOption) option).getValues();
                List uiEnumOptions = new ArrayList();
                for(Iterator itr = enumValues.iterator();itr.hasNext();) {
                    String labelValue = (String)itr.next();
                    uiEnumOptions.add(new LabelValueBean(labelValue,labelValue));
                }
                configValue.setEnumValues(uiEnumOptions);
                configValue.setIsEnumeration(true);
            }
            else if(option instanceof DirArrayConfigOption) {
                // on upgrades the value may not be set if the config option
                // was recently added.
                String oldValue = oldResponse.getValue(option.getName());
                if (oldValue != null) {
                    configValue.setValue(StringUtil.
                                         replace(new String(oldValue),
                                                 Constants.DIR_PIPE_SYM,
                                                 Constants.DIR_COMMA_SYM));
                }
                    
                configValue.setDescription( 
                    StringUtil.replace(new String(option.getDescription()), 
                        Constants.DIR_PIPE_SYM, Constants.DIR_COMMA_SYM));
                configValue.setIsDir(true);
            } else if (option instanceof ArrayConfigOption ) {
                // since the de-limiter for ArrayConfigOptions is a space, 
                // construct a StringTokenizer . If the oldKeys contains 
                // this option use its value and construct , otherwise use an 
                // eppty-string.
                StringTokenizer tok = new StringTokenizer(oldKeys.
                                            contains(option.getName()) ?
                                            oldResponse.getValue(option.getName()) :
                                            ""," ");
                List uiArrayOptions = new ArrayList();
                
                while (tok.hasMoreTokens()) {
                    String labelValue = tok.nextToken();
                    uiArrayOptions.add(new LabelValueBean(labelValue, labelValue));
                }
                configValue.setEnumValues(uiArrayOptions);
                configValue.setIsArray(true);
            }
            
            uiOptions.add(configValue);
            
            i++;
        }
        
        return uiOptions;
    }

    /**
     * @return the last error stored in the scan state
     */
    public static StringifiedException findLastError(ScanStateCore scanState)
    {
        StringifiedException lastError = scanState.getGlobalException();
        if (lastError != null)
            return lastError;

        ScanMethodState[] methodStates = scanState.getScanMethodStates();
        for (int i = 0; i < methodStates.length; i++)
        {
            StringifiedException[] exceptions = methodStates[i].getExceptions();
            if (exceptions != null)
            {
                for (int j = 0; j < exceptions.length; j++)
                {
                    if (exceptions[j] != null)
                        lastError = exceptions[j]; 
                } // for j
            } // if
        } // for i
        
        return lastError;    
    }
    
    public static ConfigResponse buildSaveConfigOptions(
            HttpServletRequest request, ConfigResponse oldResponse,
            ConfigSchema config, ActionErrors errors)
        throws InvalidOptionException, InvalidOptionValueException,
               InvalidOptionValsFoundException {

        return buildSaveConfigOptions("", request, oldResponse, config, errors);
    }

    public static ConfigResponse buildSaveConfigOptions(String prefix,
            HttpServletRequest request, ConfigResponse oldResponse,
            ConfigSchema config, ActionErrors errors)
        throws InvalidOptionException, InvalidOptionValueException,
               InvalidOptionValsFoundException 
    {
        boolean invalidConfigOptionFound = false;
        Enumeration params = request.getParameterNames();
        List keys = config.getOptions();
        List stringKeys = new ArrayList();
        Hashtable configList = new Hashtable();
        if (prefix == null) prefix = "";
        for(Iterator itr = keys.iterator();itr.hasNext();) {
            ConfigOption opt = (ConfigOption) itr.next(); 
            stringKeys.add(opt.getName());
            configList.put(opt.getName(), opt);
        }
        
        ConfigResponse configResponse;
        String value = "";
        String param = "";
        String shortParam;
        configResponse = new ConfigResponse(config);
        while(params.hasMoreElements()) {
            param = (String) params.nextElement();
            if (!param.startsWith(prefix)) continue;
            shortParam = param.substring(prefix.length());
            if(stringKeys.contains( shortParam )) {
               value = request.getParameter(param).trim();
               
                try {
                    if(!value.equals(oldResponse.getValue(shortParam))) {
                        ConfigOption opt = (ConfigOption)configList.get(shortParam);
                        
                        // filter for DirArrayConfigOption consumption
                        if (opt instanceof DirArrayConfigOption)
                            value = StringUtil.replace(new String(value), 
                                    Constants.DIR_COMMA_SYM, Constants.DIR_PIPE_SYM );
                    
                        if (value == null)
                            value = "";                 
                        configResponse.setValue(shortParam, value);
                    }
                    else {
                        configResponse.setValue(shortParam, oldResponse.getValue(shortParam));
                    }
                } catch (InvalidOptionValueException e) {
                    if (invalidConfigOptionFound = (errors != null)) {
                        errors.add( param,
                        new ActionMessage(
                            "resource.common.inventory.error.InvalidConfigOption", 
                            e.getMessage()));
                    }
                    else
                        throw e;
                }
            }
        }
        
        if (invalidConfigOptionFound)
            throw new InvalidOptionValsFoundException(
                "an config option value exception has been thrown ",
                configResponse);
            
        return configResponse;
    }

    /**
     * Gut the <code>String[]</code> appdef key values for the passed-in
     * entity ids.
     *
     * @param eids the appdef entity ids
     * @return String[] of appdef keys (type:rid)
     */
    public static String[] stringifyEntityIds(AppdefEntityID[] eids) {
        String[] eidStrs = new String[eids.length];
        for (int i=0; i<eids.length; ++i) {
            eidStrs[i] = eids[i].getAppdefKey();
        }
        return eidStrs;
    }

    /**
     * Populate a config response with values.  The config is populated
     * with values by looking at all the schema keys, and pulling their
     * corresponding values out of the requestParams.  The oldConfig is
     * supplied so that we can tell if anything has actually changed.
     *
     * @param request The servlet request
     * @param prefix Only parameters with this prefix will be considered.  The 
     * prefix will be stripped before inserting it as a key into the config.
     * @param schema The config schema that will supply the keys we'll look for
     * in the requestParams
     * @param config The ConfigResponse to populate with values
     * @param oldConfig The existing configuration, used for comparison to see
     * if anything has changed.
     * @return True if there were actually changes to the config, False if there
     * were not any changes to the config, or null if the schema has no keys, so
     * the concept of changes might not make sense.
     */
    public static Boolean populateConfig (HttpServletRequest request,
                                          String prefix,
                                          ConfigSchema schema,
                                          ConfigResponse config,
                                          ConfigResponse oldConfig)
        throws InvalidOptionException, InvalidOptionValueException {

        if ( prefix == null ) prefix = "";
        Boolean wereChanges = Boolean.FALSE;
        List keys = schema.getOptions();

        // If the schema has no keys, then there can be no changes
        int numKeys = keys.size();
        if (numKeys == 0) return null;
        
        ConfigOption opt;
        String optName, value;
        String[] values;
        for ( int i=0; i<numKeys; i++ ) {
            opt = (ConfigOption) keys.get(i);
            optName = opt.getName();
            values = request.getParameterValues(prefix + optName);
            if ( values == null  ) {
                continue;
            }
            if ( values.length > 1 ) {
                value="";
                for(int regExps=0;regExps<values.length;regExps++) {
                    value += values[regExps] + " ";
                }
            } else {
                value = values[0];
            }
            if ( value != null ) {
                config.setValue(optName, value);
                if ( wereChanges == Boolean.FALSE 
                     && !value.equals(oldConfig.getValue(optName)) ) {
                    wereChanges = Boolean.TRUE;
                }
            }
        }
        return wereChanges;
    }

    /**
     * Parse a measurement value and units string.  For example:
     * "30MB" = 31,457,280.0
     *
     * @param value The string to parse
     * @param mtv The measurement template being parsed
     * @return the measurement as parsed based on a number and its units
     * @throws ParseException
     */
    public static double parseMeasurementValue(String value, String unit)
        throws ParseException
    {
        int unitType = UnitsConvert.getUnitForUnit(unit);
        int scale = UnitsConvert.getScaleForUnit(unit);
        UnitNumber num = UnitsFormat.parse(value, unitType);
        return num.getScaledValue(scale).doubleValue();
    }

    // A map of ServerType.name -> Boolean (true for auto-approved server types)
    private static Map serverTypeCache = new HashMap();

    private synchronized static void loadServerTypeCache(int sessionId,
                                                         AppdefBoss appdefBoss){
        if (serverTypeCache.size() > 0) return;
        PageList types;
        try {
            types = appdefBoss.findAllServerTypes(sessionId, 
                                                  PageControl.PAGE_ALL);
        } catch (Exception e) {
            throw new IllegalStateException("Error loading server types: " + e);
        }
        ServerTypeValue stValue;
        String name;
        for (int i=0; i<types.size(); i++) {
            stValue = (ServerTypeValue) types.get(i);
            name = stValue.getName();
            if (stValue.getVirtual()) {
                serverTypeCache.put(name, Boolean.TRUE);
            } else {
                serverTypeCache.put(name, Boolean.FALSE);
            }
        }
    }
    
    public static boolean isAutoApprovedServer(int sessionId,
                                               AppdefBoss appdefBoss,
                                               AIServerValue aiServer) {
        // Load the server type cache if it's not loaded already
        synchronized(serverTypeCache) {
            if (serverTypeCache.size() == 0) {
                loadServerTypeCache(sessionId, appdefBoss);
            }
        }
        Boolean isAutoApproved
            = (Boolean) serverTypeCache.get(aiServer.getServerTypeName());
        if (isAutoApproved == null) {
            // Should never happen
            return false;
        }
        return isAutoApproved.booleanValue();
    }

    public static void populateAgentConnections(int sessionId,
                                               AppdefBoss appdefBoss,
                                               HttpServletRequest request,
                                               PlatformForm form,
                                               String usedIpPort)
        throws RemoteException,
               SessionTimeoutException,
               SessionNotFoundException {

        List agents = appdefBoss.findAllAgents(sessionId);
        List uiAgents = new ArrayList();
        for (Iterator itr = agents.iterator();itr.hasNext();) {
            Agent agent = (Agent)itr.next();
            uiAgents.add(new AgentBean(agent.getAddress(), 
                         agent.getPort()));

        }

        form.setAgents(uiAgents);
        request.setAttribute(Constants.AGENTS_COUNT, 
                             new Integer(uiAgents.size()));
        request.setAttribute("usedIpPort", usedIpPort);
    }

    public static Agent getAgentConnection(int sessionId,
                                           AppdefBoss appdefBoss,
                                           HttpServletRequest request,
                                           PlatformForm form)
        throws RemoteException,
               SessionTimeoutException,
               SessionNotFoundException,
               AgentNotFoundException {

        String agentIpPort = form.getAgentIpPort();

        if (agentIpPort != null) {
            StringTokenizer st = new StringTokenizer(agentIpPort, ":");
            String ip = null;
            int port = -1;
            while(st.hasMoreTokens()) {
                ip = st.nextToken();
                port = Integer.parseInt(st.nextToken());
            }

            return appdefBoss.findAgentByIpAndPort(sessionId, ip, port);
        }
        else {
            return null;
        }
    }

    public static void startAutoScan(ServletContext ctx,
                                     int sessionId,
                                     AppdefEntityID entityId) {
        try {
            AIBoss aiboss = ContextUtils.getAIBoss(ctx);

            aiboss.startScan(sessionId, entityId.getID(),
                             new ScanConfigurationCore(),
                             null, null, null);
        } catch (Exception e) {
            log.error("Error starting scan: " + e.getMessage(), e);
        }
    }
}
