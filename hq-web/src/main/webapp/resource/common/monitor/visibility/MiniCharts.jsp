<%@ page language="java" %>
<%@ page import="java.util.Iterator"%>
<%@ page import="org.hyperic.hq.appdef.shared.AppdefResourceValue"%>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
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
<jsu:script>
    // Overwrite highlight/unhighlight functions
    function highlight(e) {}
    function unhighlight(e) {}
</jsu:script>
<!-- MINI-CHARTS -->
<tiles:useAttribute id="list" name="Resources" classname="java.util.List"/>

<table cellpadding="2" cellspacing="0" border="0">
  <tr>
    <td>
       <input type="checkbox" onclick="ToggleAll(this, widgetProperties)" name="listToggleAll">
    </td>
    <td>
      <fmt:message key="common.label.SelectAll"/>
    </td>
    <td>
      &nbsp;
    </td>
    <td>
      <html:img page="/images/px_green.gif" width="10" height="10" border="0"/>
    </td>
    <td>
      <fmt:message key="resource.hub.legend.available"/>
    </td>
    <td>
      <html:img page="/images/spacer.gif" width="10" height="16" border="0"/>
    </td>
    <td>
      <html:img page="/images/px_yellow.gif" width="10" height="10" border="0"/>
    </td>
    <td>
      <fmt:message key="resource.hub.legend.partial"/>
    </td>
    <td>
      <html:img page="/images/spacer.gif" width="10" height="16" border="0"/>
    </td>
    <td>
      <html:img page="/images/px_red.gif" width="10" height="10" border="0"/>
    </td>
    <td>
      <fmt:message key="resource.hub.legend.unavailable"/>
    </td>
    <td>
      <html:img page="/images/spacer.gif" width="10" height="16" border="0"/>
    </td>
    <td>
      <html:img page="/images/px_orange.gif" width="10" height="10" border="0"/>
    </td>
    <td>
      <fmt:message key="resource.hub.legend.suspended"/>
    </td>
    <td>
      <html:img page="/images/spacer.gif" width="10" height="16" border="0"/>
    </td>
    <td>
      <html:img page="/images/px_gray.gif" width="10" height="10" border="0"/>
    </td>
    <td>
      <fmt:message key="resource.hub.legend.unknown"/>
    </td>
  </tr>
</table>

<%-- Iterate over resources, don't use <iterate> tage because JSP1.1 doesn't
     allow it --%>

<table width="425" cellpadding="2" cellspacing="0" border="0" id="listTable">
<%
    Iterator i = list.iterator();
while ( i.hasNext() ) {
    AppdefResourceValue resource = (AppdefResourceValue) i.next();
    request.setAttribute("resource", resource);
%>

  <c:set var="eid" value="${resource.entityId.appdefKey}"/>

  <c:url var="availabilityUrl" value="/resource/AvailColor">
    <c:param name="eid" value="${eid}" />
  </c:url>

  <tr>
    <td>
      <table cellpadding="0" border="0" cellspacing="0" background="<c:out value="${availabilityUrl}"/>">
        <tr>
          <td class="MiniChartHeader" width="1%">
            <html:checkbox property="resources" value="${eid}" onclick="ToggleSelection(this, widgetProperties)" styleClass="listMember"/>
          </td>
          <td class="MiniChartHeader" align="left">
            <html:link action="/Resource">
            	<html:param name="eid" value="${eid}"/>
                <c:out value="${resource.name}" escapeXml="true"/>
            </html:link>
            <fmt:message key="parenthesis">
              <fmt:param value="${resource.appdefResourceTypeValue.name}"/>
            </fmt:message>
          </td>
        </tr>
        <tr>
          <td class="MiniChartHeader" colspan="2">
            <tiles:insert name=".resource.hub.minichart">
              <tiles:put name="eid" beanName="eid"/>
              <c:if test="${resource.entityId.type < 4}">
                <tiles:put name="chartLink" value="true"/>
              </c:if>
            </tiles:insert>
          </td>
        </tr>
      </table>
    </td>
  </tr>
<%
}
%>
</table>
<!-- / MINI-CHARTS -->
