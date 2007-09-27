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

package org.hyperic.hq.appdef.shared;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.resourceTree.PlatformNode;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.appdef.shared.resourceTree.ServerNode;
import org.hyperic.hq.appdef.shared.resourceTree.ServiceNode;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.SystemException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * XXX -- it would be better if this routine returned an object which 
 * could perform lazy evaluation.  Most of the properties filled out
 * will never be used -- lots of wasted cycles.
 */

/**
 * Based on the appdef entity that is passed in, this will return a set of 
 * properties for the object.  The keys are the attributes of the entity, 
 * and are referenced using 'this.' notation.  For example:
 *
 * this.server.name = apache-2.0
 * this.server.state = started
 * this.server['myServer'].name = myServer
 * this.server.location =
 * this.server.description = Apache Web Server
 * this.platform..
 * this.service..
 */

public class EntityPropertyFetcher {
    public static final String PREFIX_PLAT      = "platform.";
    public static final String PREFIX_CFGPLAT   = PREFIX_PLAT + "config['";
    public static final String POSTFIX_CFGPLAT  = "']";
    public static final String PLAT_CERTDN      = PREFIX_PLAT + "certDN";
    public static final String PLAT_COMMENT     = PREFIX_PLAT + "comment";
    public static final String PLAT_CPUCNT      = PREFIX_PLAT + "cpuCount";
    public static final String PLAT_CTIME       = PREFIX_PLAT + "cTime";
    public static final String PLAT_DESC        = PREFIX_PLAT + "description";
    public static final String PLAT_FQDN        = PREFIX_PLAT + "FQDN";
    public static final String PLAT_ID          = PREFIX_PLAT + "id";
    public static final String PLAT_LOC         = PREFIX_PLAT + "location";
    public static final String PLAT_MODBY       = PREFIX_PLAT + "modifiedBy";
    public static final String PLAT_MTIME       = PREFIX_PLAT + "mTime";
    public static final String PLAT_NAME        = PREFIX_PLAT + "name";
    public static final String PLAT_OWNER       = PREFIX_PLAT + "owner";

    public static final String PREFIX_PLATIP    = PREFIX_PLAT + "ip";
    // The following must be prefixed like: PREFIX_PLATIP + "42" + PLATIP_ADDR
    public static final String PLATIP_ADDR      = ".address";
    public static final String PLATIP_CTIME     = ".cTime";
    public static final String PLATIP_ID        = ".id";
    public static final String PLATIP_MACADDR   = ".MACAddr";
    public static final String PLATIP_MTIME     = ".mTime";
    public static final String PLATIP_NETMASK   = ".netmask";

    public static final String PREFIX_TPLAT     = PREFIX_PLAT + "type.";
    public static final String TPLAT_CTIME      = PREFIX_TPLAT + "cTime";
    public static final String TPLAT_DESC       = PREFIX_TPLAT + "description";
    public static final String TPLAT_ID         = PREFIX_TPLAT + "id";
    public static final String TPLAT_MTIME      = PREFIX_TPLAT + "mTime";
    public static final String TPLAT_NAME       = PREFIX_TPLAT + "name";
    public static final String TPLAT_PLUGIN     = PREFIX_TPLAT + "plugin";
    
    // PREFIX_NSVR is used like: PREFIX_NSVR + serverName + POSTFIX_NSVR + attr
    public static final String PREFIX_NSVR      = "server['";
    public static final String POSTFIX_NSVR     = "'].";
    public static final String PREFIX_SVR       = "server.";
    public static final String PREFIX_CFGSVR    = "config['";
    public static final String POSTFIX_CFGSVR   = "']";
    public static final String SVR_CTIME        = "cTime";
    public static final String SVR_DESC         = "description";
    public static final String SVR_ID           = "id";
    public static final String SVR_INSTPATH     = "installPath";
    public static final String SVR_AIID         = "autoinventoryIdentifier";
    public static final String SVR_LOC          = "location";
    public static final String SVR_MODBY        = "modifiedBy";
    public static final String SVR_MTIME        = "mTime";
    public static final String SVR_NAME         = "name";
    public static final String SVR_OWNER        = "owner";
    public static final String SVR_STATE        = "state";

    public static final String PREFIX_TSVR      = "type.";
    public static final String TSVR_CTIME       = PREFIX_TSVR + "cTime";
    public static final String TSVR_DESC        = PREFIX_TSVR + "description";
    public static final String TSVR_ID          = PREFIX_TSVR + "id";
    public static final String TSVR_MTIME       = PREFIX_TSVR + "mTime";
    public static final String TSVR_NAME        = PREFIX_TSVR + "name";
    public static final String TSVR_PLUGIN      = PREFIX_TSVR + "plugin";

    public static final String PREFIX_NSVC      = "service['";
    public static final String POSTFIX_NSVC     = "'].";
    public static final String PREFIX_SVC       = "service.";
    public static final String PREFIX_CFGSVC    = "config['";
    public static final String POSTFIX_CFGSVC   = "']";
    public static final String SVC_CTIME        = "cTime";
    public static final String SVC_DESC         = "description";
    public static final String SVC_ID           = "id";
    public static final String SVC_LOC          = "location";
    public static final String SVC_MODBY        = "modifiedBy";
    public static final String SVC_MTIME        = "mMime";
    public static final String SVC_NAME         = "name";
    public static final String SVC_OWNER        = "owner";
    public static final String SVC_PARENTID     = "parentId";
    public static final String SVC_STATE        = "state";

    public static final String PREFIX_TSVC      = "type.";
    public static final String TSVC_CTIME       = PREFIX_TSVC + "cTime";
    public static final String TSVC_DESC        = PREFIX_TSVC + "description";
    public static final String TSVC_ID          = PREFIX_TSVC + "id";
    public static final String TSVC_MTIME       = PREFIX_TSVC + "mTime";
    public static final String TSVC_NAME        = PREFIX_TSVC + "name";
    public static final String TSVC_PLUGIN      = PREFIX_TSVC + "plugin";

    private static Log log = 
        LogFactory.getLog(EntityPropertyFetcher.class.getName());

    /**
     * Set a property in the props object.  If the value is null, 
     * a blank string is inserted as the value.
     */
    private static void setProp(Properties props, String key, Object val){
        props.setProperty(key, val == null ? "" : val.toString());
    }

    private static void setCProps(Properties res, AppdefEntityID aID,
                                  String prefix, String postFix)
    {
        Properties cProps;

        try {
            cProps = getCPMan().getEntries(aID);
        } catch(Exception exc){
            log.error("Unable to get custom props for " + aID.getID() +
                      ": " + exc.getMessage(), exc);
            return;
        }

        for(Iterator i=cProps.entrySet().iterator(); i.hasNext(); ){
            Map.Entry ent = (Map.Entry)i.next();

            res.setProperty(prefix + (String)ent.getKey() + postFix,
                            (String)ent.getValue());
        }
    }

    public static Properties getProperties(PlatformValue p,
                                           AuthzSubjectValue subject)
    {
        Properties res = new Properties();
        PlatformTypeValue pt;
        IpValue[] ips;

        setCProps(res, p.getEntityId(),
                  PREFIX_CFGPLAT, POSTFIX_CFGPLAT);
                  
        setProp(res, PLAT_CERTDN,  p.getCertdn());
        setProp(res, PLAT_COMMENT, p.getCommentText());
        setProp(res, PLAT_CPUCNT,  p.getCpuCount());
        setProp(res, PLAT_CTIME,   p.getCTime());
        setProp(res, PLAT_DESC,    p.getDescription());
        setProp(res, PLAT_FQDN,    p.getFqdn());
        setProp(res, PLAT_ID,      p.getId());
        setProp(res, PLAT_LOC,     p.getLocation());
        setProp(res, PLAT_MODBY,   p.getModifiedBy());
        setProp(res, PLAT_MTIME,   p.getMTime());
        setProp(res, PLAT_NAME,    p.getName());
        setProp(res, PLAT_OWNER,   p.getOwner());

        ips = p.getIpValues();

        for(int i=0; i < ips.length; i++){
            setProp(res, PREFIX_PLATIP + i + PLATIP_ADDR, ips[i].getAddress());
            setProp(res, PREFIX_PLATIP + i + PLATIP_CTIME, ips[i].getCTime());
            setProp(res, PREFIX_PLATIP + i + PLATIP_ID, ips[i].getId());
            setProp(res, PREFIX_PLATIP + i + PLATIP_MACADDR, 
                    ips[i].getMACAddress());
            setProp(res, PREFIX_PLATIP + i + PLATIP_MTIME, ips[i].getMTime());
            setProp(res, PREFIX_PLATIP + i + PLATIP_NETMASK, 
                    ips[i].getNetmask());
        }

        pt = p.getPlatformType();

        setProp(res, TPLAT_CTIME,     pt.getCTime());
        setProp(res, TPLAT_DESC,      pt.getDescription());
        setProp(res, TPLAT_ID,        pt.getId());
        setProp(res, TPLAT_MTIME,     pt.getMTime());
        setProp(res, TPLAT_NAME,      pt.getName());
        setProp(res, TPLAT_PLUGIN,    pt.getPlugin());

        return res;
    }

    public static Properties getProperties(ServerValue s, 
                                           AuthzSubjectValue subject)
    {
        return getProperties(s, false, subject);
    }

    private static Properties getProperties(ServerValue s, boolean useNamed,
                                            AuthzSubjectValue subject)
    {
        ServerTypeValue st;
        Properties res = new Properties();
        String prefix;

        if(useNamed){
            prefix = PREFIX_NSVR + s.getName() + POSTFIX_NSVR;
        } else {
            prefix = PREFIX_SVR;
        }

        setCProps(res, s.getEntityId(), prefix + PREFIX_CFGSVR, POSTFIX_CFGSVR);

        setProp(res, prefix + SVR_CTIME,    s.getCTime());
        setProp(res, prefix + SVR_DESC,     s.getDescription());
        setProp(res, prefix + SVR_ID,       s.getId());
        setProp(res, prefix + SVR_INSTPATH, s.getInstallPath());
        setProp(res, prefix + SVR_AIID,     s.getAutoinventoryIdentifier());
        setProp(res, prefix + SVR_LOC,      s.getLocation());
        setProp(res, prefix + SVR_MODBY,    s.getModifiedBy());
        setProp(res, prefix + SVR_MTIME,    s.getMTime());
        setProp(res, prefix + SVR_NAME,     s.getName());
        setProp(res, prefix + SVR_OWNER,    s.getOwner());

        st = s.getServerType();

        setProp(res, prefix + TSVR_CTIME,   st.getCTime());
        setProp(res, prefix + TSVR_DESC,    st.getDescription());
        setProp(res, prefix + TSVR_ID,      st.getId());
        setProp(res, prefix + TSVR_MTIME,   st.getMTime());
        setProp(res, prefix + TSVR_NAME,    st.getName());
        setProp(res, prefix + TSVR_PLUGIN,  st.getPlugin());

        return res;
    }

    public static Properties getProperties(ServiceValue v, 
                                           AuthzSubjectValue subject)
    {
        return getProperties(v, false, subject);
    }

    private static Properties getProperties(ServiceValue v, boolean useNamed,
                                            AuthzSubjectValue subject)
    {
        ServiceTypeValue vt;
        Properties res = new Properties();
        String prefix;

        if(useNamed){
            prefix = PREFIX_NSVC + v.getName() + POSTFIX_NSVC;
        } else {
            prefix = PREFIX_SVC;
        }

        setCProps(res, v.getEntityId(), prefix + PREFIX_CFGSVC, POSTFIX_CFGSVC);

        setProp(res, prefix + SVC_CTIME,    v.getCTime());
        setProp(res, prefix + SVC_DESC,     v.getDescription());
        setProp(res, prefix + SVC_ID,       v.getId());
        setProp(res, prefix + SVC_LOC,      v.getLocation());
        setProp(res, prefix + SVC_MODBY,    v.getModifiedBy());
        setProp(res, prefix + SVC_MTIME,    v.getMTime());
        setProp(res, prefix + SVC_NAME,     v.getName());
        setProp(res, prefix + SVC_OWNER,    v.getOwner());
        setProp(res, prefix + SVC_PARENTID, v.getParentId());

        vt = v.getServiceType();

        setProp(res, prefix + TSVC_CTIME,   vt.getCTime());
        setProp(res, prefix + TSVC_DESC,    vt.getDescription());
        setProp(res, prefix + TSVC_ID,      vt.getId());
        setProp(res, prefix + TSVC_MTIME,   vt.getMTime());
        setProp(res, prefix + TSVC_NAME,    vt.getName());
        setProp(res, prefix + TSVC_PLUGIN,  vt.getPlugin());

        return res;
    }

    /**
     * Get an entity's properties as well as the properties of all the
     * objects which it depends on.
     *
     * @param id The ID of the resource to get the properties for.  Must
     *           be a platform, server, or service.
     */
    public static Properties getProperties(AppdefEntityID id,
                                           AuthzSubjectValue subject)
    {
        boolean isPlat, isServer, isService;

        isPlat    = id.getType() == AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        isServer  = id.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVER;
        isService = id.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVICE;

        if(!isPlat && !isServer && !isService){
            throw new IllegalArgumentException("Invalid ID type: " + 
                                               id.getTypeName());
        }

        try {
            Properties props = new Properties();

            ApplicationManagerLocal appManager =
                ApplicationManagerUtil.getLocalHome().create();

            ResourceTree tree =
                appManager.getResourceTree(subject, 
                                     new AppdefEntityID[] {id},
                                     AppdefEntityConstants.RESTREE_TRAVERSE_UP);

            for (Iterator p = tree.getPlatformIterator(); p.hasNext(); ) {
                PlatformNode pNode = (PlatformNode)p.next();

                props.putAll(getProperties(pNode.getPlatform(), subject));

                for(Iterator s=pNode.getServerIterator(); s.hasNext(); ){
                    ServerNode sNode = (ServerNode)s.next();
                    ServerValue server;

                    server = sNode.getServer();
                    if(server.getEntityId().equals(id)){
                        props.putAll(getProperties(server, false, subject));
                    } 

                    props.putAll(getProperties(server, true, subject));

                    for(Iterator v=sNode.getServiceIterator(); v.hasNext(); ){
                        ServiceNode vNode = (ServiceNode)v.next();
                        ServiceValue service;

                        service = vNode.getService();
                        if(service.getEntityId().equals(id)){
                            props.putAll(getProperties(service, false, 
                                                       subject));
                        } 

                        props.putAll(getProperties(service, true, subject));
                    }
                }
            }
            
            return props;
        } catch (Exception e) {
            log.error("Unable to fetch entity properties: " + e.getMessage());

            // return empty property set
            return new Properties();
        }
    }

    private static CPropManagerLocal getCPMan(){
        try {
            return CPropManagerUtil.getLocalHome().create();
        } catch(Exception exc){
            throw new SystemException(exc);
        }
    }

    private EntityPropertyFetcher() {}
}
