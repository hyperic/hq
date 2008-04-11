<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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

<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">

<hq:constant symbol="NUM_APPSVC_DEPENDEES_ATTR" var="DependeePageSize" />
<hq:constant symbol="NUM_APPSVC_DEPENDERS_ATTR" var="DependerPageSize" />
<c:set var="widgetInstanceName" value="listServices"/>
<script type="text/javascript">
var pageData = new Array();
</script>
<script  src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetPropertiesListServices = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>
<c:url var="selfAction"    value="/resource/application/Inventory.do?mode=listServiceDependencies&appSvcId=${appSvcCurrent.id}&type=${Resource.entityId.type}&rid=${Resource.id}" />

<tiles:insert definition=".page.title.resource.application">
  <tiles:put name="titleKey" value="common.title.Edit"/>
  <tiles:put name="titleName" beanName="Resource" beanProperty="name"/>
</tiles:insert>

<c:choose>
<c:when test="${appSvcCurrent.isGroup == true}">
<c:set var="appSvcCurrentName" value="${appSvcCurrent.resourceGroup.name}" />
</c:when>
<c:otherwise>
<c:set var="appSvcCurrentName" value="${appSvcCurrent.service.name}" />
</c:otherwise>
</c:choose>

<tiles:insert definition=".header.tab">
	<tiles:put name="tabKey" value="resource.application.inventory.services.DependeesTab"/>
	<tiles:put name="tabName" beanName="appSvcCurrentName"/>
</tiles:insert>
<html:form action="/resource/application/inventory/RemoveServiceDependencies">
<html:hidden property="rid" value="${Resource.id}"/>
<html:hidden property="type" value="${Resource.entityId.type}"/>
<html:hidden property="appSvcId" value="${appSvcCurrent.id}" />
<c:choose>
<c:when test="${empty appSvcDependees}">
<div id="listDiv">
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
  <tr>
  <td width="1%" class="ListHeaderCheckbox"><html:img page="/images/spacer.gif" width="10" height="10" border="0"/></td>
  <td width="10%" class="ListHeaderOff"><fmt:message key="resource.application.DependenciesTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.ServiceTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.TypeTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.HostServerTH"/></td>
  </tr>
  <tr class="ListRow">
  <td class="ListCellCheckbox"><html:img page="/images/spacer.gif" width="10" height="10" border="0"/></td>
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
      href="/resource/application/Inventory.do?mode=listServiceDependencies&appSvcId=${service.appServiceId}&rid=${Resource.id}&type=${Resource.entityId.type}"
      id="${service.name}" 
      src="/images/fb_view.gif"/>
  </display:column>
  <display:column width="30%" property="name" sort="true" sortAttr="5"
    defaultSort="true" 
    title="resource.application.inventory.services.ServiceTH" 
    href="/resource/${service.entityId.typeName}/Inventory.do?mode=view&rid=${service.id}&type=${service.appdefType}" />
  <display:column width="30%" property="serviceType.name" sort="true" 
    sortAttr="4" defaultSort="false" 
    title="resource.application.inventory.services.TypeTH" /> 
  <display:column width="30%" property="server.name"
    title="resource.application.inventory.services.HostServerTH" />
</display:table>
</c:otherwise>
</c:choose>
<c:set var="addToListUrl" value="/resource/application/Inventory.do?mode=addDependencies&appSvcId=${appSvcCurrent.id}&rid=${Resource.id}&type=${Resource.entityId.type}" />
<c:set var="widgetInstanceName" value="listServices"/>

<tiles:insert definition=".toolbar.addToList">
  <tiles:put name="addToListUrl" beanName="addToListUrl"/>
  <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
  <tiles:put name="listItems" beanName="appSvcDependees"/>
  <tiles:put name="listSize" beanName="numAppSvcDependees"/>
  <tiles:put name="pageNumAction" beanName="selfAction"/>    
  <tiles:put name="defaultSortColumn" value="5"/>
  <tiles:put name="pageSizeAction" beanName="selfAction" />
</tiles:insert>
</html:form>
    &nbsp;<br>
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.application.inventory.services.DependersTab"/>
  <tiles:put name="tabName" beanName="appSvcCurrentName"/>
</tiles:insert>

<c:choose>
<c:when test="${empty appSvcDependers}">
<div id="listDiv">
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
  <tr>
  <td width="1%" class="ListHeaderCheckbox"><html:img page="/images/spacer.gif" width="10" height="10" border="0"/></td>
  <td width="10%" class="ListHeaderOff"><fmt:message key="resource.application.DependenciesTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.ServiceTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.TypeTH"/></td>
  <td width="30%" class="ListHeaderOff"><fmt:message key="resource.application.HostServerTH"/></td>
  </tr>
  <tr class="ListRow">
  <td class="ListCellCheckbox"><html:img page="/images/spacer.gif" width="10" height="10" border="0"/></td>
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
      href="/resource/application/Inventory.do?mode=listServiceDependencies&appSvcId=${service.appServiceId}&rid=${Resource.id}&type=${Resource.entityId.type}"
      id="${service.name}" 
      src="/images/fb_view.gif"/>
  </display:column>
  <display:column width="30%" property="name" sort="true" sortAttr="5"
    defaultSort="true" 
    title="resource.application.inventory.services.ServiceTH" 
    href="/resource/${service.entityId.typeName}/Inventory.do?mode=view&rid=${service.id}&type=${service.appdefType}" />
  <display:column width="30%" property="serviceType.name" sort="true" 
    sortAttr="4" defaultSort="false" 
    title="resource.application.inventory.services.TypeTH" /> 
  <display:column width="30%" property="server.name"
    title="resource.application.inventory.services.HostServerTH" />
</display:table>
</c:otherwise>
</c:choose>
    &nbsp;<br>

<c:url var="returnUrl" value="/resource/application/Inventory.do">
<c:param name="mode" value="view" />
<c:param name="rid" value="${Resource.id}" />
<c:param name="type" value="${Resource.entityId.type}" />
</c:url>
<html:link href="${returnUrl}"><fmt:message key="resource.application.inventory.services.ReturnLink" /></html:link>

<tiles:insert definition=".page.footer"/>

</form>

