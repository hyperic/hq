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

package org.hyperic.hq.ui.action.resource.common.inventory;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.ui.action.BaseValidatorForm;

import org.apache.struts.action.ActionMapping;

public class ChangeResourceOwnerForm
    extends BaseValidatorForm {

    // -------------------------------------instance variables

    private Integer owner;
    private Integer rid;
    private Integer type;

    // -------------------------------------constructors

    public ChangeResourceOwnerForm() {
        super();
    }

    // -------------------------------------public methods

    public Integer getRid() {
        return this.rid;
    }

    public void setRid(Integer i) {
        this.rid = i;
    }

    public Integer getO() {
        return this.owner;
    }

    public Integer getOwner() {
        return getO();
    }

    public void setO(Integer owner) {
        this.owner = owner;
    }

    public void setOwner(Integer owner) {
        setO(owner);
    }

    public Integer getType() {
        return this.type;
    }

    public void setType(Integer i) {
        this.type = i;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        this.rid = null;
        this.owner = null;
        this.type = null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append("rid=" + rid + " ");
        s.append("owner=" + owner);
        s.append("type=" + type + " ");
        return s.toString();
    }
}
