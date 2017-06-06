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

package org.hyperic.hq.ui.taglib.display;

import java.util.Locale;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.views.jsp.TagUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * Lookup in the context messages for common boolean items for
 * textual display or HTML tag building i.e.
 * "Yes" - "No"
 * "On" - "Off"
 * "/image/icon_on.gif" - "/image/icon_off.gif"
 * 
 *If you have a property return a Boolean, you can do
 * <display:booleandecorator bean="${service}" flag="${service.entryPoint}"
 * flagKey=" resource.application.inventory.service.isentrypoint"/>
 * In the properties file have these keys present:
 * resource.application.inventory.service.ispresent.true=YES
 * resource.application.inventory.service.ispresent.false=NO
 *
 * To Do:
 * Make this work with boolean's as well as Booleans so you can have some
 * more fun.  For instance, the "flag" might be a method that has indexed bits
 * i. e. in the example below getFlags() must might return java.util.BitSet
 * whose first bit we're interested in
 *
 * <display:booleandecorator bean="${bean}" flag="${flaggable.flags[0]}" 
 *      flagKey="application.properties.key.prefix"/>
 * In the properties file, have these keys present: 
 * application.properties.key.prefix.true=Yes 
 * application.properties.key.prefix.false=No
*/
public class BooleanDecorator extends BaseDecorator implements Cloneable {

    private static final Locale defaultLocale = Locale.getDefault();
    private static Log log = 
        LogFactory.getLog(BooleanDecorator.class.getName());
    
    private String flagKey;


    // tag attribute setters

    /**
     * Sets the message prefix that respresents a boolean result
     * @param theFlagKey a String that will have "true" or "false" appended to
     * it to look up in the application properties
     */
    public void setFlagKey(String theFlagKey) {
        flagKey = theFlagKey;
    }

    // our ColumnDecorator

    /* (non-Javadoc)
     * @see org.apache.taglibs.display.ColumnDecorator#decorate(java.lang.Object)
     */
    public String decorate(Object columnValue) {
        Boolean b = Boolean.FALSE;
        String msg = "";
        try {
            b = (Boolean)columnValue;
            String key = flagKey + '.' + b.toString();
            msg = RequestUtils.message( key);
        } catch (ClassCastException cce) {
            log.debug("class cast exception: ", cce);
        } 
        if (msg.length() == 0) {
            msg = (b.booleanValue()) ? "Yes" : "No";
        }
        return msg;            
    }
 
    public int doStartTag() throws JspTagException {
        ColumnTag ancestorTag =
            (ColumnTag)TagSupport.findAncestorWithClass(this, ColumnTag.class);

        if (ancestorTag == null) {
            throw new JspTagException(
                "A BooleanDecorator must be used within a ColumnTag.");
        }
        
        // You have to make a clone, otherwise, if there are more than one
        // boolean decorator in this table, then we'll end up with only one
        // boolean decorator object
        BooleanDecorator clone;
        try {
            clone = (BooleanDecorator) this.clone();
        } catch (CloneNotSupportedException e) {
            // Then just use this
            clone = this;
        }

        ancestorTag.setDecorator(clone);
        return SKIP_BODY;
    }

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    public void release() {
        super.release();
        flagKey = null;
    }
}
