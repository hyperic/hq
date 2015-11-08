<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

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


<!-- CONSTANT DEFINITIONS -->

<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 
<hq:constant classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
                 symbol="APPDEF_TYPE_GROUP" var="CONST_GROUP_TYPE" /> 
<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>
    
<tiles:importAttribute name="section" ignore="true"/>
<c:set var="entityTypeId" value="${param.aetid}"/>
<c:set var="entityId" value="${Resource.entityId}"/>

<c:if test="${empty section}">
  <hq:resourceTypeName var="section" typeId="${entityId.type}"/>
</c:if>

<c:set var="widgetInstanceName" value="configMetricsList"/>
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
</jsu:script>
<c:url var="selfAction" value="/resource/${section}/monitor/Config.do">
 <c:param name="mode" value="configure"/>
 <c:param name="rid" value="${Resource.id}"/>
 <c:param name="type" value="${entityId.type}"/>
</c:url>
<%--
TODO take care of Autogroup and Group
--%>
<s:form id="MonitoringConfigForm" name="MonitoringConfigForm" action="configMetricsAction" onSubmit="monitoringConfigFormSubmission()">
<c:choose>
<c:when test="${not empty entityTypeId}">
<!-- resource type wasnt empty -->
    <s:hidden theme="simple" name="aetid" value="%{#attr.entityTypeId}"/>
</c:when>
<c:otherwise>
    <s:hidden theme="simple" name="rid" value="%{#attr.Resource.id}"/>
    <s:hidden theme="simple" name="type" value="%{#attr.entityId.type}"/>
</c:otherwise>
</c:choose>
<!--  PAGE TITLE -->
<c:set var="ignoreBreadcrumb" value="false" scope="request"/>
<c:set var="noTitle" value="false" scope="request"/>
<c:set var="tmpTitle" value=".page.title.resource.platform.full"/>
<tiles:insertDefinition name="${tmpTitle}">
  <tiles:putAttribute name="resource" value="${Resource}"/>
  <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
  <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
  <c:choose>
    <c:when test="${not empty ResourceType}">
     <tiles:putAttribute name="titleName">
     	<s:a action="monitorConfig">
     		<s:param name="mode" value="monitor"/>
     		<fmt:message key="admin.home.ResourceTemplates"/>
     	</s:a> &gt;
        <c:out value="${ResourceType.name}"/> <c:out value="${section}"/>s</tiles:putAttribute>
      <tiles:putAttribute name="linkUrl" value=""/>
    </c:when>
    <c:otherwise>
      <tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
    </c:otherwise>
  </c:choose>
  <c:choose>
   <c:when test="${not empty ChildResourceType}">
     <tiles:putAttribute name="subTitleName" value="${ChildResourceType.name}"/>
   </c:when>
   <c:otherwise>
     <tiles:putAttribute name="titleKey" value="${resource.common.monitor.visibility.config.ConfigureVisibility.PageTitle}"/>
   </c:otherwise>
 </c:choose>
</tiles:insertDefinition>

<c:if test="${entityId.type == CONST_GROUP_TYPE }">
    <c:if test="${Resource.size == 0}">
        <c:out value="no members"/>
        <c:set var="nomembers" value="true"/>
    </c:if>
</c:if>
<!-- CONTROL BAR -->
<c:if test="${ section eq 'service' || section eq 'group' || section eq 'server'}">
 <c:if test="${ !canControl }">
  <c:set var="nocontrol" value=".nocontrol"/>
 </c:if> 
</c:if>

<c:if test="${not empty entityTypeId}">
  <c:set var="nocontrol" value=".defaults"/>
</c:if>

<!--  COLLECT METRICS TAB -->
<c:set var="tmpTabs" value=".tabs.resource.${section}.monitor.configVisibility${nocontrol}"/>
<tiles:insertDefinition name="${tmpTabs}">
 <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
 <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
 <c:if test="${not empty EntityIds}">
   <tiles:putAttribute name="entityIds" value="${EntityIds}"/>
 </c:if>
</tiles:insertDefinition>

<c:if test="${empty Resource}">
<!-- Template config -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorBlock"><img src='<s:url value="/images/tt_error.gif"/>' height="11" width="10" border="0" alt=""/></td>
    <td class="ErrorBlock" width="100%">
      <fmt:message key="admin.resource.templates.Warning">
        <fmt:param value="${ResourceType.name}"/>
        <fmt:param value="${section}"/>
      </fmt:message>
    </td>
  </tr>
</table>
</c:if>

<tiles:insertDefinition name=".portlet.error"/>
<tiles:insertDefinition name=".portlet.confirm"/>


<hq:constant symbol="MONITOR_ENABLED_ATTR" var="CONST_ENABLED" />

<c:choose>
 <c:when test="${requestScope[CONST_ENABLED]}">
 <br/>

<tiles:insertDefinition name=".resource.common.monitor.config.editConfigMetricsVisibility"/>

<c:set var="tmpMetrics" value="${requestScope.availabilityMetrics}"/>
<tiles:insertDefinition name=".resource.common.monitor.config.toolbar.addToList">
  <tiles:putAttribute name="showAddToListBtn" value="false"/>
  <tiles:putAttribute name="useDisableBtn" value="true"/>
  <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
  <tiles:putAttribute name="listItems" value="${tmpMetrics}"/>
  <tiles:putAttribute name="listSize" value="${listSize}"/>
  <%--
  When derived metrics are exposed through this UI, then the list can
  grow long and the pagination will be necessary (and will need to be
  fixed, since it wasn't working anyway).  For now, we'll suppress the
  pagination controls per PR 7821
  --%>
  <tiles:putAttribute name="showPagingControls" value="false"/>
  <tiles:putAttribute name="pageSizeParam" value="ps"/>
  <tiles:putAttribute name="pageSizeAction" value="${selfAction}"/>
  <tiles:putAttribute name="pageNumAction" value="${selfAction}"/>
  <tiles:putAttribute name="defaultSortColumn" value="4"/>
</tiles:insertDefinition>

</c:when>
 <c:when test="${not empty entityTypeId}">
 <br/>
<%-- 
<c:if test="${section eq 'group'}">

 <tiles:insertDefinition name=".resource.group.monitor.config.Availability">
  <tiles:putAttribute name="Resource" value="${Resource}"/>
 </tiles:insertDefinition>
 <br/>
</c:if>
  --%>
<tiles:insertDefinition name=".resource.common.monitor.config.editConfigMetricsVisibility"/>

<c:set var="tmpMetrics" value="${requestScope.availabilityMetrics}"/>
<tiles:insertDefinition name=".resource.common.monitor.config.toolbar.addToList">
  <tiles:putAttribute name="showAddToListBtn" value="false"/>
  <tiles:putAttribute name="useDisableBtn" value="true"/>
  <tiles:putAttribute name="useEnableIndicatorsBtn" value="true"/>
  <tiles:putAttribute name="useDisableIndicatorsBtn" value="true"/>
  <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
  <tiles:putAttribute name="addToListParamName" value="rid"/>
  <tiles:putAttribute name="addToListParamValue" value="${Resource.id}"/>
  <tiles:putAttribute name="listItems" value="${tmpMetrics}"/>
  <tiles:putAttribute name="listSize" value="${listSize}"/>
  <%--
  When derived metrics are exposed through this UI, then the list can
  grow long and the pagination will be necessary (and will need to be
  fixed, since it wasn't working anyway).  For now, we'll suppress the
  pagination controls per PR 7821
  --%>
  <tiles:putAttribute name="showPagingControls" value="false"/>
  <tiles:putAttribute name="pageSizeParam" value="ps"/>
  <tiles:putAttribute name="pageSizeAction" value="${selfAction}"/>
  <tiles:putAttribute name="pageNumAction" value="${selfAction}"/>
  <tiles:putAttribute name="defaultSortColumn" value="4"/>
</tiles:insertDefinition>

</c:when>
<c:otherwise>
 <c:choose>
  <c:when test="${section eq 'group'}">
   <c:set var="tmpMessage" >
    <fmt:message key="resource.group.monitor.visibility.NotEnabled"/>
   </c:set> 
  </c:when>
  <c:otherwise>
    <c:url var="enableControlLink" value="/resource/${section}/Inventory.do">
      <c:param name="mode" value="editConfig"/>
      <c:param name="rid" value="${Resource.id}"/>
      <c:param name="type" value="${entityId.type}"/>
    </c:url>
    <c:set var="tmpMessage">
      <fmt:message key="resource.common.monitor.NotEnabled.MonitoringNotEnabled"/> <fmt:message key="resource.common.monitor.NotEnabled.ToEnable"/> <s:a href="%{#attr.enableControlLink}"><fmt:message key="resource.common.monitor.NotEnabled.ConfPropLink"/></s:a> <fmt:message key="resource.common.monitor.NotEnabled.InInventory"/>
    </c:set>
  </c:otherwise>
 </c:choose>
   <tiles:insertDefinition name=".portlet.notenabled">
    <tiles:putAttribute name="message" value="${tmpMessage}"/>
   </tiles:insertDefinition>

</c:otherwise>
</c:choose>
<input type="hidden" value="" name="clickedType" id="clickedType"/>
<tiles:insertDefinition name=".page.footer"/>
</s:form>

<script>
	var clickedType = '';
</script>
<jsu:script>
	
	function monitoringConfigFormSubmission() {
		document.forms.MonitoringConfigForm.clickedType.value = clickedType;
	}
	
	
</jsu:script>