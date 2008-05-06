<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2008], Hyperic, Inc.
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
<%@ taglib uri="hq" prefix="hq" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<% response.setHeader("Pragma","no-cache");%>
<% response.setHeader("Cache-Control","no-store");%>
<% response.setDateHeader("Expires",-1);%>
<hq:recentAlerts var="recentAlerts" sizeVar="recentAlertsSize" maxAlerts="2"/>
  <span id="recentAlertsText">
<c:choose>
  <c:when test="${recentAlertsSize > 0}">
    <ul class="boxy">
      <c:forEach var="alert" varStatus="status" items="${recentAlerts}">
        <c:url var="alertUrl" value="/alerts/Alerts.do">
          <c:param name="mode" value="viewAlert"/>
        </c:url>
        <li class="MastheadContent" title="<fmt:message key="common.label.Resource"/> <c:out value="${alert.resourceName}"/>"><html:link href="${alertUrl}&amp;eid=${alert.type}:${alert.rid}&amp;a=${alert.id}" styleClass="MastheadLink"><hq:dateFormatter value="${alert.ctime}"/></html:link>
        <fmt:message key="common.label.Dash"/>
        <%--<c:out value="${alert.resourceName}"/><fmt:message key="common.label.Colon"/>--%>
        <c:out value="${alert.name}"/></li>
      </c:forEach>
    </ul>
  </c:when>
  <c:otherwise>
    <span class="MastheadContent" style="color: #FFF;"><fmt:message key="header.NoRecentAlerts"/></span>
  </c:otherwise>
</c:choose>
  </span>
