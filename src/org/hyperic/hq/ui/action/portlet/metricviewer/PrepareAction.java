package org.hyperic.hq.ui.action.portlet.metricviewer;

import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.config.InvalidOptionException;

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
        MeasurementBoss measBoss = ContextUtils.getMeasurementBoss(ctx);

        HttpSession session = request.getSession();
        int sessionId = RequestUtils.getSessionId(request).intValue();
        WebUser user =
            (WebUser)session.getAttribute(Constants.WEBUSER_SES_ATTR);
        PropertiesForm pForm = (PropertiesForm) form;
        PageList resources = new PageList();

        String token = pForm.getToken();

        // For multi-portlet configuration
        String numKey = PropertiesForm.NUM_TO_SHOW;
        String resKey = PropertiesForm.RESOURCES;
        String resTypeKey = PropertiesForm.RES_TYPE;
        String metricKey = PropertiesForm.METRIC;
        String descendingKey = PropertiesForm.DECSENDING;
        if (token != null) {
            numKey += token;
            resKey += token;
            resTypeKey += token;
            metricKey += token;
            descendingKey += token;
        }

        // We set defaults here rather than in DefaultUserPreferences.properites
        Integer numberToShow = new Integer(user.getPreference(numKey, "10"));
        String resourceType = user.getPreference(resTypeKey, "");
        String metric = user.getPreference(metricKey, "");
        String descending = user.getPreference(descendingKey, "true");
        List resourceList;
        try {
            DashboardUtils.verifyResources(resKey, ctx, user);
            resourceList =
                user.getPreferenceAsList(resKey,
                                         StringConstants.DASHBOARD_DELIMITER);
        } catch (InvalidOptionException e) {
            resourceList = new ArrayList();
        }

        pForm.setNumberToShow(numberToShow);
        if (resourceType != null && resourceType.length() != 0) {
            pForm.setResourceType(resourceType);
        }
        pForm.setMetric(metric);
        pForm.setDescending(descending);
        
        Iterator i = resourceList.iterator();
        while(i.hasNext()) {
            String appdefKey = (String)i.next();
            AppdefEntityID entityID = new AppdefEntityID(appdefKey);
            AppdefResourceValue resource =
                appdefBoss.findById(sessionId, entityID);
            resources.add(resource);
        }

        resources.setTotalSize(resources.size());
        request.setAttribute("descending", descending);
        request.setAttribute("metricViewerList", resources);
        request.setAttribute("metricViewerTotalSize",
                             new Integer(resources.getTotalSize()));

        PageList viewablePlatformTypes =
            appdefBoss.findViewablePlatformTypes(sessionId,
                                                 PageControl.PAGE_ALL);
        request.setAttribute("platformTypes", viewablePlatformTypes);
        PageList viewableServerTypes =
            appdefBoss.findViewableServerTypes(sessionId,
                                               PageControl.PAGE_ALL);
        request.setAttribute("serverTypes", viewableServerTypes);
        PageList viewableServiceTypes =
            appdefBoss.findViewableServiceTypes(sessionId,
                                                PageControl.PAGE_ALL);
        request.setAttribute("serviceTypes", viewableServiceTypes);

        PageList metrics = new PageList();
        if (resourceType != null && resourceType.length() != 0) {
            AppdefEntityTypeID typeId = new AppdefEntityTypeID(resourceType);
            AppdefResourceTypeValue typeVal =
                appdefBoss.findResourceTypeById(sessionId, typeId);

            metrics = measBoss.findMeasurementTemplates(sessionId,
                                                        typeVal.getName(),
                                                        PageControl.PAGE_ALL);
        }

        request.setAttribute("metrics", metrics);
        
        return null;
    }
}
