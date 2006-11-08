<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="display" prefix="display" %>
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


<div class="effectsPortlet">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.RecentlyApproved"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="dragDrop" value="true"/>
</tiles:insert>

<tiles:importAttribute name="recentlyApproved"/>

<c:choose >
  <c:when test="${not empty recentlyApproved}">
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
    <html:form action="/dashboard/ProcessRAList">
    <html:hidden property="platformId"/>
      <tr>
        <td width="3%" class="ListHeaderInactive">&nbsp;</td>
        <td width="67%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ResourceName"/></td>
        <td width="30%" class="ListHeaderInactive" align="center"><fmt:message key="dash.home.TableHeader.DateTime"/></td>
      </tr>
      <c:forEach items="${recentlyApproved}" var="platform">
      <tr class="ListRow">
        <td class="ListCell">
           <c:choose>
             <c:when test="${platform.showServers}">
               <a href="." onclick="RAListForm.platformId.value=<c:out
                  value="${platform.id}"/>; 
                  RAListForm.submit(); return false;">
                  <html:img page="/images/icon_down_arrow.gif" border="0"/>
               </a>
             </c:when>
             <c:otherwise>
               <a href="." onclick="RAListForm.platformId.value=<c:out
                  value="${platform.id}"/>; 
                  RAListForm.submit(); return false;">
                  <html:img page="/images/icon_right_arrow.gif" border="0"/>
               </a>
             </c:otherwise>
           </c:choose>
        <td class="ListCell">
            <html:link page="/Resource.do?eid=1:${platform.id}"><c:out value="${platform.name}"/>&nbsp;</html:link>
          <fmt:message key="parenthesis">
            <fmt:param value="${platform.numResources}"/>
          </fmt:message></td>
        <td class="ListCell" align="center"><hq:dateFormatter value="${platform.resourceCTime}"/>&nbsp;</td>
      </tr>
      <c:if test="${platform.showServers}">
      <!-- Show the platform's servers -->
      <c:forEach items="${platform.servers}" var="server">
      <tr class="ListRow">
        <td class="ListCell"></td>
        <td class="ListCell">
            <html:link page="/Resource.do?eid=2:${server.id}"><c:out value="${server.name}"/>&nbsp;</html:link>
          <fmt:message key="parenthesis">
            <fmt:param value="${server.numServices}"/>
          </fmt:message></td>
        <td class="ListCell" align="center"><hq:dateFormatter value="${server.CTime}"/>&nbsp;</td>
      </tr>
      </c:forEach> <!-- For each server -->
      </c:if>

      </c:forEach> <!-- For each platform -->
      </html:form>
    </table>
  </c:when>
  <c:otherwise>
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr class="ListRow">
        <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
      </tr>
    </table>
  </c:otherwise>
</c:choose>
</div>
