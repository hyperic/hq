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


<!--  GENERAL PROPERTIES TITLE -->
<c:if test="${not onlyAIplatform}">
  <tiles:insert definition=".header.tab">
    <tiles:put name="tabKey" value="resource.autodiscovery.AutoDiscoveryPropertiesTab"/>
  </tiles:insert>
</c:if>
<!--  /  -->

<!--  GENERAL PROPERTIES CONTENTS -->
<c:choose>
  <c:when test="${not onlyAIplatform}">

<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr valign="top">
		<td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="common.label.Name"/></td>
<logic:messagesPresent property="name">
		<td width="30%" class="ErrorField">
            <html:text  size="30" maxlength="40" property="name"/><br>
            <span class="ErrorFieldContent">- <html:errors property="name"/></span>
        </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="name">
		<td width="30%" class="BlockContent">
            <html:text size="30" maxlength="40" property="name"/>
            <br><span class="CaptionText"><fmt:message key="resource.autodiscovery.NameRequiredForScheduledScans"/></span>
        </td>
</logic:messagesNotPresent>
		<td width="20%" class="BlockLabel"><fmt:message key="common.label.Description"/></td>
		<td width="30%" class="BlockContent"><html:textarea cols="35" rows="3" property="description"/></td>
	</tr>
	<tr>
      <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
</table>
  </c:when>
  <c:otherwise>
    <html:hidden property="name"/>
    <html:hidden property="description"/>
  </c:otherwise>
</c:choose>
<!--  /  -->
