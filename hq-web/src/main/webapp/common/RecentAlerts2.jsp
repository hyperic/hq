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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<% response.setHeader("Pragma","no-cache");%>
<% response.setHeader("Cache-Control","no-store");%>
<% response.setDateHeader("Expires",-1);%>

<hq:recentAlerts var="recentAlerts" sizeVar="recentAlertsSize" maxAlerts="2"/>
<span id="recentAlertsText">
	<c:choose>
  		<c:when test="${recentAlertsSize > 0}">
    		<ul class="boxy">
      			<c:forEach var="alert" varStatus="status" items="${recentAlerts}">
        			<c:set var="datetime"><hq:dateFormatter value="${alert.ctime}" showDate="true"/></c:set>
        			<c:url var="alertUrl" value="viewAlertAlertPortal.action">
          				<c:param name="mode" value="viewAlert"/>
          				<c:param name="eid" value="${alert.type}:${alert.rid}"/>
          				<c:param name="a" value="${alert.id}"/>
        			</c:url>
        			<li class="MastheadContent">
						<a href="${alertUrl}"><hq:dateFormatter value="${alert.ctime}" showDate="false"/></a>
        				<fmt:message key="common.label.Dash"/>
        				<abbr title="<fmt:message key="common.label.Resource"/> <c:out value="${alert.resourceName};"/> <fmt:message key="common.label.Alert"/> <c:out value="${alert.name}"/>">
        					<c:out value="${alert.name}"/>
        				</abbr>
        			</li>
	      		</c:forEach>
    		</ul>
  		</c:when>
  		<c:otherwise>
    		<span class="MastheadContent" style="color: #FFF;"><fmt:message key="header.NoRecentAlerts"/></span>
  		</c:otherwise>
	</c:choose>
</span>