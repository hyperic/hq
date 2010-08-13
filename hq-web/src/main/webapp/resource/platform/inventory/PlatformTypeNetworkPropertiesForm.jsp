<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
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
<tiles:importAttribute name="ipCount"/>
<tiles:importAttribute name="formName"/>

<!--  TYPE AND NETWORK PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.platform.inventory.TypeAndNetworkPropertiesTab"/>
</tiles:insert>
<!--  /  -->

<html:hidden property="numIps"/>

<!--  TYPE AND HOST PROPERTIES CONTENTS (OS Type not editable in edit mode-->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
<logic:messagesPresent property="platformType">
<tr>
    <td width="30%" class="ErrorField" colspan="3">
      <span class="ErrorFieldContent">- <html:errors property="platformType"/></span>
      </td>
</tr>
</logic:messagesPresent>
  <tr>
    <td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/> <fmt:message key="resource.platform.inventory.type.MachineTypeLabel"/></td>
<logic:messagesPresent property="resourceType">
    <td width="30%" class="ErrorField">
      <html:select property="resourceType" disabled="${ ! platformOSEditable }">
        <html:option value="-1" key="resource.platform.inventory.type.SelectOption"/>
        <html:optionsCollection property="resourceTypes" value="id" label="name"/>
      </html:select><br>
      <span class="ErrorFieldContent">- <html:errors property="resourceType"/></span>
    </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="resourceType">
    <td width="30%" class="BlockContent">
      <html:select property="resourceType" disabled="${ ! platformOSEditable }">
        <html:option value="-1" key="resource.platform.inventory.type.SelectOption"/>
        <html:optionsCollection property="resourceTypes" value="id" label="name"/>
      </html:select>
    </td>
</logic:messagesNotPresent>
    <td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="resource.platform.inventory.type.FQDNLabel"/></td>
<logic:messagesPresent property="fqdn">
    <td width="30%" class="ErrorField">
      <html:text size="30" maxlength="200" property="fqdn"/><br>
      <span class="ErrorFieldContent">- <html:errors property="fqdn"/></span>
    </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="fqdn">
    <td width="30%" class="BlockContent"><html:text size="30" maxlength="200" property="fqdn"/></td>
</logic:messagesNotPresent>
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
      <html:select property="agentIpPort" value="${usedIpPort}">
        <html:optionsCollection property="agents" value="ipPort" label="ipPort"/>
      </html:select>
    </td>
    </c:otherwise>
    </c:choose>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
<c:if test="${ipCount > 0}">
<c:forEach var="i" varStatus="status" begin="0" end="${ipCount-1}">
  <tr>
    <td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="resource.platform.inventory.type.IPAddressLabel"/></td>
<logic:messagesPresent property="ip[${i}].address">
    <td width="30%" class="ErrorField">
      <html:text size="30" property="ip[${i}].address"/><br>
      <span class="ErrorFieldContent">- <html:errors property="ip[${i}].address"/></span>
    </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="ip[${i}].address">
    <td width="30%" class="BlockContent"><html:text size="30" maxlength="200" property="ip[${i}].address"/></td>
</logic:messagesNotPresent>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.platform.inventory.type.NetmaskLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="30" maxlength="200" property="ip[${i}].netmask"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.platform.inventory.type.MACAddressLabel"/></td>
    <td width="30%" class="BlockContent"><html:text size="30" property="ip[${i}].MACAddress"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">
        <html:hidden property="ip[${i}].id"/>
    </td>
  </tr>
  <c:if test="${ipCount > 1}">
  <tr>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent" colspan="3"><html:link href="javascript:document.${formName}.submit()" onclick="clickRemove('${formName}', ${i})"><fmt:message key="resource.platform.inventory.type.DeleteThisIP"/></html:link></td>
  </tr>
  </c:if>
  <c:if test="${not status.last}">
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  </c:if>
</c:forEach>
</c:if>
  <tr>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent" colspan="3"><html:link href="javascript:document.${formName}.submit()" onclick="clickAdd('${formName}')"><fmt:message key="resource.platform.inventory.type.AddAnotherSet"/></html:link></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->
