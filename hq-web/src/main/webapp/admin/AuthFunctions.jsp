<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
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


<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.home.AuthAuthZTab"/>
  <tiles:put name="icon"><html:img page="/images/group_key.gif" alt=""/></tiles:put>
</tiles:insert>

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="TableBottomLine" style="margin-bottom: 24px;">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.home.Users"/></td>
    <td width="30%" class="BlockContent">
    	<html:link action="/admin/user/UserAdmin">
    		<html:param name="mode" value="list"/>
    		<fmt:message key="admin.home.ListUsers"/>
    	</html:link>
    </td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.home.Roles"/></td>
    <td width="30%" class="BlockContent" rowspan="2" valign="top">
      <span class="italicInfo">
      <fmt:message key="feature.available.in.EE">
        <fmt:param><fmt:message key="header.roles"/></fmt:param>
        <fmt:param value="http://support.hyperic.com/confluence/display/EVO/ui-Admin.Role.List"/>
      </fmt:message>
      </span>
    </td>
  </tr>
  <tr>
    <td class="BlockLabel">&nbsp;</td>
    <td class="BlockContent" colspan="2">
    <c:choose>
    <c:when test="${useroperations['createSubject']}">
    	<html:link action="/admin/user/UserAdmin">
    		<html:param name="mode" value="new"/>
    		<fmt:message key="admin.home.NewUser"/>
    	</html:link>
    </c:when>
    <c:otherwise>
    &nbsp;
    </c:otherwise>
    </c:choose>
    </td>
  </tr>
</table>
<!--  /  -->

<!--  some empty space -->
<br>
<br>

