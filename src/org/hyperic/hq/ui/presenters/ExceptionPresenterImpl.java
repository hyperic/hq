package org.hyperic.hq.ui.presenters;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.web.WebRequest;

public class ExceptionPresenterImpl extends org.apache.tapestry.error.ExceptionPresenterImpl {

    private WebRequest _request;

    public void presentException(IRequestCycle cycle, Throwable t) {

    }

    public void setWebRequest(WebRequest request) {
        _request = request;
    }

}
