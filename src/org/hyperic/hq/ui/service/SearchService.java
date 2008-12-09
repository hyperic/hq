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
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.uibeans.SearchResult;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.json.JSONArray;
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
            page = new PageControl(Integer.parseInt(pageNum),
                                   Integer.parseInt(pageSize));

        WebUser user = (WebUser)
            _request.getSession().getAttribute(Constants.WEBUSER_SES_ATTR);
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(_servletContext);
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(_servletContext);

        PageList res;
        PageList users;
        try {
            res = appdefBoss.search(user.getSessionId(), query, page);
            users = authzBoss.getSubjectsByName(user.getSessionId(), query,
                                                page);
        } catch (Exception e) {
            log.error(e.getMessage());
            return;
        }
        _response.getWriter().write(getJsonResutls(res, users));
    }

    private String getJsonResutls(PageList res, PageList users) {
        JSONArray resArray = new JSONArray();
        JSONArray usrArray = new JSONArray();
        for (Iterator i = res.iterator(); i.hasNext();) {
            SearchResult r = (SearchResult) i.next();
            try {
                resArray.put(r.toJson());
            } catch (JSONException e) {
                log.warn("Cannot create resource search result list. " +
                         e.getStackTrace());
            }
        }
        
        for (Iterator i = users.iterator(); i.hasNext();) {
            AuthzSubject s = (AuthzSubject) i.next();
            try {
                usrArray.put(new JSONObject().put("name", s.getName())
                                             .put("id", s.getId()));
            } catch (JSONException e) {
                log.warn("Cannot create user search result list. " +
                         e.getStackTrace());
            }
        }
        
        JSONObject json = new JSONObject();
        try {
            json.put("resources", resArray);
            json.put("users", usrArray);
        } catch (JSONException e) {
            log.warn("Cannot create search result JSON. " + e.getStackTrace());
        }
        return json.toString();
    }

}
