<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

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


<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 

<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="selfAction" value="resourceMetricsMonitorGroupVisibility.action?mode=resourceMetrics&eid=${entityId.type}:${Resource.id}"/>

<c:set var="memberTypeLabel" value="${Resource.groupTypeLabel}" />

<tiles:insertDefinition name=".resource.common.monitor.visibility.dashminitabs">
  <tiles:putAttribute name="selectedIndex" value="1"/>
  <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
  <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
  <tiles:putAttribute name="entityType" value="group"/>
  <c:if test="${perfSupported}">
    <tiles:putAttribute name="tabListName" value="perf"/>
  </c:if>
</tiles:insertDefinition>

<table width="100%" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td style="background-color:#DBE3F5;">
<s:form  action="metricsDisplayAction">

<tiles:insertDefinition name=".resource.common.monitor.visibility.metricsDisplay">
  <tiles:putAttribute name="summaries" value="${MetricSummaries}"/>
  <tiles:putAttribute name="buttonMode" value="noleft"/>
  <tiles:putAttribute name="selfAction" value="${selfAction}"/>
  <tiles:putAttribute name="useChartMulti" value="false"/>
  <tiles:putAttribute name="useCurrent" value="true"/>
</tiles:insertDefinition>
  <%--
  <tiles:insertDefinition name=".header.tab">
	  <tiles:putAttribute name="tabKey" value="resource.group.monitor.visibility.CurrentHealthOfCollecting"/>
	  <tiles:putAttribute name="tabName" value="${memberTypeLabel}" />
   </tiles:insertDefinition>
 --%>
    <div style="padding-top:4px;padding-bottom:4px;border-top:1px solid #ABB1C7;font-weight:bold;">
    <fmt:message key="resource.group.monitor.visibility.CurrentHealthOfCollecting">
        <fmt:param value="${memberTypeLabel}"/>
    </fmt:message>
    </div>
<tiles:insertDefinition name=".resource.common.monitor.visibility.childResourcesCurrentHealthByType">
  <tiles:putAttribute name="summaries" value="${pagedMembers}"/>
  <tiles:putAttribute name="memberTypeLabel" value="${memberTypeLabel}" />
  <tiles:putAttribute name="selfAction" value="${selfAction}"/>
</tiles:insertDefinition>

<s:hidden theme="simple" value="%{#attr.h}" name="h"/>
<s:hidden theme="simple" value="%{#attr.rid}" name="rid"/>
<s:hidden theme="simple" value="%{#attr.type}" name="type"/>
<s:hidden theme="simple" value="%{#attr.Resource.name}" name="Resource"/>
<s:hidden theme="simple" value="%{#attr.Resource.groupEntType}:%{#attr.Resource.groupEntResType}" name="ctype" />
<s:hidden theme="simple" value="%{#attr.Resource.groupEntType}" name="appdefType"  />
</s:form>
    </td>
  </tr>
</table>
