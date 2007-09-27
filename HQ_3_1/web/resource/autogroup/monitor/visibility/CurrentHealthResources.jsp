<%@ page language="java" %>
<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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


<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="ctype"/>
<tiles:importAttribute name="showProblems" ignore="true"/>
<tiles:importAttribute name="showOptions" ignore="true"/>

<c:if test="${showProblems}">
  <script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
</c:if>

<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM" var="CONST_PLATFORM" />

<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER" var="CONST_SERVER" />

<tiles:insert page="/resource/common/monitor/visibility/ResourcesTab.jsp"/>

<table width="285" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td>
      <tiles:insert definition=".resource.autogroup.monitor.visibility.listchildresources">
        <tiles:put name="mode" beanName="mode"/>
        <tiles:put name="internal" value="false"/>
        <tiles:put name="childResourcesTypeKey" value="resource.common.inventory.security.ResourceTypeLabel"/>
        <c:if test="${mode == 'currentHealth'}">
          <tiles:put name="checkboxes" value="true"/>
        </c:if>
      </tiles:insert>
    </td>
  </tr>
  <tr>
    <td>
  <c:choose>
    <c:when test="${Resource.entityId.type == CONST_SERVER}">
      <tiles:insert definition=".resource.service.monitor.visibility.serverHealth">
        <tiles:put name="mode" beanName="mode"/>
        <tiles:put name="summaries" beanName="HostHealthSummaries"/>
        <tiles:put name="checkboxes" beanName="showProblems"/>
      </tiles:insert>
    </c:when>
    <c:when test="${Resource.entityId.type == CONST_PLATFORM}">
      <tiles:insert definition=".resource.server.monitor.visibility.platformHealth">
        <tiles:put name="mode" beanName="mode"/>
        <tiles:put name="summaries" beanName="HostHealthSummaries"/>
        <tiles:put name="checkboxes" beanName="showProblems"/>
      </tiles:insert>
    </c:when>
  </c:choose>
    </td>
  </tr>
  <tr>
    <td>
  <c:choose>
    <c:when test="${showProblems}">
      <tiles:insert definition=".resource.common.monitor.visibility.problemmetrics">
        <tiles:put name="ctype" beanName="ctype"/>
      </tiles:insert>
    </c:when>
    <c:when test="${showOptions}">
      <tiles:insert page="/resource/common/monitor/visibility/MetricsDisplayOptions.jsp"/>
    </c:when>
  </c:choose>
    </td>
  </tr>
</table>
