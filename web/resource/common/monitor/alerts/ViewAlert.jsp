<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
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

<tiles:importAttribute name="actionList"/>

<%-- Don't insert the sub-tiles if there is no alert and no alertDef. --%>
<c:if test="${not empty alert and not empty alertDef}">

<form name="AlertForm" action="<html:rewrite action="/alerts/Alerts"/>">
<input type=hidden name="a" value="<c:out value="${a}"/>"/>
<input type=hidden name="mode" id="mode" value=""/>

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="alert.current.detail.PageTitle"/>
</tiles:insert>

<tiles:insert definition=".events.alert.view.nav" flush="true"/>

<tiles:insert definition=".events.alert.view.properties"/>

&nbsp;<br>
<tiles:insert definition=".events.config.view.conditions">
  <tiles:put name="showValues" value="true"/>
</tiles:insert>

<c:forEach var="action" items="${actionList}">
  &nbsp;<br>
  <tiles:insert beanName="action"/>
</c:forEach>

<table cellpadding="5" cellspacing="0" border="0" class="BlockContent">
<tr>
  <td width="50%">&nbsp;</td>
  <td>
<tiles:insert page="/common/components/ActionButton.jsp">
  <tiles:put name="labelKey" value="resource.common.alert.action.fixed.label"/>
  <tiles:put name="buttonHref" value="javascript:document.forms[0].submit();"/>
  <tiles:put name="buttonClick">$('mode').setAttribute('value', '<fmt:message key="resource.common.alert.action.fixed.label"/>')</tiles:put>
  <tiles:put name="disabled" beanName="alert" beanProperty="fixed"/>
</tiles:insert>
  </td>
  <td width="50%">&nbsp;</td>
</tr>
</table>
<br/>

<tiles:insert definition=".events.alert.view.nav" flush="true"/>

<tiles:insert definition=".page.footer"/>

</form>

</c:if>
