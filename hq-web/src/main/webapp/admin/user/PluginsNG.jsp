<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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
 
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="admin.home.Plugins" />
  <tiles:putAttribute name="icon"><img src="/images/icon_plugin.gif" alt="Plugins"/></tiles:putAttribute>
</tiles:insertDefinition>


<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine" style="margin-bottom: 24px;">
<c:choose>
<c:when test="${not empty adminAttachments}">
<c:forEach var="attachment" items="${adminAttachments}">
    <tr>
        <td width="20%" class="BlockLabel">&nbsp;</td>
        <td class="BlockContent">
        	<s:a action="/mastheadAttach" >
			<s:param name="typeId" value="%{#attachment.attachment.id}"/>
			${attachment.HTML}</s:a>
        </td>
    </tr>
</c:forEach>
</c:when>
<c:otherwise>
    <tr>
        <td width="20%" class="BlockLabel">&nbsp;</td>
        <td class="BlockContent"><fmt:message key="admin.plugins.NoPluginsAvailable" /></td>
    </tr>
</c:otherwise>
</c:choose>
</table>

<!--  /  -->