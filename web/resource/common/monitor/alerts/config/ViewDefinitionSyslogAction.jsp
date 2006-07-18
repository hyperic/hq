<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
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


<c:if test="${CAM_SYSLOG_ACTIONS_ENABLED}">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.config.props.Syslog.Title"/>
</tiles:insert>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="2" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <c:choose>
  <c:when test="${(! empty syslogActionForm.id) and (syslogActionForm.id > 0)}">
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.Syslog.MetaProject"/>:</td>
    <td width="80%" class="BlockContent"><c:out value="${syslogActionForm.metaProject}"/></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.Syslog.Project"/>:</td>
    <td width="80%" class="BlockContent"><c:out value="${syslogActionForm.project}"/></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.Syslog.Version"/>:</td>
    <td width="80%" class="BlockContent"><c:out value="${syslogActionForm.version}"/></td>
  </tr>
  </c:when>
  <c:otherwise>
  <tr valign="top">
    <td colspan="2" class="BlockContent"><fmt:message key="alert.config.props.Syslog.NoSyslogAction"/></td>
  </tr>
  </c:otherwise>
  </c:choose>
  <tr>
    <td colspan="2" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
</c:if>
