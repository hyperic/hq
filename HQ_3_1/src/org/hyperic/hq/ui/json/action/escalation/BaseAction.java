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

package org.hyperic.hq.ui.json.action.escalation;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.json.JSONException;

public abstract class BaseAction extends Action
{

    protected static final String ID = "id";
    protected static final String NOTIFY_ALL = "notifyAll";
    protected static final String ALLOW_PAUSE = "allowPause";
    protected static final String NAME = "name";
    protected static final String MAX_WAIT_TIME = "maxWaitTime";
    protected static String DESCRIPTION = "description";
    protected static String ALERTDEF_ID = "ad";
    protected static String GALERTDEF_ID = "gad";

    public ActionForward execute(ActionMapping map,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception
    {
        JsonActionContext context =
                JsonActionContext.newInstance(map, form, request, response);
        execute(context);
        streamResult(context);
        return null;
    }

    public abstract void execute(JsonActionContext context)
        throws Exception;

    protected void streamResult(JsonActionContext context)
            throws JSONException, IOException
    {
        context.getJSONResult().write(
                context.getWriter(),
                context.isPrettyPrint());
    }
}
