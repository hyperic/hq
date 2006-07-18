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
 * Removes a set of resources from the Resource Hub.
 * 
 *
 */
public class ResourceHubForm extends BaseValidatorForm {
    
    public static final Log log = LogFactory.getLog(ResourceHubForm.class.getName());

    //-------------------------------------static constant variables
    public static final String CHART_VIEW = "chart";
    public static final String LIST_VIEW  = "list";

    //-------------------------------------instance variables

    private Integer ff;
    private String ft;
    private List functions;
    private Integer g;
    private String[] resources;
    private List types;
    private String keywords;
    private String view;

    private ImageButtonBean group = null;
    
    //-------------------------------------constructors

    public ResourceHubForm() {
        super();
        setDefaults();
    }

    //-------------------------------------public methods

    public Integer getFf() {
        return ff;
    }

    public void setFf(Integer ff) {
        this.ff = ff;
    }

    public String getFt() {
        return ft;
    }

    public void setFt(String ft) {
        this.ft = ft;
    }

    public List getFunctions() {
        return functions;
    }

    public void setFunctions(List functions) {
        this.functions = functions;
    }

    public void addFunction(LabelValueBean b) {
        if (this.functions != null) {
            this.functions.add(b);
        }
    }

    public Integer getG() {
        return g;
    }

    public void setG(Integer g) {
        this.g = g;
    }

    public String[] getResources() {
        return resources;
    }
    
    public void setResources(String[] resources) {
        this.resources = resources;
    }
    
    public List getTypes() {
        return types;
    }

    public void setTypes(List types) {
        this.types = types;
    }

    public void addType(LabelValueBean b) {
        if (this.types != null) {
            this.types.add(b);
        }
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getView() {
        return view;
    }
    
    public void setView(String view) {
        this.view = view;
    }

    public ImageButtonBean getGroup() {
        return this.group;
    }

    public void setGroup(ImageButtonBean group) {
        this.group = group;
    }
    
    public boolean isGroupClicked() {
        return getGroup().isSelected();
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    private void setDefaults() {
        ff = null;
        ft = null;
        functions = new ArrayList();
        g = new Integer(-1);
        resources = new String[0];
        types = new ArrayList();
        view = null;
        group = new ImageButtonBean();
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" ff=")
         .append(ff)
         .append(" ft=")
         .append(ft)
         .append(" functions=")
         .append(functions)
         .append(" g=")
         .append(g)
         .append(" resources=")
         .append(resources)
         .append(" types=")
         .append(types)
         .append(" view=")
         .append(view)
         .append(" group=")
         .append(group);

        return s.toString();
    }
}
