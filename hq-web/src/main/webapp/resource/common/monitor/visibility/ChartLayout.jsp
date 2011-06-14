<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
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
					<html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0" />
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
								<html:link action="/Dashboard">
									<fmt:message key="alert.current.detail.link.noresource.Rtn" />
								</html:link>
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
				<td colspan="4"><c:choose>
					<c:when test="${param.mode == MODE_MON_CHART_MMSR}">
						<c:set var="metricName">
							<fmt:message
								key="resource.common.monitor.visibility.MultipleMetric" />
						</c:set>
					</c:when>
					<c:otherwise>
						<c:set var="metricName" value="${ViewChartForm.chartName}" />
					</c:otherwise>
				</c:choose> <c:set var="entityId" value="${Resource.entityId}" /> <tiles:insert
					definition=".page.title.resource.generic">
					<c:choose>
						<c:when test="${not empty ViewChartForm.ctype}">
							<tiles:put name="eid" beanName="entityId"
								beanProperty="appdefKey" />
							<tiles:put name="ctype" beanName="ViewChartForm"
								beanProperty="ctype" />
						</c:when>
						<c:otherwise>
							<tiles:put name="eid" beanName="entityId"
								beanProperty="appdefKey" />
						</c:otherwise>
					</c:choose>
					<tiles:put name="subTitleName" beanName="metricName" />

				</tiles:insert></td>
			</tr>
			<tr>
				<td class="PageTitle"><html:img page="/images/spacer.gif"
					width="5" height="1" alt="" border="0" /></td>
				<td><html:img page="/images/spacer.gif" width="75" height="1"
					alt="" border="0" /></td>
				<td width="100%"><tiles:insert definition=".portlet.confirm" />
				<html:form action="/resource/common/monitor/visibility/ViewChart">
					<html:hidden property="chartName"
						value="${Resource.name}: ${metricName}" />
					<c:choose>
						<c:when test="${param.mode == MODE_MON_CHART_SMSR}">
							<tiles:insert
								definition=".resource.common.monitor.visibility.charts.metric.chart" />
      &nbsp;<br>
							<tiles:insert
								definition=".resource.common.monitor.visibility.charts.metric.partrsrcs" />
      &nbsp;<br>
						</c:when>

						<c:when test="${param.mode == MODE_MON_CHART_MMSR}">
							<tiles:insert
								definition=".resource.common.monitor.visibility.charts.metric.chart">
								<tiles:put name="multiMetric" value="true" />
							</tiles:insert>
      &nbsp;<br>
							<tiles:insert
								definition=".resource.common.monitor.visibility.charts.metric.partrsrcs">
								<tiles:put name="multiMetric" value="true" />
							</tiles:insert>
      &nbsp;<br>
						</c:when>

						<c:when test="${param.mode == MODE_MON_CHART_SMMR}">
							<tiles:insert
								definition=".resource.common.monitor.visibility.charts.metric.chart">
								<tiles:put name="multiResource" value="true" />
							</tiles:insert>
      &nbsp;<br>
							<tiles:insert
								definition=".resource.common.monitor.visibility.charts.metric.partrsrcs.smmr">
							</tiles:insert>
      &nbsp;<br>
						</c:when>
					</c:choose>
				</html:form></td>
				<td><html:img page="/images/spacer.gif" width="80" height="1"
					alt="" border="0" /></td>
			</tr>
		</table>
		<jsu:script>
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
