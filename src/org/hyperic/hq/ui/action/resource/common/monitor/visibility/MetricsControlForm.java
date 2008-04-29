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

// -*- Mode: Java; indent-tabs-mode: nil; -*-

/*
 * MetricsControlForm.java
 *
 */

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.ui.Constants;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ImageButtonBean;
import org.apache.struts.util.LabelValueBean;

/**
 * Represents the common set of controls on various pages that display
 * metrics.
 * 
 *
 */
public class MetricsControlForm extends MetricDisplayRangeForm {

    private static int[] RN_OPTS = { 4, 8, 12, 24, 30, 48, 60, 90, 120 };

    //-------------------------------------instance variables

    // switches to advanced metric display range
    private ImageButtonBean advanced;
    // links to metric display range edit page
    private ImageButtonBean editRange;
    // changes (simple) metric display range
    private ImageButtonBean range;
    private Boolean readOnly;
    // range display: begin and end times
    private Long rb;
    private Long re;
    // switches to simple metric display range
    private ImageButtonBean simple;
    public MetricsControlForm() {
        super();
        setDefaults();
    }

    //-------------------------------------public methods

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append(" rid=").append(this.getRid());
        s.append(" type=").append(this.getType());
        s.append(" eid=").append(Arrays.asList(this.getEid()));
        s.append(" ctype=").append(this.getCtype());
        s.append(" advanced=").append(advanced);
        s.append(" editRange=").append(editRange);
        s.append(" range=").append(range);
        s.append(" readOnly=").append(readOnly);
        s.append(" rb=").append(rb);
        s.append(" re=").append(re);
        s.append(" rn=").append(this.getRn());
        s.append(" ru=").append(this.getRu());
        s.append(" simple=").append(simple);
        return s.toString();
    }


    //-------------------------------------public accessors

    public ImageButtonBean getAdvanced() {
        return advanced;
    }

    public void setAdvanced(ImageButtonBean b) {
        advanced = b;
    }

    public ImageButtonBean getEditRange() {
        return editRange;
    }

    public void setEditRange(ImageButtonBean b) {
        editRange = b;
    }

    public ImageButtonBean getRange() {
        return range;
    }

    public void setRange(ImageButtonBean b) {
        range = b;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean b) {
        readOnly = b;
    }

    // range begin
    public Long getRb() {
        return rb;
    }

    public void setRb(Long l) {
        rb = l;
    }

    // range end
    public Long getRe() {
        return re;
    }

    public void setRe(Long l) {
        re = l;
    }

    public ImageButtonBean getSimple() {
        return simple;
    }

    public void setSimple(ImageButtonBean b) {
        simple = b;
    }

    public boolean isAdvancedClicked() {
        return getAdvanced().isSelected();
    }

    public boolean isEditRangeClicked() {
        return getEditRange().isSelected();
    }

    public boolean isRangeClicked() {
        return getRange().isSelected();
    }

    public boolean isSimpleClicked() {
        return getSimple().isSelected();
    }

    public boolean isAnythingClicked() {
        return isAdvancedClicked() || isEditRangeClicked() ||
            isRangeClicked() || isSimpleClicked();
    }

    public Date getRbDate() {
        if (getRb() == null) {
            return null;
        }
        return new Date(getRb().longValue());
    }

    public Date getReDate() {
        if (getRe() == null) {
            return null;
        }
        return new Date(getRe().longValue());
    }

    public List getRnMenu() {
        List items = new ArrayList();

        // if no rn is selected, don't bother checking if we need to
        // put it in the menu
        boolean found = getRn() == null;
        String v = null;

        for (int i = 0; i < RN_OPTS.length; i++) {

            if (!found) {
                if (getRn().intValue() == RN_OPTS[i]) {
                    // the selected rn is one of the preset options
                    found = true;
                } else if (getRn().intValue() < RN_OPTS[i]) {
                    // the selected rn is between two of the preset
                    // options
                    v = getRn().toString();
                    items.add(new LabelValueBean(v, v));
                    found = true;
                }
            }

            v = new Integer(RN_OPTS[i]).toString();
            items.add(new LabelValueBean(v, v));
        }

        // one final check to see if the selected rn is bigger than
        // any of the preset options
        if (!found && getRn() != null) {
            v = getRn().toString();
            items.add(new LabelValueBean(v, v));
        }

        return items;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    public AppdefEntityID getEntityId() {
        if (getEid().length > 1) {
            // multiparent autogroup; the jsp should never allow us to perform
            // an operation on a single parent, so we leave entityId null to 
            // provoke an NPE and piss off the guy writing the jsp
            return null;
        }
        else if (getEid().length == 1) {
            return new AppdefEntityID(getEid()[0]);
        }
        else {
            return new AppdefEntityID(getType().intValue(), getRid());
        }
    }

    public Map getForwardParams() {
        HashMap forwardParams = null;

        if (this.getEid().length > 1) {
            forwardParams = new HashMap(1);
            forwardParams.put(Constants.ENTITY_ID_PARAM, this.getEid());
        }
        else if (this.getEid().length == 1) {
            forwardParams = new HashMap(1);
            forwardParams.put(Constants.ENTITY_ID_PARAM, this.getEid()[0]);
        }
        else {
            forwardParams = new HashMap(2);
            forwardParams.put(Constants.RESOURCE_PARAM, this.getRid());
            forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, this.getType());
        }

        if (this.getCtype() != null) {
            forwardParams.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM,
                              this.getCtype());
        }

        return forwardParams;
    }


    //-------------------------------------private methods    

    protected void setDefaults() {
        super.setDefaults();
        advanced = new ImageButtonBean();
        editRange = new ImageButtonBean();
        range = new ImageButtonBean();
        rb = null;
        re = null;
        readOnly = null;
        simple = new ImageButtonBean();
    }

    protected boolean shouldValidate(ActionMapping mapping,
                                     HttpServletRequest request) {
        if (super.shouldValidate(mapping, request))
            return true;
        
        return isAdvancedClicked() && mapping.getInput() != null;
    }
}
