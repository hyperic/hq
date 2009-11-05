<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
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


<!--  GENERAL PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.server.inventory.TypeAndHostPropertiesTab"/>
</tiles:insert>
<!--  /  -->


<!--  TYPE AND HOST PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
<!--  TYPE AND HOST PROPERTIES CONTENTS -->
	<tr>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.service.inventory.type.Type"/></td>
<logic:messagesPresent property="resourceType">
    <td width="30%" class="ErrorField">
      <html:select property="resourceType">
        <html:option value="" key="resource.server.inventory.type.SelectOption"/>
        <html:optionsCollection property="resourceTypes" value="id" label="name"/>
      </html:select><br>
      <span class="ErrorFieldContent">- <html:errors property="resourceType"/></span>
    </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="resourceType">
    <td width="30%" class="BlockContent">
      <html:select property="resourceType">
        <html:option value="" key="resource.server.inventory.type.SelectOption"/>
        <html:optionsCollection property="resourceTypes" value="id" label="name"/>
      </html:select>
    </td>
</logic:messagesNotPresent>
		<td width="20%" class="BlockLabel">&nbsp;
        <c:if test="${not empty ParentResource.name}">
          <fmt:message key="resource.service.inventory.type.HostServer"/>
        </c:if>
        </td>
		<td width="30%" class="BlockContent"><c:out value="${ParentResource.name}"/></td>
	</tr>
	<tr>
      <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
</table>
<!--  /  -->
