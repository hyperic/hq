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

package org.hyperic.hq.ui.action;

import org.apache.struts.action.ActionMapping;

/**
 * An <code>ActionMapping</code> subclass that adds fields for participating in
 * a workflow struts-config.xml
 * 
 * To use: <CODE>        
 *  <set-property property="workflow" value="server/EditGeneralProperties"/>
 * </CODE>
 * 
 * The value is used as a key into a HashMap of Queues of URLs to participate in
 * a workflow.
 */
public class BaseActionMapping
    extends ActionMapping {

    /**
     * This field allows an ActionMapping to participate in a workflow, which is
     * a Stack of returnUrls that is stored in the session and retrieved for
     * building forwards for that workflow.
     */
    private String workflow = null;

    /**
     * Flag indicating whether or not this is the first action in a workflow. If
     * true, this resets the workflow.
     */
    private Boolean start = Boolean.TRUE;

    private String title;

    public BaseActionMapping() {
        super();
    }

    public String getWorkflow() {
        return this.workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public Boolean getIsFirst() {
        return this.start;
    }

    public void setIsFirst(Boolean start) {
        this.start = start;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        buf.append("];BaseActionMapping[");
        buf.append("workflow=").append(workflow).append(",");
        buf.append("start=").append(start).append(",");
        buf.append("title=").append(title).append(",");
        buf.append("]");
        return buf.toString();
    }
}
