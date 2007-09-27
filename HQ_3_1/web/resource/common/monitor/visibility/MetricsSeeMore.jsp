<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
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


<tiles:importAttribute name="widgetInstanceName"/>
<tiles:importAttribute name="useConfigureButton" ignore="true"/>
<tiles:importAttribute name="childResourceType" ignore="true"/>
<tiles:importAttribute name="ctype" ignore="true"/>

<c:if test="${empty useConfigureButton}">
  <c:set var="useConfigureButton" value="true"/>
</c:if>

<!--  METRICS GET CURRENT -->
<c:if test="${useConfigureButton}">
<c:choose>
  <c:when test="${not empty childResourceType}">
    <c:set var="resourceTypeName" value="autogroup"/>
  </c:when>
  <c:otherwise>
    <c:set var="resourceTypeName" value="${Resource.entityId.typeName}"/>
  </c:otherwise>
</c:choose>
 <table width="100%" cellpadding="5" cellspacing="0" border="0" class="MonitorToolBar">
  <tr>
    <td width="100%" align="right" nowrap>&nbsp;
    <c:choose>
      <c:when test="${MetricsDisplayForm.showAll}">
        <fmt:message key="resource.common.monitor.visibility.HideNoneMetrics"/>
      </c:when>
      <c:otherwise>
        <fmt:message key="resource.common.monitor.visibility.ShowAllMetrics"/>
      </c:otherwise>
    </c:choose>
    </td>
    <td>
    <html:hidden property="showAll"/>
    <script language="javascript">
    function reverseListing() {
      document.forms.MetricsDisplayForm.showAll.value =
        '<c:out value="${!MetricsDisplayForm.showAll}"/>';
      document.forms.MetricsDisplayForm.submit();
    }
    </script>
    <html:link href="javascript:reverseListing();"><html:img page="/images/dash-button_go-arrow.gif" border="0"/>
    </html:link>
    </td>
  </tr>
</table>
</c:if>
<!--  /  -->
