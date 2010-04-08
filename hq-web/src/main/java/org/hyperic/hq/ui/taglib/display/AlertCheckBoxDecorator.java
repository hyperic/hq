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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.ui.beans.AlertBean;

/**
 * This class is a two in one decorator/tag for use within the display:table
 * tag, it is a ColumnDecorator tag that that creates a column of checkboxes.
 */
public class AlertCheckBoxDecorator extends CheckBoxDecorator {
	private static Log log = LogFactory.getLog(AlertCheckBoxDecorator.class.getName());

	/**
	 * The class property of the checkbox.
	 */
	private Boolean fixable = null;

	/**
	 * The name="foo" property of the checkbox.
	 */
	private Boolean acknowledgeable = null;

	public Boolean getFixable() {
		return this.fixable;
	}

	public void setFixable(Boolean b) {
		this.fixable = b;
	}

	public Boolean getAcknowledgeable() {
		return this.acknowledgeable;
	}

	public void setAcknowledgeable(Boolean b) {
		this.acknowledgeable = b;
	}

	public String decorate(Object obj) {
	    AlertBean alert = (AlertBean) getObject();
        
        if (alert.isCanTakeAction()) {
            Boolean isFixable = getFixable();
            Boolean isAcknowledgeable = getAcknowledgeable();
    
            if (isFixable == null) {
                log.debug("Fixable attribute value set to null");
    
                return "";
            }
    
            if (isAcknowledgeable == null) {
                log.debug("Acknowledgeable attribute value set to null");
    
                return "";
            }
    
            if (isAcknowledgeable.booleanValue()) {
                setStyleClass("ackableAlert");
            } else if (isFixable.booleanValue()) {
                setStyleClass("fixableAlert");
            } else {
                return "";
            }
    
            return super.decorate(obj);
        }
        
        return "";
	}

	public void release() {
		super.release();
		fixable = null;
		acknowledgeable = null;
	}
}