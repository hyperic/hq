/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004 - 2008], Hyperic, Inc.
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
package org.hyperic.ui.tapestry.components.form;

import org.apache.tapestry.BaseComponent;
import org.apache.tapestry.IActionListener;
import org.apache.tapestry.annotations.Parameter;

public abstract class Button extends BaseComponent {

    public static final String BUTTON_GREEN = "Green";
    public static final String BUTTON_BLUE = "Blue";
    public static final String BUTTON_GREY = "Gray";
    public static final String BUTTON_DISABLED = "Disabled";
    
    public static final String SUBMIT_TYPE_SUBMIT = "submit";
    public static final String SUBMIT_TYPE_CANCEL = "cancel";
    public static final String SUBMITE_TYPE_RESET = "reset";
    
    @Parameter(name = "listener")
    public abstract IActionListener getListener();
    public abstract void setListener(IActionListener listener);
    
    @Parameter(name = "action")
    public abstract IActionListener getAction();
    public abstract void setAction(IActionListener action);
    
    @Parameter(name = "type", 
	       defaultValue = "@org.hyperic.ui.tapestry.components.form.Button@BUTTON_GREEN")
    public abstract String getType();
    public abstract void setType(String type);
    
    @Parameter(name = "disabled", defaultValue = "false")
    public abstract Boolean getDisabled();
    public abstract void setDisabled(Boolean disabled);
    
    @Parameter(name = "async", defaultValue = "false")
    public abstract boolean getAsync();
    public abstract void setAsync(boolean async);
    
    @Parameter(name = "json", defaultValue = "false")
    public abstract boolean getJson();
    public abstract void setJson(boolean json);

    @Parameter(name = "updateComponents")
    public abstract String[] getUpdateComponents();
    public abstract void setUpdateComponents(String[] updateComponents);
    
    @Parameter(name = "parameters")
    public abstract Object getParameters();
    public abstract void setParameters(Object parameters);
    
    @Parameter(name = "submitType", 
	       defaultValue = "@org.hyperic.ui.tapestry.components.form.Button@SUBMIT_TYPE_SUBMIT")
    public abstract String getSubmitType(); 
    public abstract void setSubmitType(String submitType);
    
    @Parameter(name = "label", required = true)
    public abstract String getLabel();
    public abstract void setLabel(String label);
    
}
