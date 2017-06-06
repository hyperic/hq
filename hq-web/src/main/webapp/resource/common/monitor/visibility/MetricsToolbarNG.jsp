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


<tiles:importAttribute name="widgetInstanceName"/>
<tiles:importAttribute name="useChartButton" ignore="true"/>
<tiles:importAttribute name="useAddButton" ignore="true"/>
<tiles:importAttribute name="useRemoveButton" ignore="true"/>
<tiles:importAttribute name="useCompareButton" ignore="true"/>
<tiles:importAttribute name="useCurrentButton" ignore="true"/>
<tiles:importAttribute name="useReloadButton" ignore="true"/>
<tiles:importAttribute name="usePager" ignore="true"/>
<tiles:importAttribute name="listItems" ignore="true"/>
<tiles:importAttribute name="listSize" ignore="true"/>
<tiles:importAttribute name="pageSizeParam" ignore="true"/>
<tiles:importAttribute name="pageSizeAction" ignore="true"/>
<tiles:importAttribute name="pageNumParam" ignore="true"/>
<tiles:importAttribute name="pageNumAction" ignore="true"/>
<tiles:importAttribute name="defaultSortColumn" ignore="true"/>

<c:if test="${empty useChartButton && (useAddButton || useRemoveButton)}">
  <c:set var="useChartButton" value="true"/>
</c:if>

<!--  METRICS TOOLBAR -->
<table width="100%" cellpadding="5" cellspacing="0" border="0" class="ToolbarContent">
  <tr>
<c:if test="${useChartButton}">
    <td width="1" align="left" id="<c:out value="${widgetInstanceName}"/>chartSelectedMetricsTd"><div id="<c:out value="${widgetInstanceName}"/>chartSelectedMetricsDiv"><img src='<s:url value="/images/tbb_chartselectedmetrics_gray.gif"/>'  border="0"/></div></td>
</c:if>
<c:if test="${useAddButton}">
    <td width="1" align="left" id="<c:out value="${widgetInstanceName}"/>addToFavoritesTd"><div id="<c:out value="${widgetInstanceName}"/>addToFavoritesDiv"><img src='<s:url value="/images/tbb_addToFavorites_gray.gif"/>' border="0"/></div></td>
</c:if>
<c:if test="${useRemoveButton}">
    <td width="1" align="left" id="<c:out value="${widgetInstanceName}"/>removeFromFavoritesTd"><div id="<c:out value="${widgetInstanceName}"/>removeFromFavoritesDiv"><img src='<s:url value="/images/tbb_removeFromFavorites_gray.gif"/>' border="0"/></div></td>
</c:if>
<c:if test="${useCompareButton}">
    <td width="1" align="left" id="<c:out value="${widgetInstanceName}"/>compareTd"><div id="<c:out value="${widgetInstanceName}"/>compareDiv"><img src='<s:url value="/images/tbb_compareMetricsOfSelected_gray.gif"/>' border="0"/></div></td>
</c:if>
<c:if test="${useCurrentButton}">
    <td width="100%" align="right"><fmt:message key="resource.common.monitor.visibility.GetCurrentValuesLabel"/></td>
    <td><a href="#" onclick="metricsControlAction.submit();return false;"><img  src="/images/4.0/icons/accept.png" border="0"/></td>
</c:if>
<c:if test="${useReloadButton}">
    <td width="100%" align="right"><fmt:message key="resource.common.monitor.visibility.GetCurrentValuesLabel"/></td>
    <td><a href="javascript:location.reload();"><img src='<s:url value="/images/4.0/icons/accept.png"/>' border="0"/></a></td>
</c:if>
    <td width="100%"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" alt="" border="0"/></td>
<c:if test="${usePager}">
  <tiles:insertDefinition name=".controls.paging">
   <tiles:putAttribute name="listItems" value="${listItems}"/>
   <tiles:putAttribute name="listSize" value="${listSize}"/>
   <tiles:putAttribute name="pageSizeParam" value="${pageSizeParam}"/>
   <tiles:putAttribute name="pageSizeAction" value="${pageSizeAction}"/>
   <tiles:putAttribute name="pageNumParam" value="${pageNumParam}"/>
   <tiles:putAttribute name="pageNumAction" value="${pageNumAction}"/>
   <tiles:putAttribute name="defaultSortColumn" value="${defaultSortColumn}"/>
  </tiles:insertDefinition>
</c:if>
  </tr>
</table>
<!--  /  -->
