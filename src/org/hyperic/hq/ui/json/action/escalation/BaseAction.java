package org.hyperic.hq.ui.json.action.escalation;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.json.JSONException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 */
public abstract class BaseAction extends Action
{

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

    public abstract void execute(JsonActionContext context);

    protected void streamResult(JsonActionContext context)
            throws JSONException, IOException
    {
        context.getJSONResult().write(context.getWriter(), false);
    }
}
