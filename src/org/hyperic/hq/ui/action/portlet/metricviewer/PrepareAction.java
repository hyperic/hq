package org.hyperic.hq.ui.action.portlet.metricviewer;

import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

public class PrepareAction extends TilesAction {

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        HttpSession session = request.getSession();
        Integer sessionId = RequestUtils.getSessionId(request);
        WebUser user =
            (WebUser)session.getAttribute(Constants.WEBUSER_SES_ATTR);
        String key = ".dashContent.metricviewer.resources";
        PropertiesForm pForm = (PropertiesForm) form;
        PageList resources = new PageList();

        Integer numberToShow =
            new Integer(user.getPreference(PropertiesForm.NUM_TO_SHOW));

        pForm.setNumberToShow(numberToShow);

        List resourceList =
            user.getPreferenceAsList(key, StringConstants.DASHBOARD_DELIMITER);

        Iterator i = resourceList.iterator();

        while(i.hasNext()) {

            ArrayList resourceIds =
                (ArrayList) StringUtil.explode((String) i.next(), ":");

            Iterator j = resourceIds.iterator();
            int type = Integer.parseInt( (String) j.next() );
            int id = Integer.parseInt( (String) j.next() );

            AppdefEntityID entityID = new AppdefEntityID(type, id);
            AppdefResourceValue resource =
                appdefBoss.findById(sessionId.intValue(), entityID);
            resources.add(resource);
        }

        resources.setTotalSize(resources.size());
        request.setAttribute("metricViewerList", resources);
        request.setAttribute("metricViewerTotalSize",
                             new Integer(resources.getTotalSize()));

        return null;
    }
}
