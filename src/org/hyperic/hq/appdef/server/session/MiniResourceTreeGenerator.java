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

package org.hyperic.hq.appdef.server.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.MiniResourceValue;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerUtil;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniPlatformNode;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniResourceTree;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniServerNode;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniServiceNode;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.SortAttribute;

/**
 *
 * An object that generates a tree of recently created resources based on
 * a creation time.  This method is specialized for the recently approved
 * resources portlet.  For a more generic tree generator, see the 
 * ResourceTreeGenerator.
 */
public class MiniResourceTreeGenerator {

    private PlatformManagerLocal    platMan;
    private ServerManagerLocal      serverMan;
    private ServiceManagerLocal     serviceMan;
    private AuthzSubjectValue      subject;

    private PageControl pc;

    MiniResourceTreeGenerator(AuthzSubjectValue subject){
        this.subject = subject;

        // Always get servers and services in order of creation.
        this.pc = new PageControl();
        this.pc.setSortattribute(SortAttribute.CTIME);
        this.pc.setSortorder(PageControl.SORT_DESC);

        try {
            this.platMan    = PlatformManagerUtil.getLocalHome().create();
            this.serverMan  = ServerManagerUtil.getLocalHome().create();
            this.serviceMan = ServiceManagerUtil.getLocalHome().create();
        } catch(NamingException exc){
            throw new SystemException("Unable to load system resources");
        } catch(CreateException exc){
            throw new SystemException("Unable to load system resources");
        }
    }

    /**
     * Generate a tree which includes the passed IDs.
     *
     * @param ids       An array of IDs to generate the tree from
     * @param traversal One of TRAVERSE_*
     *
     * @return a new MiniResourceTree
     */
    MiniResourceTree generate(AppdefEntityID[] ids, long ts)
        throws AppdefEntityNotFoundException
    {
        MiniResourceTree res;

        res = new MiniResourceTree();

        for(int i=0; i<ids.length; i++){
            Integer iID = ids[i].getId();

            switch(ids[i].getType()){
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                this.addFromPlatform(iID, ts, res);
                break;
            default:
                throw new IllegalArgumentException("MiniResourceTree " +
                                                   "generation can only " +
                                                   "be performed on " +
                                                   "platforms");
            } 
        }

        return res;
    }

    private void addFromPlatform(Integer id, long ts, MiniResourceTree tree)
        throws AppdefEntityNotFoundException
    {
        MiniResourceValue val;

        val = this.platMan.getMiniPlatformById(this.subject, id);

        this.traversePlatform(val, ts, tree);
    }

    private void traversePlatform(MiniResourceValue platform,
                                  long ts, MiniResourceTree tree)
    {
        MiniPlatformNode pNode;
        List servers;

        // First add the platform to the tree.  This makes an assumption
        // that we'll have at least one server or service that has been
        // created after the timestamp.  If not, the platform will be 
        // removed after we traverse the servers and services.
        pNode = tree.addPlatform(platform);

        try {
            servers = this.serverMan.
                getMiniServersByPlatform(this.subject,
                                         new Integer(platform.id),
                                         this.pc);
        } catch(AppdefEntityNotFoundException exc){
            throw new SystemException("Internal inconsistancy: " +
                                      "could not find servers for " +
                                      "platform '" + platform + "'");
        }

        for(Iterator i = servers.iterator(); i.hasNext(); ){
            this.traverseServer(pNode, (MiniResourceValue)i.next(), ts);
        }
    }
    
    private void traverseServer(MiniPlatformNode pNode, 
                                MiniResourceValue server, long ts)
    {
        List services =
            this.serviceMan.getMiniServicesByServer(this.subject,
                                                    new Integer(server.id),
                                                    ts, this.pc);
        
        // If there are no new services, or the server has not been created
        // after the timestamp, return.
        if (services.isEmpty() && server.ctime < ts)
            return;

        // Otherwise, add the server and it's services.

        MiniServerNode sNode = pNode.addServer(server);
        for(Iterator i = services.iterator(); i.hasNext(); ){
            sNode.addService((MiniResourceValue)i.next());
        }
    }
}
