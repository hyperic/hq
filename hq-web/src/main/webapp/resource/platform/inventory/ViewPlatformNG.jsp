<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
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
<c:set var="selfAction" value="viewPlatformInventoryPlatformVisibility.action?mode=view&eid=${entityId}"/>

<c:if test="${not empty param.eid && not empty param.resourceType && param.resourceType != -1}">
  <c:set var="ctype" value="3:${param.resourceType}"/>
</c:if>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" />

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>


<tiles:insertDefinition name=".page.title.resource.platform.full">
  <tiles:putAttribute name="resource" value="${Resource}"/>
  <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
  <tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
</tiles:insertDefinition>

<c:choose>
	<c:when test="${canControl}">
		<tiles:insertDefinition name=".tabs.resource.platform.inventory.current">
  			<tiles:putAttribute name="resourceId" value="${Resource.id}"/>
  			<tiles:putAttribute name="resourceType" value="${entityId.type}"/>
		</tiles:insertDefinition>
    </c:when>
    <c:otherwise>
		<tiles:insertDefinition name=".tabs.resource.platform.inventory.current.nocontrol">
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
</div>
</div>
<div id="panel2">
<div id="panelHeader" class="accordionTabTitleBar">
<!--  TYPE AND NETWORK PROPERTIES TITLE -->
  <fmt:message key="resource.platform.inventory.TypeAndNetworkPropertiesTab"/>
</div>
<div id="panelContent">
<tiles:insertDefinition name=".resource.platform.inventory.typeNetworkProperties.view">
  <tiles:putAttribute name="platform" value="${Resource}"/>
</tiles:insertDefinition>
</div>
</div>
<div id="panel3">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.platform.inventory.servers.ServersTab"/>
</div>
<div id="panelContent">
<tiles:insertDefinition name=".resource.platform.inventory.serverCounts">
  <tiles:putAttribute name="serverCount" value="${ChildResourcesTotalSize}"/>
  <tiles:putAttribute name="serverTypeMap" value="${ResourceTypeMap}"/>
</tiles:insertDefinition>

<s:form action="platformViewRemoveServerFromList">

<c:url var="svrAction" value="${selfAction}&accord=2">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.psg}">
    <c:param name="psg" value="${param.psg}"/>
  </c:if>
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
  <c:if test="${not empty param.png}">
    <c:param name="png" value="${param.png}"/>
  </c:if>
    <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
    <c:if test="${not empty param.sog}">
    <c:param name="sog" value="${param.sog}"/>
  </c:if>
    <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
    <c:if test="${not empty param.scg}">
    <c:param name="scg" value="${param.scg}"/>
  </c:if>
    <c:if test="${not empty param.resourceType}">
    <c:param name="resourceType" value="${param.resourceType}"/>
  </c:if>
</c:url>


<tiles:insertDefinition name=".resource.platform.inventory.servers">
  <tiles:putAttribute name="platform" value="${Resource}"/>
  <tiles:putAttribute name="servers" value="${ChildResources}"/>
  <tiles:putAttribute name="serverCount" value="${ChildResourcesTotalSize}"/>
  <tiles:putAttribute name="selfAction" value="${svrAction}"/>
</tiles:insertDefinition>

<s:hidden theme="simple" name="rid" value="%{#attr.entityId.id}" />
<s:hidden theme="simple" name="type" value="%{#attr.entityId.type}" />
<s:hidden theme="simple" name="eid" value="%{#attr.entityId}" />
</s:form>

</div>
</div>

<s:form action="platformViewRemoveServiceFromList">
<c:url var="svcAction" value="${selfAction}&accord=3">
  <c:if test="${not empty param.fs}">
    <c:param name="fs" value="${param.fs}"/>
  </c:if>
  <c:if test="${not empty param.psg}">
    <c:param name="psg" value="${param.psg}"/>
  </c:if>
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.png}">
    <c:param name="png" value="${param.png}"/>
  </c:if>
    <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
    <c:if test="${not empty param.sog}">
    <c:param name="sog" value="${param.sog}"/>
  </c:if>
    <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
    <c:if test="${not empty param.scg}">
    <c:param name="scg" value="${param.scg}"/>
  </c:if>
    <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
</c:url>

<div id="panel4">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.server.inventory.ServicesTab"/>
</div>
<div id="panelContent">

<tiles:insertDefinition name=".resource.platform.inventory.services">
  <tiles:putAttribute name="services" value="${Services}"/>
  <tiles:putAttribute name="selfAction" value="${svcAction}"/>
  <tiles:putAttribute name="ServicesTotalSize" value="${ServicesTotalSize}"/>
  <tiles:putAttribute name="autoInventory" value="${autoInventory}"/>
  <c:if test="${not empty ctype}">
    <tiles:putAttribute name="ctype" value="${ctype}"/>
  </c:if>
</tiles:insertDefinition>

<s:hidden theme="simple" name="rid" value="%{#attr.entityId.id}" />
<s:hidden theme="simple" name="type" value="%{#attr.entityId.type}" />
<s:hidden theme="simple" name="eid" value="%{#attr.entityId}" />
</s:form>

</div>
</div>
<div id="panel5">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.common.inventory.groups.GroupsTab"/>
</div>

<div id="panelContent">
<s:form action="platformViewRemoveGroupsFromList" method="post" >

<tiles:insertDefinition name=".resource.common.inventory.groups">
  <tiles:putAttribute name="resource" value="${Resource}"/>
  <tiles:putAttribute name="groups" value="${AllResGrps}"/>
  <tiles:putAttribute name="selfAction" value="${selfAction}"/>
</tiles:insertDefinition>

<s:hidden theme="simple" name="rid" value="%{#attr.entityId.id}" />
<s:hidden theme="simple" name="type" value="%{#attr.entityId.type}" />
<s:hidden theme="simple" name="eid" value="%{#attr.entityId}" />

</s:form>

</div>
</div>

<tiles:insertDefinition name=".resource.common.inventory.EffectivePolicy"/>
<div id="panel6">
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
    <tiles:putAttribute name="resourceNotControllable" value="true"/>
    <tiles:putAttribute name="agent" value="${agent}"/>
</tiles:insertDefinition>    
</div>
</div>
<jsu:script>
  	clearIfAnyChecked();
</jsu:script>

