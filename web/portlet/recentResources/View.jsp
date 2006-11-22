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
<tiles:importAttribute name="resourceHealth"/>

<div class="DropDown">
<fmt:message key="dash.home.RecentResources"/>
<c:choose>
  <c:when test="${not empty resourceHealth}">   
  <ul style="list-style-type: none;">
  <c:forEach var="resource" items="${resourceHealth}">
    <li>
            <c:choose>
              <c:when test="${resource.availability == 1}">
                <html:img page="/images/icon_available_green.gif" height="12" width="12" border="0"/> 
              </c:when>
              <c:when test="${resource.availability == -0.01}">
                <html:img page="/images/icon_available_orange.gif" height="12" width="12" border="0"/>
              </c:when>
              <c:when test="${resource.availability <= 0}">
                <html:img page="/images/icon_available_red.gif" height="12" width="12" border="0"/>
              </c:when>
              <c:when test="${resource.availability > 0 && resource.availability < 1}">
                <html:img page="/images/icon_available_yellow.gif" height="12" width="12" border="0"/>
              </c:when>
              <c:otherwise>
                <html:img page="/images/icon_available_error.gif" height="12" width="12" border="0"/>
              </c:otherwise>
            </c:choose>
    <html:link page="/Resource.do?eid=${resource.resourceTypeId}:${resource.resourceId}"><c:out value="${resource.resourceName}"/></html:link>
  </c:forEach>
    </li>
  </ul>
  </c:when>
</c:choose>
</div>
