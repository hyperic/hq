package org.hyperic.hq.ui.service;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.engine.IEngineService;
import org.apache.tapestry.engine.ILink;
import org.apache.tapestry.services.LinkFactory;

public abstract class BaseService implements IEngineService {

    public static String SERVICE_NAME = "base_service";

    public static final double SERVICE_VERSION_1_0 = 1.0;

    /**
     * All Service Parameters
     */
    public static final String PARAM_SEARCH_QUERY = "q";

    public static final String PARAM_PAGE_SIZE = "n";

    public static final String PARAM_PAGE_NUM = "p";
    
    public static final String PARAM_SERVICE_VERSION = "v";

    public static final String PARAM_RESOURCE_ID = "rid";

    public static final String PARAM_METRIC_TEMPLATE_ID = "mtid";

    public static final String PARAM_SERVICE_ID = "s_id";
    
    public static final String PARAM_TIME_RANGE = "tr";
    
    public static final String PARAM_CONFIG = "config";
    
    public static final String PARAM_REGEX_FILTER = "reg";

    public static final String PARAM_ROTATION = "rot";
    
    public static final String PARAM_INTERVAL = "ivl";
    
    /**
     * Service IDs
     */
    public static final String SERVICE_ID_CHART_WIDGET = "chart";
    
    public static final String SERVICE_ID_ALERT_SUM_WIDGET = "alert_summary";
    
    /**
     * Error Codes
     */
    public static final String ERROR_GENERIC = "{error:true}";
    
    public static final String EMPTY_RESPONSE = "{}";


    protected  HttpServletRequest _request;

    protected HttpServletResponse _response;

    protected ServletContext _servletContext;

    protected LinkFactory _linkFactory;

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
