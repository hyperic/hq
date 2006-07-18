<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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

<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="eid" value="${entityId.appdefKey}"/>
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

<tiles:insert definition=".page.title.resource.application.full">
  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${eid}" /></tiles:put>
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>

<tiles:insert definition=".tabs.resource.application.monitor.visibility">
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

<table width="100%" class="MonitorBlockContainer">
  <tr>
    <td valign="top">
    <c:choose>
      <c:when test="${isCurrentHealth}">
        <html:form
            action="/resource/common/monitor/visibility/SelectResources.do">
          <input type="hidden" name="eid" value="<c:out value="${eid}"/>">
          <c:if test="${not empty view}">
            <input type="hidden" name="view" value="<c:out value="${view}"/>">
          </c:if>
          <html:hidden property="mode"/>

          <tiles:insert page="/resource/application/monitor/visibility/CurrentHealthResources.jsp">
            <tiles:put name="mode" beanName="mode"/>
            <tiles:put name="showProblems" value="true"/>
          </tiles:insert>
        </html:form>
      </c:when>
      <c:when test="${isResourceMetrics}">
        <html:form action="/resource/application/monitor/visibility/FilterApplicationMetrics">
          <input type="hidden" name="eid" value="<c:out value="${eid}"/>">
          <tiles:insert page="/resource/application/monitor/visibility/CurrentHealthResources.jsp">
            <tiles:put name="mode" beanName="mode"/>
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
          <tiles:put name="tabListName" value="nometrics"/>
          <tiles:put name="entityType" value="application"/>
        </tiles:insert>
      </c:when>
    </c:choose>
    </td>
  </tr>
  <tr>
    <td colspan="2">
      <html:form method="GET" action="/resource/common/monitor/visibility/MetricsControl">
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
    </td>
  </tr>
</table>
    </td>
  </tr>
</table>

<tiles:insert definition=".page.footer"/>
