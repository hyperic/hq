<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="hq" prefix="hq" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
  This file is part of HQ.
  
  HQ is free software; you can redistribute it and/or modify
  it under the terms version 2 of the GNU General Public License as
  published by the Free Software Foundation. This program is distributed
  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE. See the GNU General Public License for more
  details.
  
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
  USA.
 --%>


<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="buttonMode" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="ctype" ignore="true"/>
<tiles:importAttribute name="useChart" ignore="true"/>
<tiles:importAttribute name="useChartMulti" ignore="true"/>
<tiles:importAttribute name="useCurrent" ignore="true"/>
<tiles:importAttribute name="useConfigure" ignore="true"/>
<tiles:importAttribute name="useCheckboxes" ignore="true"/>
<tiles:importAttribute name="favorites" ignore="true"/>

<script  src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<c:set var="mdsWidget" value="metricsDisplaySummary"/>
<script type="text/javascript">
initializeWidgetProperties('<c:out value="${mdsWidget}"/>');
mdsWidgetProps = getWidgetProperties('<c:out value="${mdsWidget}"/>');
</script>
<hq:constant 
    classname="org.hyperic.hq.measurement.MeasurementConstants" 
    symbol="CAT_AVAILABILITY" var="availability" />
<hq:constant 
    classname="org.hyperic.hq.measurement.MeasurementConstants" 
    symbol="CAT_PERFORMANCE" var="performance" />
<hq:constant 
    classname="org.hyperic.hq.measurement.MeasurementConstants" 
    symbol="CAT_THROUGHPUT" var="throughput" />
<hq:constant 
    classname="org.hyperic.hq.measurement.MeasurementConstants" 
    symbol="CAT_UTILIZATION" var="utilization" />

<c:if test="${empty useChart}">
  <c:set var="useChart" value="true"/>
</c:if>
<%-- 
the group metrics page turns this off, since each metric is 
multi-resource backed 
--%>
<c:if test="${empty useChartMulti}">
  <c:set var="useChartMulti" value="true"/>
</c:if>

<%--
sometimes we don't want any left side buttons or checkboxes at all
--%>
<c:if test="${not empty buttonMode && buttonMode eq 'noleft'}">
  <c:set var="useChartMulti" value="false"/>
</c:if>
<c:if test="${empty buttonMode}">
  <c:set var="buttonMode" value="baselines"/>
</c:if>

<c:if test="${empty useCheckboxes}">
  <c:set var="useCheckboxes" value="true"/>
</c:if>

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="MonitorBlock">
  <tr>
  <c:choose>
  <c:when test="${not empty useConfigure && not useConfigure}">
    <td width="100%">
  </c:when>
  <c:otherwise>
    <td width="20%">
      <tiles:insert definition=".resource.common.monitor.visibility.metricsSeeMore">
        <tiles:put name="widgetInstanceName" beanName="mdsWidget"/>
        <c:if test="${not empty useConfigure}">
          <tiles:put name="useConfigureButton" beanName="useConfigure"/>
        </c:if>
        <c:if test="${not empty childResourceType}">
          <tiles:put name="childResourceType" beanName="childResourceType"/>
        </c:if>
        <c:if test="${not empty ctype}">
          <tiles:put name="ctype" beanName="ctype"/>
        </c:if>
      </tiles:insert>
    </td>
    <td valign="top">
    <table width="100%" cellpadding="5" cellspacing="0" border="0" class="MonitorToolBar">
      <tr>
        <td width="100%" align="center" nowrap>
        <div id="UpdatedTime" style="color: grey">&nbsp;</div>
        </td>
<c:if test="${useCurrent && not MetricsDisplayForm.readOnly}">
        <td align="right" nowrap>
        <script type="text/javascript">
        <!--
          var liveUpdate = true;
          var refreshInterval = 120;

          function resetAll() {
            dojo.byId('refresh60').innerHTML = '<a href="javascript:refresh60()"><fmt:message key="resource.common.monitor.visibility.MetricRefresh.60"/></a>';
            dojo.byId('refresh120').innerHTML = '<a href="javascript:refresh120()"><fmt:message key="resource.common.monitor.visibility.MetricRefresh.120"/></a>';
            dojo.byId('refresh300').innerHTML = '<a href="javascript:refresh300()"><fmt:message key="resource.common.monitor.visibility.MetricRefresh.300"/></a>';
            dojo.byId('refreshOff').innerHTML  = '<a href="javascript:refreshOff()"><fmt:message key="OFF"/>';
          }

          function refresh60() {
            resetAll();

            liveUpdate = true;
            refreshInterval = 60;
            dojo.byId('refresh60').innerHTML = '<fmt:message key="resource.common.monitor.visibility.MetricRefresh.60"/>';
          }

          function refresh120() {
            resetAll();

            liveUpdate = true;
            refreshInterval = 120;
            dojo.byId('refresh120').innerHTML = '<fmt:message key="resource.common.monitor.visibility.MetricRefresh.120"/>';
          }

          function refresh300() {
            resetAll();

            liveUpdate = true;
            refreshInterval = 300;
            dojo.byId('refresh300').innerHTML = '<fmt:message key="resource.common.monitor.visibility.MetricRefresh.300"/>';
          }

          function refreshOff() {
            resetAll();

            liveUpdate = null;
            dojo.byId('refreshOff').innerHTML = "<fmt:message key="OFF"/>";
          }

          function initLiveMetrics() {
            metricsUpdater = new MetricsUpdater();
            //ajaxEngine.registerRequest( 'getLiveMetrics', '<html:rewrite page="/resource/common/monitor/visibility/CurrentMetricValues.do"/>');
            ajaxEngine.registerAjaxObject( 'metricsUpdater', metricsUpdater );
            setMetricsRefresh();
          }

          onloads.push( initLiveMetrics );
          -->
        </script>
        <span id="CurrentValuesLabel">
          <fmt:message key="resource.common.monitor.visibility.MetricRefreshLabel"/>
        </span>
        <span id="refresh60"><a href="javascript:refresh60()">1 min</a></span>
        |
        <span id="refresh120">2 min</span>
        |
        <span id="refresh300"><a href="javascript:refresh300()">5 min</a></span>
        |
        <span id="refreshOff"><a href="javascript:refreshOff()"><fmt:message key="OFF"/></a></span>
        </td>
</c:if>
      </tr>
    </table>
    </td>
  </tr>
  <tr>
    <td colspan="3">
  </c:otherwise>
  </c:choose>

<c:choose>
<c:when test="${not empty summaries && MetricsDisplayForm.categoryListSize gt 0}">
  <%--
  Figure out what we're gonna show first, since the table heading row is
  going to be different from all of the others
  --%>
  <c:choose>
    <c:when test="${MetricsDisplayForm.categoryList[0] == availability}">
    <c:set var="firstMetricsCategory" value="${availability}" />
    <c:set var="firstHeading" 
        value="resource.common.monitor.visibility.AvailabilityTH" />
    </c:when>
    <c:when test="${MetricsDisplayForm.categoryList[0] == performance}">
    <c:set var="firstMetricsCategory" value="${performance}" />
    <c:set var="firstHeading" 
        value="resource.common.monitor.visibility.PerformanceTH" />
    </c:when>
    <c:when test="${MetricsDisplayForm.categoryList[0] == throughput}">
    <c:set var="firstMetricsCategory" value="${throughput}" />
    <c:set var="firstHeading" 
        value="resource.common.monitor.visibility.UsageTH" />
    </c:when>
    <c:when test="${MetricsDisplayForm.categoryList[0] == utilization}">
    <c:set var="firstMetricsCategory" value="${utilization}" />
    <c:set var="firstHeading" 
        value="resource.common.monitor.visibility.UtilizationTH" />
    </c:when>
    <c:otherwise>
    <blockquote>
    Error: unknown metric category 
    <c:out value="${MetricsDisplayForm.categoryList[0]}" />
    </blockquote>
    </c:otherwise>
  </c:choose>
<c:set var="firstMetricData" value="${summaries[firstMetricsCategory]}" />
<c:set var="metrics" value="${summaries[firstMetricsCategory]}" />

<div id="listDiv">
  <table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">

    <tr class="ListHeaderDark">
      <td width="1%" class="ListHeaderInactiveSorted">
<c:choose>
  <c:when test="${useCheckboxes}">
    <input type="checkbox" name="availableListMemberAll" onclick="ToggleAllSelectionTwoButtons(this, mdsWidgetProps, 'availableListMember', '<c:out value="${buttonMode}"/>');">
    <c:if test="${useChart}">
      <c:set var="colspan" value="colspan=\"2\""/>
    </c:if>
  </c:when>
  <c:otherwise>&nbsp;</c:otherwise>
</c:choose>
      </td>
      <td width="40%" <c:out value="${colspan}" escapeXml="false"/> class="ListHeaderInactiveSorted"><fmt:message key="${firstHeading}"/></td>
      <c:if test="${MetricsDisplayForm.showMetricSource}">
      <td width="10%" class="ListHeaderInactiveSorted" align="center">
      <fmt:message key="resource.common.monitor.visibility.MetricSourceTH"/>
      </td>
      </c:if>
      <c:if test="${MetricsDisplayForm.showNumberCollecting}">
      <td width="5%" class="ListHeaderInactiveSorted" align="center" nowrap="true">
      <fmt:message key="resource.common.monitor.visibility.CollectingTH"/>
      </td>
      </c:if>
      <td width="5%" class="ListHeaderInactiveSorted" align="middle"><fmt:message key="resource.common.monitor.visibility.LowTH"/></td>
      <td width="5%" class="ListHeaderInactiveSorted" align="middle"><fmt:message key="resource.common.monitor.visibility.AvgTH"/></td>
      <td width="5%" class="ListHeaderInactiveSorted" align="middle"><fmt:message key="resource.common.monitor.visibility.PeakTH"/></td>
      <c:choose>
      <c:when test="${MetricsDisplayForm.showNumberCollecting}">
      <td width="5%" class="ListHeaderInactiveSorted" align="middle"><fmt:message key="resource.common.monitor.visibility.SumTH"/></td>
      </td>
      </c:when>
      <c:otherwise>
      <td width="5%" class="ListHeaderInactiveSorted" align="middle"><fmt:message key="resource.common.monitor.visibility.LastTH"/></td>
      </c:otherwise>
      </c:choose>
      <td width="5%" class="ListHeaderInactiveSorted" align="middle"><fmt:message key="resource.common.monitor.visibility.config.CollectionIntervalTH"/></td>
      <td width="1%" class="ListHeaderInactiveSorted">&nbsp;</td>
    </tr>

    <tiles:insert definition=".resource.common.monitor.visibility.metricsDisplay.row">
      <tiles:put name="rows" beanName="firstMetricData"/>
      <tiles:put name="useChart" beanName="useChart"/>
      <tiles:put name="category" beanName="firstMetricsCategory"/>
      <tiles:put name="buttonMode" beanName="buttonMode"/>
      <tiles:put name="useCheckboxes" beanName="useCheckboxes"/>
      <c:if test="${not empty childResourceType}">
        <tiles:put name="childResourceType" beanName="childResourceType"/>
      </c:if>
      <c:if test="${not empty ctype}">
        <tiles:put name="ctype" beanName="ctype"/>
      </c:if>
    </tiles:insert>  
    <%-- now do the other categories --%>
    <c:forEach var="category" items="${MetricsDisplayForm.categoryList}">
     <c:if test="${category != firstMetricsCategory}">
      <c:choose>
        <c:when test="${category == availability}">
        <c:set var="metrics" value="${summaries[availability]}" />
        <c:set var="heading" 
            value="resource.common.monitor.visibility.AvailabilityTH" />
        </c:when>
        <c:when test="${category == performance}">
        <c:set var="metrics" value="${summaries[performance]}" />
        <c:set var="heading" 
            value="resource.common.monitor.visibility.PerformanceTH" />
        </c:when>
        <c:when test="${category == throughput}">
        <c:set var="metrics" value="${summaries[throughput]}" />
        <c:set var="heading" 
            value="resource.common.monitor.visibility.UsageTH" />
        </c:when>
        <c:when test="${category == utilization}">
        <c:set var="metrics" value="${summaries[utilization]}" />
        <c:set var="heading" 
            value="resource.common.monitor.visibility.UtilizationTH" />
        </c:when>
      </c:choose>
    <tr>
      <td width="1%" class="ListCellHeader"><span style="width: 1px; height: 1px;"></span></td>
      <td colspan="10" class="ListCellHeader"><fmt:message key="${heading}"/></td>
    </tr>
    <tiles:insert definition=".resource.common.monitor.visibility.metricsDisplay.row">
      <tiles:put name="rows" beanName="metrics"/>
      <tiles:put name="useChart" beanName="useChart"/>
      <tiles:put name="category" beanName="category"/>
      <tiles:put name="buttonMode" beanName="buttonMode"/>
      <tiles:put name="useCheckboxes" beanName="useCheckboxes"/>
      <c:if test="${not empty childResourceType}">
      <tiles:put name="childResourceType" beanName="childResourceType"/>
      </c:if>
      <c:if test="${not empty ctype}">
        <tiles:put name="ctype" beanName="ctype"/>
      </c:if>
    </tiles:insert>  
   </c:if>
  </c:forEach>
  </table>

<c:if test="${not IsResourceUnconfigured}">
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="ToolbarContent">
<tr><td>
<tiles:insert definition=".resource.common.monitor.visibility.metricsToolbar">
  <tiles:put name="widgetInstanceName" beanName="mdsWidget"/>
<c:choose>
  <c:when test="${buttonMode eq 'add'}">
    <tiles:put name="useAddButton" value="true"/>
  </c:when>
  <c:when test="${buttonMode eq 'remove'}">
    <tiles:put name="useRemoveButton" value="true"/>
  </c:when>
  <c:when test="${buttonMode eq 'baselines'}">
    <tiles:put name="useBaselinesButtons" value="true"/>
    <tiles:put name="useAddButton" value="false"/>
    <tiles:put name="useRemoveButton" value="false"/>
  </c:when>
  <c:when test="${buttonMode eq 'noleft'}">
    <tiles:put name="useAddButton" value="false"/>
    <tiles:put name="useRemoveButton" value="false"/>
    <tiles:put name="useBaselinesButtons" value="false"/>
  </c:when>
</c:choose>
  <tiles:put name="useChartButton" beanName="useChartMulti"/>
</tiles:insert>  

</td><td>
<tiles:insert definition=".resource.common.monitor.config.toolbar.addToList">
  <tiles:put name="showAddToListBtn" value="false"/>
  <tiles:put name="useDisableBtn" value="true"/>
  <tiles:put name="widgetInstanceName" beanName="mdsWidget"/>
  <tiles:put name="listItems" beanName="metrics"/>
  <tiles:put name="listSize" value="0"/>
  <%--
  When derived metrics are exposed through this UI, then the list can
  grow long and the pagination will be necessary (and will need to be
  fixed, since it wasn't working anyway).  For now, we'll suppress the
  pagination controls per PR 7821
  --%>
  <tiles:put name="showPagingControls" value="false"/>
  <tiles:put name="pageSizeParam" value="ps"/>
  <tiles:put name="pageSizeAction" beanName="selfAction"/>
  <tiles:put name="pageNumAction" beanName="selfAction"/>
  <tiles:put name="defaultSortColumn" value="4"/>
</tiles:insert>
</td></tr>
</table>
</c:if>

</div>

</c:when>
<c:otherwise>
<tiles:insert definition=".resource.common.monitor.visibility.noMetrics">
  <c:if test="${not empty favorites}">
    <tiles:put name="favorites" beanName="favorites"/>
  </c:if>
</tiles:insert>
</c:otherwise>
</c:choose>

    </td>
  </tr>
</table>

<c:if test="${useCheckboxes}">
<script type="text/javascript">
  clearIfAnyChecked('m');
</script>
</c:if>
