<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic" prefix="logic" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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


<html:form action="/alerts/EditSyslogAction">

<c:if test="${not empty param.aetid}">
<html:hidden property="aetid"/>
</c:if>
<c:if test="${not empty param.eid}">
<html:hidden property="eid"/>
</c:if>
<html:hidden property="ad"/>

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="alert.config.props.Syslog.AddActionTitle"/>
</tiles:insert>

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.config.props.Syslog.Title"/>
</tiles:insert>

<script  src="<html:rewrite page='/js/alertConfigFunctions.js'/>" type="text/javascript"></script>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr valign="top">
    <td width="20%" class="BlockLabel">
      <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
      <fmt:message key="alert.config.props.Syslog.MetaProject"/>:
    </td>
    <logic:messagesPresent property="metaProject">
    <td width="80%" class="ErrorField">
      <html:text size="30" property="metaProject"/>
      <span class="ErrorFieldContent">- <html:errors property="metaProject"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="metaProject">
    <td width="80%" class="BlockContent">
      <html:text size="30" property="metaProject"/>
    </td>
    </logic:messagesNotPresent>
  </tr>
  <tr valign="top">
    <td class="BlockLabel">
      <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
      <fmt:message key="alert.config.props.Syslog.Project"/>:
    </td>
    <logic:messagesPresent property="project">
    <td class="ErrorField">
      <html:text size="30" property="project"/>
      <span class="ErrorFieldContent">- <html:errors property="project"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="project">
    <td class="BlockContent">
      <html:text size="30" property="project"/>
    </td>
    </logic:messagesNotPresent>
  </tr>
  <tr valign="top">
    <td class="BlockLabel">
      <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
      <fmt:message key="alert.config.props.Syslog.Version"/>:
    </td>
    <logic:messagesPresent property="version">
    <td class="ErrorField">
      <html:text size="30" property="version"/>
      <span class="ErrorFieldContent">- <html:errors property="version"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="version">
    <td class="BlockContent">
      <html:text size="30" property="version"/>
    </td>
    </logic:messagesNotPresent>
  </tr>
  <c:if test="${(! empty EditAlertDefinitionSyslogActionForm.id) and (EditAlertDefinitionSyslogActionForm.id > 0)}">
  <tr valign="top">
    <td colspan="2"  class="BlockContent">
      <html:checkbox property="shouldBeRemoved" onclick="javascript:syslogFormEnabledToggle();"/>
      <fmt:message key="alert.config.props.Syslog.Dissociate"/>
    </td>
  </tr>
  </c:if>
  <tr>
    <td colspan="2" class="BlockBottomLine">
      <html:img page="/images/spacer.gif" width="1" height="1" border="0"/>
    </td>
  </tr>
</table>

<tiles:insert definition=".form.buttons"/>

<tiles:insert definition=".page.footer"/>

</html:form>
