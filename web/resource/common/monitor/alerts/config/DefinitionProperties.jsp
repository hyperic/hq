<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="hq" prefix="hq" %>
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


<!-- Properties Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="tableBottomLine">
  <tr>
    <td colspan="4" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr valign="top">
    <%-- name --%>
    <td width="20%" class="BlockLabel">
      <html:img page="/images/icon_required.gif" width="9" height="9"
      border="0"/><fmt:message key="common.label.Name"/>
    </td>
    <logic:messagesPresent property="name">
    <td width="30%" class="ErrorField">
      <html:text size="30" maxlength="255" property="name"/>
      <br><span class="ErrorFieldContent">- <html:errors property="name"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="name">
    <td width="30%" class="BlockContent">
      <html:text size="30" maxlength="255" property="name"/>
    </td>
    </logic:messagesNotPresent>

    <%-- priority --%>
    <td width="20%" class="BlockLabel">
      <html:img page="/images/icon_required.gif" width="9" height="9"
      border="0"/><fmt:message key="alert.config.props.PB.Priority"/>
    </td>
    <logic:messagesPresent property="priority">
    <td width="30%" class="ErrorField">
      <html:select property="priority">
      <hq:optionMessageList property="priorities"
      baseKey="alert.config.props.PB.Priority" filter="true"/>
      </html:select>
      <span class="ErrorFieldContent">- <html:errors property="priority"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="priority">
    <td width="30%" class="BlockContent">
      <html:select property="priority">
      <hq:optionMessageList property="priorities"
      baseKey="alert.config.props.PB.Priority" filter="true"/>
      </html:select>
    </td>
    </logic:messagesNotPresent>
  </tr>
  
  <tr valign="top">
    <%-- description --%>
    <td width="20%" class="BlockLabel">
      <fmt:message key="common.label.Description"/>
    </td>
    <logic:messagesPresent property="description">
    <td width="30%" class="ErrorField">
      <html:textarea cols="40" rows="3" property="description"/>
      <span class="ErrorFieldContent">- <html:errors property="description"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="description">
    <td width="30%" class="BlockContent">
      <html:textarea cols="40" rows="3" property="description"/>
    </td>
    </logic:messagesNotPresent>

    <%-- active --%>
    <td width="20%" class="BlockLabel">
      <html:img page="/images/icon_required.gif" width="9" height="9"
      border="0"/><fmt:message key="alert.config.props.PB.Active"/>
    </td>
    <logic:messagesPresent property="active">
    <td width="30%" class="ErrorField">
      <html:radio property="active" value="true"/>
      <fmt:message key="alert.config.props.PB.ActiveYes"/><br>
      <html:radio property="active" value="false"/>
      <fmt:message key="alert.config.props.PB.ActiveNo"/>
      <span class="ErrorFieldContent">- <html:errors property="active"/></span>
    </td>
    </logic:messagesPresent>
    <logic:messagesNotPresent property="active">
    <td width="30%" class="BlockContent">
      <html:radio property="active" value="true"/>
      <fmt:message key="alert.config.props.PB.ActiveYes"/><br>
      <html:radio property="active" value="false"/>
      <fmt:message key="alert.config.props.PB.ActiveNo"/>
    </td>
    </logic:messagesNotPresent>
  </tr>
  <tr>
    <td colspan="4" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
&nbsp;<br>
