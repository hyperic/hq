<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
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
<html:hidden property="rid"/>
<html:hidden property="type"/>
<html:hidden property="aiPid"/>

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

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.platform.inventory.type.field.MachineType"/>
</tiles:insert>

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
          <tiles:insert page="/resource/platform/autodiscovery/ViewResourceActionDetail.jsp">
            <tiles:put name="aiResource" beanName="aiResource"/>
            <tiles:put name="resourceParam" beanName="resourceParam"/>
            <tiles:put name="aiResourceActionUrl" value="/resource/platform/autodiscovery/IgnorePlatform.do"/>
            <tiles:put name="resParam" value="aiplatform"/>
          </tiles:insert>
        
      </table>
    </td>
  </tr>

  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
   
<!--  TYPE AND HOST PROPERTIES TITLE -->

<c:set var="tmpTitle"> - <fmt:message key="resource.autodiscovery.typeAndNetworkProperties.NewModifiedEtc"/></c:set>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.platform.inventory.NetworkPropertiesTab"/>
  <tiles:put name="subTitle" beanName="tmpTitle"/>
</tiles:insert>
<!--  /  -->

<!--  DISCOVERED IPS FILTERING -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
  	<td class="FilterLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td class="FilterLabelText" nowrap align="right">View:</td>
    <td class="FilterLabelText" width="100%">
        <html:select property="ipsStatusFilter" styleClass="FilterFormText" onchange="goToSelectLocation(this, 'ipsStatusFilter', '${ipsStatusFilterAction}');">
            <html:option value="-1" key="resource.autodiscovery.discoveredServers.states.AllStates"/>
            <html:option value="${CONST_ADDED}" key="resource.autodiscovery.discoveredServers.states.New"/>
            <html:option value="${CONST_CHANGED}" key="resource.autodiscovery.discoveredServers.states.Modified"/>
            <html:option value="${CONST_UNCHANGED}" key="resource.autodiscovery.discoveredServers.states.Unchanged"/>
        </html:select>
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
          <tiles:insert page="/resource/platform/autodiscovery/ViewResourceActionDetail.jsp">
            <tiles:put name="aiResource" beanName="aiResource"/>
            <tiles:put name="resourceParam" beanName="resourceParam"/>
            <tiles:put name="aiResourceActionUrl" value="/resource/platform/autodiscovery/IgnoreIps.do"/>
          </tiles:insert>
                
      </table>
    </td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</c:forEach>  
</table>
<!--  /  -->

<!--  /  -->
