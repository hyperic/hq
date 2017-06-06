<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

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


<tiles:importAttribute name="mode" ignore="true"/>
<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="checkboxes" ignore="true"/>

<hq:constant symbol="ERR_PLATFORM_HEALTH_ATTR" var="HostErr" />
<c:set var="tabKey" value="resource.server.monitor.visibility.HostPlatformTab"/>
<c:set var="hostResourcesHealthKey" value="resource.common.monitor.visibility.PlatformTH"/>

<c:choose>
<c:when test="${not empty requestScope[HostErr]}">
  <c:set var="errKey" value="${requestScope[HostErr]}" />
  <tiles:insertDefinition name=".resource.common.monitor.visibility.hostResourcesCurrentHealth">
    <tiles:putAttribute name="mode" value="${mode}"/>
    <tiles:putAttribute name="errKey" value="${errKey}"/>
    <tiles:putAttribute name="tabKey" value="${tabKey}" />
    <tiles:putAttribute name="hostResourcesHealthKey" value="${hostResourcesHealthKey}" />
    <tiles:putAttribute name="checkboxes" value="${checkboxes}" />
  </tiles:insertDefinition>
</c:when>
<c:otherwise>
  <tiles:insertDefinition name=".resource.common.monitor.visibility.hostResourcesCurrentHealth">
    <tiles:putAttribute name="summaries" value="${summaries}"/>
    <tiles:putAttribute name="mode" value="${mode}"/>
    <tiles:putAttribute name="tabKey" value="${tabKey}" />
    <tiles:putAttribute name="hostResourcesHealthKey" value="${hostResourcesHealthKey}" />
    <tiles:putAttribute name="checkboxes" value="${checkboxes}" />
  </tiles:insertDefinition>
</c:otherwise>
</c:choose>
