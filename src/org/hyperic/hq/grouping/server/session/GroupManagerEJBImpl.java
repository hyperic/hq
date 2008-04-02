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

package org.hyperic.hq.grouping.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ejb.FinderException;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceGroupManagerUtil;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.grouping.shared.GroupEntry;
import org.hyperic.hq.grouping.shared.GroupModificationException;
import org.hyperic.hq.grouping.shared.GroupNotFoundException;
import org.hyperic.hq.grouping.shared.GroupValue;
import org.hyperic.hq.grouping.shared.GroupVisitorException;
import org.hyperic.hq.grouping.shared.GroupManagerLocal;
import org.hyperic.hq.grouping.shared.GroupManagerUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

/**
  * @ejb:bean name="GroupManager"
  *           jndi-name="ejb/grouping/GroupManager"
  *           local-jndi-name="LocalGroupManager"
  *           view-type="local"
  *           type="Stateless"
  */
public class GroupManagerEJBImpl implements javax.ejb.SessionBean {
    private final Log log = LogFactory.getLog(GroupManagerEJBImpl.class);
    public final String authzResourceGroupName = "covalentAuthzResourceGroup";

    private String fetchGroupOwner(Integer gid) {
        String retVal = "";
        try {
            ResourceGroupManagerLocal rgmLoc = getResourceGroupManager();
                
            retVal = rgmLoc.getResourceGroupOwner(gid).getName();
        } catch (Exception e){
            log.debug("Unable to set subject during create", e);
        }
        return retVal;
    }

    /**
     *  Lookup and return a group specified by the id contained within the
     *  value object provided.
     * 
     * @param subject The authz subject
     * @param retVal the group value - can have registered visitors.
     * @param pc for paging.
     * @return GroupValue
     * @throws PermissionException - you must be owner or authorized to access
     *         a particular group.
     * @throws GroupNotFoundException when group is non-existent.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public GroupValue findGroup (AuthzSubject subj, GroupValue retVal,
                                 boolean full)
        throws PermissionException, GroupNotFoundException 
    {
        AuthzSubjectValue subject = subj.getAuthzSubjectValue();
        ResourceGroupManagerLocal rgmLoc = getResourceGroupManager();

        ResourceGroup group;
        if (retVal.getName () != null) {
            /*
                group = rgmLoc.findResourceGroupByName(subj,
                                                       retVal.getName());
                                                           */
            // This used to find by name.  We are tempo rarily disabling
            // it while we remove this function.
            return null;    
        } else { 
            group = rgmLoc.findResourceGroupById(subj,retVal.getId());
        }

        if (group == null) {
            throw new GroupNotFoundException();
        }

        // Create our return group vo
        retVal.setId(group.getId());
        retVal.setName(group.getName());
        retVal.setDescription(group.getDescription());
        retVal.setLocation(group.getLocation());
        retVal.setGroupType(group.getGroupType().intValue());
        retVal.setGroupEntType(group.getGroupEntType().intValue());
        retVal.setGroupEntResType(group.getGroupEntResType().intValue());
        retVal.setTotalSize( group.getResources().size() );

        if (full) {
            retVal.setSubject(subject);
            retVal.setClusterId(group.getClusterId().intValue());
            retVal.setMTime(new Long(group.getMtime()));
            retVal.setCTime(new Long(group.getCtime()));
            retVal.setModifiedBy(group.getModifiedBy());
            retVal.setOwner(fetchGroupOwner(group.getId()));
                
            // Add the group members
            for (Iterator i = group.getResources().iterator(); i.hasNext();)
            {
                Resource resVo = (Resource) i.next();
                GroupEntry ge =
                    new GroupEntry(resVo.getInstanceId(),
                                   resVo.getResourceType().getName());
                retVal.addEntry(ge);
            }
        }
        return retVal;
    }

    /**
     *  Lookup and return a group from the specified identifier.
     * 
     * @param subj subject
     * @param gv for filtering
     * @param rv for filtering
     * @param pc for paging
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public PageList findAllGroups (AuthzSubject subj, GroupValue gv, 
                                   ResourceValue rv, PageControl pc) 
        throws  GroupNotFoundException, PermissionException,
                CloneNotSupportedException 
    {
        AuthzSubjectValue subject = subj.getAuthzSubjectValue();
        PageList retVal = null;
        try {
            ResourceGroupManagerLocal rgmLoc = getResourceGroupManager();

            List rgList;
            if (rv != null) {
                rgList = rgmLoc.getAllResourceGroupsResourceInclusive(subj,
                                                                      pc,rv);
            } else {
                rgList = rgmLoc.getAllResourceGroups(subj,pc,true);
            }

            List toBePaged = new ArrayList();

            for (Iterator i=rgList.iterator();i.hasNext();) {
                ResourceGroupValue rgVo = (ResourceGroupValue) i.next();

                // Filter out the system group entries.
                if (rgVo.getSystem())
                    continue;

                // Create a new group value to populate
                GroupValue gVal = (GroupValue)gv.clone();

                /* Create our return group vo */
                gVal.setId          (rgVo.getId());
                gVal.setName        (rgVo.getName());
                gVal.setDescription (rgVo.getDescription());
                gVal.setLocation    (rgVo.getLocation());
                gVal.setGroupType   (rgVo.getGroupType());
                gVal.setSubject     (subject);
                gVal.setGroupEntType(rgVo.getGroupEntType());
                gVal.setGroupEntResType(rgVo.getGroupEntResType());
                gVal.setClusterId   ( rgVo.getClusterId());
                gVal.setMTime       ( rgVo.getMTime() );
                gVal.setCTime       ( rgVo.getCTime() );
                gVal.setModifiedBy  ( rgVo.getModifiedBy() );
                gVal.setOwner       ( fetchGroupOwner(rgVo.getId()) );

                /* Add the group members */
                Collection resList = rgmLoc.getResources(subject, rgVo.getId());
    
                for (Iterator iter=resList.iterator();iter.hasNext();) {
                    Resource resVo = (Resource) iter.next();
                    GroupEntry ge = 
                        new GroupEntry(resVo.getInstanceId(),
                                       resVo.getResourceType().getName());
                    gVal.addEntry(ge);
                }
                toBePaged.add(gVal);
            }
            retVal = getPageList(toBePaged, pc);

        } catch (FinderException fe) {
            log.debug("Finder caught, no groups for this subject: "+
                      fe.getMessage());
            throw new GroupNotFoundException("No groups exist for specified "+
                                            "subject.",fe);
        } catch (PermissionException pe) {
            log.error("GroupManager caught PermissionException: "+
                      pe.getMessage());
            throw pe;
        }
        return retVal;
    }

    // For group paging only. Not members!
    private PageList getPageList (Collection coll, PageControl pc) {
        Pager defaultPager = Pager.getDefaultPager();
        PageControl.initDefaults(pc, SortAttribute.RESGROUP_NAME);
        return  defaultPager.seek(coll,pc.getPagenum(),pc.getPagesize());
    }

    /* Get the authz resource type value  */
    protected ResourceType getResourceType(String resType)
        throws FinderException 
    {
        return getResourceManager().findResourceTypeByName(resType);
    }

    private ResourceGroupManagerLocal getResourceGroupManager() {
        try {
            return ResourceGroupManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    private ResourceManagerLocal getResourceManager() {
        try {
            return ResourceManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    // To avoid groups with duplicate names, we're now performing a
    // a case-insensitive name based find before creation.
    private boolean groupNameExists(AuthzSubject subject, GroupValue gVo) {
        try {
            findGroup(subject, gVo, false);
        } catch (GroupNotFoundException e) {
            return false;
        } catch (PermissionException pe) {
            // this happens when a group with same name exists and user
            // is unauthorized to view it.
        }
        return true;
    }

    public static GroupManagerLocal getOne() {
        try {
            return GroupManagerUtil.getLocalHome().create();
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
    public void ejbRemove() { }
    public void setSessionContext(SessionContext ctx) {}
}
