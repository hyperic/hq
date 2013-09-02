<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
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
<c:set var="selfAction" value="/resource/platform/Inventory.do?mode=view&eid=${entityId}"/>
<c:if test="${not empty param.eid && not empty param.resourceType && param.resourceType != -1}">
  <c:set var="ctype" value="3:${param.resourceType}"/>
</c:if>

<hq:constant classname="org.hyperic.hq.ui.Constants" 
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" />

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>


<tiles:insert definition=".page.title.resource.platform.full">
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
  <tiles:put name="eid" beanName="entityId" beanProperty="appdefKey" />
</tiles:insert>

<c:choose>
	<c:when test="${canControl}">
		<tiles:insert definition=".tabs.resource.platform.inventory.current">
  			<tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  			<tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
		</tiles:insert>
    </c:when>
    <c:otherwise>
		<tiles:insert definition=".tabs.resource.platform.inventory.current.nocontrol">
  			<tiles:put name="resourceId" beanName="Resource" beanProperty="id"/>
  			<tiles:put name="resourceType" beanName="entityId" beanProperty="type"/>
		</tiles:insert>
    </c:otherwise>
</c:choose>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

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
<tiles:insert definition=".resource.platform.inventory.serverCounts">
  <tiles:put name="serverCount" beanName="ChildResources" beanProperty="totalSize"/>
  <tiles:put name="serverTypeMap" beanName="ResourceTypeMap"/>
</tiles:insert>

<html:form action="/resource/platform/inventory/RemoveServers">

<c:set var="svrAction" value="${selfAction}&accord=2"/>
<c:if test="${not empty param.ps}">
  <c:set var="svrAction"><c:out value="${svrAction}&ps=${param.ps}"/></c:set>
</c:if>
<c:if test="${not empty param.psg}">
  	<c:set var="svrAction"><c:out value="${svrAction}&psg=${param.psg}"/></c:set>
</c:if>
<c:if test="${not empty param.pn}">
  <c:set var="svrAction"><c:out value="${svrAction}&pn=${param.pn}"/></c:set>
</c:if>
<c:if test="${not empty param.png}">
  <c:set var="svrAction"><c:out value="${svrAction}&png=${param.png}"/></c:set>
</c:if>
<c:if test="${not empty param.so}">
  <c:set var="svrAction"><c:out value="${svrAction}&so=${param.so}"/></c:set>
</c:if>
<c:if test="${not empty param.sog}">
  <c:set var="svrAction"><c:out value="${svrAction}&sog=${param.sog}"/></c:set>
</c:if>
<c:if test="${not empty param.sc}">
  <c:set var="svrAction"><c:out value="${svrAction}&sc=${param.sc}"/></c:set>
</c:if>
<c:if test="${not empty param.scg}">
  <c:set var="svrAction"><c:out value="${svrAction}&scg=${param.scg}"/></c:set>
</c:if>
<c:if test="${not empty param.resourceType}">
  <c:set var="svrAction"><c:out value="${svrAction}&resourceType=${param.resourceType}"/></c:set>
</c:if>

<tiles:insert definition=".resource.platform.inventory.servers">
  <tiles:put name="platform" beanName="Resource"/>
  <tiles:put name="servers" beanName="ChildResources"/>
  <tiles:put name="serverCount" beanName="ChildResources" beanProperty="totalSize"/>
  <tiles:put name="selfAction" beanName="svrAction"/>
</tiles:insert>

<html:hidden property="rid"/>
<html:hidden property="type"/>
</html:form>

</div>
</div>
<c:set var="svcAction" value="${selfAction}&accord=3"/>
<c:if test="${not empty param.fs}">
  <c:set var="svcAction"><c:out value="${svcAction}&fs=${param.fs}"/></c:set>
</c:if>
<c:if test="${not empty param.pss}">
  <c:set var="svcAction"><c:out value="${svcAction}&pss=${param.pss}"/></c:set>
</c:if>
<c:if test="${not empty param.psg}">
	  	<c:set var="svrAction"><c:out value="${svrAction}&psg=${param.psg}"/></c:set>
</c:if>
<c:if test="${not empty param.pns}">
  <c:set var="svcAction"><c:out value="${svcAction}&pns=${param.pns}"/></c:set>
</c:if>
<c:if test="${not empty param.png}">
  <c:set var="svcAction"><c:out value="${svcAction}&png=${param.png}"/></c:set>
</c:if>
<c:if test="${not empty param.sos}">
  <c:set var="svcAction"><c:out value="${svcAction}&sos=${param.sos}"/></c:set>
</c:if>
<c:if test="${not empty param.sog}">
  <c:set var="svcAction"><c:out value="${svcAction}&sog=${param.sog}"/></c:set>
</c:if>
<c:if test="${not empty param.scs}">
  <c:set var="svcAction"><c:out value="${svcAction}&scs=${param.scs}"/></c:set>
</c:if>
<c:if test="${not empty param.scg}">
  <c:set var="svcAction"><c:out value="${svcAction}&scg=${param.scg}"/></c:set>
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

<tiles:insert definition=".resource.common.inventory.EffectivePolicy"/>

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
<jsu:script>
  	clearIfAnyChecked();
</jsu:script>
<tiles:insert definition=".page.footer"/>
