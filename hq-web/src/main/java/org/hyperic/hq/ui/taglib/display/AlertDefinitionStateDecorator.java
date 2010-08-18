/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.ui.taglib.display;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.taglib.TagUtils;

@Deprecated
public class AlertDefinitionStateDecorator extends BaseDecorator {
	private static final String ICON_URL = "/images/flag_yellow.gif";
    
	private static Log log = LogFactory.getLog(AlertDefinitionStateDecorator.class.getName());
	private String bundle = org.apache.struts.Globals.MESSAGES_KEY;
	private String locale = org.apache.struts.Globals.LOCALE_KEY;

	private Boolean active;
	private Boolean disabled;

	@Override
	public String decorate(Object columnValue) {
		return generateOutput();
	}
	
	public Boolean getActive() {
		return active;
	}
	
	public void setActive(Boolean active) {
		this.active = active;
	}
	
	public Boolean getDisabled() {
		return disabled;
	}
	
	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	private String generateOutput() {
		StringBuffer sb = new StringBuffer();
        
        try {
        	Boolean activeVal = getActive();
            Boolean disabledVal = getDisabled();
            
            sb.append("<span style=\"whitespace:nowrap\">");
        	
	        if (activeVal) {
	           	sb.append(TagUtils.getInstance().message(getPageContext(), bundle, locale, "alert.config.props.PB.ActiveYes"));
	            	
	           	if (disabledVal) {
	           		 sb.append("&nbsp;<img align=\"absmiddle\"src=\"").append(ICON_URL).append("\"")
	                   .append("           title=\"").append(TagUtils.getInstance().message(getPageContext(), bundle, locale, "alert.config.props.PB.ActiveButDisabled")).append("\" />");
	           	}
	        } else {
	           	sb.append(TagUtils.getInstance().message(getPageContext(), bundle, locale, "alert.config.props.PB.ActiveNo"));
	        }
	        
	        sb.append("</span>");
        } catch(Exception e) {
        	log.debug("Markup rendering failed.", e);
        	
        	sb.append(e.getClass().getName()).append(" triggered while rendering markup.");
        }
        
        return sb.toString();
    }
    
    protected String generateErrorComment(String exc, String attrName, String attrValue, Throwable t) {
        log.debug(attrName + " expression [" + attrValue + "] not evaluated", t);
        StringBuffer sb = new StringBuffer("<!-- ");
        sb.append(" failed due to ");
        sb.append(exc);
        sb.append(" on ");
        sb.append(attrName);
        sb.append(" = ");
        sb.append(attrValue);
        sb.append(" -->");
        return sb.toString();
    }
}
