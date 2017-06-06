<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
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
	<jsu:importScript path="/js/listWidget.js" />
</c:if>

<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM" var="CONST_PLATFORM" />

<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER" var="CONST_SERVER" />

<tiles:insertTemplate template="/resource/common/monitor/visibility/ResourcesTabNG.jsp"/>

<table width="285" cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td>
      <tiles:insertDefinition name=".resource.autogroup.monitor.visibility.listchildresources">
        <tiles:putAttribute name="mode" value="${mode}"/>
        <tiles:putAttribute name="internal" value="false"/>
        <tiles:putAttribute name="childResourcesTypeKey" value="resource.common.inventory.security.ResourceTypeLabel"/>
        <c:if test="${mode == 'currentHealth'}">
          <tiles:putAttribute name="checkboxes" value="true"/>
        </c:if>
      </tiles:insertDefinition>
    </td>
  </tr>
  <tr>
    <td>
  <c:choose>
    <c:when test="${Resource.entityId.type == CONST_SERVER}">
      <tiles:insertDefinition name=".resource.service.monitor.visibility.serverHealth">
        <tiles:putAttribute name="mode" value="${mode}"/>
        <tiles:putAttribute name="summaries" value="${HostHealthSummaries}"/>
        <tiles:putAttribute name="checkboxes" value="${showProblems}"/>
      </tiles:insertDefinition>
    </c:when>
    <c:when test="${Resource.entityId.type == CONST_PLATFORM}">
      <tiles:insertDefinition name=".resource.server.monitor.visibility.platformHealth">
        <tiles:putAttribute name="mode" value="${mode}"/>
        <tiles:putAttribute name="summaries" value="${HostHealthSummaries}"/>
        <tiles:putAttribute name="checkboxes" value="${showProblems}"/>
      </tiles:insertDefinition>
    </c:when>
  </c:choose>
    </td>
  </tr>
  <tr>
    <td>
  <c:choose>
    <c:when test="${showProblems}">
      <tiles:insertDefinition name=".resource.common.monitor.visibility.problemmetrics">
        <tiles:putAttribute name="ctype" value="${ctype}"/>
      </tiles:insertDefinition>
    </c:when>
    <c:when test="${showOptions}">
      <tiles:insertTemplate template="/resource/common/monitor/visibility/MetricsDisplayOptionsNG.jsp"/>
    </c:when>
  </c:choose>
    </td>
  </tr>
</table>
