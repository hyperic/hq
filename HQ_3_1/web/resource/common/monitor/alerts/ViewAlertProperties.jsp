<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
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



<!-- Content Block Title: Properties -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.current.detail.props.Title"/>
</tiles:insert>

<!-- Properties Content -->
<table width="100%" cellpadding="0" cellspacing="0" class="TableBottomLine">
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.Name"/></td>
    <td width="30%" class="BlockContent"><c:out value="${alertDef.name}"/>
    <c:if test="${not empty Resource}"><br>
      <html:link page="/alerts/Config.do?mode=viewDefinition&eid=${Resource.entityId.appdefKey}&ad=${alertDef.id}"><fmt:message key="alert.config.props.PB.ViewDef"/></html:link></c:if></td>
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.PB.Priority"/></td>
    <td width="30%" class="BlockContent" colspan="2">
      <fmt:message key="${'alert.config.props.PB.Priority.'}${alertDef.priority}"/>
    </td>
  </tr>
  <tr valign="top">
    <td class="BlockLabel">&nbsp;<c:if test="${not empty Resource}"><fmt:message key="common.label.Resource"/></c:if></td>
    <td class="BlockContent">
      <c:if test="${not empty Resource}">
        <html:link action="/Resource" paramId="eid" paramName="Resource" paramProperty="entityId"><c:out value="${Resource.name}"/></html:link>
      </c:if>
      &nbsp;</td>
    <td class="BlockLabel"><fmt:message key="alert.current.detail.props.AlertDate"/></td>
    <td class="BlockContent"><hq:dateFormatter time="false" value="${alert.ctime}"/></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><c:if test="${not empty alertDef.description}"><fmt:message key="common.label.Description"/></c:if>&nbsp;</td>
    <td width="30%" class="BlockContent"><c:out value="${alertDef.description}"/>&nbsp;</td>
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.PB.AlertDefinitionActive"/></td>
    <tiles:insert page="/resource/common/monitor/alerts/config/AlertDefinitionActive.jsp">
    <tiles:put name="alertDef" beanName="alertDef"/>
    </tiles:insert>
  </tr>
</table>
