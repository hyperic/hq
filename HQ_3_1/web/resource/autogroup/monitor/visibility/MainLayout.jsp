<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="display" prefix="display" %>
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


<script language="JavaScript" src="<html:rewrite page="/js/popup.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
</script>

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

<c:set var="ctype" value="${ChildResourceType.appdefTypeKey}"/>

<c:choose>
  <c:when test="${not empty Resource.name}">
<tiles:insert definition=".page.title.resource.autogroup">
  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" ctype="${ctype}"/></tiles:put>
</tiles:insert>
  </c:when>
  <c:otherwise>
<tiles:insert definition=".page.title.resource.autogroup.empty">
  <tiles:put name="titleName" beanName="cname"/>
</tiles:insert>
  </c:otherwise>
</c:choose>

<tiles:insert definition=".tabs.resource.autogroup.monitor.visibility">
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
  <tiles:put name="autogroupResourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="autogroupResourceType" beanName="ctype"/>
  <tiles:put name="entityIds" beanName="EntityIds"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

<table width="100%" class="MonitorBlockContainer">
  <tr>
    <td valign="top">
<c:choose>
  <c:when test="${isPerformance}">
    <tiles:insert page="/resource/group/monitor/visibility/GroupPerformance.jsp">
      <tiles:put name="entityId" beanName="entityId"/>
      <tiles:put name="entityType" value="autogroup"/>
      <tiles:put name="ctype" beanName="ctype"/>
    </tiles:insert>
  </c:when>
  <c:otherwise>
      <c:choose>
        <c:when test="${isCurrentHealth}">
          <html:form action="/resource/common/monitor/visibility/SelectResources.do">
            <html:hidden property="mode"/>
            <input type="hidden" name="eid" value="<c:out value="${eid}"/>">
            <input type="hidden" name="ctype" value="<c:out value="${ctype}"/>"/>
          <c:if test="${not empty view}">
            <input type="hidden" name="view" value="<c:out value="${view}"/>">
          </c:if>
            <tiles:insert page="/resource/autogroup/monitor/visibility/CurrentHealthResources.jsp">
              <tiles:put name="mode" beanName="mode"/>
              <tiles:put name="ctype" beanName="ctype"/>
              <tiles:put name="showProblems" value="true"/>
            </tiles:insert>
          </html:form>
        </c:when>
        <c:when test="${isResourceMetrics}">
          <html:form action="/resource/autogroup/monitor/visibility/FilterAutoGroupMetrics">
            <input type="hidden" name="eid" value="<c:out value="${eid}"/>">
            <input type="hidden" name="ctype" value="<c:out value="${ctype}"/>"/>
            <tiles:insert page="/resource/autogroup/monitor/visibility/CurrentHealthResources.jsp">
              <tiles:put name="mode" beanName="mode"/>
              <tiles:put name="ctype" beanName="ctype"/>
              <tiles:put name="showOptions" value="true"/>
            </tiles:insert>
          </html:form>
        </c:when>
      </c:choose>
    </td>
    <td valign="top" width="100%">
    <c:choose>
      <c:when test="${isCurrentHealth}">
        <tiles:insert page="/resource/common/monitor/visibility/Indicators.jsp">
          <c:if test="${perfSupported}">
            <tiles:put name="tabListName" value="perf"/>
          </c:if>
          <tiles:put name="entityType" value="autogroup"/>
          <tiles:put name="ctype" beanName="ctype"/>
        </tiles:insert>
      </c:when>
      <c:when test="${isResourceMetrics}">
        <tiles:insert page="/resource/autogroup/monitor/visibility/AutoGroupMetrics.jsp">
          <tiles:put name="entityId" beanName="entityId"/>
        </tiles:insert>
      </c:when>
      <c:otherwise>
        <c:out value="${mode}"/>
      </c:otherwise>
    </c:choose>
    </td>
  </tr>
  <tr>
    <td colspan="2">
      <html:form method="GET" action="/resource/common/monitor/visibility/MetricsControl">
        <input type="hidden" name="ctype" value="<c:out value="${ctype}"/>"/>
        <tiles:insert definition=".resource.common.monitor.visibility.metricsDisplayControlForm">
          <tiles:put name="form" beanName="MetricsControlForm"/>
          <tiles:put name="formName" value="MetricsControlForm"/>
          <tiles:put name="mode" beanName="mode"/>
          <tiles:put name="eid" beanName="eid"/>
          <c:if test="${not empty view}">
            <tiles:put name="view" beanName="view"/>
          </c:if>
       </tiles:insert>
     </html:form>
  </c:otherwise>
</c:choose>
    </td>
  </tr>
</table>

<tiles:insert definition=".page.footer"/>
