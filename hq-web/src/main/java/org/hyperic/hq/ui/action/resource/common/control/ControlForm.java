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

package org.hyperic.hq.ui.action.resource.common.control;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.action.ScheduleForm;

/**
 * A subclass of <code>ScheduleForm</code> representing the <em>Control</em>
 * form data.
 * 
 * @see org.hyperic.hq.ui.action.ScheduleForm
 */
public class ControlForm
    extends ScheduleForm {

    private String controlAction;
    private String description;

    public String getControlAction() {
        return this.controlAction;
    }

    public void setControlAction(String a) {
        this.controlAction = a;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.description = null;
        this.controlAction = null;
        super.reset(mapping, request);
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errs = null;

        if (!shouldValidate(mapping, request)) {
            return null;
        }

        errs = super.validate(mapping, request);
        if (errs == null) {
            errs = new ActionErrors();
        }

        return errs;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("controlAction= ").append(controlAction);
        buf.append(" description= ").append(description);
        return super.toString() + buf.toString();
    }
}
