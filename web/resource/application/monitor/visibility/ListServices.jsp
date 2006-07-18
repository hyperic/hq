<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
<%@ taglib uri="display" prefix="display" %>
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



<tiles:importAttribute name="application"/>
<tiles:importAttribute name="services"/>
<tiles:importAttribute name="mode" />
<tiles:importAttribute name="serviceType" ignore="true" />
<tiles:importAttribute name="serviceCount"/>

<c:url var="spacerUrl" value="/images/spacer.gif" />
<c:set var="baseSelfAction" value="/resource/application/monitor/Visibility.do" />
<hq:pageSize var="pageSize"/>

<c:choose>
  <c:when test="empty serviceType">
    <c:url var="selfAction" value="${baseSelfAction}">
      <c:param name="mode" value="${mode}" />
      <c:param name="rid" value="${application.id}" />
      <c:param name="type" value="${application.entityId.type}" />
    </c:url>
  </c:when>
  <c:otherwise>
    <c:url var="selfAction" value="${baseSelfAction}">
      <c:param name="mode" value="${mode}" />
      <c:param name="rid" value="${application.id}" />
      <c:param name="type" value="${application.entityId.type}" />
      <c:param name="serviceType" value="${serviceType}" />
    </c:url>
  </c:otherwise>
</c:choose>

<html:form action="/resource/common/monitor/visibility/CompareResourceMetrics">
<div id="listDiv">
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
  <tr class="ListHeaderLight">
    <td width="1%" class="ListHeaderInactiveSorted"><html:img 
    page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    <td width="22%" class="ListHeaderInactiveSorted"><fmt:message 
    key="resource.common.monitor.visibility.ServiceTH"/><html:img 
    page="/images/tb_sortup_inactive.gif" width="9" height="9" border="0"/></td>
    <td width="23%" class="ListHeaderInactive"><fmt:message 
    key="resource.common.monitor.visibility.HostServerTH"/></td>
    <td width="23%" class="ListHeaderInactive"><fmt:message 
    key="resource.common.monitor.visibility.TypeTH"/></td>
    <td width="8%" colspan="2" class="ListHeaderCheckboxLeftLine"><fmt:message 
    key="resource.common.monitor.visibility.AlertsTH"/></td>
    <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message 
    key="resource.common.monitor.visibility.AVAILTH"/></td>
    <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message 
    key="resource.common.monitor.visibility.USAGETH"/></td>
    <td width="8%" class="ListHeaderCheckboxLeftLine"><fmt:message 
    key="resource.common.monitor.visibility.PERFTH"/></td>
  </tr>
<c:set var="iteration" value="0" />
<c:forEach var="service" items="${Services}">
  <tr class="ListRow">
    <td class="ListCellCheckbox" width="1%" align="left" valign="top">
    <input type="checkbox" name="compare" 
    class="availableListMember"
    onclick="ToggleSelectionCompare(this, widgetPropertiesListServices);" 
    value="<c:out value="${service.id}"/>"></td>
    <c:url var="inventoryUrl" 
      value="${param.url}/resource/service/Inventory.do">
      <c:param name="mode" value="view" />
      <c:param name="rid" value="${service.resource.id}" />
      <c:param name="type" value="${service.resource.entityId.type}" />
    </c:url>
    <td class="ListCell" width="20%" align="left" valign="top"><a 
    href="<c:out value="${inventoryUrl}" />"><c:out 
    value="${service.resource.name}" /></a></td>
    <td class="ListCell"><c:out value="${service.parentResource.name}" /></td>
    <td class="ListCell"><c:out value="${service.resource.appdefResourceTypeValue.name}" /></td>
    <td width="4%" class="ListCellCheckboxLeftLine"><html:img 
    page="/images/icon_alert.gif" width="11" height="11" alt="Alerts" 
    border="0" /></td>
    <td width="4%" class="ListCellRightNoLine" align="center"><span 
    class="MonitorMetricsBaseline"><c:out 
    value="${service.alerts}" /></span></td>
    <td class="ListCellCheckboxLeftLine" align="center">
    <c:choose>
    <c:when test="${empty service.available}">
    <html:img 
    page="/images/icon_available_error.gif" width="12" height="12" 
    alt="Error Determining Service Availability" 
    border="0"/>
    </c:when>
    <c:when test="${service.available == true}">
    <html:img 
    page="/images/icon_available_green.gif" width="12" height="12" 
    alt="Service Is Available" 
    border="0"/>
    </c:when>
    <c:when test="${service.available == false}">
    <html:img 
    page="/images/icon_available_red.gif" width="12" height="12" 
    alt="Service Is Not" 
    border="0"/>
    </c:when>
    </c:choose>
    </td>
    <td class="ListCellCheckboxLeftLine"><div class="MetricGreen"><html:img 
    page="/images/icon_available_green.gif" width="12" height="12" alt="" 
    border="0" align="left"/> 121</div></td>
    <td class="ListCellRight">12878</td>
  </tr>
</c:forEach>
</table>
</div>
<!--  /  -->
<tiles:insert definition=".toolbar.addToList">
  <tiles:put name="addToListUrl" beanName="addToListUrl"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="listItems" beanName="services"/>
  <tiles:put name="listSize" beanName="serviceCount"/>
  <tiles:put name="pageNumAction" beanName="selfAction"/>    
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="defaultSortColumn" value="5"/>
  <tiles:put name="pageSizeAction" beanName="selfAction" />
</tiles:insert>
</html:form>

