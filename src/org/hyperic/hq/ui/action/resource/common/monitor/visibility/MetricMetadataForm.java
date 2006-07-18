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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public class MetricMetadataForm extends ActionForm {
    private Integer m;           /* template id */
    private String ctype;        /* autogroup's child resource type, if any */
    private String eid;          /* AppdefEntityID */

    /**
     * @return String
     */
    public String getCtype() {
        return ctype;
    }

    /**
     * @return String
     */
    public String getEid() {
        return eid;
    }

    /**
     * @return Integer
     */
    public Integer getM() {
        return m;
    }

    /**
     * Sets the ctype.
     * @param ctype The ctype to set
     */
    public void setCtype(String ctype) {
        this.ctype = ctype;
    }

    /**
     * Sets the eid.
     * @param eid The eid to set
     */
    public void setEid(String eid) {
        this.eid = eid;
    }

    /**
     * Sets the m.
     * @param m The m to set
     */
    public void setM(Integer m) {
        this.m = m;
    }


    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        m = null;
        eid = null;
        ctype = null;
    }

}
