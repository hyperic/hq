<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
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
<c:set var="selfAction" value="/resource/application/Inventory.do?mode=view&rid=${Resource.id}&type=${entityId.type}"/>
<c:set var="editUrl" value="/resource/application/Inventory.do?mode=editResource&rid=${Resource.id}&type=${entityId.type}"/>


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


<tiles:insert definition=".page.title.resource.application.full">
  <tiles:put name="titleName"><hq:inventoryHierarchy resource="${entityId.appdefKey}" /></tiles:put>
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="resourceOwner" beanName="ResourceOwner"/>
  <tiles:put name="resourceModifier" beanName="ResourceModifier"/>
</tiles:insert>

<tiles:insert definition=".tabs.resource.application.inventory">
  <tiles:put name="resourceId" beanName="entityId" beanProperty="id"/>
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
</div>
</div>
<div id="panel2">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.application.inventory.ApplicationProperties"/>
</div>
<div id="panelContent">
<tiles:insert definition=".resource.application.inventory.applicationProperties.view">
  <tiles:put name="application" beanName="Resource"/>
</tiles:insert>
<c:if test="${useroperations['modifyApplication']}">
<tiles:insert definition=".toolbar.edit">
  <tiles:put name="editUrl" beanName="editUrl"/>
</tiles:insert>
</c:if>
</div>
</div>
<div id="panel3">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.application.inventory.ServiceCountsTab"/>
</div>
<div id="panelContent">
<tiles:insert definition=".resource.application.inventory.serviceCounts">
  <tiles:put name="serviceCount" beanName="NumChildResources"/>
  <tiles:put name="serviceTypeMap" beanName="ResourceTypeMap"/>
</tiles:insert>
</div>
</div>

<!-- services -->
<div id="panel4">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.application.inventory.ServicesTab"/>
</div>
<div id="panelContent">
<tiles:insert definition=".resource.application.inventory.services">
  <tiles:put name="application" beanName="Resource"/>
  <tiles:put name="services" beanName="ChildResources"/>
  <tiles:put name="serviceCount" beanName="NumChildResources"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>
<!-- / -->
</div>
</div>

<div id="panel4">
<div id="panelHeader" class="accordionTabTitleBar">
  <fmt:message key="resource.common.inventory.groups.GroupsTab"/>
</div>
<div id="panelContent">
<html:form action="/resource/application/inventory/RemoveGroups">
<html:hidden property="rid"/>
<html:hidden property="type"/>
<tiles:insert definition=".resource.common.inventory.groups">
  <tiles:put name="resource" beanName="Resource"/>
  <tiles:put name="groups" beanName="AllResGrps"/>
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>
</html:form>
</div>
</div>
</div>

<script type="text/javascript">
  onloads.push( accord<c:out value="${param.accord}"/> );
</script>

<tiles:insert definition=".page.footer"/>
