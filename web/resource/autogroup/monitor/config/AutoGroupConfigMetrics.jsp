<%@ page language="java" %>
<%@ taglib uri="struts-bean" prefix="bean" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<!-- CONSTANT DEFINITIONS -->

<hq:constant classname="org.hyperic.hq.ui.Constants" 
                 symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 
<hq:constant classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
                 symbol="APPDEF_TYPE_GROUP" var="CONST_GROUP_TYPE" /> 

<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="cname" value="${ChildResourceType.name}"/>
<c:set var="ctype" value="${param.ctype}"/>
    
<tiles:importAttribute name="section" ignore="true"/>
<c:if test="${empty section}">
  <hq:resourceTypeName var="section" typeId="${param.type}"/>
</c:if>

<c:set var="widgetInstanceName" value="configMetricsList"/>
<script  src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');  
</script>

<c:url var="selfAction" value="/resource/${section}/monitor/Config.do">
 <c:param name="mode" value="configure"/>
 <c:param name="rid" value="${Resource.id}"/>
 <c:param name="type" value="${Resource.entityId.type}"/>
</c:url>

<html:form action="/resource/${section}/monitor/config/ConfigMetrics">
<html:hidden property="rid" value="${Resource.id}"/>
<html:hidden property="type" value="${Resource.entityId.type}"/>
<html:hidden property="ctype" value="${ctype}"/>

<!--  PAGE TITLE -->
<c:choose>
  <c:when test="${not empty Resource.name}">
<tiles:insert definition=".page.title.resource.autogroup">
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
  <tiles:put name="subTitleName" beanName="cname"/>
</tiles:insert>
  </c:when>
  <c:otherwise>
<tiles:insert definition=".page.title.resource.autogroup.empty">
  <tiles:put name="titleName" beanName="cname"/>
</tiles:insert>
  </c:otherwise>
</c:choose>

<!-- default a null button -->
<html:image page="/images/spacer.gif" border="0" property="nullBtn" />

<c:if test="${Resource.entityId.type == CONST_GROUP_TYPE }">
    <c:if test="${Resource.size == 0}">
        <c:out value="no members"/>
        <c:set var="nomembers" value="true"/>
    </c:if>
</c:if>
<!-- CONTROL BAR -->
<c:set var="nocontrol" value=".nocontrol"/>

<!--  COLLECT METRICS TAB -->
<c:set var="tmpTabs" value=".tabs.resource.${section}.monitor.configVisibility${nocontrol}"/>
<tiles:insert beanName="tmpTabs">
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
  <tiles:put name="autogroupResourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="autogroupResourceType" beanName="ctype"/>
  <tiles:put name="entityIds" beanName="EntityIds"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>


<hq:constant symbol="MONITOR_ENABLED_ATTR" var="CONST_ENABLED" />

<c:choose>
 <c:when test="${requestScope[CONST_ENABLED]}">
 <br/>

<tiles:insert definition=".resource.common.monitor.config.editConfigMetricsVisibility"/>

<c:set var="tmpMetrics" value="${requestScope.availabilityMetrics}"/>
<c:set var="tmpAddToListUrl" value="/resource/${section}/monitor/Config.do?mode=addMetrics&eid=${Resource.entityId.appdefKey}"/>
<tiles:insert definition=".resource.common.monitor.config.toolbar.addToList">
  <tiles:put name="showAddToListBtn" value="false"/>
  <tiles:put name="addToListUrl" beanName="tmpAddToListUrl"/>
  <c:if test="${not empty nomembers}">
      <tiles:put name="showAddToListBtn" value="false"/>
  </c:if>
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
   <c:url var="enableControlLink" value="/resource/${section}/Inventory.do">
    <c:param name="mode" value="editConfig"/>
    <c:param name="rid" value="${Resource.id}"/>
    <c:param name="type" value="${Resource.entityId.type}"/>
   </c:url>
   <c:set var="tmpMessage">
    <fmt:message key="resource.common.monitor.NotEnabled.MonitoringNotEnabled"/> <fmt:message key="resource.common.monitor.NotEnabled.ToEnable"/> <html:link href="${enableControlLink}"><fmt:message key="resource.common.monitor.NotEnabled.ConfPropLink"/></html:link> <fmt:message key="resource.common.monitor.NotEnabled.InInventory"/>
   </c:set>
   <tiles:insert definition=".portlet.notenabled">
    <tiles:put name="message" beanName="tmpMessage"/>
   </tiles:insert>

</c:otherwise>
</c:choose>

<tiles:insert definition=".page.footer"/>
</html:form>

