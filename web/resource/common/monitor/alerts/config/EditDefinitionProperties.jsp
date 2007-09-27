<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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


<html:form action="/alerts/EditProperties">

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="alert.config.edit.page.PageTitle"/>
</tiles:insert>

<tiles:insert definition=".header.tab">
<tiles:put name="tabKey" value="alert.config.props.PropertiesBox"/>
</tiles:insert>

<html:hidden property="ad"/>
<c:choose>
  <c:when test="${not empty Resource}">
<html:hidden property="eid" value="${Resource.entityId}"/>
  </c:when>
  <c:otherwise>
<html:hidden property="type" value="${ResourceType.appdefType}"/>
<html:hidden property="resourceType" value="${ResourceType.id}"/>
  </c:otherwise>
</c:choose>

<c:if test="${not empty EditAlertDefinitionPropertiesForm.aetid}">
<html:hidden property="aetid" value="${EditAlertDefinitionPropertiesForm.aetid}"/>
</c:if>

<tiles:insert definition=".events.config.properties"/>

<tiles:insert definition=".form.buttons"/>

<tiles:insert definition=".page.footer"/>

</html:form>
