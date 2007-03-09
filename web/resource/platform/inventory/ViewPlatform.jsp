<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
</script>

<c:set var="entityId" value="${Resource.entityId}"/>
<c:set var="selfAction" value="/resource/platform/Inventory.do?mode=view&eid=${entityId}"/>
<c:if test="${not empty param.eid && not empty param.resourceType && param.resourceType != -1}">
  <c:set var="ctype" value="3:${param.resourceType}"/>
</c:if>



<tiles:insert definition=".page.title.resource.platform.full">
  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" /></tiles:put>
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
</tiles:insert>

<tiles:insert definition=".tabs.resource.platform.inventory.current">
  <tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  <tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
</tiles:insert>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<div id="propertiesAccordion" style="visibility: hidden;">
<div id="panel1">
<div id="panelHeader" class="accordionTabTitleBar">
<!--  GENERAL PROPERTIES TITLE -->
  <fmt:message key="resource.common.inventory.props.GeneralPropertiesTab"/>
</div>
<div id="panelContent">
<tiles:insert definition=".resource.common.inventory.generalProperties.view">
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>
<tiles:insert definition=".resource.platform.inventory.serverCounts">
  <tiles:put name="serverCount" beanName="ChildResources" beanProperty="totalSize"/>
  <tiles:put name="serverTypeMap" beanName="ResourceTypeMap"/>
</tiles:insert>
</div>
</div>
<div id="panel2">
<div id="panelHeader" class="accordionTabTitleBar">
<!--  TYPE AND NETWORK PROPERTIES TITLE -->
  <fmt:message key="resource.platform.inventory.TypeAndNetworkPropertiesTab"/>
</div>
<div id="panelContent">
<tiles:insert definition=".resource.platform.inventory.typeNetworkProperties.view">
  <tiles:put name="platform" beanName="Resource"/>
</tiles:insert>
</div>
</div>
<div id="panel3">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.platform.inventory.servers.ServersTab"/>
</div>
<div id="panelContent">

<html:form action="/resource/platform/inventory/RemoveServers">

<c:set var="svrAction" value="${selfAction}&accord=2"/>
<c:if test="${not empty param.fs}">
  <c:set var="svrAction" value="${svrAction}&fs=${param.fs}"/>
</c:if>
<c:if test="${not empty param.ps}">
  <c:set var="svrAction" value="${svrAction}&ps=${param.ps}"/>
</c:if>
<c:if test="${not empty param.psg}">
  <c:set var="svrAction" value="${svrAction}&psg=${param.psg}"/>
</c:if>
<c:if test="${not empty param.pn}">
  <c:set var="svrAction" value="${svrAction}&pn=${param.pn}"/>
</c:if>
<c:if test="${not empty param.png}">
  <c:set var="svrAction" value="${svrAction}&png=${param.png}"/>
</c:if>
<c:if test="${not empty param.so}">
  <c:set var="svrAction" value="${svrAction}&so=${param.so}"/>
</c:if>
<c:if test="${not empty param.sog}">
  <c:set var="svrAction" value="${svrAction}&sog=${param.sog}"/>
</c:if>
<c:if test="${not empty param.sc}">
  <c:set var="svrAction" value="${svrAction}&sc=${param.sc}"/>
</c:if>
<c:if test="${not empty param.scg}">
  <c:set var="svrAction" value="${svrAction}&scg=${param.scg}"/>
</c:if>
<c:if test="${not empty param.resourceType}">
  <c:set var="svrAction" value="${svrAction}&resourceType=${param.resourceType}"/>
</c:if>

<tiles:insert definition=".resource.platform.inventory.servers">
  <tiles:put name="platform" beanName="Resource"/>
  <tiles:put name="servers" beanName="ChildResources"/>
  <tiles:put name="serverCount" beanName="ChildResources" beanProperty="totalSize"/>
  <tiles:put name="selfAction" beanName="svrAction"/>
</tiles:insert>

<html:hidden property="rid"/>
<html:hidden property="type"/>
<input type="hidden" name="accord" value="2"/>
</html:form>

</div>
</div>
<c:set var="svcAction" value="${selfAction}&accord=3"/>
<c:if test="${not empty param.fs}">
  <c:set var="svcAction" value="${svcAction}&fs=${param.fs}"/>
</c:if>
<c:if test="${not empty param.pss}">
  <c:set var="svcAction" value="${svcAction}&pss=${param.pss}"/>
</c:if>
<c:if test="${not empty param.psg}">
  <c:set var="svcAction" value="${svcAction}&psg=${param.psg}"/>
</c:if>
<c:if test="${not empty param.pns}">
  <c:set var="svcAction" value="${svcAction}&pns=${param.pns}"/>
</c:if>
<c:if test="${not empty param.png}">
  <c:set var="svcAction" value="${svcAction}&png=${param.png}"/>
</c:if>
<c:if test="${not empty param.sos}">
  <c:set var="svcAction" value="${svcAction}&sos=${param.sos}"/>
</c:if>
<c:if test="${not empty param.sog}">
  <c:set var="svcAction" value="${svcAction}&sog=${param.sog}"/>
</c:if>
<c:if test="${not empty param.scs}">
  <c:set var="svcAction" value="${svcAction}&scs=${param.scs}"/>
</c:if>
<c:if test="${not empty param.scg}">
  <c:set var="svcAction" value="${svcAction}&scg=${param.scg}"/>
</c:if>
<div id="panel4">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.server.inventory.ServicesTab"/>
</div>
<div id="panelContent">
<tiles:insert definition=".resource.platform.inventory.services">
  <tiles:put name="services" beanName="Services"/>
  <tiles:put name="selfAction" beanName="svcAction"/>
  <tiles:put name="autoInventory" beanName="autoInventory"/>
  <c:if test="${not empty ctype}">
    <tiles:put name="ctype" beanName="ctype"/>
  </c:if>
</tiles:insert>
</div>
</div>
<div id="panel5">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.common.inventory.groups.GroupsTab"/>
</div>
<div id="panelContent">
<html:form action="/resource/platform/inventory/RemoveGroups">

<tiles:insert definition=".resource.common.inventory.groups">
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="groups" beanName="AllResGrps"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>

<html:hidden property="rid"/>
<html:hidden property="type"/>

</html:form>

</div>
</div>

<div id="panel6">
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
    <tiles:put name="monitorConfigOptionsCount" beanName="monitorConfigOptionsCount"/>
    <tiles:put name="resourceNotControllable" value="true"/>
    <tiles:put name="agent" beanName="agent"/>
</tiles:insert>    
</div>
</div>
</div>

<script type="text/javascript">
  clearIfAnyChecked();

  onloads.push( accord<c:out value="${param.accord}"/> );
</script>

<tiles:insert definition=".page.footer"/>
