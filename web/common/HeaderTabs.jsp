<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2007], Hyperic, Inc.
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
<c:if test="${not empty mastheadAttachments}">
  <c:forEach var="attachment" items="${mastheadAttachments}">
      <div dojoType="MenuItem2" caption="Audit Center" onClick="location.href='<html:rewrite page="/mastheadAttach.do?id=10002"/>'"></div>
      <!--<html:link action="/mastheadAttach" paramId="id" paramName="attachment" paramProperty="id"><c:out value="${attachment.view.description}"/></html:link> -->
  </c:forEach>
</c:if>
<!--
<td class="navText" nowrap onmouseover="this.style.backgroundColor='#60a5ea';" onmouseout="this.style.backgroundColor='#336699';">
  <a href="." onclick="toggleMenu('reportcenter');return false;" style="color: #DDD"><fmt:message key="reporting.reporting.ReportCenterTitle"/></a>
  <div style="clear: both;"></div>
  <div id="reportcenter" style="background-color:#60a5ea;border:1px solid #ffffff;position:absolute;right:0px;width:100%;z-index: 300;margin-top:4px;display:none;">
    <div class="italicInfo" style="padding: 6px; text-align: center;" onclick="toggleMenu('reportcenter')">
      <fmt:message key="feature.available.in.EE">
        <fmt:param><fmt:message key="reporting.reporting.ReportCenterTitle"/></fmt:param>
        <fmt:param value="http://support.hyperic.com/confluence/display/DOC/ui-Report.Center"/>
      </fmt:message>
    </div>
  </div>
</td>
-->