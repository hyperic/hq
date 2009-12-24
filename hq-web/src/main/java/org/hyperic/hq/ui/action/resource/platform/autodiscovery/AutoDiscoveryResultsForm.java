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

/*
 * Created on Mar 13, 2003
 *
 */
package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.ui.action.resource.RemoveResourceForm;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.struts.action.ActionMapping;

/**
 * Form represents the auto-discovery results form
 * 
 * 
 */
public class AutoDiscoveryResultsForm
    extends RemoveResourceForm {

    /**
     * filter on server type for discovered servers
     */
    private Integer serverTypeFilter;

    /**
     * filter on ignored/New&modified discovered servers
     */
    private Integer stdStatusFilter;

    /**
     * filter on ignored/New&modified discovered ips
     */
    private Integer ipsStatusFilter;

    /**
     * ai platform id
     */
    private Integer aiPid;

    /**
     * ai resource id (platform properties, ai ips, ai servers)
     */
    private Integer aiRid;

    private Integer aiAction;

    private List newModifiedActionOptions = null;
    private List removedActionOptions = null;
    private List unchangedActionOptions = null;

    private List serverTypeFilterList = null;

    /**
     * 
     */
    public AutoDiscoveryResultsForm() {
        super();
    }

    /**
     * @return Integer
     */
    public Integer getServerTypeFilter() {
        return serverTypeFilter;
    }

    /**
     * @return Integer
     */
    public Integer getStdStatusFilter() {
        return stdStatusFilter;
    }

    /**
     * Sets the filterStd.
     * @param filterStd The filterStd to set
     */
    public void setServerTypeFilter(Integer filterStd) {
        this.serverTypeFilter = filterStd;
    }

    /**
     * Sets the ignoreStd.
     * @param ignoreStd The ignoreStd to set
     */
    public void setStdStatusFilter(Integer ignoreStd) {
        this.stdStatusFilter = ignoreStd;
    }

    /**
     * @return Integer
     */
    public Integer getIpsStatusFilter() {
        return ipsStatusFilter;
    }

    /**
     * Sets the ignoreIps.
     * @param ignoreIps The ignoreIps to set
     */
    public void setIpsStatusFilter(Integer ignoreIps) {
        this.ipsStatusFilter = ignoreIps;
    }

    /**
     * @return Integer id of the ai resource
     */
    public Integer getAiRid() {
        return aiRid;
    }

    /**
     * Sets the aiRid.
     * @param aiRid The aiRid to set
     */
    public void setAiRid(Integer aiRid) {
        this.aiRid = aiRid;
    }

    /**
     * @return
     */
    public Integer getAiAction() {
        return aiAction;
    }

    /**
     * @param integer
     */
    public void setAiAction(Integer integer) {
        aiAction = integer;
    }

    /**
     * @return
     */
    public List getNewModifiedActionOptions() {
        return newModifiedActionOptions;
    }

    /**
     * @return
     */
    public List getRemovedActionOptions() {
        return removedActionOptions;
    }

    /**
     * @return
     */
    public List getUnchangedActionOptions() {
        return unchangedActionOptions;
    }

    /**
     * build the ai action options
     */
    public void buildActionOptions(HttpServletRequest request) {
        newModifiedActionOptions = buildAINewModifiedCommandOptions(request);
        removedActionOptions = buildAIRemovedActionOptions(request);
        unchangedActionOptions = buildAIUnchangedCommandOptions(request);
    }

    /**
     * build remove actions for AI resource
     * 
     * @return a list
     */
    public static List buildAIRemovedActionOptions(HttpServletRequest request) {

        List groupTypes = new ArrayList();

        HashMap map2 = new HashMap(2);
        map2.put("value", new Integer(AIQueueConstants.Q_DECISION_UNIGNORE).toString());
        map2.put("label", RequestUtils
            .message(request, "resource.autodiscovery.action.uninstalled.DeleteFromInventory"));
        groupTypes.add(map2);

        HashMap map1 = new HashMap(2);
        map1.put("value", new Integer(AIQueueConstants.Q_DECISION_IGNORE).toString());
        map1.put("label", RequestUtils.message(request, "resource.autodiscovery.action.uninstalled.KeepInInventrory"));
        groupTypes.add(map1);

        return groupTypes;
    }

    /**
     * build unchanged commands for AI resource
     * 
     * @return a list
     */
    public static List buildAIUnchangedCommandOptions(HttpServletRequest request) {

        List groupTypes = new ArrayList();

        HashMap map2 = new HashMap(2);
        map2.put("value", new Integer(AIQueueConstants.Q_DECISION_DEFER).toString());
        map2.put("label", RequestUtils.message(request, "resource.autodiscovery.action.unchanged.NoActions"));
        groupTypes.add(map2);

        return groupTypes;
    }

    /**
     * build new/modified commands for AI resource
     * 
     * @return a list
     */
    public static List buildAINewModifiedCommandOptions(HttpServletRequest request) {

        List groupTypes = new ArrayList();

        HashMap map2 = new HashMap(2);
        map2.put("value", new Integer(AIQueueConstants.Q_DECISION_UNIGNORE).toString());
        map2.put("label", RequestUtils.message(request, "resource.autodiscovery.action.new.ImportServer"));
        groupTypes.add(map2);

        HashMap map1 = new HashMap(2);
        map1.put("value", new Integer(AIQueueConstants.Q_DECISION_IGNORE).toString());
        map1.put("label", RequestUtils.message(request, "resource.autodiscovery.action.new.DoNotImport"));
        groupTypes.add(map1);

        return groupTypes;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
    }

    public String toString() {
        StringBuffer s = new StringBuffer();

        return s.toString();
    }

    /**
     * @return
     */
    public Integer getAiPid() {
        return aiPid;
    }

    /**
     * @param integer
     */
    public void setAiPid(Integer integer) {
        aiPid = integer;
    }

    /**
     * @return
     */
    public List getServerTypeFilterList() {
        return serverTypeFilterList;
    }

    /**
     * This method returns 0 if there are not server types set. No server types
     * are set when a newly auto-discovered platform has not been imported into
     * the appdef.
     * 
     * @return number of serverTypes
     */
    public Integer getServerTypeFilterListCount() {
        if (serverTypeFilterList == null)
            return new Integer(0);

        return new Integer(serverTypeFilterList.size());
    }

    /**
     * @param list
     */
    public void setServerTypeFilterList(AppdefResourceTypeValue[] serverTypes) {

        serverTypeFilterList = new ArrayList();
        CollectionUtils.addAll(serverTypeFilterList, serverTypes);
    }

}
