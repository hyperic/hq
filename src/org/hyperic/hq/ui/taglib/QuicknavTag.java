package org.hyperic.hq.ui.taglib;


import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;


/**
 * This class is a two in one tag that that creates a row of quicknav icons for
 * the resource hub.
 */
public class QuicknavTag extends TagSupport{

    //----------------------------------------------------static variables

    protected static Log log =
        LogFactory.getLog(QuicknavTag.class.getName());

    //----------------------------------------------------instance variables

    private String resource;
    private PageContext context;

    public QuicknavTag() {
        super();
    }

    //----------------------------------------------------public methods

    public String getResource() {
        return resource;
    }

    public void setResource(String s) {
        resource = s;
    }

    public String decorate(Object obj) throws Exception{
        AppdefResourceValue rv = null;
        try {
            rv = (AppdefResourceValue) evalAttr("resource", getResource(),
                                                AppdefResourceValue.class);
        } catch (NullAttributeException ne) {
            log.debug("bean " + getResource() + " not found");
            return QuicknavUtil.getNA();
        } catch (JspException je) {
            log.debug("can't evaluate resource type [" + getResource() + "]",
                      je);
            return QuicknavUtil.getNA();
        }

        if (rv.getEntityId() == null) {
            return QuicknavUtil.getNA();
        }

        return QuicknavUtil.getOutput(rv, context);
    }

    public int doStartTag() throws JspException {
        try {
            String d = decorate(this);
            context.getOut().write(d);
        } catch (Exception e) {
            log.error("Error while displaying nav icons.", e);
            throw new JspException(e);
        }
        return SKIP_BODY;
    }

    public int doEndTag() {
        release();
        return EVAL_PAGE;
    }

    public void setPageContext(PageContext pc) {
        context = pc;
    }

    public void release() {
        context = null;
        resource = null;
    }

    private Object evalAttr(String name, String value, Class type)
        throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("quicknavtag", name, value,
                                          type, this, context);
    }
}
