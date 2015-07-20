<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>


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


<tiles:importAttribute name="Resource"/>
<tiles:importAttribute name="entityId"/>
<tiles:importAttribute name="MetricSummaries"/>
<tiles:importAttribute name="resourceId" />
<tiles:importAttribute name="resourceType"/>

<c:set var="selfAction" value="resourceMetricsMonitorPlatformVisibility.action?mode=resourceMetrics&eid=${entityId.type}:${Resource.id}"/>

<tiles:insertDefinition name=".resource.common.monitor.visibility.dashminitabs">
  <tiles:putAttribute name="selectedIndex" value="1"/>
  <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
  <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
  <tiles:putAttribute name="entityType" value="platform"/>
</tiles:insertDefinition>

<table width="100%" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td>
<s:form action="chartMultiMetricSingleResourcecommonVisibilityPortal.action?mode=chartMultiMetricSingleResource">
<tiles:insertDefinition name=".resource.common.monitor.visibility.metricsDisplay">
  <tiles:putAttribute name="summaries" value="${MetricSummaries}"/>
  <tiles:putAttribute name="selfAction" value="${selfAction}"/>
  <tiles:putAttribute name="buttonMode" value="baselines"/>
  <tiles:putAttribute name="useCurrent" value="true"/>
</tiles:insertDefinition>
  <s:hidden theme="simple" name="h" value="%{#attr.h}"/>
  <s:hidden theme="simple" name="rid" value="%{#attr.resourceId}"/>
  <s:hidden theme="simple" name="type" value="%{#attr.resourceType}"/>
  
</s:form>
    </td>
  </tr>
</table>
