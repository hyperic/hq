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

<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="resourceOwner" ignore="true"/>
<tiles:importAttribute name="showLocation" ignore="true"/>

<!--  APPLICATION PROPERTIES TITLE -->
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="resource.application.ApplicationPropertiesTab"/>
</tiles:insertDefinition>
<!--  /  -->


<tiles:insertDefinition name=".portlet.error"/>

<!--  APPLICATION PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="tablebottomline ">
 <tr>
        <td width="20%" class="BlockLabel"><fmt:message key="resource.application.applicationProperties.EngineeringContact"/></td>
        <td width="30%" class="BlockContent"><s:textfield size="30" maxlength="100" name="engContact"  value="%{#attr.ApplicationForm.engContact}" /></td>


        <td width="20%" class="BlockLabel"><fmt:message key="resource.application.applicationProperties.BusinessOwner"/></td>
        <td width="30%" class="BlockContent"><s:textfield size="30" maxlength="100" name="busContact"  value="%{#attr.ApplicationForm.busContact}" /></td>
    </tr>
    <tr>
        <td width="20%" class="BlockLabel"><fmt:message key="resource.application.applicationProperties.ITOperationsContact"/></td>
        <td width="30%" class="BlockContent"><s:textfield size="30" maxlength="100" name="opsContact"  value="%{#attr.ApplicationForm.opsContact}" /></td>
        <td colspan="2" class="BlockContent">&nbsp;
        <c:choose>
          <c:when test="${empty ApplicationForm.resourceTypes}">
			<s:hidden theme="simple" name="resourceType" />
            
          </c:when>
          <c:otherwise>
			<s:hidden theme="simple" name="resourceType" value="%{#attr.ApplicationForm.resourceTypes[0].id}"/>
          </c:otherwise>
        </c:choose>
        </td>
    </tr>
    
</table>

<!--  /  -->
