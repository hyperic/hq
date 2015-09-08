<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	var pageData = new Array();
</jsu:script>
<c:set var="entityId" value="${Resource.entityId}"/>
<c:url var="selfAction" value="/resource/service/Inventory.do">
	<c:param name="mode" value="view"/>
	<c:param name="rid" value="${Resource.id}"/>
	<c:param name="type" value="${entityId.type}"/>
</c:url>
<tiles:insertDefinition name=".page.title.resource.service.full">
    <tiles:putAttribute name="resource" value="${Resource}"/>
    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
    <tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
</tiles:insertDefinition>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" />

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>

<!-- CONTROL BAR -->
<c:choose>
<c:when test="${canControl}">
<tiles:insertDefinition name=".tabs.resource.service.inventory">
    <tiles:putAttribute name="resource" value="${Resource}" />
    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
</tiles:insertDefinition>
</c:when>
<c:otherwise>
<tiles:insertDefinition name=".tabs.resource.service.inventory.nocontrol">
    <tiles:putAttribute name="resource" value="${Resource}" />
    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
</tiles:insertDefinition>
</c:otherwise>
</c:choose>

<tiles:insertDefinition name=".portlet.confirm"/>
<tiles:insertDefinition name=".portlet.error"/>

<div id="panel1">
<div id="panelHeader" class="accordionTabTitleBar">
<!--  GENERAL PROPERTIES TITLE -->
  <fmt:message key="resource.common.inventory.props.GeneralPropertiesTab"/>
</div>
<div id="panelContent">
<tiles:insertTemplate template="/resource/service/inventory/ViewTypeAndHostPropertiesNG.jsp"/>
<tiles:insertDefinition name=".resource.common.inventory.generalProperties.view">
  <tiles:putAttribute name="resource" value="${Resource}"/>
  <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
  <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
  <tiles:putAttribute name="editUrl" value="editResourceGeneralInventoryServiceVisibility.action"/>
</tiles:insertDefinition>
</div>
</div>
<c:if test="${not empty Applications}">
<div id="panel2">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.service.inventory.ApplicationsTab"/>
</div>
<div id="panelContent">
<tiles:insertDefinition name=".resource.service.inventory.applications">
  <tiles:putAttribute name="service" value="${Resource}"/>
  <tiles:putAttribute name="applications" value="${Applications}"/>
  <tiles:putAttribute name="selfAction" value="${selfAction}"/>
</tiles:insertDefinition>
</div>
</div>
</c:if>

<c:set var="editGroupUrlAction" value="addGroupsInventoryServiceVisibility.action?mode=addGroups&eid=${entityId}"/>
<div id="panel3">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.common.inventory.groups.GroupsTab"/>
</div>
<div id="panelContent">
<s:form action="serviceViewRemoveGroupsFromList.action">
<s:hidden theme="simple" name="rid" value="%{#attr.entityId.id}" />
<s:hidden theme="simple" name="type" value="%{#attr.entityId.type}" />
<s:hidden theme="simple" name="eid" value="%{#attr.entityId}" />

<tiles:insertDefinition name=".resource.common.inventory.groups">
  <tiles:putAttribute name="resource" value="${Resource}"/>
  <tiles:putAttribute name="groups" value="${AllResGrps}"/>
  <tiles:putAttribute name="selfAction" value="${selfAction}"/>
  <tiles:putAttribute name="editGroupUrlAction" value="${editGroupUrlAction}"/>
</tiles:insertDefinition>
</s:form>
</div>
</div>

<tiles:insertDefinition name=".resource.common.inventory.EffectivePolicy"/>

<div id="panel4">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.common.inventory.configProps.ConfigurationPropertiesTab"/>
</div>
<div id="panelContent">
<tiles:insertDefinition name=".resource.common.inventory.viewConfigProperties">
    <tiles:putAttribute name="resource" value="${Resource}"/>
    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
    <tiles:putAttribute name="productConfigOptions" value="${productConfigOptions}"/>
    <tiles:putAttribute name="productConfigOptionsCount" value="${productConfigOptionsCount}"/>
    <tiles:putAttribute name="monitorConfigOptions" value="${monitorConfigOptions}"/>
    <tiles:putAttribute name="rtConfigOptions" value="${rtConfigOptions}"/>
    <tiles:putAttribute name="monitorConfigOptionsCount" value="${monitorConfigOptionsCount}"/>
    <tiles:putAttribute name="controlConfigOptions" value="${controlConfigOptions}"/>
    <tiles:putAttribute name="controlConfigOptionsCount" value="${controlConfigOptionsCount}"/>
</tiles:insertDefinition>    



</div>
</div>

