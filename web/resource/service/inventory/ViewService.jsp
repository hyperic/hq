<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="hq" prefix="hq" %>
<%@ taglib uri="display" prefix="display" %>
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


<script  src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
</script>

<c:set var="entityId" value="${Resource.entityId}"/>
<c:url var="selfAction" value="/resource/service/Inventory.do?mode=view&rid=${Resource.id}&type=${entityId.type}"/>
<tiles:insert definition=".page.title.resource.service.full">
    <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" /></tiles:put>
    <tiles:put name="resource" beanName="Resource"/>
    <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
    <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>

<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>

<!-- CONTROL BAR -->
<c:choose>
<c:when test="${canControl}">
<tiles:insert definition=".tabs.resource.service.inventory">
    <tiles:put name="resource" beanName="Resource" />
    <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
</tiles:insert>
</c:when>
<c:otherwise>
<tiles:insert definition=".tabs.resource.service.inventory.nocontrol">
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
<tiles:insert page="/resource/service/inventory/ViewTypeAndHostProperties.jsp"/>
<tiles:insert definition=".resource.common.inventory.generalProperties.view">
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>
</div>
</div>
<c:if test="${not empty Applications}">
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

<div id="panel3">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.common.inventory.groups.GroupsTab"/>
</div>
<div id="panelContent">
<html:form action="/resource/service/inventory/RemoveGroups">
<html:hidden property="rid"/>
<html:hidden property="type"/>
<tiles:insert definition=".resource.common.inventory.groups">
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="groups" beanName="AllResGrps"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>
</div>
</div>

<div id="panel4">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.common.inventory.configProps.ConfigurationPropertiesTab"/>
</div>
<div id="panelContent">
<tiles:insert definition=".resource.common.inventory.viewConfigProperties">
    <tiles:put name="resource" beanName="Resource"/>
    <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
    <tiles:put name="productConfigOptions" beanName="productConfigOptions"/>
    <tiles:put name="productConfigOptionsCount" beanName="productConfigOptionsCount"/>
    <tiles:put name="monitorConfigOptions" beanName="monitorConfigOptions"/>
    <tiles:put name="rtConfigOptions" beanName="rtConfigOptions"/>
    <tiles:put name="monitorConfigOptionsCount" beanName="monitorConfigOptionsCount"/>
    <tiles:put name="controlConfigOptions" beanName="controlConfigOptions"/>
    <tiles:put name="controlConfigOptionsCount" beanName="controlConfigOptionsCount"/>
</tiles:insert>    

</html:form>

</div>
</div>
</div>

<script type="text/javascript">
  onloads.push( accord<c:out value="${param.accord}"/> );
</script>

<tiles:insert definition=".page.footer"/>
