/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.xpath.operations.Bool;
import org.hyperic.hq.appdef.shared.AppdefCompatException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionException;
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
import org.hyperic.hq.measurement.server.session.Baseline;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.beans.ChartedMetricBean;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.image.chart.LineChart;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An <code>Action</code> that retrieves data from the BizApp to facilitate
 * display of the various pages that provide metrics summaries.
 * 
 */
@Component("viewChartFormPrepareActionNG")
public class ViewChartFormPrepareActionNG
    extends MetricDisplayRangeFormPrepareActionNG {
    protected final Log log = LogFactory.getLog(ViewChartFormPrepareActionNG.class.getName());
    @Autowired
    private ConfigurationProxy configurationProxy;
    @Autowired
    private AuthzBoss authzBoss;
    @Autowired
    private AppdefBoss appdefBoss;
    @Autowired
    protected MeasurementBoss measurementBoss;
    @Autowired
    private EventLogBoss eventLogBoss;
    @Autowired
    private DashboardManager dashboardManager;
    
    
    /**
     * Retrieve data needed to display a Metrics Display Form. Respond to
     * certain button clicks that alter the form display.
     */
    public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
        final boolean debug = log.isDebugEnabled();
        request = getServletRequest();
        StopWatch watch = new StopWatch();
        ViewChartFormNG chartForm = null;
        
        if(request.getSession().getAttribute("whole_chart") == null){
        	chartForm = new ViewChartFormNG();
        }else{
        	chartForm = (ViewChartFormNG)request.getSession().getAttribute("whole_chart");
        	request.getSession().removeAttribute("whole_chart");
        }
        chartForm.setShowValues(request.getSession().getAttribute("chartForm_showValues") == null ? true : (Boolean)request.getSession().getAttribute("chartForm_showValues"));
        if(request.getParameter("showValues") != null){
        	chartForm.setShowValues(Boolean.parseBoolean(request.getParameter("showValues")));
        }
        chartForm.setShowPeak(request.getSession().getAttribute("chartForm_showPeak") == null ? true : (Boolean)request.getSession().getAttribute("chartForm_showPeak"));
        if(request.getParameter("showPeak") != null){
        	chartForm.setShowPeak(Boolean.parseBoolean(request.getParameter("showPeak")));
        }
        chartForm.setShowAverage(request.getSession().getAttribute("chartForm_showAverage") == null ? true : (Boolean)request.getSession().getAttribute("chartForm_showAverage"));
        if(request.getParameter("showAverage") != null){
        	chartForm.setShowAverage(Boolean.parseBoolean(request.getParameter("showAverage")));
        }
        chartForm.setShowLow(request.getSession().getAttribute("chartForm_showLow") == null ? true : (Boolean)request.getSession().getAttribute("chartForm_showLow"));
        if(request.getParameter("showLow") != null){
        	chartForm.setShowLow(Boolean.parseBoolean(request.getParameter("showLow")));
        }
        chartForm.setShowBaseline(request.getSession().getAttribute("chartForm_showBaseline") == null ? true : (Boolean)request.getSession().getAttribute("chartForm_showBaseline"));
        if(request.getParameter("showBaseline") != null){
        	chartForm.setShowBaseline(Boolean.parseBoolean(request.getParameter("showBaseline")));
        }
        chartForm.setShowEvents(request.getSession().getAttribute("chartForm_showEvents") == null ? true : (Boolean)request.getSession().getAttribute("chartForm_showEvents"));
        if(request.getParameter("showEvents") != null){
        	chartForm.setShowEvents(Boolean.parseBoolean(request.getParameter("showEvents")));
        }
        chartForm.setShowLowRange(request.getSession().getAttribute("chartForm_showLowRange") == null ? true : (Boolean)request.getSession().getAttribute("chartForm_showLowRange"));
        if(request.getParameter("showLowRange") != null){
        	chartForm.setShowLowRange(Boolean.parseBoolean(request.getParameter("showLowRange")));
        }
        chartForm.setShowHighRange(request.getSession().getAttribute("chartForm_showHighRange") == null ? true : (Boolean)request.getSession().getAttribute("chartForm_showHighRange"));
        if(request.getParameter("showHighRange") != null){
        	chartForm.setShowHighRange(Boolean.parseBoolean(request.getParameter("showHighRange")));
        }
        
        chartForm.setOrigM(request.getSession().getAttribute("chartForm_origM") == null ? null : (Integer[])request.getSession().getAttribute("chartForm_origM"));
        if(request.getParameterValues("origM") != null){
        	chartForm.setOrigM(ArrayUtil.stringToInteger(request.getParameterValues("origM")));
        }
		doExecute(chartForm);

        

        int sessionId;
		try {
			sessionId = RequestUtils.getSessionId(getServletRequest()).intValue();
		} catch (ServletException e2) {
			log.error(e2);
			return;
		}
        ServletContext ctx = getServletRequest().getSession().getServletContext();

        AppdefResourceValue resource = RequestUtils.getResource(getServletRequest());
        if (resource == null) {
        	try {
				removeBadDashboardLink(ctx);
			} catch (SessionTimeoutException e) {
				log.error(e);
			} catch (SessionNotFoundException e) {
				log.error(e);
			} catch (ApplicationException e) {
				log.error(e);
			} catch (RemoteException e) {
				log.error(e);
			}
            return; 
        }

        AppdefEntityID adeId = resource.getEntityId();
        chartForm.setRid(resource.getId());
        chartForm.setType(new Integer(adeId.getType()));

        try {
            AppdefEntityTypeID atid = RequestUtils.getChildResourceTypeId(getServletRequest());
            chartForm.setCtype(atid.getAppdefKey());
        } catch (ParameterNotFoundException e) {
            // This is not an autogroup
        }

        // These private methods have side-effects and must be
        // called in this order. Lame, I know, but I wanted to
        // have this stuff in easier-to-manage code blocks (JW).
        setupDateRange( chartForm);
        setupMetricIds(chartForm);

        if (debug) watch.markTimeBegin("_setupResources");
        AppdefResourceValue[][] resources;
		try {
			resources = setupResources( sessionId, chartForm, resource);
		
        if (debug) watch.markTimeEnd("_setupResources");

        try {
            if (resources.length == 0 || resources[0].length == 0){
            	getServletRequest().setAttribute(Constants.CHART_DATA_KEYS_SIZE, new Integer(1));
                getServletRequest().setAttribute("chartLegend", new ArrayList<List<EventLog>>());
                tilesContext.getSessionScope().put("ViewChartForm", chartForm);
                throw new MeasurementNotFoundException("No resources found for chart");
            }
            if (debug) watch.markTimeBegin("_setupMetricData");
            setupMetricData( sessionId, chartForm, resources[1], ctx);
            if (debug) watch.markTimeEnd("_setupMetricData");
        } catch (MeasurementNotFoundException e) {
             removeBadDashboardLink( ctx);
             return;
        }

        if (debug) watch.markTimeBegin("_setupPageData");
        chartForm.setMode(getServletRequest().getParameter("mode"));
        setupPageData( sessionId, chartForm, resources[0]);
        tilesContext.getSessionScope().put("ViewChartForm", chartForm);
        if (debug) {
          watch.markTimeEnd("_setupPageData");
          log.debug("workflow: " + watch);
        }
		} catch (SessionTimeoutException e1) {
			log.error(e1);
			
		} catch (AppdefEntityNotFoundException e1) {
			log.error(e1);
		} catch (MeasurementNotFoundException e1) {
			log.error(e1);
		} catch (SessionException e1) {
			log.error(e1);
		} catch (PermissionException e1) {
			log.error(e1);
		} catch (RemoteException e1) {
			log.error(e1);
		}catch(ApplicationException e1){
			log.error(e1);
		}
        
    }

    private String removeBadDashboardLink( ServletContext ctx)
        throws SessionTimeoutException, SessionNotFoundException, ApplicationException, RemoteException {
        // This was probably a bad favorites chart
        String query = getServletRequest().getQueryString();
        HttpSession session = getServletRequest().getSession();
        WebUser user = SessionUtils.getWebUser(session);

        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        String userCharts = dashPrefs.getValue(Constants.USER_DASHBOARD_CHARTS);
        List<String> chartList = StringUtil.explode(userCharts, Constants.DASHBOARD_DELIMITER);
        for (Iterator<String> i = chartList.iterator(); i.hasNext();) {
            String chart = i.next();
            if (chart.indexOf(query) > 0) {

                // Remove this and direct user to dash
                userCharts = StringUtil.remove(userCharts, chart);

                dashPrefs.setValue(Constants.USER_DASHBOARD_CHARTS, userCharts);

                configurationProxy.setUserDashboardPreferences(dashPrefs, user);
                getServletRequest().setAttribute("toDashboard", "true");
                return null;
            }
        }
        return null;
    }

    private void setupDateRange( ViewChartFormNG chartForm) {
        // decide what timeframe we're showing. it may have been
        // shifted on previous views of this page.
        MetricRange range = (MetricRange) getServletRequest().getAttribute(Constants.METRIC_RANGE);
        if (null == range) {
            // this is the first time out
            range = new MetricRange();
            range.setBegin(new Long(chartForm.getStartDate().getTime()));
            range.setEnd(new Long(chartForm.getEndDate().getTime()));
            range.shiftNow();
            getServletRequest().setAttribute(Constants.METRIC_RANGE, range);
        }

        // Since we have two ways to adjust the range of data we're
        // looking at (paging back and forth and explicitly choosing
        // something in the display range tile), we will try to always
        // keep the display range tile "up to date". That is, if the
        // end date is "now", we'll select "last n" and otherwise
        // we'll select "date range".
        chartForm.synchronizeDisplayRange();
    }

    private void setupMetricIds( ViewChartFormNG chartForm) {
        // metric ids
        String[] metricTemplateIds = getServletRequest().getParameterValues(Constants.METRIC_PARAM);
        Integer[] m = ArrayUtil.stringToInteger(metricTemplateIds);
        chartForm.setM(m);

        // originally-selected metric ids
        String[] origMetricTemplateIds = getServletRequest().getParameterValues("origM");
                
        if (null == origMetricTemplateIds || origMetricTemplateIds.length == 0) {
        	if(getServletRequest().getSession().getAttribute("chartForm_origM") != null){
        		chartForm.setOrigM((Integer[]) getServletRequest().getSession().getAttribute("chartForm_origM"));
        		chartForm.setM(chartForm.getOrigM().clone());
        		getServletRequest().getSession().removeAttribute("chartForm_origM");
        	}else{
        		chartForm.setOrigM((Integer[]) chartForm.getM().clone());
        	}
        } else {
            Integer[] origM = ArrayUtil.stringToInteger(origMetricTemplateIds);
            chartForm.setOrigM(origM);
        }
    }

    private AppdefResourceValue[][] setupResources(int sessionId, ViewChartFormNG chartForm,
                                                   AppdefResourceValue resource) throws SessionTimeoutException,
        SessionException, AppdefEntityNotFoundException, PermissionException, RemoteException,
        MeasurementNotFoundException {

        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        // get list of all child resources
        AppdefResourceValue[] resources = null;
        if (null != chartForm.getCtype() && !chartForm.getCtype().equals(ViewChartFormNG.NO_CHILD_TYPE)) {
            AppdefEntityID adeId = resource.getEntityId();

            AppdefEntityTypeID atid = new AppdefEntityTypeID(chartForm.getCtype());
            if (debug) watch.markTimeBegin("findChildResources");
            PageList<? extends AppdefResourceValue> children = appdefBoss.findChildResources(sessionId, adeId, atid,
                PageControl.PAGE_ALL);
            if (debug) watch.markTimeEnd("findChildResources");
            String[] rids = getServletRequest().getParameterValues("r");
            Integer[] r = ArrayUtil.stringToInteger(rids);
            // if we've been passed a list of resource ids, we are
            // comparing metrics and need to prune out all but the
            // resources corresponding to the passed-in resource ids
            if (null != r) {
                log.debug("r=" + StringUtil.arrayToString(r));
                for (Iterator<? extends AppdefResourceValue> it = children.iterator(); it.hasNext();) {
                    AppdefResourceValue res = it.next();
                    boolean found = false;
                    for (int i = 0; i < r.length; ++i) {
                        if (found = res.getId().equals(r[i]))
                            break;
                    }

                    if (!found) {
                        log.debug("removing resource: " + res.getId());
                        it.remove();
                    }
                }
            }
            resources = new AppdefResourceValue[children.size()];
            resources = (AppdefResourceValue[]) children.toArray(resources);
        } else if (resource instanceof AppdefGroupValue) {
            AppdefGroupValue grpVal = (AppdefGroupValue) resource;
            AppdefEntityID[] grpMembers = new AppdefEntityID[grpVal.getAppdefGroupEntries().size()];
            grpMembers = (AppdefEntityID[]) grpVal.getAppdefGroupEntries().toArray(grpMembers);
            if (debug) watch.markTimeBegin("findByIds");
            List<AppdefResourceValue> memVals = appdefBoss.findByIds(sessionId, grpMembers, PageControl.PAGE_ALL);
            if (debug) watch.markTimeEnd("findByIds");
            resources = new AppdefResourceValue[memVals.size()];
            resources = memVals.toArray(resources);
        } else {
            resources = new AppdefResourceValue[] { resource };
        }
        if (debug) watch.markTimeBegin("pruneResourcesNotCollecting");
        resources = measurementBoss.pruneResourcesNotCollecting(sessionId, resources, chartForm.getM()[0]);
        if (debug) watch.markTimeEnd("pruneResourcesNotCollecting");
        getServletRequest().setAttribute("resources", resources);
        getServletRequest().setAttribute("resourcesSize", new Integer(resources.length));

        // if no specific resourceIds were checked, checkedResources
        // is the same as resources and chartForm.resourceIds contains
        // all resource ids
        String[] resourceIds = getServletRequest().getParameterValues("resourceIds");
        if(resourceIds == null && request.getSession().getAttribute("chartForm_resourceIds")!= null){
        	resourceIds = (String[]) request.getSession().getAttribute("chartForm_resourceIds");
        	request.getSession().removeAttribute("chartForm_resourceIds");
        }
        AppdefResourceValue[] checkedResources = null;
        if (debug) watch.markTimeBegin("checkedResources");
        if (null == resourceIds || resourceIds.length == 0) {
            int maxResources = getMaxResources(getServletRequest(), resources.length);
            log.debug("maxResources=" + maxResources);
            checkedResources = new AppdefResourceValue[maxResources];
            System.arraycopy(resources, 0, checkedResources, 0, maxResources);
            Integer[] rids = new Integer[checkedResources.length];
            for (int i = 0; i < rids.length; ++i) {
                rids[i] = checkedResources[i].getId();
            }
            chartForm.setResourceIds(rids);
            if (debug) {
                log.debug("no resourceIds specified: " + StringUtil.arrayToString(rids));
            }
            getServletRequest().setAttribute("checkedResources", checkedResources);
            getServletRequest().setAttribute("checkedResourcesSize", new Integer(checkedResources.length));
           
        } else {
            Integer[] rids = new Integer[resourceIds.length];
            for(int ind =0;ind < resourceIds.length;++ind){
            	rids[ind] = Integer.parseInt(resourceIds[ind]);
            }
            checkedResources = new AppdefResourceValue[rids.length];
            for (int i = 0; i < rids.length; ++i) {
                for (int j = 0; j < resources.length; ++j) {
                    if (resources[j].getId().equals(rids[i])) {
                        checkedResources[i] = resources[j];
                    }
                }
            }
            if (debug) {
                log.debug("resourceIds specified: " + org.hyperic.util.StringUtil.arrayToString(rids));
            }
            getServletRequest().setAttribute("checkedResources", checkedResources);
            getServletRequest().setAttribute("checkedResourcesSize", new Integer(checkedResources.length));
           
        }
        if (debug) {
          watch.markTimeEnd("checkedResources");
           log.debug("_setupResources: " + watch);
         }
        return new AppdefResourceValue[][] {resources, checkedResources};
    }

    private void setupMetricData( int sessionId, ViewChartFormNG chartForm,
                                 AppdefResourceValue[] resources, ServletContext ctx) throws SessionNotFoundException,
        SessionTimeoutException, MeasurementNotFoundException, RemoteException, AppdefEntityNotFoundException,
        PermissionException {

        final boolean debug = log.isDebugEnabled();
        List<List<EventLog>> eventPointsList = new ArrayList<List<EventLog>>(resources.length);

        // Get data for charts and put it in session. In reality only
        // one of either resources or metrics can have more than one
        // entry, so it's really not as much of a nested loop as it
        // seems. However, the code is written this way so that it
        // can be used in both the multi-resource and the multi-metric
        // case.
        // data points for chart
        Integer m[] = chartForm.getM();
        String[] chartDataKeys = new String[m.length];
        if (debug) log.debug("number of metrics: " + m.length);

        for (int i = 0; i < m.length; ++i) {
            // Use the current time concatenated with metric
            // template id for key.
            chartDataKeys[i] = String.valueOf(System.currentTimeMillis()) + m[i];

            for (int j = 0; j < resources.length; ++j) {
                if (debug) {
                    log.debug("mtid=" + m[i] + ", rid=" + resources[j].getId());
                    log.debug("startDate=" + chartForm.getStartDate());
                    log.debug("endDate=" + chartForm.getEndDate());
                }

                long interval = TimeUtil.getInterval(chartForm.getStartDate().getTime(), chartForm.getEndDate()
                    .getTime(), Constants.DEFAULT_CHART_POINTS);

                if (i == 0) {
                    if (interval > 0) {
                        List<EventLog> controlActions = eventLogBoss.getEvents(sessionId, ControlEvent.class.getName(),
                            resources[j].getEntityId(), chartForm.getStartDate().getTime(), chartForm.getEndDate()
                                .getTime());
                        // We need to make sure that the event IDs get set
                        // for the legend.
                        int k = 0;
                        for (EventLog event : controlActions) {

                            event.setEventID(++k);
                        }
                        eventPointsList.add(controlActions);
                    }
                }
            }
            if (debug) log.debug("Store into session: " + chartDataKeys[i]);
        }
        getServletRequest().setAttribute(Constants.CHART_DATA_KEYS_SIZE, new Integer(chartDataKeys.length));
        getServletRequest().setAttribute("chartLegend", eventPointsList);
    }

    private static final class BaseMetricDisplayComparator implements Comparator<BaseMetricDisplay> {
        public int compare(BaseMetricDisplay bmd1, BaseMetricDisplay bmd2) {

            return bmd1.getLabel().compareTo(bmd2.getLabel());
        }

        public boolean equals(Object obj) {
            return obj instanceof BaseMetricDisplayComparator;
        }
    }

    protected static final BaseMetricDisplayComparator comp = new BaseMetricDisplayComparator();

    protected void setupPageData( int sessionId, ViewChartFormNG chartForm,
                                 AppdefResourceValue[] resources) throws SessionTimeoutException,
        SessionNotFoundException, AppdefEntityNotFoundException, PermissionException, AppdefCompatException,
        RemoteException, MeasurementNotFoundException, BaselineCreationException {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        List<Integer> mtids = Arrays.asList(chartForm.getOrigM());
        ArrayList<MetricDisplaySummary> metricSummaries = new ArrayList<MetricDisplaySummary>();
        if (debug) watch.markTimeBegin("findMetrics");
        for (int i = 0; i < resources.length; ++i) {
            Map<String, Set<MetricDisplaySummary>> metrics = measurementBoss.findMetrics(sessionId, resources[i]
                .getEntityId(), mtids, chartForm.getStartDate().getTime(), chartForm.getEndDate().getTime());
           
            for (Iterator<Set<MetricDisplaySummary>> it = metrics.values().iterator(); it.hasNext();) {
                metricSummaries.addAll(it.next());
            }
            
            if ((Constants.MODE_MON_CHART_SMMR.equals(chartForm.getMode()) || Constants.MODE_MON_CHART_SMMR.equals(getServletRequest().getParameter("mode"))) 
            		&& !metricSummaries.isEmpty()) {
                // HQ-1916: For SMMR charts, get the metric display summary
                // for only the first resource
                break;
             }
        }
        Collections.sort(metricSummaries, comp);
        getServletRequest().setAttribute("metricSummaries", metricSummaries);
        getServletRequest().setAttribute("metricSummariesSize", new Integer(metricSummaries.size()));
        
        if (debug) {
          watch.markTimeEnd("findMetrics");
          watch.markTimeBegin("chartedMetrics");
        }

        // pick out the charted metrics from the metric summaries
        ChartedMetricBean[] chartedMetrics = new ChartedMetricBean[chartForm.getM().length];
        for (int i = 0; i < chartedMetrics.length; ++i) {
            for (int j = 0; j < metricSummaries.size(); ++j) {
                MetricDisplaySummary mds = (MetricDisplaySummary) metricSummaries.get(j);
                if (mds.getTemplateId().equals(chartForm.getM()[i])) {
                    int unitUnits = UnitsConvert.getUnitForUnit(mds.getUnits());
                    int unitScale = UnitsConvert.getScaleForUnit(mds.getUnits());
                    chartedMetrics[i] = new ChartedMetricBean(mds.getLabel(), unitUnits, unitScale, mds
                        .getCollectionType().intValue(), mds.getTemplateId());
                    break;
                }
            }
        }
        if (debug) watch.markTimeEnd("chartedMetrics");
        getServletRequest().setAttribute("chartedMetrics", chartedMetrics);

        if (chartedMetrics.length > 0 && chartedMetrics[0] != null) {
          watch.markTimeBegin("_setupBaselineExpectedRange");
          setupBaselineExpectedRange( sessionId, chartForm, resources, chartedMetrics);
          watch.markTimeEnd("_setupBaselineExpectedRange");           
        }
        
        if (debug) {
            log.debug("_setupPageData: " + watch);
        }
    }

    protected void setupBaselineExpectedRange( int sessionId, ViewChartFormNG chartForm,
                                              AppdefResourceValue[] resources, ChartedMetricBean[] chartedMetrics)
        throws SessionTimeoutException, SessionNotFoundException, MeasurementNotFoundException,
        BaselineCreationException, PermissionException, AppdefEntityNotFoundException, RemoteException {
        Measurement m = null;

        if (chartForm.getMode().equals(Constants.MODE_MON_CHART_SMSR) ||
            chartForm.getMode().equals(Constants.MODE_MON_CHART_SMMR)) {
            m = measurementBoss.findMeasurement(sessionId, chartForm.getM()[0], resources[0].getEntityId());
            getServletRequest().setAttribute("metric", m);

            // Set the name to be displayed
            chartForm.setChartName(m.getTemplate().getName());
        } else if (chartForm.getMode().equals(Constants.MODE_MON_CHART_MMSR)) {
            AppdefEntityID aeid = resources[0].getEntityId();
            for (int i = 0; i < chartedMetrics.length; i++) {
                if (chartedMetrics[i] == null)
                    continue;

                m = measurementBoss.findMeasurement(sessionId, chartedMetrics[i].getTemplateId(), aeid);
                if (null != m) {
                    Baseline baselineValue = m.getBaseline();
                    if (null != baselineValue) {
                        if (null != baselineValue.getMean())
                            chartedMetrics[i].setBaselineRaw(baselineValue.getMean());

                        if (null != baselineValue.getMaxExpectedVal())
                            chartedMetrics[i].setHighRangeRaw(baselineValue.getMaxExpectedVal());

                        if (null != baselineValue.getMinExpectedVal())
                            chartedMetrics[i].setLowRangeRaw(baselineValue.getMinExpectedVal());
                    }
                }
            }
        }
    }

    private int getMaxResources(HttpServletRequest request, int allResourcesLength) {
        int maxResources = LineChart.getNumColors();
		String maxResourcesS = RequestUtils.message("resource.common.monitor.visibility.chart"
                                                             + ".MaxResources");
        if (null != maxResourcesS && !maxResourcesS.startsWith("???")) {
            try {
                maxResources = Math.min(maxResources, Integer.parseInt(maxResourcesS));
            } catch (NumberFormatException e) {
                // just use 10
                log.trace("invalid resource.common.monitor.visibility.chart" + ".MaxResources resource: " +
                          maxResourcesS);
            }
        }
        if (maxResources > allResourcesLength) {
            maxResources = allResourcesLength;
        }
        return maxResources;
    }
}
