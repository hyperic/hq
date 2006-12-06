<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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


<tiles:importAttribute name="ParentResource" ignore="true"/>

<!--  /  -->


<!--  TYPE AND HOST PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
    <tr>
        <c:choose>
            <c:when test="${empty ParentResource}">
            <td width="20%" class="BlockLabel"><fmt:message key="resource.service.inventory.type.HostServer"/></td>
            <td width="30%" class="BlockContent"><html:link page="/resource/server/Inventory.do?mode=view&type=${Resource.server.entityId.type}&rid=${Resource.server.id}"><c:out value="${Resource.server.name}"/></html:link></td>
            </c:when>
            <c:otherwise>
            <td width="20%" class="BlockLabel"><fmt:message key="resource.server.inventory.type.HostPlatform"/></td>
            <td width="30%" class="BlockContent"><html:link page="/resource/platform/Inventory.do?mode=view&type=${ParentResource.entityId.type}&rid=${ParentResource.id}"><c:out value="${ParentResource.name}"/></html:link></td>
            </c:otherwise>
        </c:choose>
        <td width="50%" class="BlockLabel">&nbsp;</td>
    </tr>
    <tr>
      <td colspan="3" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
</table>

<!--  /  -->
