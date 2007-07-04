<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
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

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</script>


<html:hidden property="rid"/>
<html:hidden property="type"/>
<html:hidden property="aiPid"/>

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

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.autodiscovery.DiscoveredServersTab"/>
  <tiles:put name="subTitle" beanName="tmpTitle"/>
</tiles:insert>
<!--  /  -->

<c:set var="widgetInstanceName" value="listServers"/>

<!--  SERVERS FILTERING -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr>
  	<td class="FilterLine" colspan="4"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td class="FilterLabelText" nowrap align="right">View:</td>
    <c:choose>
        <c:when test="${AutoDiscoveryResultsForm.serverTypeFilterListCount > 0}">
            <td class="FilterLabelText">
                <html:select property="serverTypeFilter" styleClass="FilterFormText" 
                                onchange="goToSelectLocation(this, 'serverTypeFilter', '${serverTypeFilterAction}');">
                    <html:option value="-1" key="resource.platform.inventory.servers.filter.AllServerTypes"/>
                    <html:optionsCollection property="serverTypeFilterList" value="id" label="name"/>
                </html:select>
            </td>
        </c:when>
        <c:otherwise>
            <td class="FilterLabelText">
            </td>
        </c:otherwise>
    </c:choose>
    <td class="FilterLabelText" width="100%">
        <html:select property="stdStatusFilter" styleClass="FilterFormText" onchange="goToSelectLocation(this, 'stdStatusFilter', '${stdStatusFilterAction}');">
            <html:option value="-1" key="resource.autodiscovery.discoveredServers.states.AllStates"/>
            <html:option value="${CONST_ADDED}" key="resource.autodiscovery.discoveredServers.states.NewAndModified"/>
            <html:option value="${CONST_UNCHANGED}" key="resource.autodiscovery.discoveredServers.states.Unchanged"/>
        </html:select>
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
            <html:select property="aiserver:${aiResource.id}" value="${resIgnored}">
                <html:option value="${CONST_UNIGNORE}"><fmt:message key="resource.autodiscovery.action.uninstalled.DeleteFromInventory"/></html:option>
                <html:option value="${CONST_IGNORE}"><fmt:message key="resource.autodiscovery.action.uninstalled.KeepInInventrory"/></html:option>
            </html:select>
        </c:when>
        <c:when test="${aiResource.queueStatus == CONST_UNCHANGED}">
            <fmt:message key="resource.autodiscovery.action.unchanged.NoActions"/>
        </c:when>
        <c:otherwise>
            <html:select property="aiserver:${aiResource.id}" value="${resIgnored}">
               <html:option value="${CONST_UNIGNORE}"><fmt:message key="resource.autodiscovery.action.new.ImportServer"/></html:option>
               <html:option value="${CONST_IGNORE}"><fmt:message key="resource.autodiscovery.action.new.DoNotImport"/></html:option>
            </html:select>
        </c:otherwise>
      </c:choose>
  </td>
	</tr>
    </c:forEach>
</table>
  
  
</div>
<!--  /  -->
