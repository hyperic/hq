<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

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


<tiles:importAttribute name="resource"/>
<tiles:importAttribute name="deleteMessage"/>

<hq:constant
    classname="org.hyperic.hq.authz.shared.AuthzConstants" 
    symbol="platformPrototypeVmwareVsphereVm" var="platformPrototypeVmwareVsphereVm" />

<hq:constant
    classname="org.hyperic.hq.authz.shared.AuthzConstants" 
    symbol="platformPrototypeVmwareVsphereHost" var="platformPrototypeVmwareVsphereHost" />

<hq:constant
    classname="org.hyperic.hq.authz.shared.AuthzConstants" 
    symbol="serverPrototypeVmwareVcenter" var="serverPrototypeVmwareVcenter" />

<c:choose>
	<c:when test="${(resource.entityId.platform && (resource.platformType.name == platformPrototypeVmwareVsphereVm || 
	                                                resource.platformType.name == platformPrototypeVmwareVsphereHost)) ||
	                (resource.entityId.server && resource.serverType.name == serverPrototypeVmwareVcenter)}">
		<c:set var="userMsg">
			<fmt:message key="delete.vsphere.resource" />
		</c:set>
	</c:when>
	<c:otherwise>
		<c:set var="userMsg">
			<fmt:message key="delete.resource" />
		</c:set>
	</c:otherwise>
</c:choose>
<html:link page="javascript:hyperic.utils.deleteResource('${resource.entityId.type}:${resource.id}', '${userMsg}');">
	<c:out value="${deleteMessage}"/>
</html:link>
<html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/>