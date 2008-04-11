<%@ page language="java" %>
<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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


<tiles:insert page="/resource/common/monitor/visibility/ResourcesTab.jsp"/>

<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="showProblems" ignore="true"/>
<tiles:importAttribute name="showOptions" ignore="true"/>

<c:if test="${showProblems}">
  <script  src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
</c:if>

<table width="285" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td>
      <tiles:insert definition=".resource.application.monitor.visibility.listchildresources">
        <tiles:put name="mode" beanName="mode"/>
        <tiles:put name="childResourceType" value="3"/>
        <tiles:put name="childResourcesHealthKey" value="resource.server.inventory.ServicesTab" />
        <tiles:put name="childResourcesTypeKey" value="resource.application.monitor.visibility.ServiceTypeTH"/>
        <tiles:put name="checkboxes" beanName="showProblems"/>
      </tiles:insert>
    </td>
  </tr>
  <tr>
    <td>
      <tiles:insert definition=".resource.common.monitor.visibility.hostResourcesCurrentHealth">
        <tiles:put name="mode" beanName="mode"/>
        <tiles:put name="summaries" beanName="HostHealthSummaries"/>
        <tiles:put name="showHostPlatform" value="true"/>
        <tiles:put name="tabKey" value="resource.common.monitor.visibility.HostServersTab"/>
        <tiles:put name="hostResourcesHealthKey" value="resource.common.monitor.visibility.ServerTH"/>
        <tiles:put name="checkboxes" beanName="showProblems"/>
      </tiles:insert>
    </td>
  </tr>
  <tr>
    <td>
  <c:choose>
    <c:when test="${showProblems}">
      <tiles:insert definition=".resource.common.monitor.visibility.problemmetrics"/>
    </c:when>
    <c:when test="${showOptions}">
      <tiles:insert page="/resource/common/monitor/visibility/MetricsDisplayOptions.jsp"/>
    </c:when>
  </c:choose>
    </td>
  </tr>
</table>
