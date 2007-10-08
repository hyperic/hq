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

<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="display" prefix="display" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<tiles:importAttribute name="resources"/>

<c:choose>
<c:when test="${not empty resources}">
  <c:forEach var="resource" items="${resources}">
    <div dojoType="MenuItem2" caption="<c:out value="${resource.name}"/>" onClick="location.href='/Resource.do?eid=<c:out value="${resource.entityId}"/>'"></div>



      <!--<html:link page="/Resource.do?eid=${resource.entityId}"><c:out value="${resource.name}"/></html:link> -->
  </c:forEach>

  </c:when>
  <c:otherwise>
      <div dojoType="MenuItem2" caption="<fmt:message key="common.label.None"/>"></div>
  </c:otherwise>
</c:choose>
