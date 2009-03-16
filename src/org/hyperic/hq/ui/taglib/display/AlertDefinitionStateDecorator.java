package org.hyperic.hq.ui.taglib.display;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.taglib.TagUtils;

@Deprecated
public class AlertDefinitionStateDecorator extends BaseDecorator {
	private static final String ICON_URL = "/images/flag_yellow.gif";
    private static final Locale defaultLocale = Locale.getDefault();

	private static Log log = LogFactory.getLog(AlertDefinitionStateDecorator.class.getName());
	private String bundle = org.apache.struts.Globals.MESSAGES_KEY;
	private String locale = org.apache.struts.Globals.LOCALE_KEY;

	private String active;
	private String disabled;

	@Override
	public String decorate(Object columnValue) {
		return generateOutput();
	}
	
	public String getActive() {
		return active;
	}
	
	public void setActive(String active) {
		this.active = active;
	}
	
	public String getDisabled() {
		return disabled;
	}
	
	public void setDisabled(String disabled) {
		this.disabled = disabled;
	}

	private String generateOutput() {
		StringBuffer sb = new StringBuffer();
        Boolean activeVal = Boolean.FALSE;
        Boolean disabledVal = Boolean.FALSE;
        
        try {      
        	activeVal = (Boolean) evalAttr("active", this.active, Boolean.class);
        } catch(Exception e) {
        	sb.append(generateErrorComment(e.getClass().getName(), "active", getActive(), e));
        }
        
        if (sb.length() > 0) return sb.toString();

        try {      
        	disabledVal = (Boolean) evalAttr("disabled", this.disabled, Boolean.class);
        } catch(Exception e) {
        	sb.append(generateErrorComment(e.getClass().getName(), "disabled", getDisabled(), e));
        }
        
        if (sb.length() > 0) return sb.toString();

        try {
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
