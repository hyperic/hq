<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>


<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_PSS" var="CONST_ADHOC_PSS" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_GRP" var="CONST_ADHOC_GRP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_ADHOC_APP" var="CONST_ADHOC_APP" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_COMPAT_PS" var="CONST_COMPAT_PS" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_GROUP_COMPAT_SVC" var="CONST_COMPAT_SVC" />
<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>
<c:url var="selfAction" value="/resource/group/Inventory.do">
	<c:param name="mode" value="view"/>
	<c:param name="rid" value="${Resource.id}"/>
</c:url>
    
<!-- TITLE BAR -->
<c:set var="ignoreBreadcrumb" value="false" scope="request"/>
<c:set var="entityId" value="${Resource.entityId}"/>
<tiles:insertDefinition name=".page.title.resource.group.full">
    <tiles:putAttribute name="resource" value="${Resource}"/>
    <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
    <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
    <tiles:putAttribute name="eid"  value="${entityId.appdefKey}" />
</tiles:insertDefinition>

<!-- CONTROL BAR -->
<c:choose>
    <c:when test="${Resource.groupType == CONST_ADHOC_PSS ||
                    Resource.groupType == CONST_ADHOC_GRP ||
                    Resource.groupType == CONST_ADHOC_APP }"> 
        <tiles:insertDefinition name=".tabs.resource.group.inventory.inventoryonly">
            <tiles:putAttribute name="resource" value="${Resource}" />
            <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
        </tiles:insertDefinition>
    </c:when>
    <c:when test="${ canControl }"> 
        <tiles:insertDefinition name=".tabs.resource.group.inventory">
            <tiles:putAttribute name="resource" value="${Resource}" />
            <tiles:putAttribute name="resourceId" value="${Resource.id}"/>
        </tiles:insertDefinition>
    </c:when>
    <c:otherwise>
        <tiles:insertDefinition name=".tabs.resource.group.inventory.nocontrol">
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
<c:set var="listSize" value="${fn:length(AppdefEntries)}" />
<tiles:insertTemplate template="/resource/group/inventory/ResourceCountsNG.jsp">
    <tiles:putAttribute name="resourceCount" value="${listSize}" />
    <tiles:putAttribute name="resourceTypeMap" value="${ResourceTypeMap}"/>
</tiles:insertTemplate>
<tiles:insertDefinition name=".resource.common.inventory.generalProperties.view">
  <tiles:putAttribute name="resource" value="${Resource}"/>
  <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
  <tiles:putAttribute name="resourceModifier" value="${ResourceModifier}"/>
  <tiles:putAttribute name="editUrl" value="editResourceGeneralInventoryGroupVisibility.action"/>
</tiles:insertDefinition>
</div>
</div>

<c:if test="${not empty Applications}">
<!-- LIST APPLICATIONS SECTION -->
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

<!-- LIST RESOURCES SECTION -->
<div id="panel3">
<div id="panelHeader" class="accordionTabTitleBar">
<!--  RESOURCES, COMPATIBLE TITLE -->
<fmt:message key="resource.group.inventory.ResourcesTab"/>
<span class="BlockSubtitle">
           <c:choose>
              <c:when test="${Resource.groupType == CONST_ADHOC_PSS }"> 
                <fmt:message key="resource.group.inventory.tab.Mixed"/>
              </c:when>
              <c:when test="${Resource.groupType == CONST_COMPAT_PS ||
                              Resource.groupType == CONST_COMPAT_SVC}"> 
                <fmt:message key="resource.group.inventory.tab.Compatible">
                    <fmt:param value="${Resource.appdefResourceTypeValue.name}"/>
                </fmt:message>
              </c:when>
              <c:when test="${Resource.groupType == CONST_ADHOC_APP }"> 
                <fmt:message key="resource.group.inventory.tab.GroupOfApplication"/>
              </c:when>
              <c:otherwise> 
                <fmt:message key="resource.group.inventory.tab.GroupOfGroup"/>
              </c:otherwise>
            </c:choose>
</span>
<!--  /  -->
</div>
<div id="panelContent">
<tiles:insertTemplate template="/resource/group/inventory/ListResourcesNG.jsp">
  <tiles:putAttribute name="resourceOwner" value="${ResourceOwner}"/>
</tiles:insertTemplate>
</div>
</div>

<!-- FOOTER SECTION -->
<tiles:insertDefinition name=".page.footer"/>


