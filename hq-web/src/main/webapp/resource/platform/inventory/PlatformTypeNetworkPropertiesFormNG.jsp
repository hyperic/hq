<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

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


<tiles:importAttribute name="platform" ignore="true"/>
<tiles:importAttribute name="ipCount" />
<tiles:importAttribute name="formName"/>
<tiles:importAttribute name="displayErrorLocally" ignore="true"/>
<style>
.BlockContentErrorField {
	background-color:#FFFD99;
}
#e {
	background-color:#FFFD99;
}
</style>

<!--  TYPE AND NETWORK PROPERTIES TITLE -->
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.platform.inventory.TypeAndNetworkPropertiesTab"/>
</tiles:insertDefinition>
<!--  /  -->
<c:if test="${displayErrorLocally}">
<tiles:insertDefinition name=".portlet.confirm"/>
<tiles:insertDefinition name=".portlet.error"/>
</c:if>

<s:hidden theme="simple" name="numIps" value="%{#attr.ipCount}"/>
<!--  TYPE AND HOST PROPERTIES CONTENTS (OS Type not editable in edit mode-->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="20%" class="BlockLabel"><img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/>
 <fmt:message key="resource.platform.inventory.type.MachineTypeLabel"/></td>
    <td width="30%" class="BlockContent">
	       <s:select list="%{#attr.editForm.resourceTypes}"  name="resourceType" value="%{#attr.editForm.resourceType}" headerKey="-1" headerValue="%{getText('resource.platform.inventory.type.SelectOption')}" listKey="id" listValue="name"  errorPosition="bottom" disabled="%{ ! #attr.platformOSEditable }"></s:select>
    </td>
    <td width="20%" class="BlockLabel"><img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/><fmt:message key="resource.platform.inventory.type.FQDNLabel"/></td>
    <td width="30%" class="BlockContent"><s:textfield size="30" maxlength="200" name="fqdn"  value="%{#attr.editForm.fqdn}" errorPosition="bottom"/></td>
  </tr>
  <tr>
    <td class="BlockLabel">&nbsp;</td>
    <td colspan="3" class="BlockContent"><span class="CaptionText"><fmt:message key="resource.platform.inventory.type.Note"/></span></td>
  </tr>
  
<tr>
    <c:choose>
    <c:when test="${agentsCount == 0}">
    <td width="100%" colspan="4"><i><fmt:message key="resource.platform.inventory.configProps.NoAgentsAvailable"/></i></td>
    </c:when>
    <c:otherwise>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.platform.inventory.type.AgentConnectionLabel"/></td>
    <td width="30%" colspan="3" class="BlockContent">
	  <s:select name="agentIpPort" value="%{#attr.editForm.usedIpPort}" list="%{#attr.editForm.agents}" listValue="ipPort" listKey="ipPort">
      </s:select>
	</td>
    </c:otherwise>
    </c:choose>
  </tr>
   <tr>
    <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
<c:if test="${ipCount > 0}">
<c:forEach var="i" varStatus="status" begin="0" end="${ipCount-1}">
  <tr>
	 
    
	<td width="20%" class="BlockLabel"><img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/><fmt:message key="resource.platform.inventory.type.IPAddressLabel"/></td>
	
	<c:set var="curName" scope="request">addresses[${i}]</c:set> 
	<!--%{#attr.curName}-->

	<c:set var="currIp" value="${editForm.ips[i].address}"/> 
	<c:set var="currNetMask" value="${editForm.ips[i].netmask}"/>	
	<!-- insert here some condition about e from request to set td class-->	
	
	<c:if test="${fieldErrors.containsKey(curName)}"> 
		<td width="30%" class="BlockContentErrorField">
	</c:if>
	<c:if test="${!fieldErrors.containsKey(curName)}"> 
		<td width="30%" class="BlockContent">
	</c:if>
	<s:textfield size="30" maxlength="200" name="addresses"   value="%{#attr.currIp}" theme="simple"/>
	<c:if test="${fieldErrors.containsKey(curName)}"> 
		<br/><i><c:out value="${fieldErrors.get(curName).get(0)}" /></i>
	</c:if>			
	</td>
	
    <td width="20%" class="BlockLabel"><fmt:message key="resource.platform.inventory.type.NetmaskLabel"/></td>
    <td width="30%" class="BlockContent"><s:textfield size="30" maxlength="200" name="netmasks" value="%{#attr.currNetMask}" errorPosition="bottom"/></td>
  </tr>
 
  <tr>
  <c:set var="currMacAddress" value="${editForm.ips[i].MACAddress}"/> 
    <td width="20%" class="BlockLabel"><fmt:message key="resource.platform.inventory.type.MACAddressLabel"/></td>
    <td width="30%" class="BlockContent"><s:textfield size="30" name="mACAddresses" value="%{#attr.currMacAddress}"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">
	   <c:set var="currId" value="${editForm.ips[i].id}"/> 
       <s:hidden theme="simple" name="ids" value="%{#attr.currId}"/>
    </td>
  </tr>
  <c:if test="${ipCount > 1}">
  <tr>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent" colspan="3"><a href="javascript:document.${formName}.submit()" onclick="clickRemove('${formName}', ${i})"><fmt:message key="resource.platform.inventory.type.DeleteThisIP"/></:a></td>
  </tr>
  </c:if>
  <c:if test="${not status.last}">
  <tr>
    <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
  </c:if>
</c:forEach>
</c:if>
   <tr>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent" colspan="3"><a href="javascript:document.${formName}.submit()" onclick="clickAdd('${formName}')"><fmt:message key="resource.platform.inventory.type.AddAnotherSet"/></a></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
</table>

