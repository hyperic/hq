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

<html:form action="/alerts/Alerts">
<input type=hidden name="a" value="<c:out value="${alert.id}"/>"/>
<input type=hidden name="mode" id="mode" value=""/>

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="alert.current.detail.PageTitle"/>
</tiles:insert>

<tiles:insert definition=".events.alert.view.nav" flush="true"/>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<tiles:insert definition=".events.alert.view.properties"/>

&nbsp;<br>
<tiles:insert definition=".events.config.view.conditions">
  <tiles:put name="showValues" value="true"/>
</tiles:insert>

<c:forEach var="action" items="${actionList}">
  &nbsp;<br>
  <tiles:insert beanName="action"/>
</c:forEach>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.common.alert.action.fix.header"/>
</tiles:insert>                                                                                                                           
<table cellpadding="10" cellspacing="0" border="0" width="100%" id="fixedSection">
<tr>
<c:choose>
<c:when test="${not alert.fixed}">
  <td class="BlockContent"  align="middle" valign="top">
    <c:if test="${not empty fixedNote}">
        <span class="BoldText"><fmt:message key="resource.common.alert.previousFix"/></span>
        <c:out value="${fixedNote}"/>
    </c:if>
    </td>
     <td colspan="2" class="BlockContent">
    <span class="BoldText"><fmt:message key="resource.common.alert.fixedNote"/></span><br><html:textarea property="fixedNote" cols="70" rows="5"/>
  </td>
</tr>
<tr>
  <td class="BlockContent" width="40%" align="right">&nbsp;</td>
  <td class="BlockContent" style="padding-top: 6px; padding-bottom: 6px;">
</c:when>
<c:when test="${not empty fixedNote}">
  <td class="BlockContent" align="middle">
  <div style="padding: 4px;"><c:out value="${fixedNote}"/></div>
</c:when>
<c:otherwise>
  <td class="BlockContent" align="middle">
  <div style="padding: 4px;"><fmt:message key="resource.common.alert.beenFixed"/></div>
</c:otherwise>
</c:choose>
<tiles:insert page="/common/components/ActionButton.jsp">
  <tiles:put name="labelKey" value="resource.common.alert.action.fixed.label"/>
  <tiles:put name="buttonHref" value="javascript:document.forms[0].submit();"/>
  <tiles:put name="buttonClick">dojo.byId('mode').setAttribute('value', '<fmt:message key="resource.common.alert.action.fixed.label"/>')</tiles:put>
  <tiles:put name="icon"><html:img page="/images/icon_fixed.gif" alt="Click to mark as Fixed" align="middle"/></tiles:put>
  <tiles:put name="disabled" beanName="alert" beanProperty="fixed"/>
</tiles:insert>
    <c:if test="${not alert.fixed}">
      <td class="BlockContent"  style="width:100%;">
        <fmt:message key="resource.common.alert.clickToFix"/>
      </td>
    </c:if>
  </td>
</tr>
</table>

<tiles:insert definition=".events.alert.view.nav" flush="true"/>

<tiles:insert definition=".page.footer"/>

</html:form>

</c:if>
