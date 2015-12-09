<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	var pageData = new Array();
</jsu:script>
<c:set var="entityId" value="${Resource.entityId}"/>
<c:url var="selfAction" value="viewResourceInventoryApplicationVisibility.action">
	<c:param name="mode" value="view"/>
	<c:param name="rid" value="${Resource.id}"/>
	<c:param name="type" value="${entityId.type}"/>
</c:url>
<c:url var="editUrl" value="editApplicationPropertiesInventoryApplicationVisibility.action">
	<c:param name="mode" value="editResource"/>
	<c:param name="rid" value="${Resource.id}"/>
	<c:param name="type" value="${entityId.type}"/>
</c:url>

<c:url var="psAction" value="${selfAction}">
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
</c:url>

<c:url var="pnAction" value="${selfAction}">
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
</c:url>

<tiles:insertDefinition name=".page.title.resource.application.full">
    <tiles:putAttribute name="resource" value="${Resource}"/>
    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
    <tiles:putAttribute name="eid" value="${entityId.appdefKey}" />
	<tiles:putAttribute name="ignoreBreadcrumb"  value="false" />
</tiles:insertDefinition>

<tiles:insertDefinition name=".tabs.resource.application.inventory">
    <tiles:putAttribute name="resource" value="${Resource}" />
    <tiles:putAttribute name="resourceId" value="${entityId.id}"/>
	<tiles:putAttribute name="resourceType" value="${entityId.type}" />
</tiles:insertDefinition>


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
  <tiles:putAttribute name="editUrl" value="editResourceGeneralInventoryApplicationVisibility.action"/>
</tiles:insertDefinition>
</div>
</div>

<div id="panel2">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.application.inventory.ApplicationProperties"/>
</div>
<div id="panelContent">
<tiles:insertDefinition name=".ng.resource.application.inventory.applicationProperties.view">
  <tiles:putAttribute name="application" value="${Resource}" />
</tiles:insertDefinition>
<c:if test="${useroperations['modifyApplication']}">
<tiles:insertDefinition name=".toolbar.edit">
  <tiles:putAttribute name="editUrl" value="${editUrl}"/>
</tiles:insertDefinition>
</c:if>
</div>
</div>

<div id="panel3">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.application.inventory.ServiceCountsTab"/>
</div>
<div id="panelContent">
<tiles:insertDefinition name=".ng.resource.application.inventory.serviceCounts">
  <tiles:putAttribute name="serviceCount" value="${NumChildResources}"/>
  <tiles:putAttribute name="serviceTypeMap" value="${ResourceTypeMap}"/>
</tiles:insertDefinition>
</div>
</div>

<!-- services -->
<div id="panel4">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.application.inventory.ServicesTab"/>
</div>
<div id="panelContent">
<tiles:insertDefinition name=".ng.resource.application.inventory.services">
  <tiles:putAttribute name="application" value="${Resource}"/>
  <tiles:putAttribute name="services" value="${ChildResources}"/>
  <tiles:putAttribute name="serviceCount" value="${NumChildResources}"/>
  <tiles:putAttribute name="selfAction" value="${selfAction}"/>
</tiles:insertDefinition>
<!-- / -->
</div>
</div>

<c:set var="editGroupUrlAction" value="addGroupsInventoryApplicationVisibility.action?mode=addGroups&eid=${entityId}"/>

<div id="panel4">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.common.inventory.groups.GroupsTab"/>
</div>
<div id="panelContent">
<s:form action="applicationViewRemoveGroupsFromList" >

<tiles:insertDefinition name=".resource.common.inventory.groups">
  <tiles:putAttribute name="resource" value="${Resource}"/>
  <tiles:putAttribute name="groups" value="${AllResGrps}"/>
  <tiles:putAttribute name="selfAction" value="${selfAction}"/>
    <tiles:putAttribute name="editGroupUrlAction" value="${editGroupUrlAction}"/>
</tiles:insertDefinition>

<s:hidden theme="simple" name="rid" value="%{#attr.entityId.id}" />
<s:hidden theme="simple" name="type" value="%{#attr.entityId.type}" />
<s:hidden theme="simple" name="eid" value="%{#attr.entityId}" />

</s:form>
</div>
</div>

