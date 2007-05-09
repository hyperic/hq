<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
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


<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 

<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="selfAction" value="/resource/group/monitor/Visibility.do?mode=resourceMetrics&eid=${entityId.type}:${Resource.id}"/>

<c:set var="memberTypeLabel" value="${Resource.groupTypeLabel}" />

<tiles:insert definition=".resource.common.monitor.visibility.dashminitabs">
  <tiles:put name="selectedIndex" value="1"/>
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
  <tiles:put name="entityType" value="group"/>
  <c:if test="${perfSupported}">
    <tiles:put name="tabListName" value="perf"/>
  </c:if>
</tiles:insert>

<table width="100%" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td style="background-color:#DBE3F5;">
<html:form action="/resource/group/monitor/visibility/GroupMetrics">

<tiles:insert definition=".resource.common.monitor.visibility.metricsDisplay">
  <tiles:put name="summaries" beanName="MetricSummaries"/>
  <tiles:put name="buttonMode" value="noleft"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
  <tiles:put name="useChartMulti" value="false"/>
  <tiles:put name="useCurrent" value="true"/>
</tiles:insert>
  <%--
  <tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.group.monitor.visibility.CurrentHealthOfCollecting"/>
  <tiles:put name="tabName" beanName="memberTypeLabel" />
</tiles:insert>
 --%>
    <div style="padding-top:4px;padding-bottom:4px;border-top:1px solid #ABB1C7;font-weight:bold;">
    <fmt:message key="resource.group.monitor.visibility.CurrentHealthOfCollecting">
        <fmt:param value="${memberTypeLabel}"/>
    </fmt:message>
    </div>
<tiles:insert definition=".resource.common.monitor.visibility.childResourcesCurrentHealthByType">
  <tiles:put name="summaries" beanName="pagedMembers"/>
  <tiles:put name="memberTypeLabel" beanName="memberTypeLabel" />
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>

<html:hidden property="h"/>
<html:hidden property="rid"/>
<html:hidden property="type"/>
<html:hidden name="Resource" property="name"/>
<html:hidden property="ctype" value="${Resource.groupEntType}:${Resource.groupEntResType}" />
<html:hidden property="appdefType" value="${Resource.groupEntType}" />
</html:form>
    </td>
  </tr>
</table>
