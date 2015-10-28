<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

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
 
<tiles:importAttribute name="adminUrl" ignore="true"/>
<tiles:importAttribute name="portletName" ignore="true"/>
<tiles:importAttribute name="recentlyAdded" ignore="true" />

<div class="effectsPortlet">
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="dash.home.RecentlyApproved"/>
  <tiles:putAttribute name="adminUrl" value="${adminUrl}" />
  <tiles:putAttribute name="portletName" value="${portletName}" />
</tiles:insertDefinition>




<c:choose>
  <c:when test="${not empty recentlyAdded}">
    <table width="100%" cellpadding="0" cellspacing="0" border="0" class="portletLRBorder">
      <tr>
        <td width="70%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ResourceName"/></td>
        <td width="30%" class="ListHeaderInactive" align="center"><fmt:message key="dash.home.TableHeader.Time"/></td>
      </tr>
      <c:forEach items="${recentlyAdded}" var="platform">
      <tr class="ListRow">
        <td class="ListCell">
            <s:a action="currentHealthMonitorPlatformVisibility">
            	<s:param name="eid" value="%{'1:'+#attr.platform.id}"/>
				<s:param name="mode" value="currentHealth"/>
            	<c:out value="${platform.name}"/>&nbsp;
            </s:a>
        </td>
        <td class="ListCell" align="center">
        <c:set var="formattedTime">
        <hq:dateFormatter time="true" approx="true" value="${current - platform.CTime}"/>
        </c:set>
        <fmt:message key="dash.recentlyApproved.ago">
          <fmt:param value="${formattedTime}"/>
        </fmt:message>
        </td>
      </tr>
      </c:forEach> <!-- For each platform -->
	  
    </table>
  </c:when>
  <c:otherwise>
    <table width="100%" cellpadding="0" cellspacing="0" border="0" class="portletLRBorder">
      <tr class="ListRow">
        <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
      </tr>
    </table>
  </c:otherwise>
</c:choose>

</div>
