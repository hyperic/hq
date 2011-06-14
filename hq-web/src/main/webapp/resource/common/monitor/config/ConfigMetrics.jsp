<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
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

<html:form action="/resource/${section}/monitor/config/ConfigMetrics">
<c:choose>
<c:when test="${not empty entityTypeId}">
<!-- resource type wasnt empty -->
    <html:hidden property="aetid" value="${entityTypeId}"/>
</c:when>
<c:otherwise>
    <html:hidden property="rid" value="${Resource.id}"/>
    <html:hidden property="type" value="${entityId.type}"/>
</c:otherwise>
</c:choose>
<!--  PAGE TITLE -->
<c:set var="tmpTitle" value=".page.title.resource.${section}.full"/>
<tiles:insert beanName="tmpTitle">
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
  <c:choose>
    <c:when test="${not empty ResourceType}">
     <tiles:put name="titleName">
     	<html:link action="/admin/config/Config">
     		<html:param name="mode" value="monitor"/>
     		<fmt:message key="admin.home.ResourceTemplates"/>
     	</html:link> &gt;
        <bean:write name="ResourceType" property="name"/> <c:out value="${section}"/>s</tiles:put>
      <tiles:put name="linkUrl" value=""/>
    </c:when>
    <c:otherwise>
      <tiles:put name="eid" beanName="entityId" beanProperty="appdefKey" />
    </c:otherwise>
  </c:choose>
  <c:choose>
   <c:when test="${not empty ChildResourceType}">
     <tiles:put name="subTitleName" beanName="ChildResourceType" beanProperty="name"/>
   </c:when>
   <c:otherwise>
     <tiles:put name="titleKey" beanName="resource.common.monitor.visibility.config.ConfigureVisibility.PageTitle"/>
   </c:otherwise>
 </c:choose>
</tiles:insert>

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
<tiles:insert beanName="tmpTabs">
 <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
 <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
 <c:if test="${not empty EntityIds}">
   <tiles:put name="entityIds" beanName="EntityIds"/>
 </c:if>
</tiles:insert>

<c:if test="${empty Resource}">
<!-- Template config -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorBlock"><html:img page="/images/tt_error.gif" height="11" width="10" border="0" alt=""/></td>
    <td class="ErrorBlock" width="100%">
      <fmt:message key="admin.resource.templates.Warning">
        <fmt:param value="${ResourceType.name}"/>
        <fmt:param value="${section}"/>
      </fmt:message>
    </td>
  </tr>
</table>
</c:if>

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>


<hq:constant symbol="MONITOR_ENABLED_ATTR" var="CONST_ENABLED" />

<c:choose>
 <c:when test="${requestScope[CONST_ENABLED]}">
 <br/>

<tiles:insert definition=".resource.common.monitor.config.editConfigMetricsVisibility"/>

<c:set var="tmpMetrics" value="${requestScope.availabilityMetrics}"/>
<tiles:insert definition=".resource.common.monitor.config.toolbar.addToList">
  <tiles:put name="showAddToListBtn" value="false"/>
  <tiles:put name="useDisableBtn" value="true"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="listItems" beanName="tmpMetrics"/>
  <tiles:put name="listSize" beanName="listSize"/>
  <%--
  When derived metrics are exposed through this UI, then the list can
  grow long and the pagination will be necessary (and will need to be
  fixed, since it wasn't working anyway).  For now, we'll suppress the
  pagination controls per PR 7821
  --%>
  <tiles:put name="showPagingControls" value="false"/>
  <tiles:put name="pageSizeParam" value="ps"/>
  <tiles:put name="pageSizeAction" beanName="selfAction"/>
  <tiles:put name="pageNumAction" beanName="selfAction"/>
  <tiles:put name="defaultSortColumn" value="4"/>
</tiles:insert>

</c:when>
 <c:when test="${not empty entityTypeId}">
 <br/>
<%-- 
<c:if test="${section eq 'group'}">

 <tiles:insert definition=".resource.group.monitor.config.Availability">
  <tiles:put name="Resource" beanName="Resource"/>
 </tiles:insert>
 <br/>
</c:if>
  --%>
<tiles:insert definition=".resource.common.monitor.config.editConfigMetricsVisibility"/>

<c:set var="tmpMetrics" value="${requestScope.availabilityMetrics}"/>
<tiles:insert definition=".resource.common.monitor.config.toolbar.addToList">
  <tiles:put name="showAddToListBtn" value="false"/>
  <tiles:put name="useDisableBtn" value="true"/>
  <tiles:put name="useIndicatorsBtn" value="true"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="addToListParamName" value="rid"/>
  <tiles:put name="addToListParamValue" beanName="Resource" beanProperty="id"/>
  <tiles:put name="listItems" beanName="tmpMetrics"/>
  <tiles:put name="listSize" beanName="listSize"/>
  <%--
  When derived metrics are exposed through this UI, then the list can
  grow long and the pagination will be necessary (and will need to be
  fixed, since it wasn't working anyway).  For now, we'll suppress the
  pagination controls per PR 7821
  --%>
  <tiles:put name="showPagingControls" value="false"/>
  <tiles:put name="pageSizeParam" value="ps"/>
  <tiles:put name="pageSizeAction" beanName="selfAction"/>
  <tiles:put name="pageNumAction" beanName="selfAction"/>
  <tiles:put name="defaultSortColumn" value="4"/>
</tiles:insert>

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
      <fmt:message key="resource.common.monitor.NotEnabled.MonitoringNotEnabled"/> <fmt:message key="resource.common.monitor.NotEnabled.ToEnable"/> <html:link href="${enableControlLink}"><fmt:message key="resource.common.monitor.NotEnabled.ConfPropLink"/></html:link> <fmt:message key="resource.common.monitor.NotEnabled.InInventory"/>
    </c:set>
  </c:otherwise>
 </c:choose>
   <tiles:insert definition=".portlet.notenabled">
    <tiles:put name="message" beanName="tmpMessage"/>
   </tiles:insert>

</c:otherwise>
</c:choose>

<tiles:insert definition=".page.footer"/>
</html:form>

