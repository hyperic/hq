<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>

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


<tiles:importAttribute name="multiMetric" ignore="true"/>
<tiles:importAttribute name="multiResource" ignore="true"/>

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

<c:if test="${empty multiMetric}">
<c:set var="multiMetric" value="false"/>
</c:if>
<c:if test="${empty multiResource}">
<c:set var="multiResource" value="false"/>
</c:if>

<c:if test="${not multiMetric}">
<s:hidden theme="simple" name="m" value="%{#attr.ViewChartForm.m[0]}"/>
</c:if>

<c:set var="widgetInstanceName" value="listMetrics"/>
<jsu:script>
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<!--  PARTICIPATING RESOURCE TITLE -->
<tiles:insertDefinition name=".header.tab">
<tiles:putAttribute name="tabKey" value="resource.common.monitor.visibility.chart.ParticipatingResourceTab"/>
</tiles:insertDefinition>
<!--  /  -->

<div id="listDiv">
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
  <tr class="ListHeaderDark">
    <c:if test="${multiMetric}">
    <td width="1%" class="ListHeaderInactiveSorted"><img
      src='<s:url value="/images/spacer.gif"/>' width="1" height="1" alt=""
      border="0"/></td>
    </c:if>
    <td width="29%" class="ListHeaderInactiveSorted"><fmt:message
      key="resource.common.monitor.visibility.MetricNameTH"/></td>
    <c:if test="${multiResource}">
    <td width="1%" class="ListHeaderInactiveSorted"><img
      src='<s:url value="/images/spacer.gif"/>' width="1" height="1" alt=""
      border="0"/></td>
    </c:if>
    <td width="69%" class="ListHeaderInactiveSorted"><fmt:message
      key="resource.common.monitor.visibility.ResourceTH"/></td>
<%--
      Don't display the metric summary min, average, max and last
      because they are confusing due to the "averaged" nature of the chart
      data. (2003/05/23 --JW)
    <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message
      key="resource.common.monitor.visibility.LowTH"/></td>
    <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message
      key="resource.common.monitor.visibility.AvgTH"/></td>
    <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message
      key="resource.common.monitor.visibility.PeakTH"/></td>
    <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message
      key="resource.common.monitor.visibility.LastTH"/></td>
--%>
  </tr>
  <c:forEach var="metricSummary" varStatus="msStatus" items="${metricSummaries}">
  <c:choose>
  <c:when test="${multiResource}">
  <c:set var="resource" value="${resources[msStatus.index]}"/>
  </c:when>
  <c:otherwise>
  <c:set var="resource" value="${resources[0]}"/>
  </c:otherwise>
  </c:choose>
  <c:url var="resourceUrl" value="resourceAction.action">
    <c:param name="rid" value="${resource.id}"/>
    <c:param name="type" value="${resource.entityId.type}"/>
    <c:param name="eid" value="${resource.entityId}"/>
    <c:param name="mode" value="currentHealth"/>
  </c:url>
  <tr class="ListRow">
    <c:if test="${multiMetric}">
    <td class="ListCellCheckbox" valign="top">
	<c:set var="resId" value="${metricSummary.templateId}"/>
		<c:set var="checked" value=""/>
		
		<c:forEach var="tmpRes" items="${ViewChartForm.m}">
			
			<c:if test="${tmpRes == resId}">
				<c:set var="checked" value="checked"/>
			</c:if>
		</c:forEach>
		
	
	<input type="checkbox" <c:out value="${checked}"/>
      name="m" value="${metricSummary.templateId}"
      onclick="ToggleSelection(this, widgetProperties);"
      class="metricList"/></td>
    </c:if>
    <c:if test="${!multiResource || (multiResource && msStatus.first)}">
    <td<c:if test="${multiResource && msStatus.first}"> rowspan="<c:out value='${resourcesSize}'/>"</c:if>
      class="ListCellPrimary" valign="top"><c:out value="${metricSummary.label}"/></td>
    </c:if>
    <c:choose>
    <c:when test="${multiResource}">
      <c:set var="maxResources"><c:out value="${checkedResourcesSize}"/></c:set>
      <c:set var="maxMessage">
      <fmt:message key="resource.common.monitor.visibility.chart.TooManyResources">
      <fmt:param value="${maxResources}"/>
      </fmt:message>
      </c:set>
    <td class="ListCellLeftLineNoPadding" valign="top">
		<c:set var="resId" value="${resource.id}"/>
		<c:set var="checked" value=""/>
		
		<c:forEach var="tmpRes" items="${checkedResources}">
			<c:if test="${tmpRes.id == resId}">
				<c:set var="checked" value="checked"/>
			</c:if>
		</c:forEach>
		<input type="checkbox" <c:out value="${checked}"/>
		  name="resourceIds" value="${resource.id}"
		  onclick="ToggleSelection(this, widgetProperties, %{#attr.maxResources}, '%{#attr.maxMessage}');"
		  id="resourceList"/></td>
      <c:set var="resCellClass" value="ListCellPrimary"/>
    </c:when>
    <c:otherwise>
      <c:set var="resCellClass" value="ListCellLeftLine"/>
    </c:otherwise>
    </c:choose>
    <c:if test="${!multiMetric || (multiMetric && msStatus.first)}">
    <td<c:if test="${multiMetric && msStatus.first}"> rowspan="<c:out value='${metricSummariesSize}'/>"</c:if>
      class="<c:out value='${resCellClass}'/>" valign="top">
      <s:a href="%{#attr.resourceUrl}"><c:out value="${resource.name}"/></s:a>
    </td>
    </c:if>
<%--
      Don't display the metric summary min, average, max and last
      because they are confusing due to the "averaged" nature of the chart
      data. (2003/05/23 --JW)
    <td class="ListCellRight" nowrap><c:out value="${metricSummary.metrics[min].valueFmt}"/></td>
    <td class="ListCellRight" nowrap><c:out value="${metricSummary.metrics[average].valueFmt}"/></td>
    <td class="ListCellRight" nowrap><c:out value="${metricSummary.metrics[max].valueFmt}"/></td>
    <td class="ListCellRight" nowrap><c:out value="${metricSummary.metrics[last].valueFmt}"/></td>
--%>
  </tr>
  </c:forEach>
</table>

<!--  REDRAW SELECTED TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <tr>
    <td id="<c:out value="${widgetInstanceName}"/>RedrawTd"><div
      id="<c:out value="${widgetInstanceName}"/>RedrawDiv"><c:if
      test="${multiMetric || multiResource}"><img
      src='<s:url value="/images/tbb_redrawselectedonchart_gray.gif"/>' width="171"
      height="16" alt="" border="0"/></c:if></div></td>
    <td width="100%" align="right">&nbsp;</td>
<%--
      Don't display the metric summary min, average, max and last
      because they are confusing due to the "averaged" nature of the chart
      data. (2003/05/23 --JW)
    <td width="100%" align="right"><fmt:message
      key="resource.common.monitor.visibility.GetCurrentValuesLabel"/></td>
    <td><input type="image" name="redraw"
      src='<s:url value="/images/4.0/icons/accept.png"/>' border="0"/></td>
--%>
  </tr>
</table>
<!--  /  -->

</div>

<input type="Hidden" id="privateChart">
<jsu:script>
  	testCheckboxes('<c:out value="${widgetInstanceName}"/>');
</jsu:script>