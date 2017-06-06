<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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
<tiles:importAttribute name="showHostPlatform" ignore="true"/>
<tiles:importAttribute name="errKey" ignore="true"/>
<tiles:importAttribute name="tabKey"/>
<tiles:importAttribute name="hostResourcesHealthKey"/>
<tiles:importAttribute name="checkboxes" ignore="true"/>

<c:set var="mode" value="${param.mode}"/>
<c:if test="${empty mode}">
  <c:set var="mode" value="currentHealth"/>
</c:if>

<c:if test="${checkboxes}">
  	<c:set var="widgetInstanceName" value="hostResources"/>
  	<jsu:script>
    	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
    	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	</jsu:script>
</c:if>

<c:forEach var="summary" items="${HostHealthSummaries}" varStatus="status">
  <div id="<c:out value="${summary.resourceTypeId}_${summary.resourceId}_menu"/>" class="menu">
    <ul>
      <li><div class="BoldText"><fmt:message key="${hostResourcesHealthKey}"/> <fmt:message key="resource.common.monitor.visibility.TypeTH"/></div>
      <c:out value="${summary.resourceTypeName}"/>
      </li>
    <c:if test="${showHostPlatform}">
      <li><div class="BoldText"><fmt:message key="resource.common.monitor.visibility.HostPlatformTH"/></div>
      <s:a action="%{#attr.mode}MonitorPlatformVisibility.action" >
      	<s:param name="mode" value="%{#attr.param['mode']}"/>
      	<s:param name="eid">${summary.parentResourceTypeId}:${summary.parentResourceId}</s:param>
      	<c:out value="${summary.parentResourceName}" default="PARENT RESOURCE NAME NOT SET"/>
      </s:a>
    </c:if>
      <li><div class="BoldText"><fmt:message key="resource.common.monitor.visibility.USAGETH"/></div>
        <hq:metric metric="${summary.throughput}" unit="none"  defaultKey="common.value.notavail" />
      </li>
    </ul>
  </div>

  <c:set var="count" value="${status.count}"/>
</c:forEach>

<hq:pageSize var="pageSize"/>
<!--  HOST RESOURCES CONTENTS -->
<c:choose>
  <c:when test="${count > 5}">
    <div id="hostResourcesDiv" class="scrollable">
  </c:when>
  <c:otherwise>
    <div id="hostResourcesDiv">
  </c:otherwise>
</c:choose>
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
  <tr class="ListHeaderLight">
    <c:if test="${not empty HostHealthSummaries && checkboxes}">
    <td class="ListHeaderCheckbox" width="3%"><input type="checkbox" onclick="ToggleAllGroup(this, widgetProperties, '<c:out value="${listMembersName}"/>')" name="listToggleAll"></td>
    </c:if>

    <td class="ListHeader"><fmt:message key="${tabKey}"/></td>
    <c:if test="${not empty HostHealthSummaries}">
    <td width="24" class="ListHeaderCheckbox"><fmt:message key="resource.common.monitor.visibility.AVAILTH"/></td>
    <td class="ListHeaderInactive" width="14">&nbsp;</td>
    </c:if>
  </tr>

    <c:forEach var="summary" items="${HostHealthSummaries}">
  <tr class="ListRow"> 
	  <c:if test="${summary.resourceTypeId=='1'}" >
	  <c:url var="url" value="currentHealthMonitorPlatformVisibility.action">
		<c:param name="mode" value="currentHealth"/>
		<c:param name="eid" value="${summary.resourceTypeId}:${summary.resourceId}"/>
	  </c:url>
	  </c:if>
	  <c:if test="${summary.resourceTypeId=='2'}" >
	  <c:url var="url" value="currentHealthMonitorServerVisibility.action">
		<c:param name="mode" value="currentHealth"/>
		<c:param name="eid" value="${summary.resourceTypeId}:${summary.resourceId}"/>
	  </c:url>
	  </c:if>
	  <c:if test="${summary.resourceTypeId=='3'}" >
	  <c:url var="url" value="currentHealthMonitorServiceVisibility.action">
		<c:param name="mode" value="currentHealth"/>
		<c:param name="eid" value="${summary.resourceTypeId}:${summary.resourceId}"/>
	  </c:url>
	  </c:if>
	  <c:if test="${summary.resourceTypeId=='5'}" >
	  <c:url var="url" value="currentHealthMonitorGroupVisibility.action">
		<c:param name="mode" value="currentHealth"/>
		<c:param name="eid" value="${summary.resourceTypeId}:${summary.resourceId}"/>
	  </c:url>
	  </c:if>
    <c:if test="${checkboxes}">
    <td class="ListCellCheckbox" width="3%"><s:checkbox name="host" value="%{#attr.summary.resourceTypeId}:%{#attr.summary.resourceId}" id="%{#attr.listMembersName}"/></td>
    </c:if>

    <td class="ListCell"><a href="<c:out value="${url}"/>"><c:out value="${summary.resourceName}"/></a></td>
    <td class="ListCellCheckbox">
    <tiles:insertTemplate template="/resource/common/monitor/visibility/AvailIconNG.jsp">
        <tiles:putAttribute name="availability" value="${summary.availability}" />
    </tiles:insertTemplate>
    </td>
    <td class="ListCellCheckbox resourceCommentIcon"
        onmouseover="menuLayers.show('<c:out value="${summary.resourceTypeId}" />_<c:out value="${summary.resourceId}" />_menu', event)" 
        onmouseout="menuLayers.hide()">&nbsp;
	</td>
  </tr>

    </c:forEach>
</table>

<c:if test="${empty HostHealthSummaries}">
  <c:if test="${empty errKey}">
    <c:set var="errKey" value="resource.common.monitor.visibility.NoHealthsEtc" />
  </c:if>
  <tiles:insertDefinition name=".resource.common.monitor.visibility.HostHealthError">
    <tiles:putAttribute name="errKey" value="${errKey}" />
  </tiles:insertDefinition>
</c:if>
</div>
