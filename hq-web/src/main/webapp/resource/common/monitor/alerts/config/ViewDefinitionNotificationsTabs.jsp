<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
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

<tiles:importAttribute name="tabList"/>

<tiles:importAttribute name="viewUsersUrl"/>
<tiles:importAttribute name="viewOthersUrl"/>
<tiles:importAttribute name="viewRolesUrl" ignore="true"/>
<tiles:importAttribute name="viewEscalationUrl"/>
<tiles:importAttribute name="viewSnmpUrl" ignore="true"/>
<tiles:importAttribute name="viewScriptUrl" ignore="true"/>
<tiles:importAttribute name="viewOpenNMSUrl" ignore="true"/>
<tiles:importAttribute name="viewControlUrl" ignore="true"/>

<c:set var="mode" value="${param.mode}"/>
<c:if test="${mode == 'viewDefinition'}">
  <c:set var="mode" value="viewEscalation"/>
</c:if>

<!-- MINI-TABS -->
<table width="100%" border="0" cellspacing="0" cellpadding="0" style="border-bottom: 3px solid #D9D9D9;">
  <tr> 
    <td class="MiniTabEmpty" width="20">&nbsp;</td>

    <td nowrap>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr> 
<c:forEach var="tab" items="${tabList}">
  <c:choose>
    <c:when test="${tab.value == 'Escalation'}">
      <c:set var="tabUrl" value="${viewEscalationUrl}"/>
    </c:when>
    <c:when test="${tab.value == 'Users'}">
      <c:set var="tabUrl" value="${viewUsersUrl}"/>
    </c:when>
    <c:when test="${tab.value == 'Others'}">
      <c:set var="tabUrl" value="${viewOthersUrl}"/>
    </c:when>
    <c:when test="${tab.value == 'Roles'}">
      <c:set var="tabUrl" value="${viewRolesUrl}"/>
    </c:when>
    <c:when test="${tab.value == 'SNMP'}">
      <c:set var="tabUrl" value="${viewSnmpUrl}"/>
    </c:when>
    <c:when test="${tab.value == 'Script'}">
      <c:set var="tabUrl" value="${viewScriptUrl}"/>
    </c:when>
    <c:when test="${tab.value == 'OpenNMS'}">
      <c:set var="tabUrl" value="${viewOpenNMSUrl}"/>
    </c:when>
    <c:when test="${tab.value == 'Control'}">
      <c:set var="tabUrl" value="${viewControlUrl}"/>
    </c:when>
  </c:choose>

  <c:choose>
    <c:when test="${mode == tab.link}">
      <td valign="top" width="15"><html:img page="/images/miniTabs_left_on.gif" width="11" height="19" alt="" border="0"/></td>
      <td class="MiniTabOn" nowrap><fmt:message key="monitoring.events.MiniTabs.${tab.value}"/></td>
      <td valign="top" width="17"><html:img page="/images/miniTabs_right_on.gif" width="11" height="19" alt="" border="0"/></td>
    </c:when>
    <c:when test="${tab.value == 'SNMP' && not snmpEnabled}">
        <!-- Skip SNMP -->
    </c:when>
    <c:when test="${tab.value == 'OpenNMS' && not openNMSEnabled}">
        <!-- Skip OpenNMS -->
    </c:when>
    <c:otherwise>
      <td valign="top" width="15"><html:img page="/images/miniTabs_left_off.gif" width="11" height="19" alt="" border="0"/></td>
      <td class="MiniTabOff" nowrap>
        <html:link href="${tabUrl}"><fmt:message key="monitoring.events.MiniTabs.${tab.value}"/></html:link></td>
      <td valign="top" width="17"><html:img page="/images/miniTabs_right_off.gif" width="11" height="19" alt="" border="0"/></td>
    </c:otherwise>
  </c:choose>
</c:forEach>
        </tr>
      </table>
    </td>

    <td width="100%" class="MiniTabEmpty">&nbsp;</td>
  </tr>
</table>
<!-- / MINI-TABS -->
