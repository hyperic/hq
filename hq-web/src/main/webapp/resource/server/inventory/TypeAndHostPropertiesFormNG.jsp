<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
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


<!--  GENERAL PROPERTIES TITLE -->
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.server.inventory.TypeAndHostPropertiesTab"/>
</tiles:insertDefinition>



<table width="100%" cellpadding="0" cellspacing="0" border="0">

<!--  TYPE AND HOST PROPERTIES CONTENTS -->
	<tr>
		<td width="20%" class="BlockLabel"><img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/><fmt:message key="resource.server.inventory.type.Type"/></td>
	<td width="30%" class="BlockContent">

	   <s:select name="resourceType" id="resourceType" value="%{#attr.newForm.resourceType}" headerKey="-1" headerValue="%{getText('resource.platform.inventory.type.SelectOption')}" list="%{#attr.newForm.resourceTypes}" listValue="name" listKey="id" errorPosition="bottom">
      </s:select>
	   
    </td>

		<td width="20%" class="BlockLabel"><fmt:message key="resource.server.inventory.type.HostPlatform"/></td>
		<td width="30%" class="BlockContent"><c:out value="${ParentResource.name}"/></td>
	</tr>
    <!-- / -->
    
    <!-- Server Properties -->
	<tr>
		<td width="20%" class="BlockLabel"><img src='<s:url value="/images/icon_required.gif"/>' width="9" height="9" border="0"/><fmt:message key="resource.server.inventory.type.InstallPath"/></td>
	<s:if test="%{fieldErrors.containsKey('installPath')}"><td width="30%" class="ErrorField" colspan="3"></s:if> 
	 <s:else><td width="30%" class="BlockContent" colspan="3"></s:else>
 
	<s:textfield theme="simple" name="installPath" size="90" errorPosition="bottom" maxlength="300" />
    <s:if test="%{fieldErrors.containsKey('installPath')}">
		<span class="ErrorFieldContent">- &nbsp;<hq:extractError errorFieldName="installPath" /></span>
    </s:if>
    <s:else>
      <span class="CaptionText"><br><fmt:message key="resource.server.inventory.type.EnterTheFullEtc"/></span>
	 </s:else>
    </td>

	</tr>
	<!-- / -->
	<tr>
      <td colspan="4" class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
    </tr>
</table>
<!--  /  -->
