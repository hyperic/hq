<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
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
  
  Copyright (C) [2004-2008], Hyperic, Inc.
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
<c:set var="selfAction" value="/resource/server/Inventory.do?mode=view&eid=${entityId}"/>


<hq:pageSize var="pageSize"/>

<tiles:insertDefinition name=".page.title.resource.inventory.full">
    <tiles:putAttribute name="resource" value="${Resource}"/>
    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
    <tiles:putAttribute name="eid" beanName="entityId" value="${appdefKey}" />
</tiles:insertDefinition>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" />

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>

<!-- CONTROL BAR -->
<c:choose>
<c:when test="${canControl}">
<tiles:insertDefinition name=".tabs.resource.server.inventory">
    <tiles:putAttribute name="resource" value="${Resource}" />
    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
</tiles:insertDefinition>
</c:when>
<c:otherwise>
<tiles:insertDefinition name=".tabs.resource.server.inventory.nocontrol">
    <tiles:putAttribute name="resource" value="${Resource}" />
    <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
    <tiles:putAttribute name="resourceType" value="${entityId.type}"/>
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
<tiles:insertDefinition name=".resource.common.inventory.generalProperties.view">
  <tiles:putAttribute name="resource" value="${Resource}"/>
  <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
  <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
</tiles:insertDefinition>
<div class="accordionTabTitleBar">
<!--  GENERAL PROPERTIES TITLE -->
  <fmt:message key="resource.server.inventory.TypeAndHostPropertiesTab"/>
</div>
<tiles:insertTemplate template="/resource/server/inventory/ViewTypeAndHostPropertiesNG.jsp"/>
</div>
</div>

<!-- new -->
<div id="panel2">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.server.inventory.ServicesTab"/>
</div>
<div id="panelContent">
<tiles:insertDefinition name=".resource.server.inventory.services">
  <tiles:putAttribute name="serviceCount" value="${NumChildResources}"/>
  <tiles:putAttribute name="serviceTypeMap" value="${ResourceTypeMap}"/>
  <tiles:putAttribute name="server" value="${Resource}"/>
  <tiles:putAttribute name="services" value="${ChildResources}"/>
  <tiles:putAttribute name="serviceCount" value="${NumChildResources}"/>
  <tiles:putAttribute name="selfAction" value="${selfAction}"/>
  <tiles:putAttribute name="autoInventory" value="${autoInventory}"/>
</tiles:insertDefinition>
</div>
</div>

<div id="panel3">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.common.inventory.groups.GroupsTab"/>
</div>
<div id="panelContent">
<s:form action="/resource/server/inventory/RemoveGroups">
<input type="hidden" name="rid" value="<c:out value="${Resource.id}"/>"/>
<input type="hidden" name="type" value="<c:out value="${entityId.type}"/>"/>
<tiles:insertDefinition name=".resource.common.inventory.groups">
  <tiles:putAttribute name="resource" value="${Resource}"/>
  <tiles:putAttribute name="groups" value="${AllResGrps}"/>
  <tiles:putAttribute name="selfAction" value="${selfAction}"/>
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
    <tiles:putAttribute name="monitorConfigOptionsCount" value="${monitorConfigOptionsCount}"/>
    <tiles:putAttribute name="controlConfigOptions" value="${controlConfigOptions}"/>
    <tiles:putAttribute name="controlConfigOptionsCount" value="${controlConfigOptionsCount}"/>
    <tiles:putAttribute name="autodiscoveryMessageServiceList" value="${autodiscoveryMessageServiceList}"/>
</tiles:insertDefinition>
</div>
</div>

<tiles:insertDefinition name=".page.footer"/>
