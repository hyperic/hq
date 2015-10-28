<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>

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

<s:hidden theme="simple" name="m" value="%{#attr.ViewChartForm.m[0]}"/>

<c:set var="widgetInstanceName" value="listMetrics"/>
<jsu:script>
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<!--  PARTICIPATING RESOURCE TITLE -->
<tiles:insertDefinition name=".header.tab">
<tiles:putAttribute name="tabKey" value="resource.common.monitor.visibility.chart.ParticipatingResourceTab"/>
</tiles:insertDefinition>
<!--  /  -->

<div id="listDiv">
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
  <tr class="ListHeaderDark">
    <td width="29%" class="ListHeaderInactiveSorted"><fmt:message
      key="resource.common.monitor.visibility.MetricNameTH"/></td>
    <td width="1%" class="ListHeaderInactiveSorted"><img
      src='<s:url value="/images/spacer.gif"/>' width="1" height="1" alt=""
      border="0"/></td>
    <td width="69%" class="ListHeaderInactiveSorted"><fmt:message
      key="resource.common.monitor.visibility.ResourceTH"/></td>
  </tr>
  
  <c:forEach var="resource" varStatus="rStatus" items="${resources}">
  <c:url var="resourceUrl" value="/resourceAction.action">
    
    <c:param name="eid" value="${resource.entityId}"/>
  </c:url>
  <tr class="ListRow">
    <c:if test="${rStatus.first}">
    <td rowspan="<c:out value='${resourcesSize}'/>"
      class="ListCellPrimary" valign="top"><c:out value="${metricSummaries[0].label}"/></td>
    </c:if>
      <c:set var="maxResources"><c:out value="${checkedResourcesSize}"/></c:set>
      <c:set var="maxMessage">
      <fmt:message key="resource.common.monitor.visibility.chart.TooManyResources">
      <fmt:param value="${maxResources}"/>
      </fmt:message>
      </c:set>
    <td class="ListCellLeftLineNoPadding" valign="top">
		<c:set var="resId" value="${resource.id}"/>
		<c:set var="checked" value=""/>
		
		<c:forEach var="tmpRes" items="${checkedResources}">
			<c:if test="${tmpRes.id == resId}">
				<c:set var="checked" value="checked"/>
			</c:if>
		</c:forEach>
	<input type="checkbox"
      name="resourceIds" value="${resource.id}" <c:out value="${checked}"/>
      onclick="ToggleSelection(this, widgetProperties, ${maxResources}, '${maxMessage}');"
      id="resourceList" class="resourceList"/></td>
      <c:set var="resCellClass" value="ListCellPrimary"/>
    <td
      class="<c:out value='${resCellClass}'/>" valign="top">
      <s:a href="%{#attr.resourceUrl}"><c:out value="${resource.name}"/></s:a>
    </td>
  </tr>
  </c:forEach>
</table>

<!--  REDRAW SELECTED TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <tr>
    <td id="<c:out value="${widgetInstanceName}"/>RedrawTd"><div
      id="<c:out value="${widgetInstanceName}"/>RedrawDiv"><img 
      src='<s:url value="/images/tbb_redrawselectedonchart_gray.gif"/>' width="171"
      height="16" alt="" border="0"/></div></td>
    <td width="100%" align="right">&nbsp;</td>
  </tr>
</table>
<!--  /  -->

</div>

<input type="hidden" id="privateChart">
<jsu:script>
  	testCheckboxes('<c:out value="${widgetInstanceName}"/>');
</jsu:script>