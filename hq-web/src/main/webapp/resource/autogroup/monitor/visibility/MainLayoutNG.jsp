<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
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
<c:set var="eid" value="${Resource.entityId.appdefKey}"/>
<c:set var="view" value="${param.view}"/>
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
</c:choose>

<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="cname" value="${ChildResourceType.name}"/>
<fmt:message var="ChildTH" key="resource.autogroup.monitor.visibility.ChildTH"><fmt:param value="${cname}"/></fmt:message>

<c:set var="ctype" value="${ChildResourceType.appdefResourceTypeValue.appdefTypeKey}"/>
<c:set var="ignoreBreadcrumb" value="false" scope="request"/>
<c:choose>
  <c:when test="${not empty Resource.name}">
<tiles:insertDefinition name=".page.title.resource.autogroup">
  <tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
  <tiles:putAttribute name="ctype" value="${ctype}" />
</tiles:insertDefinition>
  </c:when>
  <c:otherwise>
<tiles:insertDefinition name=".page.title.resource.autogroup.empty">
  <tiles:putAttribute name="titleName" value="${cname}"/>
</tiles:insertDefinition>
  </c:otherwise>
</c:choose>

<tiles:insertDefinition name=".tabs.resource.autogroup.monitor.visibility">
  <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
  <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
  <tiles:putAttribute name="autogroupResourceId" value="${Resource.id}"/>
  <tiles:putAttribute name="autogroupResourceType" value="${ctype}"/>
  <tiles:putAttribute name="entityIds" value="${EntityIds}"/>
</tiles:insertDefinition>

<tiles:insertDefinition name=".portlet.error"/>
<tiles:insertDefinition name=".portlet.confirm"/>

<table width="100%" class="MonitorBlockContainer">
  <tr>
<c:choose>
  <c:when test="${isPerformance}">
    <td valign="top">
	<!--TODO this page is not defined in HQ, it exists only in HQEE. Make sure it's converted when we get there-->
    <tiles:insertTemplate template="/resource/group/monitor/visibility/GroupPerformanceNG.jsp">
      <tiles:putAttribute name="entityId" value="${entityId}"/>
      <tiles:putAttribute name="entityType" value="autogroup"/>
      <tiles:putAttribute name="ctype" value="${ctype}"/>
    </tiles:insertTemplate>
    </td>
  </c:when>
  <c:otherwise>
    <td colspan="2" style="padding-bottom: 10px;">
      <s:form method="GET" action="metricsControlAction.action">
        <input type="hidden" name="ctype" value="<c:out value="${ctype}"/>"/>
        <tiles:insertDefinition name=".resource.common.monitor.visibility.metricsDisplayControlForm">
          <tiles:putAttribute name="form" value="${MetricsControlForm}"/>
          <tiles:putAttribute name="formName" value="MetricsControlForm"/>
          <tiles:putAttribute name="mode" value="${mode}"/>
          <tiles:putAttribute name="eid" value="${eid}"/>
          <c:if test="${not empty view}">
            <tiles:putAttribute name="view" value="${view}"/>
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
            <s:hidden theme="simple" name="mode" value="%{#attr.mode}"/>
            <input type="hidden" name="eid" value="<c:out value="${eid}"/>">
            <input type="hidden" name="ctype" value="<c:out value="${ctype}"/>"/>
          <c:if test="${not empty view}">
            <input type="hidden" name="view" value="<c:out value="${view}"/>">
          </c:if>
            <tiles:insertTemplate template="/resource/autogroup/monitor/visibility/CurrentHealthResourcesNG.jsp">
              <tiles:putAttribute name="mode" value="${mode}"/>
              <tiles:putAttribute name="ctype" value="${ctype}"/>
              <tiles:putAttribute name="showProblems" value="true"/>
            </tiles:insertTemplate>
          </s:form>
        </c:when>
        <c:when test="${isResourceMetrics}">
          <s:form id="filterMetricsForm" name="filterMetricsForm" action="resourceMetricsMonitorAutogroupVisibility.action">
            <input type="hidden" name="eid" value="<c:out value="${eid}"/>">
            <input type="hidden" name="ctype" value="<c:out value="${ctype}"/>"/>
			<input type="hidden" name="mode" value="<c:out value="${mode}"/>">
            <tiles:insertTemplate template="/resource/autogroup/monitor/visibility/CurrentHealthResourcesNG.jsp">
              <tiles:putAttribute name="mode" value="${mode}"/>
              <tiles:putAttribute name="ctype" value="${ctype}"/>
              <tiles:putAttribute name="showOptions" value="true"/>
            </tiles:insertTemplate>
          </s:form>
        </c:when>
      </c:choose>
    </td>
    <td valign="top" width="100%">
    <c:choose>
      <c:when test="${isCurrentHealth}">
        <tiles:insertTemplate template="/resource/common/monitor/visibility/IndicatorsNG.jsp">
          <c:if test="${perfSupported}">
            <tiles:putAttribute name="tabListName" value="perf"/>
          </c:if>
          <tiles:putAttribute name="entityType" value="autogroup"/>
          <tiles:putAttribute name="ctype" value="${ctype}"/>
        </tiles:insertTemplate>
      </c:when>
      <c:when test="${isResourceMetrics}">
        <tiles:insertTemplate template="/resource/autogroup/monitor/visibility/AutoGroupMetricsNG.jsp">
          <tiles:putAttribute name="entityId" value="${entityId}"/>
        </tiles:insertTemplate>
      </c:when>
      <c:otherwise>
        <c:out value="${mode}"/>
      </c:otherwise>
    </c:choose>
    </td>
  </c:otherwise>
</c:choose>
  </tr>
</table>

<tiles:insertDefinition name=".page.footer"/>
