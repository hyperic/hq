<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

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


<tiles:importAttribute name="eid"/>
<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="chartLink" ignore="true"/>

<c:choose>
  <c:when test="${not empty summaries}">
  <table border="0" cellpadding="0" cellspacing="2" bgcolor="#CCCCCC">
    <tr>
    <logic:iterate id="template" name="summaries">
      <c:choose>
        <c:when test="${template.name eq 'Availability'}">
          <c:set var="url" value="/resource/AvailHealthChart"/>
        </c:when>
        <c:when test="${template.category.name eq 'UTILIZATION'}">
          <c:set var="url" value="/resource/UtilizationHealthChart"/>
        </c:when>
        <c:otherwise>
          <c:set var="url" value="/resource/UsageHealthChart"/>
        </c:otherwise>
      </c:choose>

      <c:url var="healthChartUrl" value="${url}">
        <c:param name="eid" value="${eid}"/>
        <c:param name="tid" value="${template.id}"/>
      </c:url>

      <c:if test="${chartLink}">
        <c:url var="healthChartLink"
               value="/resource/common/monitor/Visibility.do">
          <c:param name="eid" value="${eid}"/>
          <c:param name="mode" value="chartSingleMetricSingleResource"/>
          <c:param name="m" value="${template.id}"/>
        </c:url>
      </c:if>

    <td>
      <table border="0" cellpadding="0" cellspacing="0">
        <tr>
          <td class="MiniChartTitle"><c:out value="${template.name}"/></td>
        </tr>
        <tr>
         <td>
           <c:choose>
             <c:when test="${chartLink}">
               <a href="${healthChartLink}"><img src="${healthChartUrl}" width="200" height="100" border="0"></a>
             </c:when>
             <c:otherwise>
               <img src="${healthChartUrl}" width="200" height="100">
             </c:otherwise>
           </c:choose>
         </td>
         </tr>
       </table>
    </td>
    </logic:iterate>
    <c:if test="${perfSupported}">
    <td>
      <table border="0" cellpadding="0" cellspacing="0">
        <tr>
          <td class="MiniChartTitle"><fmt:message key="resource.common.monitor.visibility.PerformanceTH"/></td>
        </tr>
        <tr>
         <td><html:img page="/resource/PerfHealthChart?eid=${eid}" width="200" height="100"/></td>
         </tr>
       </table>
    </td>
    </c:if>
      </tr>
    </table>
  </c:when>
  <c:otherwise>
      <fmt:message key="resource.common.monitor.visibility.NoMetricsEtc"/>
  </c:otherwise>
</c:choose>

