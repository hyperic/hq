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

<tiles:importAttribute name="labelKey"/>
<tiles:importAttribute name="buttonHref"/>
<tiles:importAttribute name="buttonClick"/>
<tiles:importAttribute name="icon" ignore="true"/>
<tiles:importAttribute name="disabled" ignore="true"/>

<table cellspacing="0" cellpadding="0">
<tr><td>
<html:img page="/images/button_left.gif"/>
</td>
<td class="Button" valign="middle" style="background-image: url(<html:rewrite page="/images/button_middle.gif"/>);">
<c:choose>
<c:when test="${disabled}">
  <span class="InactiveText"><fmt:message key="${labelKey}"/></span>
</c:when>
<c:otherwise>
  <a href="<c:out value="${buttonHref}" escapeXml="false"/>" onclick="<c:out value="${buttonClick}" escapeXml="false"/>"><fmt:message key="${labelKey}"/></a>
</c:otherwise>
</c:choose>
</td>

<c:if test="${not empty icon}">
<td class="Button" valign="middle" style="background-image: url(<html:rewrite page="/images/button_middle.gif"/>); padding-left: 3px;">

<c:if test="${disabled}">
  <span style="filter: alpha(opacity=50); opacity: 0.5;">
</c:if>
    <c:out value="${icon}" escapeXml="false"/>
<c:if test="${disabled}">
  </span>
</c:if>

</td>
</c:if>

<td>
<html:img page="/images/button_right.gif"/>
</td></tr>
</table>

