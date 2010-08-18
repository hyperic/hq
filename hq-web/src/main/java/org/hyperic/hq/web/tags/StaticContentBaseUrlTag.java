package org.hyperic.hq.web.tags;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.hyperic.hq.web.StaticContentBaseUrlResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class StaticContentBaseUrlTag implements Tag, Serializable {
	private static final long serialVersionUID = 1L;
	
	private PageContext pageContext;
	private Tag parentTag;
	
	public int doStartTag() throws JspException {
		StaticContentBaseUrlResolver staticContentBaseUrlResolver = getStaticContentBaseUrlResolver();
		
		try {
			pageContext.getOut().write(staticContentBaseUrlResolver.getBaseUrl());
		} catch (IOException e) {
			throw new JspTagException(e);
		}
		
		return SKIP_BODY;
	}

	private StaticContentBaseUrlResolver getStaticContentBaseUrlResolver() throws JspException {
		ServletContext servletContext = pageContext.getServletContext();
        ApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);	
		Map<String, StaticContentBaseUrlResolver> resolvers = applicationContext.getBeansOfType(StaticContentBaseUrlResolver.class);
		
		if (resolvers.size() == 0) {
			throw new JspTagException("No visible org.hyperic.hq.web.util.StaticContentBaseUrlResolver instance could be found in the application " +
                                      "context. There must be one in order to support the 'staticContentBaseUrl' JSP tag.");
		}

		// ...return the first one found (really should only be one)...
		return resolvers.values().iterator().next();
	}
	public int doEndTag() throws JspException {
		return EVAL_PAGE;
	}

	public void release() {
		pageContext = null;
		parentTag = null;
	}

	public void setPageContext(PageContext pageContext) {
		this.pageContext = pageContext;
	}

	public void setParent(Tag parentTag) {
		this.parentTag = parentTag;
	}

	public Tag getParent() {
		return parentTag;
	}
}