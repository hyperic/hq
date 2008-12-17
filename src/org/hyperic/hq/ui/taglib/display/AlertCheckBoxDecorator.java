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

package org.hyperic.hq.ui.taglib.display;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * This class is a two in one decorator/tag for use within the display:table
 * tag, it is a ColumnDecorator tag that that creates a column of checkboxes.
 */
public class AlertCheckBoxDecorator extends CheckBoxDecorator
{

    //----------------------------------------------------static variables

    private static Log log =
        LogFactory.getLog(AlertCheckBoxDecorator.class.getName());

    //----------------------------------------------------instance variables

    /** The class property of the checkbox.
     */
    private String fixable = null;

    /** The name="foo" property of the checkbox.
     */
    private String acknowledgeable = null;


    // ctors
    public AlertCheckBoxDecorator() {
    }

    // accessors 
    public String getFixable() {
        return this.fixable;
    }
    
    public void setFixable(String b) {
        this.fixable = b;
    }
    
    public String getAcknowledgeable() {
        return this.acknowledgeable;
    }
    
    public void setAcknowledgeable(String b) {
        this.acknowledgeable = b;
    }
    

    public String decorate(Object obj) {        
        Boolean isFixable;
        Boolean isAcknowledgeable;
        
        try {
            isFixable = (Boolean) evalAttr("fixable",
                                           this.fixable,
                                           Boolean.class);
        }
        catch (NullAttributeException ne) {
            log.debug("bean " + this.fixable + " not found");
            return "";
        }
        catch (JspException je) {
            log.debug("can't evaluate name [" + this.fixable + "]: ", je);
            return "";
        }

        try {
            isAcknowledgeable = (Boolean) evalAttr("acknowledgeable", 
                                                   this.acknowledgeable,
                                                   Boolean.class);            
        }
        catch (NullAttributeException ne) {
            log.debug("bean " + this.acknowledgeable + " not found");
            return "";
        }
        catch (JspException je) {
            log.debug("can't evaluate name [" + this.acknowledgeable + "]: ", je);
            return "";
        }

        if (isAcknowledgeable.booleanValue()) {
            setStyleClass("ackableAlert");
        } else if (isFixable.booleanValue()){
            setStyleClass("fixableAlert");
        } else {
            return "";
        }
        
        return super.decorate(obj);
    }
    
    public void release() {
        super.release();       
        fixable = null;
        acknowledgeable = null;
    }
    
    private Object evalAttr(String name, String value, Class type)
        throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("alertcheckboxdecorator", name, value,
                                          type, this, context);
    }
}
