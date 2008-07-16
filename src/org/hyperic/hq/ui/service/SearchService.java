package org.hyperic.hq.ui.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.engine.ILink;
import org.apache.tapestry.services.ServiceConstants;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.uibeans.SearchResult;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The User Interface's search service
 * 
 * The query parameters for the service are 
 * q - the query [REQUIRED]
 * n - the page size (number of results per page) [OPTIONAL] 
 * p - the page number [OPTIONAL]
 * 
 * The encoded service link will look like
 * http://foo.bar:port/search.shtml?q=my%20search%20query&p=1&n=10
 * 
 * The default behavior is to return the 1st page containing
 * at most 10 results
 */
public class SearchService extends BaseService {
    static Log log = LogFactory.getLog(SearchService.class);

    public static final String SERVICE_NAME = "search";

    public static final int DEFAULT_PAGE_SIZE = 10;

    public String getName() {
        return SERVICE_NAME;
    }

    public ILink getLink(boolean post, Object parameter) {

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(ServiceConstants.SERVICE, getName());

        // The map can contain the SEARCH_STRING, SIZE and NUM params
        // all optionally
        if (parameter != null)
            parameters.putAll((Map) parameter);
        return _linkFactory.constructLink(this, post, parameters, true);
    }

    /**
     * Supports service version 1.0+
     */
    public void service(IRequestCycle cycle) throws IOException {
        String query = cycle.getParameter(PARAM_SEARCH_QUERY);
        String pageSize = cycle.getParameter(PARAM_PAGE_SIZE);
        String pageNum = cycle.getParameter(PARAM_PAGE_NUM);

        // Holds the page size and page number
        PageControl page;

        if (query == null)
            return;

        if (pageSize == null)
            page = new PageControl(0, DEFAULT_PAGE_SIZE);

        else if (pageNum == null)
            page = new PageControl(0, Integer.parseInt(pageSize));

        else
            page = new PageControl(Integer.parseInt(pageNum), Integer.parseInt(pageSize));

        WebUser user = (WebUser) _request.getSession().getAttribute(Constants.WEBUSER_SES_ATTR);
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(_servletContext);

        PageList res;
        try {
            res = appdefBoss.search(user.getSessionId(), query, page);
        } catch (Exception e) {
            log.error(e.getMessage());
            return;
        }
        _response.getWriter().write(getJsonResutls(res));
    }

    private String getJsonResutls(PageList res) {
        Iterator i = res.iterator();
        int key = 0;
        JSONObject jObject = new JSONObject();
        while (i.hasNext()) {
            SearchResult r = (SearchResult) i.next();
            try {
                jObject.put(Integer.toString(key), r.toJson());
            } catch (JSONException e) {
                log.warn("Cannot create search result list" + e.getStackTrace());
            }
            key++;
        }
        return jObject.toString();
    }

}
