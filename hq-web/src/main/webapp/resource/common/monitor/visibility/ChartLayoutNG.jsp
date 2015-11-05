<%@ page language="java"%>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>


<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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


<c:choose>
	<c:when test="${not empty toDashboard}">
		<table width="100%" border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td rowspan="3" class="PageTitle">
					<img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" alt="" border="0" />
				</td>
				<td width="100%">
					<table width="100%" border="0" cellspacing="0" cellpadding="4">
						<tr class="ListHeader">
							<td><fmt:message key="dash.home.SavedQueries" /></td>
						</tr>
						<tr class="ListRow">
							<td><fmt:message key="resource.common.monitor.visibility.error.ChartRemoved" /></td>
						</tr>
						<tr class="ListRow">
							<td>
								<s:a action="Dashboard.action">
									<fmt:message key="alert.current.detail.link.noresource.Rtn" />
								</s:a>
							</td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
	</c:when>
	<c:otherwise>
		<hq:constant classname="org.hyperic.hq.ui.Constants" symbol="MODE_MON_CHART_SMSR" var="MODE_MON_CHART_SMSR" />
		<hq:constant classname="org.hyperic.hq.ui.Constants" symbol="MODE_MON_CHART_MMSR" var="MODE_MON_CHART_MMSR" />
		<hq:constant classname="org.hyperic.hq.ui.Constants" symbol="MODE_MON_CHART_SMMR" var="MODE_MON_CHART_SMMR" />
		<jsu:importScript path="/js/chart.js" />
		<table width="100%" border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td colspan="4">
					<c:choose>
						<c:when test="${param.mode == MODE_MON_CHART_MMSR}">
							<c:set var="metricName">
								<fmt:message key="resource.common.monitor.visibility.MultipleMetric" />
							</c:set>
						</c:when>
						<c:otherwise>
							<c:set var="metricName" value="${ViewChartForm.chartName}" />
						</c:otherwise>
					</c:choose>
					<c:set var="entityId" value="${Resource.entityId}" /> 
					<tiles:insertDefinition name=".page.title.resource.generic">
						<tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
						<c:if test="${not empty ViewChartForm.ctype}">
							<tiles:putAttribute name="ctype" value="${ViewChartForm.ctype}" />
						</c:if>
						<tiles:putAttribute name="subTitleName"  value="${metricName}" />
					</tiles:insertDefinition>
				</td>
			</tr>
			<tr>
				<td class="PageTitle"><img src='<s:url value="/images/spacer.gif"/>' width="5" height="1" alt="" border="0" /></td>
				<td><img src='<s:url value="/images/spacer.gif"/>' width="75" height="1" alt="" border="0" /></td>
				<td width="100%">
				<tiles:insertDefinition name=".portlet.confirm" />
				<tiles:insertDefinition name=".portlet.error" />
							
				
				<s:form name="ViewChartForm"  action="viewChartAction" method="GET" onsubmit="makeNumeric()">
				
					<s:hidden theme="simple" name="chartName" value="%{#attr.Resource.name}: %{#attr.metricName}" />
					<c:set var="eid" value="${param.eid}" />
					<c:if test="${empty eid}">
						<c:set var="eid" value="${param.type}:${param.rid}" />
					</c:if>	
					<input type="hidden" name="eid" value="${eid}"/>
					<c:choose>
						<c:when test="${param.mode == MODE_MON_CHART_SMSR}">
							<tiles:insertDefinition name=".resource.common.monitor.visibility.charts.metric.chart" />
							&nbsp;<br>
							<tiles:insertDefinition name=".resource.common.monitor.visibility.charts.metric.partrsrcs" />
							&nbsp;<br>
						</c:when>

						<c:when test="${param.mode == MODE_MON_CHART_MMSR}">
							<tiles:insertDefinition name=".resource.common.monitor.visibility.charts.metric.chart">
								<tiles:putAttribute name="multiMetric" value="true" />
							</tiles:insertDefinition>
							&nbsp;<br>
							<tiles:insertDefinition name=".resource.common.monitor.visibility.charts.metric.partrsrcs">
								<tiles:putAttribute name="multiMetric" value="true" />
							</tiles:insertDefinition>
							&nbsp;<br>
						</c:when>

						<c:when test="${param.mode == MODE_MON_CHART_SMMR}">
							<tiles:insertDefinition name=".resource.common.monitor.visibility.charts.metric.chart">
								<tiles:putAttribute name="multiResource" value="true" />
							</tiles:insertDefinition>
							&nbsp;<br>
							<tiles:insertDefinition name=".resource.common.monitor.visibility.charts.metric.partrsrcs.smmr">
							</tiles:insertDefinition>
							&nbsp;<br>
						</c:when>
					</c:choose>
				</s:form>
				</td>
				<td><img src='<s:url value="/images/spacer.gif"/>' width="80" height="1" alt="" border="0" /></td>
			</tr>
		</table>
		<jsu:script>
		    function makeNumeric(){
				var curRn = document.getElementById("rn").value;
				if(curRn && !(!isNaN(parseFloat(curRn)) && isFinite(curRn))){
						document.getElementById("rn").value =0;
				}
			}
			hyperic.data.metric_chart = {
				message: {
					chartSaved: '<fmt:message key="resource.common.monitor.visibility.chart.confirm.ChartSaved"/>'
				}
			};
		
			var MyMetricChart = new hyperic.MetricChart(document.forms["ViewChartForm"]);
		
			<c:if test="${not empty metric}">
				var exportParam = {};
		
				exportParam.eid = "<c:out value="${Resource.entityId.type}:${Resource.id}" />";
				exportParam.metricId = "<c:out value="${metric.id}" />";
		
		      	<c:if test="${not empty param.ctype}">
		      		exportParam.ctype = "<c:out value="${param.ctype}" />";
		      	</c:if>
			</c:if>
		
			setInterval("MyMetricChart.refresh()",300000); // 5 minute page refresh  300000
		</jsu:script>
	</c:otherwise>
</c:choose>
