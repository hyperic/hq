/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.ui.json.action;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.ui.action.BaseActionMapping;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * context for json struts action apis
 */
public class JsonActionContext extends HashMap
{
    private static Object ACTION_MAPPING = new Object();
    private static Object ACTION_FORM = new Object();
    private static Object ACTION_REQUEST= new Object();
    private static Object ACTION_RESPONSE= new Object();
    private static Object JSON_RESULT = new Object();

    public static JsonActionContext newInstance(ActionMapping map,
                                                ActionForm form,
                                                HttpServletRequest request,
                                                HttpServletResponse response)
    {
        return new JsonActionContext(map, form, request, response);
    }

    protected JsonActionContext(){}

    protected JsonActionContext(ActionMapping map,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
    {
        put(ACTION_MAPPING, map);
        put(ACTION_FORM, form);
        put(ACTION_REQUEST, request);
        put(ACTION_RESPONSE, response);
    }

    public Integer getId() {
        String id;
        try {
            id = RequestUtils.getStringParameter(getRequest(), "id");
        } catch (ParameterNotFoundException e) {
            id = getMapping().getParameter();
        }
        return Integer.valueOf(id);
    }

    public EscalationAlertType getAlertDefType() {
        // XXX: implement for galertDef
        return ClassicEscalationAlertType.CLASSIC;
    }

    public String getName()
    {
        return getMapping().getParameter();
    }

    public int getSessionId()
    {
        try {
            return RequestUtils.getSessionId(getRequest()).intValue();
        } catch (ServletException e) {
            throw new SystemException(e);
        }
    }

    public Writer getWriter() throws IOException
    {
        return getResponse().getWriter();
    }

    public JSONResult getJSONResult()
    {
        return (JSONResult)get(JSON_RESULT);
    }

    public void setJSONResult(JSONResult result)
    {
        put(JSON_RESULT, result);
    }

    public boolean isPrettyPrint()
    {
        String pretty = getRequest().getParameter("pretty");
        return pretty != null;
    }

    public Map getParameterMap()
    {
        return getRequest().getParameterMap();
    }

    public ServletContext getServletContext()
    {
        return getSession().getServletContext();
    }

    public HttpSession getSession()
    {
        return getRequest().getSession();
    }

    private BaseActionMapping getMapping()
    {
        return (BaseActionMapping)get(ACTION_MAPPING);
    }

    public HttpServletRequest getRequest()
    {
        return (HttpServletRequest)get(ACTION_REQUEST);
    }

    private HttpServletResponse getResponse()
    {
        return (HttpServletResponse)get(ACTION_RESPONSE);
    }
}
