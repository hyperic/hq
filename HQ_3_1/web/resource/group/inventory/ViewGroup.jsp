<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
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
<c:url var="selfAction" value="/resource/group/Inventory.do?mode=view&rid=${Resource.id}"/>
    
<!-- TITLE BAR -->
<c:set var="entityId" value="${Resource.entityId}"/>
<tiles:insert definition=".page.title.resource.group.full">
    <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" /></tiles:put>
    <tiles:put name="resource" beanName="Resource"/>
    <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
    <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>

<!-- CONTROL BAR -->
<c:choose>
    <c:when test="${Resource.groupType == CONST_ADHOC_PSS ||
                    Resource.groupType == CONST_ADHOC_GRP ||
                    Resource.groupType == CONST_ADHOC_APP }"> 
        <tiles:insert definition=".tabs.resource.group.inventory.inventoryonly">
            <tiles:put name="resource" beanName="Resource" />
            <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
        </tiles:insert>
    </c:when>
    <c:when test="${ canControl }"> 
        <tiles:insert definition=".tabs.resource.group.inventory">
            <tiles:put name="resource" beanName="Resource" />
            <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
        </tiles:insert>
    </c:when>
    <c:otherwise>
        <tiles:insert definition=".tabs.resource.group.inventory.nocontrol">
            <tiles:put name="resource" beanName="Resource" />
            <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
        </tiles:insert>
    </c:otherwise>
</c:choose>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<div id="propertiesAccordion" style="visibility: hidden;">
<div id="panel1">
<div id="panelHeader" class="accordionTabTitleBar">
<!--  GENERAL PROPERTIES TITLE -->
  <fmt:message key="resource.common.inventory.props.GeneralPropertiesTab"/>
</div>
<div id="panelContent">
<tiles:insert page="/resource/group/inventory/ResourceCounts.jsp">
    <tiles:put name="resourceCount" beanName="AppdefEntries" beanProperty="totalSize" />
    <tiles:put name="resourceTypeMap" beanName="ResourceTypeMap"/>
</tiles:insert>
<tiles:insert definition=".resource.common.inventory.generalProperties.view">
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>
</div>
</div>

<c:if test="${not empty Applications}">
<!-- LIST APPLICATIONS SECTION -->
<div id="panel2">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.service.inventory.ApplicationsTab"/>
</div>
<div id="panelContent">
  <tiles:insert definition=".resource.service.inventory.applications">
    <tiles:put name="service" beanName="Resource"/>
    <tiles:put name="applications" beanName="Applications"/>
    <tiles:put name="selfAction" beanName="selfAction"/>
  </tiles:insert>
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
<tiles:insert page="/resource/group/inventory/ListResources.jsp"/>
</div>
</div>
</div>

<script type="text/javascript">
  onloads.push( accord<c:out value="${param.accord}"/> );
</script>

<!-- FOOTER SECTION -->
<tiles:insert definition=".page.footer"/>


