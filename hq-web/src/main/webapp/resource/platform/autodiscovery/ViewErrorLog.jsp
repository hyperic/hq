<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
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
<jsu:importScript path="/js/functions.js" />
<link rel=stylesheet href="<html:rewrite page="/css/win.css"/>" type="text/css">

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.autodiscovery.ErrorLogTab"/>
</tiles:insert>

<hq:constant
    classname="org.hyperic.hq.ui.Constants" 
    symbol="LAST_AI_ERROR_ATTR" var="CONST_LAST_ERROR" />

<c:set var="lastError" value="${requestScope[CONST_LAST_ERROR]}"/>    
<!--  /  -->
<!-- we shouldn't be in this tile if we don't have a ScanState -->
<tiles:importAttribute name="scanstate" />

<!--  SCHEDULE CONTENTS -->
<div id="listDiv">
	<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
    <c:choose>
      <c:when test="${not empty lastError}">
		<tr class="ListHeader">
			<td class="ListCell" width="80%" ><fmt:message key="resource.autodiscovery.errorLogTab.ErrorTH"/></td>
			<td class="ListCell" width="20%" ><fmt:message key="resource.autodiscovery.errorLogTab.DateTH"/></td>
		</tr>
        <c:if test="${not empty scanstate.globalException}">
            <tr class="ListRow">
                <td class="ListCell"><c:out value="${scanstate.globalException.message}"/></td>
                <td class="ListCell"><hq:dateFormatter value="${scanstate.globalException.CTime}"/></td>
            </tr>
        </c:if>
        <c:forEach var="methodState" items="${scanstate.scanMethodStates}">
            <c:forEach var="exception" items="${methodState.exceptions}">
                <tr class="ListRow">
                    <td class="ListCell"><c:out value="${exception.message}"/></td>
                    <td class="ListCell"><hq:dateFormatter value="${exception.CTime}"/></td>
                </tr>
            </c:forEach>
        </c:forEach>
    </c:when>
    <c:otherwise>
        <tr class="ListRow">
            <td class="ListCell" width="80%" colspan="2">
                <fmt:message key="resource.platform.inventory.autoinventory.status.NoErrors"/>
            </td>
        </tr>
    </c:otherwise>
</c:choose>        
	</table>
</div>
<!--  /  -->
