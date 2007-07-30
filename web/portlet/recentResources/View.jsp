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

<div id="recent" style="background-color:#60a5ea;border:1px solid #ffffff;position:absolute;right:0px;width:100%;z-index: 300;margin-top:4px;display:none;">
  <div style="height:100%;width:100%;padding:0px;">
  <ul style="list-style-type: none;width:100%;height:100%;margin:0px auto;padding-left:10px;padding-right:30px;">
<c:choose>
<c:when test="${not empty resources}">
  <c:forEach var="resource" items="${resources}">
    <li style="white-space: nowrap;padding-top:2px;padding-bottom:2px;">
      <html:link page="/Resource.do?eid=${resource.entityId}"><c:out value="${resource.name}"/></html:link>
  </c:forEach>
    </li>
  </c:when>
  <c:otherwise>
    <li style="color:#ffffff;white-space: nowrap;padding-top:2px;padding-bottom:2px;"><fmt:message key="common.label.None"/></li>
  </c:otherwise>
</c:choose>
  </ul>
</div>
</div>
