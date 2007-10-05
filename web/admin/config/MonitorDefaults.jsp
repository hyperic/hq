<%@ page language="java" %>
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
  
  Copyright (C) [2004-2007], Hyperic, Inc.
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

<tiles:importAttribute name="message"/>

<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>
<div class="ListHeaderInactive" style="border: 1px solid rgb(213, 216, 222); margin-bottom: 10px;padding: 3px;">
  <c:out value="${message}" />
</div>

<div dojoType="TreeSelector" widgetId="tSelector" eventNames="select:nodeSelected" ></div>

<div dojoType="Tree" id="TypesTree" selector="tSelector">
  <div dojoType="TreeNode" title="<fmt:message key="resource.hub.PlatformTypeTH"/>s">
    <c:forEach var="entry" varStatus="status" items="${platformTypes}">
      <div dojoType="TreeNode" title="<c:out value="${entry.name}"/>" id="<c:out value="1:${entry.id}"/>"></div>
    </c:forEach>
  </div>
  <div dojoType="TreeNode" title="<fmt:message key="resource.hub.PlatformServiceTypeTH"/>s">
 	<c:forEach var="entry" varStatus="psStatus" items="${platformServiceTypes}">
      <div dojoType="TreeNode" title="<c:out value="${entry.name}"/>" id="<c:out value="3:${entry.id}"/>"></div>
    </c:forEach>
  </div>
  <div dojoType="TreeNode" title="<fmt:message key="resource.hub.ServerTypeTH"/>s">
     <c:forEach var="entry" varStatus="status" items="${serverTypes}">
      <c:set var="server" value="${entry.key}"/>
      <c:set var="services" value="${entry.value}"/>
      <div dojoType="TreeNode" title="<c:out value="${server.name}"/>" id="<c:out value="2:${server.id}"/>">
 	    <c:forEach var="serviceType" varStatus="ssStatus" items="${services}">
          <div dojoType="TreeNode" title="<c:out value="${serviceType.name}"/>" id="<c:out value="3:${server.id}"/>"></div>
        </c:forEach>
      </div>
     </c:forEach>
  </div>
</div>

<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>
