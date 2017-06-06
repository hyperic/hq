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



<tiles:importAttribute name="application"/>
<tiles:importAttribute name="services"/>
<tiles:importAttribute name="serviceCount"/>

<hq:pageSize var="pageSize"/>
<c:url var="addToListUrl" value="addServicesInventoryApplicationVisibility.action">
	<c:param name="mode" value="addServices"/>
	<c:param name="eid" value="${Resource.entityId}" />
</c:url>
<c:set var="widgetInstanceName" value="listServices"/>

<c:url var="selfAction" value="viewResourceInventoryApplicationVisibility.action">
	<c:param name="mode" value="view"/>
	<c:param name="accord" value="3"/>
	<c:param name="eid" value="${Resource.entityId}"/>
</c:url>

<c:url var="ctxSelfAction" value="${selfAction}"/>

<c:url var="selfPssAction" value="${selfAction}">
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
  <c:if test="${not empty param.psg}">
    <c:param name="psg" value="${param.psg}"/>
  </c:if>
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

<c:url var="selfPnsAction" value="${selfAction}">
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.sos}">
    <c:param name="sos" value="${param.sos}"/>
  </c:if>
  <c:if test="${not empty param.psg}">
    <c:param name="psg" value="${param.psg}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
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
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetPropertiesListServices = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>

<s:form action="applicationViewRemoveServiceFromList" method="post" >
<s:hidden theme="simple" name="rid"  value="%{#attr.Resource.id}"/>
<s:hidden theme="simple" name="type" value="%{#attr.Resource.entityId.type}"/>
<s:hidden theme="simple" name="eid"  value="%{#attr.eid}"/>
<!--  /  -->
<!--  SERVICES CONTENTS -->
<div id="listDiv">

<display:table items="${Services}" cellspacing="0" cellpadding="0" orderValue="sos" order="${param.sos}"  sortValue="scs" sort="${param.scs}"
  width="100%" action="${ctxSelfAction}" var="service" pageSize="${pageSize}">
  <display:column width="1%" property="appServiceId" 
    title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetPropertiesListServices, true)\" name=\"listToggleAll\">"  
    isLocalizedTitle="false" styleClass="ListCellCheckbox" 
    headerStyleClass="ListHeaderCheckbox" >
    <display:checkboxdecorator name="resources" 
      onclick="ToggleSelection(this, widgetPropertiesListServices, true)" 
      styleClass="listMember"/>
  </display:column>
  <display:column width="10%" align="middle" property="appServiceId" 
    title="resource.application.inventory.services.DependenciesTH"  
    styleClass="ListCell" headerStyleClass="ListHeader">
    <display:imagelinkdecorator 
      href="listServiceDependenciesInventoryApplicationVisibility.action?mode=listServiceDependencies&appSvcId=${service.appServiceId}&rid=${Resource.id}&type=${Resource.entityId.type}"
      id="${service.name}" 
      src="/images/fb_view.gif"/>
  </display:column>
  <display:column width="20%" property="name" sort="true" sortAttr="22"
    defaultSort="true" 
    title="resource.application.inventory.services.ServiceTH" 
    href="viewResourceInventoryServiceVisibility.action?mode=view&rid=${service.id}&type=${service.appdefType}" />
  <display:column width="8%" property="entryPoint" align="middle" styleClass="ListCell" 
    title="resource.application.inventory.services.EntryPointTH">
    <display:booleandecorator flagKey="yesno"/>
  </display:column>
  <display:column width="20%" property="serviceType.name" sort="true" 
    sortAttr="23" defaultSort="false" 
    title="resource.application.inventory.services.TypeTH" /> 
  <display:column width="8%" property="cluster" align="middle" styleClass="ListCell" 
    title="resource.application.inventory.services.ClusterTH">
    <display:booleandecorator 
      flagKey="resource.application.inventory.service.iscluster"/>
  </display:column>
  <display:column width="19%" property="server.name"
    title="resource.application.inventory.services.HostServerTH" />
  <display:column property="id" 
    title="resource.common.monitor.visibility.AvailabilityTH"
    width="10%" styleClass="ListCellCheckbox" 
    headerStyleClass="ListHeaderCheckbox" valign="middle">
    <display:availabilitydecorator resourceId="${service.id}" resourceTypeId="${service.appdefType}" monitorable="${service.cluster}" />
  </display:column>
</display:table>
</div>
<!--  /  -->

<tiles:insertDefinition name=".ng.toolbar.addToList">
  <tiles:putAttribute name="addToListUrl" value="${addToListUrl}"/>
  <tiles:putAttribute name="listItems" value="${services}"/>
  <tiles:putAttribute name="listSize" value="${serviceCount}"/>
  <tiles:putAttribute name="pageSizeAction" value="${selfPssAction}" />
  <tiles:putAttribute name="pageSizeParam" value="pss"/>
  <tiles:putAttribute name="pageNumAction" value="${selfPnsAction}"/>    
  <tiles:putAttribute name="pageNumParam" value="pns"/>
  <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
  <tiles:putAttribute name="defaultSortColumn" value="5"/>
</tiles:insertDefinition>
</s:form>
