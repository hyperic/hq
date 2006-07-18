<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<script type="text/javascript">
var pageData = new Array();
</script>

<c:set var="name" value="${AIPlatform.name}"/>
<c:if test="${not empty Resource}">
    <c:set var="name" value="${Resource.name}"/>
</c:if>


<c:set var="selfAction" 
        value="/resource/platform/AutoDiscovery.do?mode=results&aiPid=${param.aiPid}"/>

<tiles:insert definition=".page.title.resource.platform">
  <tiles:put name="titleKey" value="resource.autodiscovery.inventory.ViewAutodiscoveryResultsPageTitle"/>
  <tiles:put name="titleName" beanName="name" />
</tiles:insert>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<!-- if ScanState is not set, then the platform has not been imported into appdef. don't
     display the error section. -->    
<c:if test="${not empty ScanState}">
    <tiles:insert page="/resource/platform/autodiscovery/ViewAutoDiscoveryProperties.jsp">
      <tiles:put name="scanstate" beanName="ScanState" />
    </tiles:insert>
    &nbsp;<br>

    <tiles:insert page="/resource/platform/autodiscovery/ViewErrorLog.jsp">
      <tiles:put name="scanstate" beanName="ScanState" />
    </tiles:insert>
    &nbsp;<br>
</c:if>    
<html:form action="/resource/platform/autodiscovery/ImportAIResources">
<tiles:insert page="/resource/platform/autodiscovery/ViewTypeAndNetworkProperties.jsp">
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>
&nbsp;<br>

<tiles:insert page="/resource/platform/autodiscovery/ListDiscoveredServers.jsp">
  <tiles:put name="selfAction" beanName="selfAction"/>
</tiles:insert>
&nbsp;<br>
    
<html:hidden property="rid"/>
<html:hidden property="type"/>
<html:hidden property="aiPid"/>
    
    <tiles:insert definition=".form.buttons">
        <tiles:put name="noReset" value="true"/>
    </tiles:insert>
</html:form>    

<tiles:insert definition=".page.footer"/>

