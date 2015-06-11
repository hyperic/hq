<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

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


<div class="effectsPortlet">
	<tiles:importAttribute name="charts" ignore="true"/>
	
	<!-- Content Block Title -->
	<tiles:insertDefinition name=".header.tab">
  		<tiles:putAttribute name="tabKey" value="dash.home.SavedQueries"/>
  		<tiles:putAttribute name="adminUrl" value="${sessionScope.adminUrl}" />
  		<tiles:putAttribute name="portletName" value="${sessionScope.portletName}" />
	</tiles:insertDefinition>

	
	<c:out value="avi12"/> <c:out value="${requestScope.portletName}"/>
	<!-- Content Block Contents -->
	<table width="100%" cellpadding="0" cellspacing="0" border="0" class="portletLRBorder">
    	<c:choose>    
      		<c:when test="${empty charts}">
        		<tr class="ListRow">
          			<td class="ListCell"><fmt:message key="dash.home.no.charts.to.display"/></td>
        		</tr>
      		</c:when>
      		<c:otherwise>
        		<c:forEach var="chart" items="${charts}">        
          			<tr class="ListRow">
            			<td class="ListCell" valign="middle" nowrap="true">&nbsp;<img src='<s:url value="/images/icon_chart.gif" />' /></td>
            			<td class="ListCell"><s:a href="#request.chart.value">${chart.key}</s:a></td>        
          			</tr>
        		</c:forEach>
      		</c:otherwise>
    	</c:choose>
	</table>
</div>