/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


/**
 * A DependencyTree has child DependencyNodes (who in turn my have children
 * of their own), structurally, it's a list of lists.
 */
public class DependencyTree implements Serializable {
    
    // all the entry point nodes
    private List _nodes;
    // the application to which this tree applies
    private ApplicationValue _appValue;

    // an empty tree
    public DependencyTree(ApplicationValue appValue) {
        _appValue = appValue;
        _nodes = new ArrayList();
    }

    // get the app value
    public ApplicationValue getApplication() {
        return _appValue;
    }

    public Integer getAppPK() {
        return getApplication().getId();
    }

    /**
     * Adds a top level DependencyNode
     */
    private void addNode(DependencyNode aNode) {
        _nodes.add(aNode);
    }

    /**
     * Returns a List of top level DependencyNodes
     */
    public List getNodes() {
        return _nodes;
    }

    /**
     * Adds an AppServiceValue <code>depSvc</code> as a child of another
     * AppServiceValue <code>appSvc</code>. If the <code>appSvc</code> does
     * not exist, it will be created as a toplevel node.
     */
    public void addNode(AppServiceValue appSvc, AppServiceValue depSvc) {
        // find the item in the list which matches the source appservice
        DependencyNode aNode = null;
        try {
            aNode = findAppService(appSvc);
        } catch (NoSuchElementException e) {
            // looks like we need to create the node and add it to the list
            aNode = new DependencyNode(appSvc, new ArrayList());
            // only add the node if its a new one
            addNode(aNode);
        }
        aNode.addChild(depSvc);
    }

    /**
     * Add a node with no dependents
     */
    public void addNode(AppServiceValue appSvc) {
        try {
            // find it first
            findAppService(appSvc);
        } catch (NoSuchElementException e) {
            // not there, add it
            DependencyNode aNode = new DependencyNode(appSvc, null);
            addNode(aNode);
        }
    }

    /**
     * Returns a top level DependencyNode
     */
    public DependencyNode findAppService(AppServiceValue aService)
        throws NoSuchElementException {
        // iterate over the nodes in the list, match by app service, 
        // since a single app service should exist only once at the top level
        for (int i = 0; i < _nodes.size(); i++) {
            DependencyNode aNode = (DependencyNode) _nodes.get(i);
            if (aNode.getAppService().equals(aService)) {
                return aNode;
            }
        }
        throw new NoSuchElementException(aService + " was not found in tree");
    }

    public DependencyNode findAppService(AppdefResourceValue aResource)
        throws NoSuchElementException {
        // iterate over nodes in the list
        for (int i = 0; i < _nodes.size(); i++) {
            DependencyNode aNode = (DependencyNode) _nodes.get(i);
            if (aNode.getEntityId().equals(aResource.getEntityId())) {
                return aNode;
            }
        }
        throw new NoSuchElementException(aResource + " was not found in tree");
    }

    public List getDependencies(AppServiceValue aService) throws
        NoSuchElementException {
        DependencyNode aNode = findAppService(aService);
        return aNode.getChildren();
    }
    
    /**
     * Check if the app service represents an entry point within this
     * tree. An entry point is defined as an AppService which is not a 
     * dependee in any point in the tree
     * @return int
     */
    public boolean isEntryPoint(AppServiceValue aService)
        throws NoSuchElementException {
        boolean isEntry = true;
        for(int i = 0; i < getTopLevelCount(); i++) {
            DependencyNode aNode = (DependencyNode)getNodes().get(i);
            // if any of the children include the appservice in question,
            // we know its not an entry point
            if(aNode.getChildren().contains(aService)) {
                isEntry = false;        
            }
        }
        return isEntry;
    }

    public int getTopLevelCount() {
        return _nodes.size();
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(DependencyTree.class.getName());
        sb.append("[");
        sb.append(" parentAppId=").append(_appValue.getId());
        sb.append(" parentAppName=").append(_appValue.getName());
        sb.append(" nodes={\n");
        for (Iterator iter = _nodes.iterator(); iter.hasNext();) {
            DependencyNode node = (DependencyNode) iter.next();
            sb.append(node.toString());
        }
        sb.append("\n}]\n");
        
        return sb.toString();
    }

    /* 
     * If <code>o</code> is not a DependencyTree, returns false.
     * If <code>o</code>'s set of nodes are equal, returns true. 
     */
    public boolean equals(Object o) {
        if (! (o instanceof DependencyTree))
            return false;
        DependencyTree aTree= (DependencyTree)o;
        if (aTree.comparable().equals(comparable()))
            return true;
        return false;
    }

    private HashSet comparable() {
        return new HashSet(_nodes);
    }

    public static boolean nodeHasChild(DependencyNode node, Integer appSvcId) {
        for (Iterator iter = node.getChildren().iterator(); iter.hasNext();) {
            AppServiceValue appSvc = (AppServiceValue) iter.next();
            if (appSvc.getId().equals(appSvcId))
                return true;            
        }
        return false;
    }

    public static DependencyNode findAppServiceById(DependencyTree tree,
                                                    Integer appSvcId) {
        List children = tree.getNodes();
        DependencyNode returnNode = null;
        for (Iterator iter = children.iterator(); iter.hasNext();) {
            DependencyNode node = (DependencyNode) iter.next();
            if (node.getAppService().getId().equals(appSvcId)) {
                returnNode = node;
                break;
            }
        }
        return returnNode;
    }

    public static DependencyNode findServiceById(DependencyTree tree,
                                                 Integer serviceId) {
        List children = tree.getNodes();
        DependencyNode returnNode = null;
        for (Iterator iter = children.iterator(); iter.hasNext();) {
            DependencyNode node = (DependencyNode) iter.next();
            if (node.getService().getId().equals(serviceId)) {
                returnNode = node;
                break;
            }
        }
        return returnNode;
    }

    public static Map mapServices(List services) {
        Map serviceMap = new LinkedHashMap();
        for (Iterator serviceIter = services.iterator(); serviceIter.hasNext();)
        {
            AppdefResourceValue service =
                (AppdefResourceValue) serviceIter.next();
            serviceMap.put(service.getEntityId(), service);                
        }
        return serviceMap;  
    }

    /**
     * Method findDependees.
     * 
     * Returns a list of application services that on a given dependency
     * element
     * 
     * @param services
     * @return List a list of AppServiceNodeBeans
     */
    public static List findDependees(DependencyTree tree, 
                                     DependencyNode appSvcNode, 
                                     List services) {
        List returnList = new ArrayList();
        Map serviceMap = DependencyTree.mapServices(services);
        for (Iterator iter = appSvcNode.getChildren().iterator();
             iter.hasNext();) {
            AppServiceValue appSvc = (AppServiceValue) iter.next();
            AppdefEntityID contained ;
            if(appSvc.getIsCluster())
                contained = AppdefEntityID.newGroupID(appSvc.getServiceCluster()
                                                      .getGroupId().intValue());
            else
                contained = appSvc.getService().getEntityId();

             if (serviceMap.containsKey(contained)) {
                DependencyNode node =
                    DependencyTree.findAppServiceById(tree, appSvc.getId());
                returnList.add(new AppServiceNodeBean(
                    (AppdefResourceValue) serviceMap.get(contained), node));
             }                    
        }
        return returnList;        
    }

    /**
     * Method findDependers.
     * 
     * Performs a  search for DependencyNodes that have the current
     * one (identified by the appSvcId) among its children i.e. our current 
     * one depends on all of the ones returned
     * 
     * @param tree the DependencyTree
     * @param appSvcId the current ApplicationService
     * @param services a list of ServiceValues associated with the application
     * @return List a list of AppServiceNodeBeans
     */
    public static List findDependers(DependencyTree tree,   
                                     Integer appSvcId, 
                                     List services) {
        List returnList = new ArrayList();
        Map serviceMap = DependencyTree.mapServices(services);
        for (Iterator iter = tree.getNodes().iterator(); iter.hasNext();) {
            DependencyNode node = (DependencyNode) iter.next();
            // look at who depends on this node by cycling through the
            // list of AppServiceValues and checking for appSvcId
            for (Iterator i = node.getChildren().iterator(); i.hasNext();) {
                AppServiceValue appSvc = (AppServiceValue) i.next();
                if (appSvc.getId().equals(appSvcId)) {
                    returnList.add(new AppServiceNodeBean(
                        (AppdefResourceValue)serviceMap.get(
                            node.getEntityId()),node));
                }
            }
        }        
        return returnList;
    }

    /**
     * 
     * The criteria for qualifying AppServices as potential dependees for a 
     * given node are
     * the following:
     * <ol>
     * <li>The candidate AppService is in the DependencyTree already, 
     * i.e. it has a DependencyNode already.
     * <li>A candidate's node cannot be the given node, self dependencies 
     * are disallowed.
     * <li>The candidate AppService is not already a dependee, defining a 
     * dependency twice is disallowed.
     * <i>It may depend on indirectly already i.e. if something it already 
     * depends on currently depends on another node, that does not disqualify 
     * the other node.
     * <li>A candidate's node cannot have the given node as a depender 
     * either directly or indirectly, circular dependencies are disallowed.
     * </ol>
     * 
     * @param tree
     * @param currentNode
     * @param services
     * @return
     */
    public static List findPotentialDependees(DependencyTree tree,
                                              DependencyNode currentNode,
                                              List services) {
        // id of the current node's AppService
        Integer appSvcId = currentNode.getAppService().getId();
        // any DependencyNodes left in this list when we're done disqualify
        // candidates is what we'll return
        List candidateNodes = new ArrayList(tree.getNodes());
    
        // disqualify self
        for (Iterator iter = candidateNodes.iterator(); iter.hasNext();) {
            DependencyNode aNode= (DependencyNode) iter.next();
            if (aNode.getAppService().getId().equals(appSvcId)) {
                iter.remove();
            }            
        }
        ArrayList filtered = new ArrayList();
        // disqualify existing dependencies
        for (Iterator iter = candidateNodes.iterator(); iter.hasNext();) {
            DependencyNode aNode= (DependencyNode) iter.next();
            Integer anAppSvcId = aNode.getAppService().getId();
            for (Iterator appSvcIter = currentNode.getChildren().iterator();
                 appSvcIter.hasNext();) {
                AppServiceValue currentDependeeAppSvc =
                    (AppServiceValue) appSvcIter.next();
                if (currentDependeeAppSvc.getId().equals(anAppSvcId))  {
                    //iter.remove();
                    filtered.add(aNode);
                }
            }
        }
        candidateNodes.removeAll(filtered);
        filtered.clear();
        // build a map of the nodes keyed on the AppService's id
        Map nodeMap = new LinkedHashMap();
        for (Iterator iter = candidateNodes.iterator(); iter.hasNext();) {
            DependencyNode aNode= (DependencyNode) iter.next();
            nodeMap.put(aNode.getAppService().getId(), aNode);
        }               
        // disqualify current dependers by doing a two level check
        for (Iterator iter = candidateNodes.iterator(); iter.hasNext();) {
            DependencyNode aNode= (DependencyNode) iter.next();
            // see if the node directly or indirectly depends on the currentNode
            for (Iterator appSvcIter = aNode.getChildren().iterator();
                 appSvcIter.hasNext();) {
                AppServiceValue anAppSvc = (AppServiceValue) appSvcIter.next();
                // see if the node directly depends on the currentNode
                if (appSvcId.equals(anAppSvc.getId()))  {
                    // disqualify the candidate for directly depending on the
                    // currentNode
                    filtered.add(aNode);
                    //iter.remove();
                    break;
                }
                // now see if the node indirectly depends on the currentNode by
                // checking it's children (with this data structure, we only
                // need to check two levels instead of a fully recursive check)
                for (Iterator childAppSvcIter = aNode.getChildren().iterator();
                     childAppSvcIter .hasNext();) {
                    AppServiceValue childAppSvc =
                        (AppServiceValue) childAppSvcIter .next();
                    // see if the node directly depends on the currentNode
                    if (appSvcId.equals(childAppSvc.getId()))  {
                        filtered.add(aNode);
                        //iter.remove();
                        break;
                    }
                }
            }            
        }         
        candidateNodes.removeAll(filtered);
        filtered.clear();
        return candidateNodes;       
    }
}
