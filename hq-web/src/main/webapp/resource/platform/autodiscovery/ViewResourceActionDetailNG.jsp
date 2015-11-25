<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

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
    symbol="Q_STATUS_PLACEHOLDER" var="CONST_PLACEHOLDER" />
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

<tiles:importAttribute name="aiResource" ignore="true"/>
<tiles:importAttribute name="aiResourceActionUrl"/>
<tiles:importAttribute name="resourceParam" ignore="true"/>
<tiles:importAttribute name="resParam" ignore="true"/>

<c:set var="aiResourceUrl" 
        value="${aiResourceActionUrl}?${resourceParam}"/>

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

  <c:set var="trClass" value="ListRow"/>
  <c:choose>
    <c:when test="${aiResource.queueStatus == CONST_REMOVED}">
        <fmt:message key="resource.autodiscovery.typeAndNetworkProperties.RemovedAtPlatform" var="queueStatusText"/>
    </c:when>
    <c:when test="${aiResource.queueStatus == CONST_ADDED}">
        <fmt:message key="resource.autodiscovery.typeAndNetworkProperties.NewProperties" var="queueStatusText"/>
    </c:when>
    <c:when test="${aiResource.queueStatus == CONST_CHANGED}">
        <fmt:message key="resource.autodiscovery.typeAndNetworkProperties.ModifiedProperties" var="queueStatusText"/>
    </c:when>
    <c:when test="${aiResource.queueStatus == CONST_UNCHANGED}">
        <fmt:message key="resource.autodiscovery.typeAndNetworkProperties.UnchangedProperties" var="queueStatusText"/>
    </c:when>
  </c:choose>

<c:set var="resIgnored" value="${CONST_UNIGNORE}"/>
<c:if test="${aiResource.ignored}">
    <c:set var="resIgnored" value="${CONST_IGNORE}"/>
</c:if>

<tr>
  <td width="20%" class="AutoDiscContent">
  <c:out value="${queueStatusText}"/>
  </td>
  <td width="80%" class="AutoDiscContent">
      <c:choose>
        <c:when test="${aiResource.queueStatus == CONST_REMOVED}">
          <c:choose>
            <c:when test="${empty resParam}">
                <fmt:message key="resource.autodiscovery.action.uninstalled.DeleteFromInventory"/>
            </c:when>
            <c:otherwise>
			<c:set var="genName" value="${resParam}:${aiResource.id}" scope="request"/>
			<s:select theme="simple" name="%{#attr.genName}" value="%{#attr.resIgnored}" 
			list="#{ #attr.CONST_IGNORE:getText('resource.autodiscovery.action.uninstalled.KeepInInventrory'), #attr.CONST_UNIGNORE:getText('resource.autodiscovery.action.uninstalled.DeleteFromInventory') }"  />  
            </c:otherwise>
          </c:choose>
        </c:when>
        <c:when test="${aiResource.queueStatus == CONST_UNCHANGED}">
            <fmt:message key="resource.autodiscovery.action.unchanged.NoActions"/>
        </c:when>
        <c:otherwise>
          <c:choose>
            <c:when test="${empty resParam}">
                <fmt:message key="resource.autodiscovery.typeAndNetworkProperties.filter.ImportValues"/>
            </c:when>
            <c:otherwise>
			<c:set var="genName" value="${resParam}:${aiResource.id}" scope="request"/>
			<s:select theme="simple" name="%{#attr.genName}" value="%{#attr.resIgnored}" 
			list="#{ #attr.CONST_UNIGNORE:getText('resource.autodiscovery.typeAndNetworkProperties.filter.ImportValues'), #attr.CONST_IGNORE:getText('resource.autodiscovery.action.new.DoNotImport') }"  />  

            </c:otherwise>
          </c:choose>
        </c:otherwise>
      </c:choose>
  </td>
</tr> 
