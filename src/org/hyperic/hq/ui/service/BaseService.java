package org.hyperic.hq.ui.service;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.engine.IEngineService;
import org.apache.tapestry.engine.ILink;
import org.apache.tapestry.services.LinkFactory;

/**
 * 
 * 
 */
public abstract class BaseService implements IEngineService {

    public static String SERVICE_NAME = "base_service";

    public static double SERVICE_VERSION_1_0 = 1.0;

    protected  HttpServletRequest _request;

    protected HttpServletResponse _response;

    protected ServletContext _servletContext;

    protected LinkFactory _linkFactory;

    /**
     * All Service Parameters
     */
    public static final String SEARCH_PARAM = "q";

    public static final String PAGE_SIZE_PARAM = "n";

    public static final String PAGE_NUM_PARAM = "p";
    
    public static final String SERVICE_VERSION_PARAM = "v";

    public static final String PARAM_RESOURCE_ID = "rid";

    public static final String PARAM_METRIC_TEMPLATE_ID = "mtid";

    public static final String PARAM_SERVICE_ID = "s_id";

    public static final String SERVICE_ID_CHART_WIDGET = "chart";
    
    public static final String SERVICE_ID_ALERT_SUM_WIDGET = "alert_summary";
    
    public abstract ILink getLink(boolean arg0, Object arg1);

    public abstract String getName();

    public abstract void service(IRequestCycle arg0) throws IOException;
    
    public void setRequest(HttpServletRequest request) {
        _request = request;
    }

    public void setResponse(HttpServletResponse response) {
        _response = response;
    }

    public void setLinkFactory(LinkFactory linkFactory) {
        _linkFactory = linkFactory;
    }

    public void setServletContext(ServletContext context) {
        _servletContext = context;
    }

}
