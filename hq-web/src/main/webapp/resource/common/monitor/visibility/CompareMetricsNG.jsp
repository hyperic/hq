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

<%-- start vit: delete this block --%>
<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>



<jsu:importScript path="/js/functions.js" />
<jsu:importScript path="/js/listWidget.js" />
<link rel=stylesheet href="<s:url value="/css/win.css"/>" type="text/css">
<%-- end vit: delete this block --%>
<c:set var="widgetInstanceName" value="compareMetrics"/>
<jsu:script>
	var imagePath = "/images/";
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<hq:constant 
  classname="org.hyperic.hq.measurement.MeasurementConstants" 
  symbol="COLL_TYPE_DYNAMIC"
  var="dynamic"/>
<hq:constant 
  classname="org.hyperic.hq.measurement.MeasurementConstants" 
  symbol="COLL_TYPE_STATIC"
  var="static"/>
<hq:constant 
  classname="org.hyperic.hq.measurement.MeasurementConstants" 
  symbol="COLL_TYPE_TRENDSUP"
  var="trendsup"/>
<hq:constant 
  classname="org.hyperic.hq.measurement.MeasurementConstants" 
  symbol="COLL_TYPE_TRENDSDOWN"
  var="trendsdown"/>

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
<hq:constant 
  classname="org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants" 
  symbol="MAX_KEY"
  var="max"/>
<hq:constant 
  classname="org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants" 
  symbol="MIN_KEY"
  var="min"/>
<hq:constant 
  classname="org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants" 
  symbol="AVERAGE_KEY"
  var="average"/>
<hq:constant 
  classname="org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants" 
  symbol="LAST_KEY"
  var="last"/>
<!--  COMPARE METRICS TITLE -->
<c:set var="titleName" value="${CompareMetricsForm.name}" />
<tiles:insertDefinition name=".page.title.resource.generic">
  <tiles:putAttribute name="titleKey" value="resource.common.monitor.visibility.CompareMetricsTitle"/>
  <tiles:putAttribute name="titleName" value="${titleName}" />
</tiles:insertDefinition>
<s:form name="metricsControlAction" id="metricsControlAction" action="compareMetricscommonVisibilityPortal">

<s:a href="resourceMetricsMonitorCompatGroupVisibility.action?mode=resourceMetrics&eid=%{#attr.eid}" onclick="clickLink('CompareMetricsForm', 'back')">
	<fmt:message key="resource.common.monitor.visibility.CompareMetricsReturnLink">
		<fmt:param>
			<c:out value="${CompareMetricsForm.name}"/>
		</fmt:param>
	</fmt:message>
</s:a>
<div id="listDiv" style="padding-top: 24px; padding-bottom: 24px;">
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.common.monitor.visibility.CompareMetricsTab"/>
</tiles:insertDefinition>
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
<c:forEach var="category" items="${CompareMetricsForm.metrics}">
  <c:choose>
    <c:when test="${category.key == availability}">
    <c:set var="heading" 
        value="resource.common.monitor.visibility.AvailabilityTH" />
    </c:when>
    <c:when test="${category.key == performance}">
    <c:set var="heading" 
        value="resource.common.monitor.visibility.PerformanceTH" />
    </c:when>
    <c:when test="${category.key == throughput}">
    <c:set var="heading" 
        value="resource.common.monitor.visibility.UsageTH" />
    </c:when>
    <c:when test="${category.key == utilization}">
    <c:set var="heading" 
        value="resource.common.monitor.visibility.UtilizationTH" />
    </c:when>
    <c:otherwise>
    <blockquote>
    Error: unknown metric category 
    <c:out value="${MetricsDisplayForm.categoryList[0]}" />
    </blockquote>
    </c:otherwise>
  </c:choose>
<tr class="ListHeaderDark">
<td width="1%" class="ListHeaderInactiveSorted">&nbsp;</td>
<td width="68%" colspan="2" class="ListHeaderInactiveSorted"><fmt:message key="${heading}"/></td>
<td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message key="resource.common.monitor.visibility.LowTH"/></td>
<td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message key="resource.common.monitor.visibility.AvgTH"/></td>
<td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message key="resource.common.monitor.visibility.PeakTH"/></td>
<td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message key="resource.common.monitor.visibility.LastTH"/></td>
</tr>

<c:forEach var="metricList" items="${category.value}">
  <c:url var="chartUrl" value="chartSingleMetricMultiResourcecommonVisibilityPortal.action">
    <c:param name="type" value="${CompareMetricsForm.type}" />
    <c:param name="rid" value="${CompareMetricsForm.rid}" />
    <c:param name="mode" value="chartSingleMetricMultiResource" />
    <c:param name="m" value="${metricList.key.id}" />
    <c:forEach var="rmds" items="${metricList.value}">
    <c:param name="r" value="${rmds.resource.id}" />
    </c:forEach>
  </c:url>
    <tr class="ListRow">
      <td class="ListCellCheckbox">&nbsp;</td>
      <td width="1%" class="ListCellCheckbox"><a href="${chartUrl}"><img src='<s:url value="/images/icon_chart.gif"/>' width="10" height="10" alt="" border="0"/></a></td>
      <td class="ListCell"><a href="${chartUrl}">${metricList.key.name}</a></td>
      <td class="ListCellRight" nowrap>&nbsp;</td>
      <td class="ListCellRight" nowrap>&nbsp;</td>
      <td class="ListCellRight" nowrap>&nbsp;</td>
      <td class="ListCellRight" nowrap>&nbsp;</td>
    </tr>
    <!-- iterate over the resources -->
  <c:forEach var="rmds" items="${metricList.value}">
    <tr class="ListRow">
      <td class="ListCellCheckbox">&nbsp;</td>
      <td width="1%" class="ListCellCheckbox">&nbsp;</td>
      <td class="ListCellCompareMetrics"><c:out value="${rmds.resource.name}" /></td>
      <c:choose>
      <c:when test="${rmds.designated && category.key == availability}">
        <td class="ListCellRight" width="%5" nowrap> &nbsp; </td>
        <td class="ListCellRight" width="%5" nowrap><c:out value="${rmds.metrics[average].valueFmt}" /></td>
        <td class="ListCellRight" width="%5" nowrap> &nbsp; </td>
        <td class="ListCellCheckboxLeftLine" width="%5" nowrap>
          <c:choose>
            <c:when test="${rmds.metrics[last].value == 1}">
            <img src='<s:url value="/images/icon_available_green.gif"/>' width="12" height="12" alt="" border="0" align="middle"/>
            </c:when>
            <c:when test="${rmds.metrics[last].value == 0}">
            <img src='<s:url value="/images/icon_available_red.gif"/>' width="12" height="12" alt="" border="0" align="middle"/>
            </c:when>
            <c:when test="${rmds.metrics[last].value == -0.01}">
            <img src='<s:url value="/images/icon_available_orange.gif"/>' width="12" height="12" alt="" border="0" align="middle"/>
            </c:when>
            <c:when test="${rmds.metrics[last].value == -0.02}">
            <img src='<s:url value="/images/icon_available_black.gif"/>' width="12" height="12" alt="" border="0" align="middle"/>
            </c:when>
            <c:otherwise>
            <img src='<s:url value="/images/icon_available_yellow.gif"/>' width="12" height="12" alt="" border="0" align="middle"/>
            </c:otherwise>
            </c:choose>
       </c:when>
       <c:when test="${rmds.collectionType == dynamic}">
    <td class="ListCellRight" width="%5" nowrap><c:out value="${rmds.metrics[min].valueFmt}" /></td>
    <td class="ListCellRight" width="%5" nowrap><c:out value="${rmds.metrics[average].valueFmt}" /></td>
    <td class="ListCellRight" width="%5" nowrap><c:out value="${rmds.metrics[max].valueFmt}" /></td>
    <td class="ListCellRight" width="%5" nowrap><c:out value="${rmds.metrics[last].valueFmt}" /></td>
      </c:when>
      <c:otherwise>
    <td class="ListCellRight" width="%5" nowrap> - </td>
    <td class="ListCellRight" width="%5" nowrap> - </td>
    <td class="ListCellRight" width="%5" nowrap> - </td>
    <td class="ListCellRight" width="%5" nowrap><c:out value="${rmds.metrics[last].valueFmt}" /></td>
      <!-- baseline not shown -->
      </c:otherwise>
      </c:choose> 
      <!--
      <td class="ListCellRight" nowrap><span class="MonitorMetricsValue"><c:out value="${rmds.metrics[min].valueFmt}" /></span></td>
      <td class="ListCellRight" nowrap><span class="MonitorMetricsValue"><c:out value="${rmds.metrics[average].valueFmt}" /></span></td>
      <td class="ListCellRight" nowrap><span class="MonitorMetricsValue"><c:out value="${rmds.metrics[last].valueFmt}" /></span></td>
      <td class="ListCellRight" nowrap><span class="MonitorMetricsValue"><c:out value="${rmds.metrics[last].valueFmt}" /></span></td>
    -->
    </tr>
  </c:forEach>
</c:forEach>
</c:forEach>
</table>
<c:set var="isCompareGroups" value="true" scope="request"/>  
<tiles:insertDefinition name=".resource.common.monitor.visibility.metricsDisplayControlForm">
  <tiles:putAttribute name="form" value="${CompareMetricsForm}"/>
  <tiles:putAttribute name="formName" value="CompareMetricsForm"/>
</tiles:insertDefinition>

<tiles:insertDefinition name=".resource.common.monitor.visibility.metricsToolbar">
  <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
  <tiles:putAttribute name="useCurrentButton" value="true"/>
</tiles:insertDefinition> 
</div>

<s:a href="resourceMetricsMonitorCompatGroupVisibility.action?mode=resourceMetrics&eid=%{#attr.eid}" onclick="clickLink('CompareMetricsForm', 'back')">
	<fmt:message key="resource.common.monitor.visibility.CompareMetricsReturnLink">
		<fmt:param>
			<c:out value="${CompareMetricsForm.name}"/>
		</fmt:param>
	</fmt:message>
</s:a>

<input type="hidden" name="rid" value="<c:out value="${CompareMetricsForm.rid}"/>">
<input type="hidden" name="type" value="<c:out value="${CompareMetricsForm.type}"/>">
<input type="hidden" name="ctype" value="<c:out value="${CompareMetricsForm.ctype}"/>">
<input type="hidden" name="appdefTypeId" value="<c:out value="${CompareMetricsForm.appdefTypeId}"/>">
<input type="hidden" name="name" value="<c:out value="${CompareMetricsForm.name}"/>">
<input type="hidden" name="ru" value="<c:out value="${CompareMetricsForm.ru}"/>">
<input type="hidden" name="rn" value="<c:out value="${CompareMetricsForm.rn}"/>">

<c:forEach var="r" items="${CompareMetricsForm.r}">
<input type="hidden" name="r" value="<c:out value="${r}"/>">
</c:forEach>

</s:form>

<tiles:insertDefinition name=".page.footer"/>
  
