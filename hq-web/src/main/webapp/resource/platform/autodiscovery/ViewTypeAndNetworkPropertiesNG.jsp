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

<!--  TYPE AND HOST PROPERTIES CONTENTS -->
<c:set var="ignorePlatformForm" value="AIPlatformResultsForm"/>

<tiles:importAttribute name="selfAction"/>

<!-- setup the plaform AI resource -->
<c:set var="aiResource" value="${AIPlatform}"/>
<!-- / -->

<c:set var="widgetInstanceName" value="listIps"/>
<hq:pageSize var="pageSize"/>
<jsu:importScript path="/js/listWidget.js" />
<jsu:script>
	initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
	widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
</jsu:script>


<c:set var="resourceParam" value=""/>

<c:if test="${not empty param.rid}">
    <c:set var="resourceParam" value="eid=${param.type}:${param.rid}"/>
</c:if>

<c:set var="fullSelfAction" 
    value="${selfAction}&${resourceParam}"/>
        
<c:set var="ipsStatusFilterAction1" 
    value="${selfAction}&${resourceParam}"/>

<c:url var="ipsStatusFilterAction" value="${ipsStatusFilterAction1}">
  <c:if test="${not empty param.stdStatusFilter}">
    <c:param name="stdStatusFilter" value="${param.stdStatusFilter}"/>
  </c:if>
  <c:if test="${not empty param.serverTypeFilter}">
    <c:param name="serverTypeFilter" value="${param.serverTypeFilter}"/>
  </c:if>
</c:url>

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

<c:if test="${aiResource.ignored}">
    <c:set var="trClass" value="AutoDiscRowIgnored"/>
</c:if>

<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.platform.inventory.type.field.MachineType"/>  
</tiles:insertDefinition>

 <table width="100%" cellpadding="0" cellspacing="0" border="0" style="padding-bottom: 10px;">
  <tr valign="top" class="<c:out value='${trClass}'/>">
	<td width="20%" class="AutoDiscLabel"><fmt:message key="resource.platform.inventory.type.MachineTypeLabel"/></td>
	<td width="30%" class="AutoDiscContent"><c:out value="${aiResource.platformTypeName}"/></td>
	<td width="20%" class="AutoDiscLabel"><fmt:message key="resource.platform.inventory.type.FQDNLabel"/></td>
	<td width="30%" class="AutoDiscContent"><c:out value="${aiResource.fqdn}"/></td>
  </tr>
  <tr valign="top" class="<c:out value='${trClass}'/>">
		<td width="20%" class="AutoDiscLabel"><fmt:message key="resource.platform.inventory.type.AutoDiscoveryDetailLabel"/></td>
		<td colspan="3">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
	      <tr>
          <td width="20%" class="AutoDiscContent"><fmt:message key="resource.autodiscovery.typeAndNetworkProperties.PropertiesStateLabel"/></td>
          <td width="80%" class="AutoDiscContent"><fmt:message key="resource.autodiscovery.typeAndNetworkProperties.ActionLabel"/></td>
        </tr>
        
          <!-- include auto-discovery detail-->
		  <tiles:insertTemplate template="/resource/platform/autodiscovery/ViewResourceActionDetailNG.jsp">
            <tiles:putAttribute name="aiResource" value="${aiResource}"/>
            <tiles:putAttribute name="resourceParam" value="${resourceParam}"/>
            <tiles:putAttribute name="aiResourceActionUrl" value="/resource/platform/autodiscovery/IgnorePlatform.do"/>
            <tiles:putAttribute name="resParam" value="aiplatform"/>
          </tiles:insertTemplate>
        
      </table>
    </td>
  </tr>

  <tr>
    <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0" /></td>
  </tr>
</table>
   
<!--  TYPE AND HOST PROPERTIES TITLE -->

<c:set var="tmpTitle"> - <fmt:message key="resource.autodiscovery.typeAndNetworkProperties.NewModifiedEtc"/></c:set>

<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.platform.inventory.NetworkPropertiesTab"/>  
  <tiles:putAttribute name="subTitle" value="${tmpTitle}"/>  
</tiles:insertDefinition>
<!--  /  -->

<!--  DISCOVERED IPS FILTERING -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
  	<td class="FilterLine" colspan="2"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0" /></td>
  </tr>
  <tr>
    <td class="FilterLabelText" nowrap align="right">View:</td>
    <td class="FilterLabelText" width="100%">
	<s:select theme="simple" name="ipsStatusFilter" value="%{#attr.aForm.ipsStatusFilter}" 
	list="#{ '-1':getText('resource.autodiscovery.discoveredServers.states.AllStates'), #attr.CONST_ADDED:getText('resource.autodiscovery.discoveredServers.states.New'), #attr.CONST_CHANGED:getText('resource.autodiscovery.discoveredServers.states.Modified'), #attr.CONST_UNCHANGED:getText('resource.autodiscovery.discoveredServers.states.Unchanged') }" 
	onchange="goToSelectLocation(this, 'ipsStatusFilter', '%{#attr.ipsStatusFilterAction}');" class="FilterFormText"/>    
	</td>
  </tr>
</table>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
 
<!-- IPS 1 -->

<c:forEach var="ip" items="${AIIps}">

<!-- setup the ip ai resource -->
<c:set var="aiResource" value="${ip}"/>
<!-- / -->
<c:set var="aiResourceActionUrlItem" 
        value="${aiResourceActionUrl}&aiRid=${aiResource.id}"/>

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

  <tr valign="top" class="<c:out value='${trClass}'/>">
		<td width="20%" class="AutoDiscLabel"><fmt:message key="resource.platform.inventory.type.IPAddressLabel"/></td>
		<td width="30%" class="AutoDiscContent"><c:out value="${aiResource.address}"/></td>
		<td width="20%" class="AutoDiscLabel"><fmt:message key="resource.platform.inventory.type.NetmaskLabel"/></td>
		<td width="30%" class="AutoDiscContent"><c:out value="${aiResource.netmask}"/></td>
  </tr>
  <tr valign="top" class="<c:out value='${trClass}'/>">
		<td width="20%" class="AutoDiscLabel"><fmt:message key="resource.platform.inventory.type.MACAddressLabel"/></td>
		<td width="30%" class="AutoDiscContent"><c:out value="${aiResource.MACAddress}"/></td>
		<td width="20%" class="AutoDiscLabel">&nbsp;</td>
		<td width="30%" class="AutoDiscContent">&nbsp;</td>
  </tr>
  <tr valign="top" class="<c:out value='${trClass}'/>">
		<td width="20%" class="AutoDiscLabel"><fmt:message key="resource.platform.inventory.type.AutoDiscoveryDetailLabel"/></td>
		<td colspan="3">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
	      <tr>
            <td width="20%" class="AutoDiscContent"><fmt:message key="resource.autodiscovery.typeAndNetworkProperties.PropertiesStateLabel"/></td>
            <td width="80%" class="AutoDiscContent"><fmt:message key="resource.autodiscovery.typeAndNetworkProperties.ActionLabel"/></td>
          </tr>
        
          <!-- include auto-discovery detail-->
          <tiles:insertTemplate template="/resource/platform/autodiscovery/ViewResourceActionDetailNG.jsp">
            <tiles:putAttribute name="aiResource" value="${aiResource}"/>
            <tiles:putAttribute name="resourceParam" value="${resourceParam}"/>
            <tiles:putAttribute name="aiResourceActionUrl" value="/resource/platform/autodiscovery/IgnorePlatform.do"/>

          </tiles:insertTemplate>
                
      </table>
    </td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0" /></td>
  </tr>
</c:forEach>  
</table>
<!--  /  -->

<!--  /  -->
