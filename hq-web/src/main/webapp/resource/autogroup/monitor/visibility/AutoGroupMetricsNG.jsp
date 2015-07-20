<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
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


<tiles:importAttribute name="entityId"/>
<c:set var="childResourceTypeId" value="${ChildResourceType.id}"/>
<c:set var="selfAction" value="/resource/autogroup/monitor/Visibility.do?mode=resourceMetrics&eid=${entityId.type}:${Resource.id}&ctype=${childResourceTypeId}"/>
<c:set var="ctype" value="${param.ctype}"/>
<c:set var="cname" value="${ChildResourceType.name}"/>
<fmt:message var="ChildTH" key="resource.autogroup.monitor.visibility.ChildTH"><fmt:param value="${ChildResourceType.name}"/></fmt:message>

<tiles:insertDefinition name=".resource.common.monitor.visibility.dashminitabs">
  <tiles:putAttribute name="selectedIndex" value="1"/>
  <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
  <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
  <tiles:putAttribute name="entityType" value="autogroup"/>
  <tiles:putAttribute name="autogroupResourceType" value="${ctype}"/>
  <c:if test="${perfSupported}">
    <tiles:putAttribute name="tabListName" value="perf"/>
  </c:if>
</tiles:insertDefinition>

<s:form action="/resource/autogroup/monitor/visibility/AutoGroupMetrics">

<table width="100%" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td>
<tiles:insertDefinition name=".resource.common.monitor.visibility.metricsDisplay">
  <tiles:putAttribute name="summaries" value="${MetricSummaries}"/>
  <tiles:putAttribute name="selfAction" value="${selfAction}"/>
  <tiles:putAttribute name="childResourceType" value="${ChildResourceType}"/>
  <tiles:putAttribute name="buttonMode" value="noleft"/>
  <tiles:putAttribute name="useCurrent" value="true"/>
  <tiles:putAttribute name="useConfigure" value="true"/>
  <tiles:putAttribute name="ctype" value="${ctype}"/>
</tiles:insertDefinition>

<s:hidden theme="simple" name="h" value="%{#attr.h}"/>
<c:forEach var="eid" items="${MetricsDisplayForm.eid}">
<input type="hidden" name="eid" value="<c:out value="${eid}"/>">
</c:forEach>
<s:hidden theme="simple" name="ctype" value="%{#attr.ctype}"/>
</s:form>
    </td>
  </tr>
</table>

