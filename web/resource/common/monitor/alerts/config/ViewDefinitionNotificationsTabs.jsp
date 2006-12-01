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


<tiles:importAttribute name="viewUsersUrl"/>
<tiles:importAttribute name="viewOthersUrl"/>
<tiles:importAttribute name="viewEscalationUrl"/>

<!-- MINI-TABS -->
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td class="MiniTabEmpty"><html:img page="/images/spacer.gif"
      width="20" height="1" alt="" border="0"/>
    </td>

    <td nowrap>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr> 
          <c:choose>
          <c:when test="${param.mode == 'viewUsers' || param.mode == 'viewDefinition'}">
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_on.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOn" nowrap><fmt:message key="monitoring.events.MiniTabs.CAMusers"/></td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_on.gif" width="11" height="19" alt="" border="0"/></td>
          </c:when>
          <c:otherwise>
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_off.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOff" nowrap><html:link
              href="${viewUsersUrl}"><fmt:message
              key="monitoring.events.MiniTabs.CAMusers"/></html:link></td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_off.gif" width="11" height="19" alt="" border="0"/></td>
          </c:otherwise>
          </c:choose>
        </tr>
      </table>
    </td>
    
    <td nowrap>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr> 
          <c:choose>
          <c:when test="${param.mode == 'viewOthers'}">
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_on.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOn" nowrap><fmt:message key="monitoring.events.MiniTabs.OR"/></td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_on.gif" width="11" height="19" alt="" border="0"/></td>
          </c:when>
          <c:otherwise>
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_off.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOff" nowrap><html:link
              href="${viewOthersUrl}"><fmt:message
              key="monitoring.events.MiniTabs.OR"/></html:link>
            </td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_off.gif" width="11" height="19" alt="" border="0"/></td>
          </c:otherwise>
          </c:choose>
        </tr>
      </table>
    </td>

    <td nowrap>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr> 
          <c:choose>
          <c:when test="${param.mode == 'viewEscalation'}">
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_on.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOn" nowrap><fmt:message key="monitoring.events.MiniTabs.Escalation"/></td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_on.gif" width="11" height="19" alt="" border="0"/></td>
          </c:when>
          <c:otherwise>
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_off.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOff" nowrap><html:link
              href="${viewEscalationUrl}"><fmt:message
              key="monitoring.events.MiniTabs.Escalation"/></html:link>
            </td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_off.gif" width="11" height="19" alt="" border="0"/></td>
          </c:otherwise>
          </c:choose>
        </tr>
      </table>
    </td>

    <td width="100%" class="MiniTabEmpty"><html:img
      page="/images/spacer.gif" width="1" height="1" alt=""
      border="0"/>
    </td>
  </tr>
  
  <tr> 
    <td colspan="6" width="100%" class="SubTabCell"><html:img
      page="/images/spacer.gif" width="1" height="3" alt=""
      border="0"/>
    </td>
  </tr>
</table>
<!-- / MINI-TABS -->
