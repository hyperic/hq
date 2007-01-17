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

import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.action.BaseValidatorForm;

/**
 *
 * The form object which captures the view name used for the indicator charts
 */
public class IndicatorViewsForm extends BaseValidatorForm {

    private String   action;
    private String   view;
    private String[] views;
    private String[] metric;
    private String   addMetric;
    private long     timeToken;
    private String   update;

    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public String getView() {
        return view;
    }
    public void setView(String view) {
        this.view = view;
    }
    public String[] getViews() {
        return views;
    }
    public void setViews(String[] views) {
        this.views = views;
    }
    public String[] getMetric() {
        return metric;
    }
    public void setMetric(String[] metric) {
        this.metric = metric;
    }
    public void setAddMetric(String addMetric) {
        this.addMetric = addMetric;
    }
    public String getAddMetric() {
        return addMetric;
    }
    public long getTimeToken() {
        return timeToken;
    }
    public void setTimeToken(long timeToken) {
        this.timeToken = timeToken;
    }
    public String getUpdate() {
        return update;
    }
    public void setUpdate(String update) {
        this.update = update;
    }

    protected void setDefaults() {
        this.action = null;
        this.view = null;
        this.metric = new String[0];
        this.addMetric = null;
        this.timeToken = System.currentTimeMillis();
    }
    
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        setDefaults();
        super.reset(mapping, request);
    }
}
