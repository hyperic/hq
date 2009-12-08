package org.hyperic.hq.ui.taglib.display;

import javax.servlet.jsp.JspException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;

public class ConditionalLinkDecorator
extends BaseDecorator
{
    private static Log log = LogFactory.getLog(ConditionalLinkDecorator.class.getName());
    
    private String test;
    private String href;
    
    @Override
    public String decorate(Object columnValue) {
        // This is the actual text of the column taken from the column tag...
        String columnText = (String) columnValue;
        
        // ...now figure out if we have to render a link or not, if we can't 
        // determine it, we just render the plain text as the default behavior...
        boolean doRenderLink = false;
        
        try {
            String attrValue = (String) evalAttr("test", getTest(), String.class);
            
            doRenderLink =Boolean.parseBoolean(attrValue);
        } catch(NullAttributeException e) {
            // No attribute specified, so assume no link to render
            log.debug("Assuming no link to render because test attribute not specified.");
        } catch(JspException e) {
            // Couldn't determine
            log.warn("Can't render link because error occurred while processing test attribute.", e);
        }
                
        // ...If we can render a link, see if we have a href and render it, if not
        // we just render the default text...
        if (doRenderLink) {
            String href = null;
            
            try {
                href = (String) evalAttr("href", getHref(), String.class);
            } catch(NullAttributeException e) {
                // No attribute specified, so assume no link to render
                log.debug("Assuming no link to render because href attribute not specified.");
            } catch(JspException e) {
                // Couldn't determine
                log.warn("Can't render link because error occurred while processing href attribute.", e);
            }
            
            // ...at this point we should know enough to render the link or not
            if (href != null) {
                StringBuffer result = new StringBuffer();
            
                result.append("<a href=\"").append(href).append("\">");
                result.append(columnText);
                result.append("</a>");
                
                return result.toString();
            }
        }
        
        return columnText;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
