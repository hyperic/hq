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
