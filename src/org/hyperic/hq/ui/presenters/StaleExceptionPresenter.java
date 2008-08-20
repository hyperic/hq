package org.hyperic.hq.ui.presenters;

import java.io.IOException;

import org.apache.hivemind.util.PropertyUtils;
import org.apache.tapestry.IPage;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.StaleLinkException;
import org.apache.tapestry.StaleSessionException;
import org.apache.tapestry.services.ResponseRenderer;

public class StaleExceptionPresenter {

    private ResponseRenderer _responseRenderer;

    private String _pageName;

    public void presentStaleLinkException(IRequestCycle cycle, StaleLinkException cause) throws IOException {
        IPage exceptionPage = cycle.getPage(_pageName);

        String timeoutMsg = exceptionPage.getMessages().getMessage("timeout");
        
        PropertyUtils.write(exceptionPage, "message", timeoutMsg);

        cycle.activate(exceptionPage);

        _responseRenderer.renderResponse(cycle);
    }

    public void presentStaleSessionException(IRequestCycle cycle, StaleSessionException cause) throws IOException {
        IPage exceptionPage = cycle.getPage(_pageName);
        
        String timeoutMsg = exceptionPage.getMessages().getMessage("timeout");

        PropertyUtils.write(exceptionPage, "message", timeoutMsg);
        
        cycle.activate(exceptionPage);

        _responseRenderer.renderResponse(cycle);
    }

    public void setPageName(String pageName) {
        _pageName = pageName;
    }

    public void setResponseRenderer(ResponseRenderer responseRenderer) {
        _responseRenderer = responseRenderer;
    }

}
