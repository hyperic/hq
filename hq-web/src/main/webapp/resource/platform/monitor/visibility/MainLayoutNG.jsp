<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
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
  
  Copyright (C) [2004-2008], Hyperic, Inc.
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
<jsu:importScript path="/js/popup.js" />
<jsu:script>
	var pageData = new Array();
</jsu:script>
<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="eid" value="${Resource.entityId.appdefKey}"/>
<c:set var="view"><c:out value="${param.view}"/></c:set>

<c:set var="mode" value="${param.mode}"/>
<c:if test="${empty mode}">
  <c:set var="mode" value="currentHealth"/>
</c:if>

<c:choose>
  <c:when test="${mode == 'currentHealth'}">
    <c:set var="isCurrentHealth" value="true"/>
  </c:when>
  <c:when test="${mode == 'resourceMetrics'}">
    <c:set var="isResourceMetrics" value="true"/>
  </c:when>
  <c:when test="${mode == 'performance'}">
    <c:set var="isPerformance" value="true"/>
  </c:when>
  <c:when test="${mode == 'topN'}">
    <c:set var="isTopN" value="true"/>
  </c:when>
</c:choose>
<c:set var="ignoreBreadcrumb" value="false" scope="request"/>
<tiles:insertDefinition name=".page.title.resource.platform.full">
  <tiles:putAttribute name="resource" value="${Resource}"/>
  <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
  <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
  <tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
</tiles:insertDefinition>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" />

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>
<c:choose>
    <c:when test="${canControl}">
        <tiles:insertDefinition name=".tabs.resource.platform.monitor.visibility">
          <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
          <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
        </tiles:insertDefinition>
    </c:when>
    <c:otherwise>
        <tiles:insertDefinition name=".tabs.resource.platform.monitor.visibility.nocontrol">
          <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
          <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
        </tiles:insertDefinition>
    </c:otherwise>
</c:choose>


<tiles:insertDefinition name=".portlet.error"/>
<tiles:insertDefinition name=".portlet.confirm"/>


<table width="100%" class="MonitorBlockContainer">
 <tr>
    <td colspan="2" style="padding-bottom: 10px;">
      <s:form method="GET" action="metricsControlAction.action">
        <tiles:insertDefinition name=".resource.common.monitor.visibility.metricsDisplayControlForm">
          <tiles:putAttribute name="form" value="${MetricsControlForm}"/>
          <tiles:putAttribute name="formName" value="MetricsControlForm"/>
          <tiles:putAttribute name="mode" value="${mode}"/>
          <tiles:putAttribute name="eid" value="${eid}"/>
          <c:if test="${not empty IndicatorViewsForm.view}">
            	<tiles:putAttribute name="view" value="${IndicatorViewsForm.view}"/>
          </c:if>
          </tiles:insertDefinition>
      </s:form>
    </td>
  </tr>
  <tr>
    <td valign="top">
      <c:choose>
        <c:when test="${isCurrentHealth}">
          <s:form id="ProblemMetricsDisplayForm" name="ProblemMetricsDisplayForm" action="resourceAction">
            <input type="hidden" name="eid" value="<c:out value="${eid}"/>">
          <c:if test="${not empty view}">
            <input type="hidden" name="view" value="<c:out value="${view}"/>"/>">
          </c:if>
            <s:hidden theme="simple" value="%{#attr.mode}"/>
            
            <tiles:insertTemplate template="/resource/platform/monitor/visibility/CurrentHealthResourcesNG.jsp">
              <tiles:putAttribute name="mode" value="${mode}"/>
              <tiles:putAttribute name="showProblems" value="true"/>
            </tiles:insertTemplate>
          </s:form>
        </c:when>
        <c:when test="${isResourceMetrics}">
          <s:form id="filterMetricsForm" name="filterMetricsForm" action="resourceMetricsMonitorPlatformVisibility.action" method="GET">
            <input type="hidden" name="eid" value="<c:out value="${eid}"/>">
            <input type="hidden" name="mode" value="<c:out value="${mode}"/>">
            <tiles:insertTemplate template="/resource/platform/monitor/visibility/CurrentHealthResourcesNG.jsp">
              <tiles:putAttribute name="mode" value="${mode}"/>
              <tiles:putAttribute name="showOptions" value="true"/>
            </tiles:insertTemplate>
          </s:form>
        </c:when>
      </c:choose>
    </td>
	  <td valign="top" align="left" width="100%">
	  
<c:choose>
  <c:when test="${isCurrentHealth}">
    <tiles:insertTemplate template="/resource/common/monitor/visibility/IndicatorsNG.jsp">
      <tiles:putAttribute name="entityType" value="platform"/>
    </tiles:insertTemplate>
  </c:when>
  <c:when test="${isResourceMetrics}">
    <tiles:insertTemplate template="/resource/platform/monitor/visibility/PlatformMetricsNG.jsp">
      <tiles:putAttribute name="Resource" value="${Resource}"/>
      <tiles:putAttribute name="entityId" value="${entityId}"/>
      <tiles:putAttribute name="MetricSummaries" value="${MetricSummaries}"/>
	  <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
      <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
    </tiles:insertTemplate>
  </c:when>
    <c:when test="${isTopN}">
        <tiles:insertTemplate template="/resource/platform/monitor/visibility/PlatformMetricsNG.jsp">
            <tiles:putAttribute name="Resource" value="${Resource}"/>
            <tiles:putAttribute name="entityId" value="${entityId}"/>
            <tiles:putAttribute name="MetricSummaries" value="${MetricSummaries}"/>
			<tiles:putAttribute name="resourceId" value="${Resource.id}"/>
            <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
        </tiles:insertTemplate>
    </c:when>
  <c:otherwise>
    <c:out value="${mode}"/>
  </c:otherwise>
</c:choose>
    </td>
  </tr>
</table>

<tiles:insertDefinition name=".page.footer"/>

