<%@ page language="java" %>
<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
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


<!-- NOT SURE
<tiles:importAttribute name="resource" ignore="true"/>
-->

<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM" var="CONST_PLATFORM" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER" var="CONST_SERVER" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVICE" var="CONST_SERVICE" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_APPLICATION" var="CONST_APPLICATION" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP" var="CONST_GROUP" />

<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>
<jsu:importScript path="/js/listWidget.js" />
<c:set var="widgetInstanceName" value="listAlertDefinitions"/>
<jsu:script>
	function setActiveInactive() {
	    document.RemoveConfigForm.setActiveInactive.value='y';
	    document.RemoveConfigForm.submit();
	}
	
	var pageData = new Array();
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>
<c:set var="entityId" value="${Resource.entityId}"/>
<c:url var="pnAction" value="listDefinitionsAlertsConfigPortal.action">
  <c:param name="mode" value="list"/>
  <c:param name="rid" value="${Resource.id}"/>
  <c:param name="type" value="${entityId.type}"/>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>
<c:url var="psAction" value="listDefinitionsAlertsConfigPortal.action">
  <c:param name="mode" value="list"/>
  <c:param name="rid" value="${Resource.id}"/>
  <c:param name="type" value="${entityId.type}"/>
  <c:if test="${not empty param.ps}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>
<c:url var="sortAction" value="listDefinitionsAlertsConfigPortal.action">
  <c:param name="mode" value="list"/>
  <c:param name="rid" value="${Resource.id}"/>
  <c:param name="type" value="${entityId.type}"/>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
</c:url>
<c:url var="newAction" value="newDefinitionAlertsConfigPortal.action">
	<c:param name="mode" value="new"/>
	<c:param name="eid" value="${entityId.appdefKey}"/>
</c:url>

<c:set var="entityId" value="${Resource.entityId}"/>


<c:if test="${ CONST_PLATFORM == entityId.type}">
	<tiles:insertDefinition name=".page.title.events.list.platform">
	    <tiles:putAttribute name="resource" value="${Resource}"/>
	    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
	    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
	    <tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
	</tiles:insertDefinition>
    <c:choose>
        <c:when test="${ canControl }">
			<tiles:insertDefinition name=".tabs.resource.platform.alert.configAlerts">
			    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
			    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
			</tiles:insertDefinition>
        </c:when>
        <c:otherwise>
            <tiles:insertDefinition name=".tabs.resource.platform.alert.configAlerts.nocontrol">
                    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
                    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
            </tiles:insertDefinition>
        </c:otherwise>
    </c:choose>
</c:if>
<c:if test="${ CONST_SERVER == entityId.type}">
	<tiles:insertDefinition name=".page.title.events.list.server">
	    <tiles:putAttribute name="resource" value="${Resource}"/>
	    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
	    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
	    <tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
	</tiles:insertDefinition>
	
	<c:choose>
	 <c:when test="${ canControl }">
	  <tiles:insertDefinition name=".tabs.resource.server.alert.configAlerts">
	   <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
	   <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
	  </tiles:insertDefinition>
	 </c:when>
	 <c:otherwise>
	  <tiles:insertDefinition name=".tabs.resource.server.alert.configAlerts.nocontrol">
	   <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
	   <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
	  </tiles:insertDefinition>
	 </c:otherwise>
	</c:choose>
</c:if>
<c:if test="${ CONST_SERVICE == entityId.type}">
<tiles:insertDefinition name=".page.title.events.list.service">
    <tiles:putAttribute name="resource" value="${Resource}"/>
    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
    <tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
</tiles:insertDefinition>

<c:choose>
 <c:when test="${ canControl }">
  <tiles:insertDefinition name=".tabs.resource.service.alert.configAlerts">
   <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
   <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
  </tiles:insertDefinition>
 </c:when>
 <c:otherwise>
  <tiles:insertDefinition name=".tabs.resource.service.alert.configAlerts.nocontrol">
   <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
   <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
  </tiles:insertDefinition>
 </c:otherwise>
</c:choose>

</c:if>
<c:if test="${ CONST_APPLICATION == entityId.type}">
<tiles:insertDefinition name=".page.title.events.list.application">
    <tiles:putAttribute name="titleName" value="${Resource.name}"/>
    <tiles:putAttribute name="resource" value="${Resource}"/>
    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
</tiles:insertDefinition>
<tiles:insertDefinition name=".tabs.resource.application.monitor.configAlerts">
    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
</tiles:insertDefinition>
</c:if>
<c:if test="${ CONST_GROUP == entityId.type}">
    <tiles:insertDefinition name=".page.title.events.list.group">
        <tiles:putAttribute name="titleName" value="${Resource.name}"/>
        <tiles:putAttribute name="resource" value="${Resource}"/>
        <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
        <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
    </tiles:insertDefinition>
   
</c:if>

<!-- FORM -->
<s:form name="RemoveConfigForm" id="RemoveConfigForm" action="removeDefinitionAction">
<s:hidden theme="simple" name="rid"  id="rid" value="%{#attr.Resource.id}"/>
<s:hidden theme="simple" name="type" id="type" value="%{#attr.Resource.entityId.type}"/>

<tiles:insertDefinition name=".portlet.confirm"/>
<tiles:insertDefinition name=".portlet.error"/>
<display:table cellspacing="0" cellpadding="0" width="100%"
               action="${sortAction}" items="${Definitions}" var="def" >
  <display:column width="1%" property="id" 
                  title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties)\" name=\"listToggleAll\"/>"  
                   isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" >
    <display:checkboxdecorator name="definitions" onclick="ToggleSelection(this,widgetProperties)" styleClass="listMember"/>
  </display:column>
  <display:column width="1%" property="parentId"
                  title="nbsp" styleClass="redTableCell">
    <display:equalsdecorator flagKey="alerts.config.service.DefinitionList.isResourceAlert" value="null"/>
  </display:column>

  <display:column width="18%" property="name" sort="true" sortAttr="1"
                  defaultSort="true" title="alerts.config.DefinitionList.ListHeader.AlertDefinition" href="viewEscalationAlertsConfigPortal.action?mode=viewDefinition&eid=${Resource.entityId.appdefKey}" paramId="ad" paramProperty="id"/>
    
  <display:column width="20%" property="description"
                  title="common.header.Description" />

  <display:column width="15%" property="ctime" sort="true" sortAttr="2"
                  defaultSort="false" title="alerts.config.DefinitionList.ListHeader.DateCreated" >
    <display:datedecorator/>
  </display:column>

   <display:column width="15%" property="mtime" sort="true" sortAttr="2"
                  defaultSort="false" title="resource.common.monitor.visibility.metricmetadata.collection.lastModified" >
    <display:datedecorator/>
  </display:column>
                  
  <display:column width="10%" property="active" title="alerts.config.DefinitionList.ListHeader.Active">
	
	<display:alertdefstatedecorator active="${def.active}" disabled="${def.active && !def.enabled}" />

  </display:column>

</display:table>

<tiles:insertDefinition name=".toolbar.list">
<!-- only show new alert def link if user can see it -->
<hq:userResourcePermissions debug="false" resource="${Resource}"/>
<c:choose>
       <c:when test="${canModify}" >
 	 		<tiles:putAttribute name="listNewUrl" value="${newAction}"/>
 	 		<tiles:putAttribute name="goButtonLink" value="javascript:setActiveInactive()"/>
 	 	</c:when>
 	 	<c:otherwise>
 	 		<tiles:putAttribute name="noButtons" value="true"/>
 	 	</c:otherwise>
</c:choose>

 <s:set var="totalSize" scope="request" ><s:property value="#attr.Definitions.getTotalSize()"/></s:set>
  <tiles:putAttribute name="listItems" value="${Definitions}"/>
  <tiles:putAttribute name="listSize" value="${totalSize}"/>
  <tiles:putAttribute name="pageNumAction" value="${pnAction}"/>
  <tiles:putAttribute name="pageSizeAction" value="${psAction}"/>
  <tiles:putAttribute name="defaultSortColumn" value="1"/>
  <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
</tiles:insertDefinition>

<br>
<s:hidden theme="simple" id="setActiveInactive" name="setActiveInactive" value="%{#attr.setActiveInactive}"/>
</s:form>

<!-- /  -->
