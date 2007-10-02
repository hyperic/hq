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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.appdef.shared.AppdefCompatException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventLogBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.BaseMetricDisplay;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.measurement.BaselineCreationException;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.data.DataNotAvailableException;
import org.hyperic.hq.measurement.shared.BaselineValue;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.beans.ChartedMetricBean;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * An <code>Action</code> that retrieves data from the BizApp to
 * facilitate display of the various pages that provide metrics
 * summaries.
 *
 */
public class ViewChartFormPrepareAction extends MetricDisplayRangeFormPrepareAction {
    protected static Log log =
        LogFactory.getLog( ViewChartFormPrepareAction.class.getName() );

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve data needed to display a Metrics Display Form. Respond
     * to certain button clicks that alter the form display.
     */
    public ActionForward workflow(ComponentContext cc, ActionMapping mapping,
                                  ActionForm form, HttpServletRequest request,
                                  HttpServletResponse response)
        throws Exception {
        super.workflow(cc, mapping, form, request, response);

        ViewChartForm chartForm = (ViewChartForm)form;

        int sessionId = RequestUtils.getSessionId(request).intValue();
        ServletContext ctx = getServlet().getServletContext();

        AppdefResourceValue resource = RequestUtils.getResource(request);
        if (resource == null)
            return removeBadDashboardLink(request, ctx);
        
        AppdefEntityID adeId = resource.getEntityId();
        chartForm.setRid( resource.getId() );
        chartForm.setType( new Integer( adeId.getType() ) );

        try {
            AppdefEntityTypeID atid =
                RequestUtils.getChildResourceTypeId(request);
            chartForm.setCtype(atid.getAppdefKey());
        } catch (ParameterNotFoundException e) {
            // This is not an autogroup
        }

        // These private methods have side-effects and must be
        // called in this order.  Lame, I know, but I wanted to
        // have this stuff in easier-to-manage code blocks (JW).
        _setupDateRange(request, chartForm);
        _setupMetricIds(request, chartForm);
        
        MeasurementBoss mb = ContextUtils.getMeasurementBoss(ctx);

        AppdefResourceValue[][] resources =
            _setupResources(request, sessionId, chartForm, resource, mb);

        try {
            if (resources.length == 0 || resources[0].length == 0)
                throw new MeasurementNotFoundException(
                    "No resources found for chart");

            _setupMetricData(request, sessionId, chartForm, resources[1], mb,
                             ctx);
        } catch (MeasurementNotFoundException e) {
            return removeBadDashboardLink(request, ctx);
        }

        _setupPageData(request, sessionId, chartForm, resources[0], mb);

        return null;
    }


    // ---------------------------------------------------- Private Methods
    
    private ActionForward removeBadDashboardLink(HttpServletRequest request,
                                                 ServletContext ctx)
        throws SessionTimeoutException, SessionNotFoundException, 
               ApplicationException, RemoteException {
        // This was probably a bad favorites chart
        String query = request.getQueryString();
        HttpSession session = request.getSession();
        ConfigResponse userDashPrefs = (ConfigResponse) session.getAttribute(Constants.USER_DASHBOARD_CONFIG);
        WebUser user =
            (WebUser) session.getAttribute( Constants.WEBUSER_SES_ATTR );
        String userCharts = userDashPrefs.getValue(Constants.USER_DASHBOARD_CHARTS);
        List chartList =
            StringUtil.explode(userCharts, Constants.DASHBOARD_DELIMITER);
        for (Iterator i = chartList.iterator(); i.hasNext();) {
            String chart = (String) i.next();
            if (chart.indexOf(query) > 0) {
                AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
                
                // Remove this and direct user to dash
                userCharts = StringUtil.remove(userCharts, chart);

                userDashPrefs.setValue(Constants.USER_DASHBOARD_CHARTS, userCharts);
                
                ConfigurationProxy.getInstance().setUserDashboardPreferences(userDashPrefs, boss, user );
                request.setAttribute("toDashboard", "true");
                return null;
            }
        }
        return null;
    }

    private void _setupDateRange(HttpServletRequest request,
                                 ViewChartForm chartForm) {
        // decide what timeframe we're showing. it may have been
        // shifted on previous views of this page.
        MetricRange range = (MetricRange)
            request.getAttribute(Constants.METRIC_RANGE);
        if (null == range) {
            // this is the first time out
            range = new MetricRange();
            range.setBegin( new Long( chartForm.getStartDate().getTime() ) );
            range.setEnd( new Long( chartForm.getEndDate().getTime() ) );
            range.shiftNow();
            request.setAttribute(Constants.METRIC_RANGE, range);
        }

        // Since we have two ways to adjust the range of data we're
        // looking at (paging back and forth and explicitly choosing
        // something in the display range tile), we will try to always
        // keep the display range tile "up to date".  That is, if the
        // end date is "now", we'll select "last n" and otherwise
        // we'll select "date range".
        chartForm.synchronizeDisplayRange();
    }

    private void _setupMetricIds(HttpServletRequest request,
                                 ViewChartForm chartForm) {
        // metric ids
        String[] metricTemplateIds = request.getParameterValues
            (Constants.METRIC_PARAM);
        Integer[] m = ArrayUtil.stringToInteger(metricTemplateIds);
        chartForm.setM(m);

        // originally-selected metric ids
        String[] origMetricTemplateIds = request.getParameterValues("origM");
        if (null == origMetricTemplateIds || origMetricTemplateIds.length == 0) {
            chartForm.setOrigM( (Integer[]) chartForm.getM().clone() );
        } else {
            Integer[] origM = ArrayUtil.stringToInteger(origMetricTemplateIds);
            chartForm.setOrigM(origM);
        }
    }

    private AppdefResourceValue[][] _setupResources(HttpServletRequest request,
                                                    int sessionId,
                                                    ViewChartForm chartForm,
                                                    AppdefResourceValue resource,
                                                    MeasurementBoss mb)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException,
               RemoteException, MeasurementNotFoundException,
               DataNotAvailableException {
        ServletContext ctx = getServlet().getServletContext();
        // get list of all child resources
        AppdefResourceValue[] resources = null;
        if ( null != chartForm.getCtype() &&
             !chartForm.getCtype().equals(ViewChartForm.NO_CHILD_TYPE) ) {
            AppdefEntityID adeId = resource.getEntityId();
            AppdefBoss ab = ContextUtils.getAppdefBoss(ctx);
            AppdefEntityTypeID atid =
                new AppdefEntityTypeID(chartForm.getCtype());
            PageList children = ab.findChildResources( sessionId, adeId,
                                                       atid,
                                                       PageControl.PAGE_ALL );
            String[] rids = request.getParameterValues("r");
            Integer[] r = ArrayUtil.stringToInteger(rids);
            // if we've been passed a list of resource ids, we are
            // comparing metrics and need to prune out all but the
            // resources corresponding to the passed-in resource ids
            if (null != r) {
                log.debug("r=" + StringUtil.arrayToString(r));
                for (Iterator it=children.iterator(); it.hasNext();) {
                    AppdefResourceValue res = (AppdefResourceValue)it.next();
                    boolean found = false;
                    for (int i = 0; i < r.length; ++i) {
                        if ( found = res.getId().equals(r[i]) )
                            break;
                    }

                    if (!found) {
                        log.debug("removing resource: " + res.getId());
                        it.remove();
                    }
                }
            }
            resources = new AppdefResourceValue[children.size()];
            resources = (AppdefResourceValue[])children.toArray(resources);
        } else if (resource instanceof AppdefGroupValue) {
            AppdefGroupValue grpVal = (AppdefGroupValue) resource;
            AppdefEntityID[] grpMembers =
                new AppdefEntityID[grpVal.getAppdefGroupEntries().size()];
            grpMembers = (AppdefEntityID[])
                grpVal.getAppdefGroupEntries().toArray(grpMembers);

            AppdefBoss ab = ContextUtils.getAppdefBoss(ctx);
            List memVals = ab.findByIds(sessionId, grpMembers);
            resources = new AppdefResourceValue[memVals.size()];
            resources = (AppdefResourceValue[]) memVals.toArray(resources);
        } else {
            resources = new AppdefResourceValue[] { resource };
        }
        resources = mb.pruneResourcesNotCollecting(sessionId, resources,
                                                   chartForm.getM()[0]);
        request.setAttribute("resources", resources);
        request.setAttribute( "resourcesSize", new Integer(resources.length) );

        // if no specific resourceIds were checked, checkedResources
        // is the same as resources and chartForm.resourceIds contains
        // all resource ids
        String[] resourceIds = request.getParameterValues("resourceIds");
        if (null == resourceIds || resourceIds.length == 0) {
            int maxResources = _getMaxResources(request, resources.length);
            log.debug("maxResources=" + maxResources);
            AppdefResourceValue[] checkedResources =
                new AppdefResourceValue[maxResources];
            System.arraycopy(resources, 0, checkedResources, 0, maxResources);
            Integer[] rids = new Integer[checkedResources.length];
            for (int i = 0; i < rids.length; ++i) {
                rids[i] = checkedResources[i].getId();
            }
            chartForm.setResourceIds(rids);
            if (log.isDebugEnabled()) {
                log.debug("no resourceIds specified: " +
                          StringUtil.arrayToString(rids));
            }
            request.setAttribute("checkedResources", checkedResources);
            request.setAttribute("checkedResourcesSize",
                                 new Integer(checkedResources.length));
            return new AppdefResourceValue[][] { resources, checkedResources };
        } else {
            Integer[] rids = chartForm.getResourceIds();
            AppdefResourceValue[] checkedResources =
                new AppdefResourceValue[rids.length];
            for (int i=0; i<rids.length; ++i) {
                for (int j=0; j<resources.length; ++j) {
                    if ( resources[j].getId().equals(rids[i]) ) {
                        checkedResources[i] = resources[j];
                    }
                }
            }
            if ( log.isDebugEnabled() ) {
                log.debug( "resourceIds specified: " +
                           org.hyperic.util.StringUtil.arrayToString(rids) );
            }
            request.setAttribute("checkedResources", checkedResources);
            request.setAttribute("checkedResourcesSize",
                                 new Integer(checkedResources.length) );
            return new AppdefResourceValue[][] {resources, checkedResources};
        }
    }

    private void _setupMetricData(HttpServletRequest request, int sessionId,
                                  ViewChartForm chartForm,
                                  AppdefResourceValue[] resources,
                                  MeasurementBoss mb, ServletContext ctx)
        throws SessionNotFoundException, SessionTimeoutException,
               DataNotAvailableException, MeasurementNotFoundException,
               RemoteException, AppdefEntityNotFoundException,
               PermissionException {
        
        EventLogBoss eb = ContextUtils.getEventLogBoss(ctx);
        List eventPointsList = new ArrayList(resources.length);

        // Get data for charts and put it in session.  In reality only
        // one of either resources or metrics can have more than one
        // entry, so it's really not as much of a nested loop as it
        // seems.  However, the code is written this way so that it
        // can be used in both the multi-resource and the multi-metric
        // case.
        // data points for chart
        Integer m[] = chartForm.getM();
        String[] chartDataKeys = new String[m.length];
        log.trace("number of metrics: " + m.length);

        for (int i = 0; i < m.length; ++i) {
            // Use the current time concatenated with metric
            // template id for key.
            chartDataKeys[i] =
                String.valueOf( System.currentTimeMillis() ) + m[i];

            for (int j = 0; j < resources.length; ++j) {
                if (log.isDebugEnabled()) {
                    log.debug("mtid=" + m[i] + ", rid=" + resources[j].getId());
                    log.debug("startDate=" + chartForm.getStartDate());
                    log.debug("endDate=" + chartForm.getEndDate());
                }

                long interval = TimeUtil.getInterval(
                    chartForm.getStartDate().getTime(),
                    chartForm.getEndDate().getTime(),
                    Constants.DEFAULT_CHART_POINTS);

                if (i == 0) {
                    if (interval > 0) {
                        List controlActions = eb.getEvents(sessionId,
                                 ControlEvent.class.getName(),
                                 resources[j].getEntityId(),
                                 chartForm.getStartDate().getTime(),
                                 chartForm.getEndDate().getTime());
                        // We need to make sure that the event IDs get set
                        // for the legend.
                        int k = 0;
                        for (Iterator it = controlActions.iterator();
                             it.hasNext(); ) {
                            EventLog event = (EventLog) it.next();
                            event.setEventID(++k);
                        }
                        eventPointsList.add(controlActions);
                    }
                }
            }
            log.debug("Store into session: " + chartDataKeys[i]);
        }
        request.setAttribute(Constants.CHART_DATA_KEYS_SIZE, new Integer(
                             chartDataKeys.length));
        request.setAttribute("chartLegend", eventPointsList);
    }

    private static final class BaseMetricDisplayComparator
        implements Comparator {
        public int compare(Object o1, Object o2) {
            BaseMetricDisplay bmd1 = (BaseMetricDisplay)o1;
            BaseMetricDisplay bmd2 = (BaseMetricDisplay)o2;
            return bmd1.getLabel().compareTo( bmd2.getLabel() );
        }

        public boolean equals(Object obj) {
            return obj instanceof BaseMetricDisplayComparator;
        }
    }
    protected static final BaseMetricDisplayComparator comp =
        new BaseMetricDisplayComparator();

    protected void _setupPageData(HttpServletRequest request,
                                  int sessionId,
                                  ViewChartForm chartForm,
                                  AppdefResourceValue[] resources,
                                  MeasurementBoss mb)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException,
               AppdefCompatException, RemoteException,
               MeasurementNotFoundException, BaselineCreationException {
        List mtids = (List)Arrays.asList( chartForm.getOrigM() );
        ArrayList metricSummaries = new ArrayList();
        for (int i=0; i<resources.length; ++i) {
            Map metrics =
                mb.findMetrics(sessionId, resources[i].getEntityId(),
                               mtids, chartForm.getStartDate().getTime(),
                               chartForm.getEndDate().getTime() );
            MonitorUtils.formatMetrics( metrics, request.getLocale(),
                                        getResources(request) );
            for (Iterator it = metrics.values().iterator(); it.hasNext();) {
                metricSummaries.addAll( (Collection) it.next() );
            }
        }
        Collections.sort(metricSummaries, comp);
        request.setAttribute("metricSummaries", metricSummaries);
        request.setAttribute( "metricSummariesSize",
                              new Integer( metricSummaries.size() ) );

        // pick out the charted metrics from the metric summaries
        ChartedMetricBean[] chartedMetrics =
            new ChartedMetricBean[chartForm.getM().length];
        for (int i = 0; i < chartedMetrics.length; ++i) {
            for (int j = 0; j < metricSummaries.size(); ++j) {
                MetricDisplaySummary mds =
                    (MetricDisplaySummary) metricSummaries.get(j);
                if (mds.getTemplateId().equals(chartForm.getM()[i])) {
                    int unitUnits = UnitsConvert.getUnitForUnit(mds.getUnits());
                    int unitScale =
                        UnitsConvert.getScaleForUnit(mds.getUnits());
                    chartedMetrics[i] =
                        new ChartedMetricBean(mds.getLabel(), unitUnits,
                            unitScale, mds.getCollectionType().intValue(),
                            mds.getTemplateId());
                    break;
                }                
            }
        }
        request.setAttribute("chartedMetrics", chartedMetrics);

        _setupBaselineExpectedRange(request, sessionId, chartForm, resources,
                                    chartedMetrics, mb);
    }

    protected void _setupBaselineExpectedRange(HttpServletRequest request,
                                               int sessionId,
                                               ViewChartForm chartForm,
                                               AppdefResourceValue[] resources,
                                               ChartedMetricBean[] chartedMetrics,
                                               MeasurementBoss mb)
        throws SessionTimeoutException,
               SessionNotFoundException,
               MeasurementNotFoundException,
               BaselineCreationException,
               PermissionException,
               AppdefEntityNotFoundException,
               RemoteException
    {
        DerivedMeasurementValue dmv = null;
        
        if (chartForm.getMode().equals(Constants.MODE_MON_CHART_SMSR) ||
            chartForm.getMode().equals(Constants.MODE_MON_CHART_SMMR)) {
            dmv = mb.findMeasurement(sessionId, chartForm.getM()[0],
                                     resources[0].getEntityId());
            request.setAttribute("metric", dmv);
        
            // Set the name to be displayed
            chartForm.setChartName(dmv.getTemplate().getName());
        } else if (chartForm.getMode().equals(Constants.MODE_MON_CHART_MMSR)) {
            AppdefEntityID aeid = resources[0].getEntityId();
            for (int i = 0; i < chartedMetrics.length; i++) {
                if (chartedMetrics[i] == null)
                    continue;
                
                dmv = mb.findMeasurement(sessionId,
                                         chartedMetrics[i].getTemplateId(),
                                         aeid);
                BaselineValue baselineValue = dmv.getBaseline();
                if (null != baselineValue) {
                    if (null != baselineValue.getMean())
                        chartedMetrics[i].setBaselineRaw(
                            baselineValue.getMean());
                    
                    if (null != baselineValue.getMaxExpectedValue())
                        chartedMetrics[i].setHighRangeRaw(
                            baselineValue.getMaxExpectedValue());

                    if (null != baselineValue.getMinExpectedValue())
                        chartedMetrics[i].setLowRangeRaw(
                            baselineValue.getMinExpectedValue());
                }
            }
        }
    }

    private int _getMaxResources(HttpServletRequest request,
                                 int allResourcesLength) {
        int maxResources = 10;
        String maxResourcesS =
            RequestUtils.message(request,
                                 "resource.common.monitor.visibility.chart" +
                                 ".MaxResources");
        if ( null != maxResourcesS && !maxResourcesS.startsWith("???") ) {
            try {
                maxResources = Integer.parseInt(maxResourcesS);
            } catch (NumberFormatException e) {
                // just use 10
                log.trace("invalid resource.common.monitor.visibility.chart" +
                          ".MaxResources resource: " + maxResourcesS);
            }
        }
        if (maxResources > allResourcesLength) {
            maxResources = allResourcesLength;
        }
        return maxResources;
    }
}

// EOF
