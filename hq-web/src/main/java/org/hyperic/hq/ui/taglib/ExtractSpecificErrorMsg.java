package org.hyperic.hq.ui.taglib;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

public class ExtractSpecificErrorMsg extends TagSupport {

	private String errorFieldName;

	public String getErrorFieldName() {
		return errorFieldName;
	}

	public void setErrorFieldName(String errorFieldName) {
		this.errorFieldName = errorFieldName;
	}

	@Override
    public int doStartTag() throws JspException {
    
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		
		Map<String, List<String>> fErrors = (Map<String, List<String>>) request.getAttribute("fieldErrors");
		List<String> outputError =  fErrors.get(errorFieldName);
		if (outputError != null && outputError.size()>0){
			try {
				pageContext.getOut().write(outputError.get(0));
			} catch (IOException e) {
				 throw new JspException(e);
			}
		}
    	return SKIP_BODY;
    }
}
