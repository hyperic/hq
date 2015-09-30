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


<tiles:insertDefinition name=".page.title.resource.platform" >
	<tiles:putAttribute name="titleName" value="${Resource.name}"/> 
</tiles:insertDefinition>

<c:set var="selfAction"  value="newAutoDiscoveryPlatformAutoDiscovery.action?mode=new"/>

<c:set var="actionUrl"   value="NewAutoDiscovery.action"/>
<c:set var="formName"   value="PlatformAutoDiscoveryForm"/>
        

<s:form name="%{#attr.formName}" action="save%{#attr.actionUrl}" method="POST">
<s:hidden theme="simple" name="rid"/>
<s:hidden theme="simple" name="type"/>
    
<tiles:insertDefinition name=".portlet.confirm"/>
<tiles:insertDefinition name=".portlet.error"/>

<tiles:insertTemplate template="/resource/platform/autodiscovery/HeaderMessageNG.jsp"/>

<!-- FORM BUTTONS -->
<tiles:insertDefinition name=".form.buttons" >
	<tiles:putAttribute name="cancelAction"  value="cancel%{#attr.actionUrl}" />
	<tiles:putAttribute name="resetAction"  value="reset%{#attr.actionUrl}" />
</tiles:insertDefinition>
<br>

<!-- SCAN PROPERTIES -->
<tiles:insertDefinition name=".ng.resource.platform.autodiscovery.scanProperties" >
	<tiles:putAttribute name="isNewScan" value="true"/> 
	<tiles:putAttribute name="selfAction" value="${selfAction}"/> 
	<tiles:putAttribute name="actionUrl" value="${selfAction}"/> 
	<tiles:putAttribute name="formName" value="${formName}"/> 
</tiles:insertDefinition>

&nbsp;<br>
	
<!-- FORM BUTTONS -->
<tiles:insertDefinition name=".form.buttons" >
	<tiles:putAttribute name="cancelAction"  value="cancel%{#attr.actionUrl}" />
	<tiles:putAttribute name="resetAction"  value="reset%{#attr.actionUrl}" />
</tiles:insertDefinition>

</s:form>



