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

import java.util.Collection;

import org.apache.tapestry.BaseComponent;
import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.form.validator.Validator;

public abstract class InputField extends BaseComponent {

    /**
     * Is this input field used for a password
     * @return
     * @see org.apache.tapestry.form.TextField#isHidden()
     */
    @Parameter(name = "isPassword", defaultValue = "false")
    public abstract Boolean getIsHidden();
    public abstract void setIsHidden(Boolean hidden);

    /**
     * 
     * @return
     */
    @Parameter(name = "isRequired", defaultValue = "false")
    public abstract Boolean getIsRequired();
    public abstract void setIsRequired(Boolean value);

    /**
     * 
     * @return
     */
    @Parameter(name = "disabled", defaultValue = "false")
    public abstract Boolean getIsDisabled();
    public abstract void setIsDisabled(Boolean value);

    /**
     * 
     * @param the value of the input field
     */
    @Parameter(name = "fieldValue", required = true)
    public abstract String getFieldValue();
    public abstract void setFieldValue(String value);

    /**
     *
     * @param the field title
     */
    @Parameter(name = "fieldLabel", required = true)
    public abstract String getFieldLabel();
    public abstract void setFieldLabel(String title);
    
    /**
     *
     * @param the field title
     */
    @Parameter(name = "fieldTitle", required = true)
    public abstract String getFieldTitle();
    public abstract void setFieldTitle(String title);

    /**
     *
     * @param the field title
     */
    @Parameter(name = "fieldHint", defaultValue = "literal:")
    public abstract String getFieldHint();
    public abstract void setFieldHint(String hint);

    /**
     * The short version of the help to be placed in
     * at title widget on mouseover of the help icon
     * @param the help title summary text
     */
    @Parameter(name = "helpTitle", defaultValue = "literal:")
    public abstract String getHelpTitle();
    public abstract void setHelpTitle(String title);
    
    /**
    * The id of the help topic to be displayed in a new
    * window on help icon click
    * @param the help topic id
    */
   @Parameter(name = "helpID")
   public abstract String getHelpID();
   public abstract void setHelpID(String id);
    
    /**
     * 
     * @return
     */
    @Parameter(name = "validators", required = false)
    public abstract Collection<Validator> getValidators();
    public abstract void setValidators(Collection<Validator> validators);
    

    public String getLabel() {
	if (getIsRequired()) {
	    return getFieldLabel() + "<span class='req'>*</span>";
	} else
	    return getFieldLabel();
    }
    
    public String getHintStyle() {
	if (getFieldHint() != "")
	    return "";
	else
	    return "display:none;";
    }
}
