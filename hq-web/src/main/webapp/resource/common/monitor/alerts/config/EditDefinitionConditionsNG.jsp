<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2010], VMware, Inc.
  This file is part of Hyperic.
  
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

<s:form action="saveEditAlertDefinitionConditionsAction.action" name="EditAlertDefinitionConditionsForm" >

<tiles:insertDefinition name=".page.title.events">
  <tiles:putAttribute name="titleKey" value="alert.config.edit.page.Cond.PageTitle"/>
</tiles:insertDefinition>

<s:hidden theme="simple" id="ad" name="ad" value="%{#attr.ad}"/>
<c:choose>
  <c:when test="${not empty Resource}">
<s:hidden theme="simple" id="eid" name="eid" value="%{#attr.Resource.entityId}"/>

  </c:when>
  <c:otherwise>
<s:hidden theme="simple" id="aetid" name="aetid" value="%{#attr.ResourceType.appdefTypeKey}"/>
  </c:otherwise>
</c:choose>

<s:if test="fieldErrors.containsKey('editAlertDefinitionError')">
  <div class="ErrorField"><s:fielderror fieldName="editAlertDefinitionError"/></div>
</s:if>

<tiles:insertDefinition name=".events.config.conditions">
  <tiles:putAttribute name="formName" value="EditAlertDefinitionConditionsForm"/>
  <c:if test="${defForm.whenEnabled == enableTimePeriod}">
    <tiles:putAttribute name="showDuration" value="true"/>
  </c:if>
</tiles:insertDefinition>

<tiles:insertDefinition name=".form.buttons">
	<tiles:putAttribute name="cancelAction"  value="cancelEditAlertDefinitionConditionsAction" />
	<tiles:putAttribute name="resetAction"  value="resetEditAlertDefinitionConditionsAction" />
</tiles:insertDefinition>

<tiles:insertDefinition name=".page.footer"/>

</s:form>
