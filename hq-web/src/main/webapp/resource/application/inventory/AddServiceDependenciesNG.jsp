<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
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


<s:form method="POST" action="executeServiceDependencies">

<tiles:insertDefinition name=".page.title.resource.application">
    <tiles:putAttribute name="titleKey" value="common.title.Edit"/>
    <tiles:putAttribute name="titleName" value="${Resource.name}"/>
	<tiles:putAttribute name="ignoreBreadcrumb" value="false"/>
</tiles:insertDefinition>

<tiles:insertDefinition name=".portlet.error"/>

<tiles:insertDefinition name=".ng.resource.application.inventory.addServiceDependenciesForm">
  <tiles:putAttribute name="resource" value="${Resource}"/>
  <tiles:putAttribute name="availableServices" value="${reqAvailableSvcDeps}"/>
  <tiles:putAttribute name="availableServicesCount" value="${reqNumAvailableSvcDeps}"/>
  <tiles:putAttribute name="pendingServices" value="${reqPendingSvcDeps}"/>
  <tiles:putAttribute name="pendingServicesCount" value="${reqNumPendingSvcDeps}"/>
</tiles:insertDefinition>

<tiles:insertDefinition name=".form.buttons">
  <tiles:putAttribute name="addToList" value="true"/>
  <tiles:putAttribute name="cancelAction"  value="cancelServiceDependencies" />
  <tiles:putAttribute name="resetAction"   value="resetServiceDependencies" />
  <tiles:putAttribute name="addedAction"   value="executeServiceDependencies" />
</tiles:insertDefinition>

<input type="hidden" name="rid" value="<c:out value="${param.rid}"/>"    />
<input type="hidden" name="type" value="<c:out value="${param.type}"/>"     />
<input type="hidden" name="appSvcId" value="<c:out value="${param.appSvcId}"/>"     />

</s:form>


