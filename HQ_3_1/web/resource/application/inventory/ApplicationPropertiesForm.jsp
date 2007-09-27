<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="resourceOwner" ignore="true"/>
<tiles:importAttribute name="showLocation" ignore="true"/>

<!--  APPLICATION PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.application.ApplicationPropertiesTab"/>
</tiles:insert>
<!--  /  -->

<!--  APPLICATION PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <tr>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.application.applicationProperties.EngineeringContact"/></td>
		<td width="30%" class="BlockContent"><html:text size="30" maxlength="100" property="engContact"/></td>


		<td width="20%" class="BlockLabel"><fmt:message key="resource.application.applicationProperties.BusinessOwner"/></td>
		<td width="30%" class="BlockContent"><html:text size="30" maxlength="100" property="busContact"/></td>
	</tr>
	<tr>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.application.applicationProperties.ITOperationsContact"/></td>
		<td width="30%" class="BlockContent"><html:text size="30" maxlength="100" property="opsContact"/></td>
        <td colspan="2" class="BlockContent">&nbsp;
        <c:choose>
          <c:when test="${empty ApplicationForm.resourceTypes}">
            <html:hidden property="resourceType"/>
          </c:when>
          <c:otherwise>
            <html:hidden property="resourceType"
                         value="${ApplicationForm.resourceTypes[0].id}"/>
          </c:otherwise>
        </c:choose>
        </td>

  <!-- Resource type does nothing at the moment
  <td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="resource.application.applicationProperties.ApplicationType"/></td>


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
  -->

	</tr>
	
	<tr>
      <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
</table>
<!--  /  -->
