<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
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
</c:choose>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" />

<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>

<tiles:insert definition=".page.title.resource.server.full">
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
  <tiles:put name="eid" beanName="entityId" beanProperty="appdefKey" />
</tiles:insert>

<c:choose>
    <c:when test="${canControl}">
        <tiles:insert definition=".tabs.resource.server.monitor.visibility">
          <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
          <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
        </tiles:insert>
    </c:when>
    <c:otherwise>
        <tiles:insert definition=".tabs.resource.server.monitor.visibility.nocontrol">
          <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
          <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
        </tiles:insert>
    </c:otherwise>
</c:choose>

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

<table width="100%" class="MonitorBlockContainer">
  <tr>
    <td colspan="2" style="padding-bottom: 10px;">
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
  <tr>
    <td valign="top">
      <c:choose>
        <c:when test="${isCurrentHealth}">
          <html:form action="/resource/common/monitor/visibility/SelectResources.do">
            <input type="hidden" name="eid" value="<c:out value="${eid}"/>">
          <c:if test="${not empty view}">
            <input type="hidden" name="view" value="<c:out value="${view}"/>">
          </c:if>
            <html:hidden property="mode"/>

            <tiles:insert page="/resource/server/monitor/visibility/CurrentHealthResources.jsp">
              <tiles:put name="mode" beanName="mode"/>
              <tiles:put name="showProblems" value="true"/>
            </tiles:insert>
          </html:form>
        </c:when>
        <c:when test="${isResourceMetrics}">
          <html:form action="/resource/server/monitor/visibility/FilterServerMetrics">
            <input type="hidden" name="eid" value="<c:out value="${eid}"/>">
            <tiles:insert page="/resource/server/monitor/visibility/CurrentHealthResources.jsp">
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
      <c:if test="${perfSupported}">
        <tiles:put name="tabListName" value="perf"/>
      </c:if>
      <tiles:put name="entityType" value="server"/>
    </tiles:insert>
  </c:when>
  <c:when test="${isResourceMetrics}">
    <tiles:insert page="/resource/server/monitor/visibility/ServerMetrics.jsp">
      <tiles:put name="entityId" beanName="entityId"/>
    </tiles:insert>
  </c:when>
</c:choose>
    </td>
  </tr>
</table>

<tiles:insert definition=".page.footer"/>
