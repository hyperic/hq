<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
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

 <% 
	String agentType = request.getHeader("User-Agent");
	boolean iemode=false;
	if (agentType.contains("MSIE")) {
		iemode = true;
	}
 %>

<% if (!iemode) { %>
<s:if test="hasActionMessages()">
<table width="100%" cellpadding="0" cellspacing="0" border="0" id="confirm">
  <tr>
    <td class="ConfirmationBlock" width="100%" id="message">
		<s:actionmessage/>
    </td>
  </tr>
</table>
</s:if>
<% } else { %>
<table width="100%" cellpadding="0" cellspacing="0" border="0">
<s:if test="hasActionMessages()">
		<s:iterator value="actionMessages" var="it">
			<s:iterator value="it">
			  <tr>
				<td class="ConfirmationBlock"><img src='<s:url value="/images/tt_check.gif" />'  width="9" height="9" alt="" border="0"/></td>
				<td class="ConfirmationBlock" width="100%"><s:property /></td>
			  </tr>
		  </s:iterator>
		</s:iterator>
</s:if>
</table>

<% } %>

