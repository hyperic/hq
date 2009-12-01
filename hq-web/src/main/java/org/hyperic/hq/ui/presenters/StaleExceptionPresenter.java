package org.hyperic.hq.ui.presenters;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.hivemind.util.PropertyUtils;
import org.apache.tapestry.IPage;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.StaleLinkException;
import org.apache.tapestry.StaleSessionException;
import org.apache.tapestry.services.ResponseRenderer;

public class StaleExceptionPresenter 
    implements org.apache.tapestry.error.StaleSessionExceptionPresenter, org.apache.tapestry.error.StaleLinkExceptionPresenter{

    private ResponseRenderer _responseRenderer;

    private String _pageName;
    
    private HttpServletRequest _request;

    public void presentStaleLinkException(IRequestCycle cycle, StaleLinkException cause) throws IOException {
        HttpSession session = _request.getSession(false);

        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ex) {
            }
        }
        session = _request.getSession(true);
        
        IPage exceptionPage = cycle.getPage(_pageName);

        String timeoutMsg = exceptionPage.getMessages().getMessage("timeout");
        
        PropertyUtils.write(exceptionPage, "message", timeoutMsg);

        cycle.activate(exceptionPage);

        _responseRenderer.renderResponse(cycle);
    }

    public void presentStaleSessionException(IRequestCycle cycle, StaleSessionException cause) throws IOException {
        HttpSession session = _request.getSession(false);

        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ex) {
            }
        }
        session = _request.getSession(true);
        
        IPage exceptionPage = cycle.getPage(_pageName);
        
        String timeoutMsg = exceptionPage.getMessages().getMessage("timeout");

        PropertyUtils.write(exceptionPage, "message", timeoutMsg);
        
        cycle.activate(exceptionPage);

        _responseRenderer.renderResponse(cycle);
    }

    public void setPageName(String pageName) {
        _pageName = pageName;
    }
    
    public void setRequest(HttpServletRequest req){
        _request = req;
    }

    public void setResponseRenderer(ResponseRenderer responseRenderer) {
        _responseRenderer = responseRenderer;
    }

}
