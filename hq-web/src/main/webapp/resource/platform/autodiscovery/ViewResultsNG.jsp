<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
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

<jsu:script>
	var pageData = new Array();
</jsu:script>
<c:set var="name" value="${AIPlatform.name}"/>
<c:if test="${not empty Resource}">
    <c:set var="name" value="${Resource.name}"/>
</c:if>


<c:set var="selfAction" 
        value="viewResultsPlatformAutoDiscovery.action?mode=results&aiPid=${param.aiPid}&eid=${param.eid}"/>


<tiles:insertDefinition name=".page.title.resource.platform" >
	<tiles:putAttribute name="titleKey" value="resource.autodiscovery.inventory.ViewAutodiscoveryResultsPageTitle"/> 
	<tiles:putAttribute name="titleName" value="${Resource.name}"/> 
</tiles:insertDefinition>

<tiles:insertDefinition name=".portlet.confirm"/>
<tiles:insertDefinition name=".portlet.error"/>

<c:set var="actionUrl"   value="ImportAIResourcesAutoDiscovery"/>

<!-- if ScanState is not set, then the platform has not been imported into appdef. don't
     display the error section. -->    
<c:if test="${not empty ScanState}">
	<tiles:insertTemplate template="/resource/platform/autodiscovery/ViewAutoDiscoveryPropertiesNG.jsp">
		<tiles:putAttribute name="scanstate" value="${ScanState}" />
	</tiles:insertTemplate>
    &nbsp;<br>
	<tiles:insertTemplate template="/resource/platform/autodiscovery/ViewErrorLogNG.jsp">
		<tiles:putAttribute name="scanstate" value="${ScanState}" />
	</tiles:insertTemplate>

    &nbsp;<br>
</c:if>    
<s:form action="saveImportAIResourcesAutoDiscovery" >
	<tiles:insertTemplate template="/resource/platform/autodiscovery/ViewTypeAndNetworkPropertiesNG.jsp">
		<tiles:putAttribute name="selfAction" value="${selfAction}" />
	</tiles:insertTemplate>
&nbsp;<br>
	<tiles:insertTemplate template="/resource/platform/autodiscovery/ListDiscoveredServersNG.jsp">
		<tiles:putAttribute name="selfAction" value="${selfAction}" />
	</tiles:insertTemplate>

&nbsp;<br>
    
<input type="hidden" name="rid" value="<c:out value="${param.rid}"/>"    />
<input type="hidden" name="type" value="<c:out value="${param.type}"/>"     />
<input type="hidden" name="aiPid" value="<c:out value="${param.aiPid}"/>"     />
	<tiles:insertDefinition name=".form.buttons" >
		<tiles:putAttribute name="cancelAction"  value="cancel${actionUrl}" />
		<tiles:putAttribute name="noReset"  value="true" />
	</tiles:insertDefinition>
</s:form>    

