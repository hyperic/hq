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
 * ResourceHubFormPrepareAction.java
 *
 */

package org.hyperic.hq.ui.action.resource.hub;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.hyperic.hq.ui.action.BaseValidatorFormNG;
import org.hyperic.hq.ui.util.ImageButtonBean;

/**
 * Form for fetching and working with the set of resources for the Resource Hub.
 * 
 * 
 */
public class ResourceHubFormNG  extends BaseValidatorFormNG {

    public static final Log log = LogFactory.getLog(ResourceHubFormNG.class.getName());

    // -------------------------------------static constant variables
    public static final String CHART_VIEW = "chart";
    public static final String LIST_VIEW = "list";

    // -------------------------------------query param strings
    public static final String ENTITY_TYPE_ID_PARAM = "ff";
    public static final String RESOURCE_TYPE_ID_PARAM = "ft";
    public static final String GROUP_TYPE_ID_PARAM = "g";
    public static final String GROUP_ID_PARAM = "fg";
    public static final String KEYWORDS_PARAM = "keywords";
    public static final String VIEW_PARAM = "view";
    public static final String ANY_FLAG_PARAM = "any";
    public static final String OWNER_FLAG_PARAM = "own";
    public static final String UNAVAILABLE_FLAG_PARAM = "unavail";

    // -------------------------------------instance variables

    private Integer _ff;
    private String _ft; // Resource type to filter by
    private Map<String, String> _functions;
    private Integer _g; // Group type
    private String[] _resources;
    private Map<String, String> _types;
    private String _keywords;
    private String _view;
    private String _fg; // The group to filter by
    private boolean _any; // Meet any criteria (vs. all)
    private boolean _own;
    private boolean _unavail;

    private ImageButtonBean _group = null;
    private ImageButtonBean _enableAlerts = null;
    private ImageButtonBean _disableAlerts = null;

   
    
    // -------------------------------------constructors

    public ResourceHubFormNG() {
        super();
        setDefaults();
    }

    // -------------------------------------public methods

    public Integer getFf() {
        return _ff;
    }

    public void setFf(Integer ff) {
        _ff = ff;
    }

    public String getFt() {
        return _ft;
    }

    public void setFt(String ft) {
        _ft = ft;
    }

    public Map getFunctions() {
        return _functions;
    }

    public void setFunctions(Map functions) {
        _functions = functions;
    }

    public void addFunction(String value, String  key) {
        if (_functions != null) {
            _functions.put(key,value);
        }
    }

    public Integer getG() {
        return _g;
    }

    public void setG(Integer g) {
        _g = g;
    }

    public String[] getResources() {
        return _resources;
    }

    public void setResources(String[] resources) {
        _resources = resources;
    }

    public Map getTypes() {
        return _types;
    }

    public void setTypes(Map types) {
        _types = types;
    }

    public void addType( String value,String key) {
        if (_types != null) {
            _types.put(key, value);
        }
    }

    public void setKeywords(String keywords) {
        _keywords = keywords;
    }

    public String getKeywords() {
        return _keywords;
    }

    public String getView() {
        return _view;
    }

    public void setView(String view) {
        _view = view;
    }

    public ImageButtonBean getGroup() {
        return _group;
    }

    public void setGroup(ImageButtonBean group) {
        _group = group;
    }

    public boolean isGroupClicked() {
        return getGroup().isSelected();
    }

    public ImageButtonBean getEnableAlerts() {
        return _enableAlerts;
    }

    public void setEnableAlerts(ImageButtonBean enableAlerts) {
        _enableAlerts = enableAlerts;
    }

    public ImageButtonBean getDisableAlerts() {
        return _disableAlerts;
    }

    public void setDisableAlerts(ImageButtonBean disableAlerts) {
        _disableAlerts = disableAlerts;
    }

    public String getFg() {
        return _fg;
    }

    public void setFg(String fg) {
        _fg = fg;
    }

    public boolean isAny() {
        return _any;
    }

    public void setAny(boolean any) {
        _any = any;
    }

    public boolean isOwn() {
        return _own;
    }

    public void setOwn(boolean own) {
        _own = own;
    }

    public boolean isUnavail() {
        return _unavail;
    }

    public void setUnavail(boolean unavail) {
        _unavail = unavail;
    }

    public void reset( ) {
        super.reset( );
        setDefaults();
    }

    private void setDefaults() {
        _ff = null;
        _ft = null;
        _functions = new LinkedHashMap<String, String>();
        _g = new Integer(-1);
        _resources = new String[0];
        _types = new LinkedHashMap<String, String>();
        _view = null;
        _group = new ImageButtonBean();
        _enableAlerts = new ImageButtonBean();
        _disableAlerts = new ImageButtonBean();
        _fg = null;
        _any = false;
        _own = false;
        _unavail = false;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" ff=").append(_ff).append(" ft=").append(_ft).append(" functions=").append(_functions).append(" g=")
            .append(_g).append(" resources=").append(_resources).append(" types=").append(_types).append(" view=")
            .append(_view).append(" group=").append(_group).append(" fg=").append(_fg).append(" any=").append(_any)
            .append(" own=").append(_own).append(" unavail=").append(_unavail);

        return s.toString();
    }
}
