<%@ page language="java" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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
<!-- Content Block -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.SummaryCounts"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="dragDrop" value="true"/>
</tiles:insert>

<tiles:importAttribute name="summary"/>
<tiles:importAttribute name="server"/>
<tiles:importAttribute name="serverTypes"/>
<tiles:importAttribute name="service"/>
<tiles:importAttribute name="serviceTypes"/>
<tiles:importAttribute name="application"/>
<tiles:importAttribute name="applicationTypes"/>
<tiles:importAttribute name="platform"/>
<tiles:importAttribute name="platformTypes"/>
<tiles:importAttribute name="cluster"/>
<tiles:importAttribute name="clusterTypes"/>

<tiles:importAttribute name="groupMixed"/>
<tiles:importAttribute name="groupGroups"/>
<tiles:importAttribute name="groupPlatServerService"/>
<tiles:importAttribute name="groupApplication"/>

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="portletLRBorder">
  <tr>
    <td class="BlockContent" align="right">
      <tiles:insert page="/resource/hub/ResourceHubLinks.jsp"/>
    </td>
  </tr>
  <tr>
    <td class="BlockContent">    
      <table width="100%" cellpadding="1" cellspacing="0" border="0">
      <c:choose>
        <c:when test="${application}">      
          <tr>
            <td class="FormLabel"><html:link page="/ResourceHub.do?ff=4"><fmt:message key="dash.home.DisplayCategory.AppTotal"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/ResourceHub.do?ff=4"><c:out value="${summary.applicationCount}"/></html:link></td>
          </tr>
          <c:forEach var="type" items="${applicationTypes}">
            <c:if test="${not empty summary.appTypeMap[type]}">
              <tr>
                <td><c:out value="${type}"/></td>
                <td align="right"><c:out value="${summary.appTypeMap[type]}"/></td>
              </tr>
            </c:if>
          </c:forEach>
        </c:when>
        <c:when test="${not empty applicationTypes }">
            <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
            <tr>
              <td class="FormLabel" colspan="2"><html:link page="/ResourceHub.do?ff=4"><fmt:message key="dash.home.DisplayCategory.AppTotal"/></html:link></td>
            </tr>
          <c:forEach var="type" items="${applicationTypes}">        
            <c:if test="${not empty summary.appTypeMap[type]}">
              <tr>
                <td><c:out value="${type}"/></td>
                <td align="right"><c:out value="${summary.appTypeMap[type]}"/></td>
              </tr>
            </c:if>
          </c:forEach>
        </c:when>        
      </c:choose>
      
      <c:choose>
        <c:when test="${platform}">      
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/ResourceHub.do?ff=1"><fmt:message key="dash.home.DisplayCategory.PlatformTotal"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/ResourceHub.do?ff=1"><c:out value="${summary.platformCount}"/></html:link></td>
          </tr>
          <c:forEach var="type" items="${platformTypes}">        
            <c:if test="${not empty summary.platformTypeMap[type]}">
              <tr>
                <td><c:out value="${type}"/></td>
                <td align="right"><c:out value="${summary.platformTypeMap[type]}"/></td>
              </tr>
            </c:if>
          </c:forEach>
        </c:when>
        <c:when test="${not empty platformTypes}">
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/ResourceHub.do?ff=1"><fmt:message key="dash.home.DisplayCategory.PlatformTotal"/></html:link></td>
            <td>&nbsp;</td>
          </tr>
          <c:forEach var="type" items="${platformTypes}">        
            <c:if test="${not empty summary.platformTypeMap[type]}">
              <tr>
                <td><c:out value="${type}"/></td>
                <td align="right"><c:out value="${summary.platformTypeMap[type]}"/></td>
              </tr>
            </c:if>
          </c:forEach>
        </c:when>        
      </c:choose>
      
      <c:choose>
        <c:when test="${server}">      
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/ResourceHub.do?ff=2"><fmt:message key="dash.home.DisplayCategory.ServerTotal"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/ResourceHub.do?ff=2"><c:out value="${summary.serverCount}"/></html:link></td>
          </tr>
          <c:forEach var="type" items="${serverTypes}">        
            <c:if test="${not empty summary.serverTypeMap[type]}">
              <tr>
                <td><c:out value="${type}"/></td>
                <td align="right"><c:out value="${summary.serverTypeMap[type]}"/></td>
              </tr>
            </c:if>
          </c:forEach>
        </c:when>
        <c:when test="${not empty serverTypes}">
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/ResourceHub.do?ff=2"><fmt:message key="dash.home.DisplayCategory.ServerTotal"/></html:link></td>
            <td>&nbsp;</td>
          </tr>
          <c:forEach var="type" items="${serverTypes}">        
            <c:if test="${not empty summary.serverTypeMap[type]}">
              <tr>
                <td><c:out value="${type}"/></td>
                <td align="right"><c:out value="${summary.serverTypeMap[type]}"/></td>
              </tr>
            </c:if>
          </c:forEach>
        </c:when>        
      </c:choose>      
      
      <c:choose>
        <c:when test="${service}">      
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/ResourceHub.do?ff=3"><fmt:message key="dash.home.DisplayCategory.ServiceTotal"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/ResourceHub.do?ff=3"><c:out value="${summary.serviceCount}"/></html:link></td>
          </tr>
          <c:forEach var="type" items="${serviceTypes}">        
            <c:if test="${not empty summary.serviceTypeMap[type]}">
              <tr>
                <td><c:out value="${type}"/></td>
                <td align="right"><c:out value="${summary.serviceTypeMap[type]}"/></td>
              </tr>
            </c:if>
          </c:forEach>
        </c:when>
        <c:when test="${not empty serviceTypes}">
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/ResourceHub.do?ff=3"><fmt:message key="dash.home.DisplayCategory.ServiceTotal"/></html:link></td>
            <td>&nbsp;</td>
          </tr>
          <c:forEach var="type" items="${serviceTypes}">        
            <c:if test="${not empty summary.serviceTypeMap[type]}">
              <tr>
                <td><c:out value="${type}"/></td>
                <td align="right"><c:out value="${summary.serviceTypeMap[type]}"/></td>
              </tr>
            </c:if>
          </c:forEach>
        </c:when>        
      </c:choose>
      
      <c:if test="${cluster}">      
        <tr>
          <td colspan="2">&nbsp;</td>
        </tr>
        <tr>
          <td class="FormLabel"><html:link page="/ResourceHub.do?ff=5&g=1"><fmt:message key="dash.home.DisplayCategory.group.ClusterTotal"/></html:link></td>
          <td class="FormLabelRight"><html:link page="/ResourceHub.do?ff=5&g=1"><c:out value="${summary.compatGroupCount}"/></html:link></td>
        </tr>
      </c:if>

  
      <c:choose>
        <c:when test="${groupMixed}">      
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/ResourceHub.do?ff=5&g=2"><fmt:message key="dash.home.DisplayCategory.group.mixedTotal"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/ResourceHub.do?ff=5&g=2"><c:out value="${summary.groupCountAdhocGroup + summary.groupCountAdhocPSS + summary.groupCountAdhocApp}"/></html:link></td>
          </tr>
          <c:if test="${groupGroups}">
            <tr>
              <td><fmt:message key="dash.home.DisplayCategory.group.groupsTotal"/></td>
              <td align="right"><c:out value="${summary.groupCountAdhocGroup}"/></td>
            </tr>
          </c:if>
          <c:if test="${groupPlatServerService}">
            <tr>
              <td><fmt:message key="dash.home.DisplayCategory.group.plat.server.serviceTotal"/></td>
              <td align="right"><c:out value="${summary.groupCountAdhocPSS}"/></td>
            </tr>
          </c:if>
          <c:if test="${groupApplication}">
            <tr>
              <td><fmt:message key="dash.home.DisplayCategory.group.applicationTotal"/></td>
              <td align="right"><c:out value="${summary.groupCountAdhocApp}"/></td>
            </tr>
          </c:if>
          
        </c:when>
        <c:when test="${groupGroups || groupPlatServerService || groupApplication}">      
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><fmt:message key="dash.home.DisplayCategory.group.mixedTotal"/></td>
            <td class="FormLabelRight"><c:out value="${summary.clusterCount}"/></td>
          </tr>
          <c:if test="${groupGroups}">
            <tr>
              <td><fmt:message key="dash.home.DisplayCategory.group.groupsTotal"/></td>
              <td align="right"><c:out value="${summary.groupCountAdhocGroup}"/></td>
            </tr>
          </c:if>
          <c:if test="${groupPlatServerService}">
            <tr>
              <td><fmt:message key="dash.home.DisplayCategory.group.plat.server.serviceTotal"/></td>
              <td align="right"><c:out value="${summary.groupCountAdhocPSS}"/></td>
            </tr>
          </c:if>
          <c:if test="${groupApplication}">
            <tr>
              <td><fmt:message key="dash.home.DisplayCategory.group.applicationTotal"/></td>
              <td align="right"><c:out value="${summary.groupCountAdhocApp}"/></td>
            </tr>
          </c:if>
        </c:when>
              
      </c:choose>
      
      </table>
    </td>
  </tr>
  <tr>
    <td class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
</div>
