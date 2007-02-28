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
 * ResourceHubFormPrepareAction.java
 *
 */

package org.hyperic.hq.ui.action.resource.hub;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.ui.action.BaseValidatorForm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ImageButtonBean;
import org.apache.struts.util.LabelValueBean;

/**
 * Form for fetching and working with the set of resources for the Resource Hub.
 * 
 *
 */
public class ResourceHubForm extends BaseValidatorForm {
    
    public static final Log log =
        LogFactory.getLog(ResourceHubForm.class.getName());

    //-------------------------------------static constant variables
    public static final String CHART_VIEW = "chart";
    public static final String LIST_VIEW  = "list";

    //-------------------------------------instance variables

    private Integer _ff;
    private String _ft;      // Resource type to filter by
    private List _functions;
    private Integer _g;      // Group type
    private String[] _resources;
    private List _types;
    private String _keywords;
    private String _view;
    private String _fg;      // The group to filter by

    private ImageButtonBean _group = null;
    
    //-------------------------------------constructors

    public ResourceHubForm() {
        super();
        setDefaults();
    }

    //-------------------------------------public methods

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

    public List getFunctions() {
        return _functions;
    }

    public void setFunctions(List functions) {
        _functions = functions;
    }

    public void addFunction(LabelValueBean b) {
        if (_functions != null) {
            _functions.add(b);
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
    
    public List getTypes() {
        return _types;
    }

    public void setTypes(List types) {
        _types = types;
    }

    public void addType(LabelValueBean b) {
        if (_types != null) {
            _types.add(b);
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

    public String getFg() {
        return _fg;
    }

    public void setFg(String fg) {
        _fg = fg;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    private void setDefaults() {
        _ff = null;
        _ft = null;
        _functions = new ArrayList();
        _g = new Integer(-1);
        _resources = new String[0];
        _types = new ArrayList();
        _view = null;
        _group = new ImageButtonBean();
        _fg = null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" ff=")
         .append(_ff)
         .append(" ft=")
         .append(_ft)
         .append(" functions=")
         .append(_functions)
         .append(" g=")
         .append(_g)
         .append(" resources=")
         .append(_resources)
         .append(" types=")
         .append(_types)
         .append(" view=")
         .append(_view)
         .append(" group=")
         .append(_group)
         .append(" fg=")
         .append(_fg);

        return s.toString();
    }
}
