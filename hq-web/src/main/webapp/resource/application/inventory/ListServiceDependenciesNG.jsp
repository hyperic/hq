<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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

<link rel=stylesheet href="<s:url value="/css/win.css"/>" type="text/css">

<hq:constant symbol="NUM_APPSVC_DEPENDEES_ATTR" var="DependeePageSize" />
<hq:constant symbol="NUM_APPSVC_DEPENDERS_ATTR" var="DependerPageSize" />
<c:set var="widgetInstanceName" value="listServices"/>
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	var pageData = new Array();
	
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetPropertiesListServices = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>

<c:url var="selfAction" value="addServiceDependencies.action">
	<c:param name="mode" value="listServiceDependencies"/>
	<c:param name="appSvcId" value="${appSvcCurrent.id}"/>
	<c:param name="type" value="${Resource.entityId.type}"/>
	<c:param name="rid" value="${Resource.id}" />
</c:url>

<tiles:insertDefinition name=".page.title.resource.application">
    <tiles:putAttribute name="titleKey" value="common.title.Edit"/>
    <tiles:putAttribute name="titleName" value="${Resource.name}"/>
	<tiles:putAttribute name="ignoreBreadcrumb" value="false"/>
</tiles:insertDefinition>

<c:choose>
<c:when test="${appSvcCurrent.isGroup == true}">
<c:set var="appSvcCurrentName" value="${appSvcCurrent.resourceGroup.name}" />
</c:when>
<c:otherwise>
<c:set var="appSvcCurrentName" value="${appSvcCurrent.service.name}" />
</c:otherwise>
</c:choose>

<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.application.inventory.services.DependeesTab"/>
  <tiles:putAttribute name="tabName" value="${appSvcCurrentName}"/>
</tiles:insertDefinition>

<s:form action="applicationViewRemoveServiceDepdenciesFromList">
<input type="hidden" name="rid" value="<c:out value="${Resource.id}"/>"    />
<input type="hidden" name="type" value="<c:out value="${Resource.entityId.type}"/>"     />
<input type="hidden" name="appSvcId" value="<c:out value="${appSvcCurrent.id}"/>"     />
<c:choose>
<c:when test="${empty appSvcDependees}">
<div id="listDiv">
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
  <tr>
  <td width="1%" class="ListHeaderCheckbox"><img src='<s:url value="/images/spacer.gif"/>' width="10" height="10" border="0"/></td>
  <td width="10%" class="ListHeaderOff"><fmt:message key="resource.application.DependenciesTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.ServiceTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.TypeTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.HostServerTH"/></td>
  </tr>
  <tr class="ListRow">
  <td class="ListCellCheckbox"><img src='<s:url value="/images/spacer.gif"/>' width="10" height="10" border="0"/></td>
  <td class="ListCell" colspan="4">
  <i><fmt:message key="resource.application.inventory.services.NoDependees"><c:choose><c:when test="${appSvcCurrent.isGroup == true}"><fmt:param value="${appSvcCurrent.resourceGroup.name}" /></c:when><c:otherwise><fmt:param value="${appSvcCurrent.service.name}"/></c:otherwise></c:choose></fmt:message></i></td>
  </tr>
</table>
</div>
</c:when>
<c:otherwise>
<display:table items="${appSvcDependees}" cellspacing="0" cellpadding="0" 
  width="100%" action="${selfAction}" var="service" pageSize="${requestScope[DependeePageSize]}">
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
  <display:column width="30%" property="name" sort="true" sortAttr="5"
    defaultSort="true" 
    title="resource.application.inventory.services.ServiceTH" 
    href="viewResourceInventoryServiceVisibility.action?mode=view&rid=${service.id}&type=${service.appdefType}" />
  <display:column width="30%" property="serviceType.name" sort="true" 
    sortAttr="4" defaultSort="false" 
    title="resource.application.inventory.services.TypeTH" /> 
  <display:column width="30%" property="server.name"
    title="resource.application.inventory.services.HostServerTH" />
</display:table>
</c:otherwise>
</c:choose>
<c:url var="addToListUrl" value="addDependenciesInventoryApplicationVisibility.action">
	<c:param name="mode" value="addDependencies"/>
	<c:param name="appSvcId" value="${appSvcCurrent.id}"/>
	<c:param name="rid" value="${Resource.id}"/>
	<c:param name="type" value="${Resource.entityId.type}"/>
</c:url>
<c:set var="widgetInstanceName" value="listServices"/>

<tiles:insertDefinition name=".ng.toolbar.addToList">
  <tiles:putAttribute name="addToListUrl" value="${addToListUrl}"/>
  <tiles:putAttribute name="listItems" value="${appSvcDependees}"/>
  <tiles:putAttribute name="listSize" value="${numAppSvcDependees}"/>
  <tiles:putAttribute name="pageSizeAction" value="${selfAction}" />
  <tiles:putAttribute name="pageSizeParam" value="pss"/>
  <tiles:putAttribute name="pageNumAction" value="${selfAction}"/> 
  <tiles:putAttribute name="pageNumParam" value="pns"/>
  <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
  <tiles:putAttribute name="defaultSortColumn" value="5"/>
</tiles:insertDefinition>

</s:form>
    &nbsp;<br>
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.application.inventory.services.DependersTab"/>
  <tiles:putAttribute name="tabName" value="${appSvcCurrentName}"/>
</tiles:insertDefinition>

<c:choose>
<c:when test="${empty appSvcDependers}">
<div id="listDiv">
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
  <tr>
  <td width="1%" class="ListHeaderCheckbox"><img src='<s:url value="/images/spacer.gif"/>' width="10" height="10" border="0"/></td>
  <td width="10%" class="ListHeaderOff"><fmt:message key="resource.application.DependenciesTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.ServiceTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.TypeTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.HostServerTH"/></td>
  </tr>
  <tr class="ListRow">
  <td class="ListCellCheckbox"><img src='<s:url value="/images/spacer.gif"/>' width="10" height="10" border="0"/></td>
  <td class="ListCell" colspan="4"><i><fmt:message key="resource.application.inventory.services.NoDependers"><c:choose><c:when test="${appSvcCurrent.isGroup == true}"><fmt:param value="${appSvcCurrent.resourceGroup.name}" /></c:when><c:otherwise><fmt:param value="${appSvcCurrent.service.name}"/></c:otherwise></c:choose></fmt:message></i></td>
  </tr>
</table>
</div>
</c:when>
<c:otherwise>
<display:table items="${appSvcDependers}" cellspacing="0" cellpadding="0" 
  width="100%" action="${selfAction}" var="service" pageSize="${requestScope[DependerPageSize]}">
  <display:column width="10%" align="middle" property="appServiceId" 
    title="resource.application.inventory.services.DependenciesTH"  
    styleClass="ListCell" headerStyleClass="ListHeader">
    <display:imagelinkdecorator 
      href="listServiceDependenciesInventoryApplicationVisibility.action?mode=listServiceDependencies&appSvcId=${service.appServiceId}&rid=${Resource.id}&type=${Resource.entityId.type}"
      id="${service.name}" 
      src="/images/fb_view.gif"/>
  </display:column>
  <display:column width="30%" property="name" sort="true" sortAttr="5"
    defaultSort="true" 
    title="resource.application.inventory.services.ServiceTH" 
    href="viewResourceInventoryServiceVisibility.action?mode=view&rid=${service.id}&type=${service.appdefType}" />
  <display:column width="30%" property="serviceType.name" sort="true" 
    sortAttr="4" defaultSort="false" 
    title="resource.application.inventory.services.TypeTH" /> 
  <display:column width="30%" property="server.name"
    title="resource.application.inventory.services.HostServerTH" />
</display:table>
</c:otherwise>
</c:choose>
    &nbsp;<br>

<c:url var="returnUrl" value="viewResourceInventoryApplicationVisibility.action">
	<c:param name="mode" value="view" />
	<c:param name="rid" value="${Resource.id}" />
	<c:param name="type" value="${Resource.entityId.type}" />
</c:url>

<s:a action="%{#attr.returnUrl}"><fmt:message key="resource.application.inventory.services.ReturnLink" /></s:a>


