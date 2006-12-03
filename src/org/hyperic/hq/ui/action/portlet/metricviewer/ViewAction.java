package org.hyperic.hq.ui.action.portlet.metricviewer;

import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.product.MetricValue;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.util.List;

/**
 * This action class is used by the Availability Summary portlet.  It's main
 * use is to generate the JSON objects required for display into the UI.
 */
public class ViewAction extends TilesAction {

    private Log _log = LogFactory.getLog("METRIC VIEWER");

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        ServletContext ctx = getServlet().getServletContext();
        MeasurementBoss mBoss = ContextUtils.getMeasurementBoss(ctx);

        WebUser user = (WebUser) request.getSession().getAttribute(
            Constants.WEBUSER_SES_ATTR);

        List entityIds =
            DashboardUtils.preferencesAsEntityIds(PropertiesForm.RESOURCES,
                                                  user);
        AppdefEntityID[] arrayIds =
            (AppdefEntityID[])entityIds.toArray(new AppdefEntityID[0]);

        int count = Integer.parseInt(user.
            getPreference(PropertiesForm.NUM_TO_SHOW));

        String metric = user.getPreference(PropertiesForm.METRIC);

        int sessionId = user.getSessionId().intValue();

        if (metric == null || metric.length() == 0) {
            return null;
        }

        for (int i = 0; i < arrayIds.length; i++) {
            Integer[] tids = new Integer[] { new Integer(metric) };
            MetricValue[] val = mBoss.getLastMetricValue(sessionId, arrayIds[i],
                                                         tids);
            _log.info(arrayIds[i] + " metric=" + metric + " value="  +
                      val[0]);
        }
        return null;
    }
}
