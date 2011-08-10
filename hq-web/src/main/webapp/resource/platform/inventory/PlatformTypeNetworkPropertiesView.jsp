<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
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


<!-- XXX: delete this set and add another set links -->

<tiles:importAttribute name="platform" ignore="true"/>
<tiles:importAttribute name="agent" ignore="true"/>

<c:url var="editUrl" value="/resource/platform/Inventory.do">
	<c:param name="mode" value="editType"/>
	<c:param name="rid" value="${platform.id}"/>
	<c:param name="type" value="${platform.entityId.type}"/>
</c:url>

<!--  TYPE AND HOST PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockBg">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.platform.inventory.type.MachineTypeLabel"/></td>
    <td width="30%" class="BlockContent">
      <c:out value="${platform.platformType.name}"/>
    </td>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.platform.inventory.type.FQDNLabel"/></td>
    <td width="30%" class="BlockContent"><c:out value="${platform.fqdn}"/></td>
  </tr>
  <tr>
    <c:choose>
    <c:when test="${agent == null}">
    <td width="100%" colspan="4"><i><fmt:message key="resource.platform.inventory.configProps.NoAgentConnection"/></i></td>
    </c:when>
    <c:otherwise>
    <td width="30%" class="BlockLabel"><fmt:message key="resource.platform.inventory.type.AgentConnectionLabel"/>
    <td width="30%" class="BlockContent"><c:out value="${agent.address}"/>:<c:out value="${agent.port}"/></td>
    </c:otherwise>
    </c:choose>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
<c:forEach var="ip" items="${platform.ipValues}">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.platform.inventory.type.IPAddressLabel"/></td>
    <td width="30%" class="BlockContent"><c:out value="${ip.address}"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.platform.inventory.type.NetmaskLabel"/></td>
    <td width="30%" class="BlockContent"><c:out value="${ip.netmask}"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.platform.inventory.type.MACAddressLabel"/></td>
    <td width="30%" class="BlockContent"><c:out value="${ip.MACAddress}"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</c:forEach>
</table>
<!--  /  -->

<c:if test="${useroperations['modifyPlatform']}">
<tiles:insert definition=".toolbar.edit">
  <tiles:put name="editUrl" beanName="editUrl"/>
</tiles:insert>
</c:if>
