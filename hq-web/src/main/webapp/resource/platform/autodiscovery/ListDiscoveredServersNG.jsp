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


<!-- CONSTANT DEFINITIONS -->
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AIQueueConstants" 
    symbol="Q_DECISION_IGNORE" var="CONST_IGNORE" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AIQueueConstants" 
    symbol="Q_DECISION_UNIGNORE" var="CONST_UNIGNORE" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AIQueueConstants" 
    symbol="Q_STATUS_ADDED" var="CONST_ADDED" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AIQueueConstants" 
    symbol="Q_STATUS_CHANGED" var="CONST_CHANGED" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AIQueueConstants" 
    symbol="Q_STATUS_REMOVED" var="CONST_REMOVED" />
<hq:constant
    classname="org.hyperic.hq.appdef.shared.AIQueueConstants" 
    symbol="Q_STATUS_PLACEHOLDER" var="CONST_UNCHANGED" />

<tiles:importAttribute name="selfAction"/>

<c:set var="widgetInstanceName" value="listServers"/>
<hq:pageSize var="pageSize"/>
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>

<c:set var="resourceParam" value=""/>

<c:if test="${not empty param.rid}">
    <c:set var="resourceParam" value="rid=${param.rid}&type=${param.type}"/>
</c:if>

<c:set var="fullSelfAction" value="${selfAction}&${resourceParam}"/>
    
<c:url var="serverTypeFilterAction" value="${fullSelfAction}">
  <c:if test="${not empty param.ipsStatusFilter}">
    <c:param name="ipsStatusFilter" value="${param.ipsStatusFilter}"/>
  </c:if>
  <c:if test="${not empty param.stdStatusFilter}">
    <c:param name="stdStatusFilter" value="${param.stdStatusFilter}"/>
  </c:if>
</c:url>

<c:url var="stdStatusFilterAction" value="${fullSelfAction}">
  <c:if test="${not empty param.ipsStatusFilter}">
    <c:param name="ipsStatusFilter" value="${param.ipsStatusFilter}"/>
  </c:if>
  <c:if test="${not empty param.serverTypeFilter}">
    <c:param name="serverTypeFilter" value="${param.serverTypeFilter}"/>
  </c:if>
</c:url>

<c:set var="aiResourceUrl" 
        value="/resource/platform/autodiscovery/IgnoreDiscoveredServers.do?${resourceParam}"/>

<c:url var="aiResourceActionUrl" value="${aiResourceUrl}">
  <c:if test="${not empty param.ipsStatusFilter}">
    <c:param name="ipsStatusFilter" value="${param.ipsStatusFilter}"/>
  </c:if>
  <c:if test="${not empty param.stdStatusFilter}">
    <c:param name="stdStatusFilter" value="${param.stdStatusFilter}"/>
  </c:if>
  <c:if test="${not empty param.serverTypeFilter}">
    <c:param name="serverTypeFilter" value="${param.serverTypeFilter}"/>
  </c:if>
</c:url>
        
<!--  SERVERS TITLE -->

<c:set var="tmpTitle"> - <fmt:message key="resource.autodiscovery.typeAndNetworkProperties.NewModifiedEtc"/></c:set>

<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.autodiscovery.DiscoveredServersTab"/>  
  <tiles:putAttribute name="subTitle" value="${tmpTitle}"/>  
</tiles:insertDefinition>
<!--  /  -->

<c:set var="widgetInstanceName" value="listServers"/>

<!--  SERVERS FILTERING -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr>
  	<td class="FilterLine" colspan="4"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0" /></td>
  </tr>
  <tr>
    <td class="FilterLabelText" nowrap align="right">View:</td>

    <c:choose>
        <c:when test="${aForm.serverTypeFilterListCount > 0}">
            <td class="FilterLabelText">
			<s:select name="serverTypeFilter" class="FilterFormText" value="%{#attr.aForm.serverTypeFilter}" headerKey="-1" headerValue="%{getText('resource.platform.inventory.servers.filter.AllServerTypes')}" 
			list="%{#attr.aForm.serverTypeFilterList}" listKey="key" listValue="value"	onchange="goToSelectLocation(this, 'serverTypeFilter', '%{#attr.serverTypeFilterAction}');" />
            </td>
        </c:when>
        <c:otherwise>
            <td class="FilterLabelText">
            </td>
        </c:otherwise>
    </c:choose>
    <td class="FilterLabelText" width="100%">

	<s:select theme="simple" name="stdStatusFilter" value="%{#attr.aForm.stdStatusFilter}" 
	list="#{ '-1':getText('resource.autodiscovery.discoveredServers.states.AllStates'), #attr.CONST_ADDED:getText('resource.autodiscovery.discoveredServers.states.New'), #attr.CONST_CHANGED:getText('resource.autodiscovery.discoveredServers.states.Modified'), #attr.CONST_UNCHANGED:getText('resource.autodiscovery.discoveredServers.states.Unchanged') }" 
	onchange="goToSelectLocation(this, 'stdStatusFilter', '%{#attr.stdStatusFilterAction}');"/>    
    </td>
  </tr>
</table>

<!--  SERVERS CONTENTS 2 -->
  
    <table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
        <tr class="tableRowHeader">
	    <th width="15%" class="tableRowInactive"><fmt:message key="resource.platform.inventory.servers.ServerTH"/></th>
	    <th width="15%" class="tableRowInactive"><fmt:message key="resource.autodiscovery.discoveredServers.ServerTypeTH"/></th>
	    <th width="25%" class="tableRowInactive"><fmt:message key="resource.platform.inventory.servers.InstallPathTH"/></th>
            <th width="15%" class="tableRowInactive"><fmt:message key="resource.autodiscovery.discoveredServers.ServerStatusTH"/></th>
            <th width="15%" class="tableRowInactive"><fmt:message key="resource.autodiscovery.discoveredServers.ActionTH"/></th>
        </tr>
     <c:forEach var="aiResource" items="${AIServers}">
     
<c:set var="resIgnored" value="${CONST_UNIGNORE}"/>
<c:if test="${aiResource.ignored}">
    <c:set var="resIgnored" value="${CONST_IGNORE}"/>
</c:if>
     
  <c:set var="trClass" value="ListRow"/>
  <c:choose>
    <c:when test="${aiResource.queueStatus == CONST_REMOVED ||
                    aiResource.queueStatus == CONST_ADDED}">
        <c:set var="trClass" value="AutoDiscRowNew"/>
    </c:when>
    <c:when test="${aiResource.queueStatus == CONST_CHANGED}">
        <c:set var="trClass" value="AutoDiscRowModified"/>
    </c:when>
  </c:choose>
	<tr class="<c:out value='${trClass}'/>"> <%-- AutoDiscRowModified, ListRow  --%>
	    <td class="ListCellPrimary"><c:out value="${aiResource.name}"/></td>
	    <td class="ListCell"><c:out value="${aiResource.serverTypeName}"/></td>
	    <td class="ListCell"><c:out value="${aiResource.installPath}"/></td>
	    <td class="ListCell"><c:out value="${aiResource.queueStatusStr}"/></td>
	    
  <td class="ListCellNoPadding">
      <c:choose>
        <c:when test="${aiResource.queueStatus == CONST_REMOVED}">
			<c:set var="genName" value="aiserver${aiResource.id}" scope="request"/>
			<s:select theme="simple" name="%{#attr.genName}" value="%{#attr.resIgnored}" 
			list="#{ #attr.CONST_IGNORE:getText('resource.autodiscovery.action.uninstalled.KeepInInventrory'), #attr.CONST_UNIGNORE:getText('resource.autodiscovery.action.uninstalled.DeleteFromInventory') }"  />  
        </c:when>
        <c:when test="${aiResource.queueStatus == CONST_UNCHANGED}">
            <fmt:message key="resource.autodiscovery.action.unchanged.NoActions"/>
        </c:when>
        <c:otherwise>
			<c:set var="genName" value="aiserver${aiResource.id}" scope="request"/>
			<s:select theme="simple" name="%{#attr.genName}" value="%{#attr.resIgnored}" 
			list="#{ #attr.CONST_IGNORE:getText('resource.autodiscovery.action.new.DoNotImport'), #attr.CONST_UNIGNORE:getText('resource.autodiscovery.action.new.ImportServer') }"  />  
        </c:otherwise>
      </c:choose>
  </td>
	</tr>
    </c:forEach>
</table>
  
  
</div>
<!--  /  -->
