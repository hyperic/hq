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

// -*- Mode: Java; indent-tabs-mode: nil; -*-

/*
 * MetricDisplayRangeForm.java
 *
 */

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.ImageButtonBean;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.ui.action.CalendarForm;

/**
 * Represents the controls on various pages that display metrics summaries.
 * 
 * 
 */
public class MetricDisplayRangeForm
    extends CalendarForm {

    public static final Integer ACTION_LASTN = new Integer(1);
    public static final Integer ACTION_DATE_RANGE = new Integer(2);

    // action radio button: "1" (last n) or "2" (date range)
    private Integer a;
    private String ctype;
    private String[] eid;
    private Integer rid;
    // simple fields: Last "5" (rn) "Hours" (ru)
    private Integer rn;
    private Integer ru;
    private Integer type;
    protected ImageButtonBean prevRange;
    protected ImageButtonBean nextRange;

    // -------------------------------------constructors

    public MetricDisplayRangeForm() {
        super();
        setDefaults();
    }

    // -------------------------------------public methods

    public Integer getA() {
        return a;
    }

    public void setA(Integer b) {
        a = b;
    }

    public String getCtype() {
        return ctype;
    }

    public void setCtype(String i) {
        ctype = i;
    }

    public String[] getEid() {
        return eid;
    }

    public void setEid(String[] s) {
        eid = s;
    }

    public Integer getRid() {
        if (rid != null) {
            return rid;
        }

        if (eid.length == 1) {
            return new AppdefEntityID(eid[0]).getId();
        }

        return null;
    }

    public void setRid(Integer i) {
        rid = i;
    }

    // range number
    public Integer getRn() {
        return rn;
    }

    public void setRn(Integer i) {
        rn = i;
    }

    // range unit
    public Integer getRu() {
        return ru;
    }

    public void setRu(Integer i) {
        ru = i;
    }

    public Integer getType() {
        if (type != null) {
            return type;
        }

        if (eid.length == 1) {
            return new Integer(new AppdefEntityID(eid[0]).getType());
        }

        return null;
    }

    public void setType(Integer i) {
        type = i;
    }

    /**
     * Always check the end date.
     */
    public boolean getWantEndDate() {
        return true;
    }

    public boolean isLastnSelected() {
        return a != null && a.intValue() == ACTION_LASTN.intValue();
    }

    public boolean isDateRangeSelected() {
        return a != null && a.intValue() == ACTION_DATE_RANGE.intValue();
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    protected boolean shouldValidateDateRange() {
        return isDateRangeSelected();
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" rid=").append(rid);
        s.append(" type=").append(type);
        if (eid != null)
            s.append(" eid=").append(Arrays.asList(eid));
        s.append(" ctype=").append(ctype);
        s.append(" a=").append(a);
        s.append(" rn=").append(rn);
        s.append(" ru=").append(ru);

        return s.toString();
    }

    // -------------------------------------private methods

    protected void setDefaults() {
        a = null;
        eid = new String[0];
        rn = null;
        ru = null;
        ctype = null;
        rid = null;
        type = null;
        prevRange = new ImageButtonBean();
        nextRange = new ImageButtonBean();
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);
        if (isLastnSelected()) {
            Integer lastN = this.getRn();
            if (lastN == null || lastN.intValue() == 0) {
                if (errors == null)
                    errors = new ActionErrors();

                errors.add("rn", new ActionMessage("resource.common.monitor.error.LastNInteger"));
            }
        }
        return errors;
    }

    public ImageButtonBean getPrevRange() {
        return prevRange;
    }

    public void setPrevRange(ImageButtonBean prevRange) {
        this.prevRange = prevRange;
    }

    public boolean isPrevRangeClicked() {
        return getPrevRange().isSelected();
    }

    public ImageButtonBean getNextRange() {
        return nextRange;
    }

    public void setNextRange(ImageButtonBean nextRange) {
        this.nextRange = nextRange;
    }

    public boolean isNextRangeClicked() {
        return getNextRange().isSelected();
    }
}
