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


<table width="100%" cellpadding="3" cellspacing="0" border="0">
  <tr>
    <td width="100%">
      <b><fmt:message key="resource.common.monitor.visibility.chart.ResourceAndControlActionKeyLabel"/></b><br>

      <c:forEach var="resource" varStatus="resStatus" items="${checkedResources}">
      <fmt:formatNumber var="imgidx" pattern="00" value="${resStatus.index + 1}"/>
      <p><b><fmt:message key="resource.common.monitor.visibility.chart.ResourceLabel"/></b>
      <html:img page="/images/icon_resource_${imgidx}.gif" width="11" height="11" border="0"/>
      <c:out value="${resource.name}"/><br>
      <c:forEach var="event" varStatus="evStatus" items="${chartLegend[resStatus.index]}">
      <b>(<c:out value="${event.eventID}"/>)</b>
      <c:out value="${event.detail}"/> -
      <hq:dateFormatter value="${event.timestamp}"/><c:if test="${! evStatus.last}">,</c:if>
      </c:forEach></p>
      </c:forEach>
    </td>
  </tr>
</table>
