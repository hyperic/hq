<%@ page language="java" %>
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
<!-- Content Block -->
<tiles:insertDefinition name=".header.tab">
  <tiles:putAttribute name="tabKey" value="dash.home.SummaryCounts"/>
  <tiles:putAttribute name="adminUrl" value="${adminUrl}" />
  <tiles:putAttribute name="portletName" value="${portletName}" />
</tiles:insertDefinition>

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
      <tiles:insertTemplate template="/resource/hub/ResourceHubLinks.jsp"/>
    </td>
  </tr>
  <tr>
    <td class="BlockContent">    
      <table width="100%" cellpadding="1" cellspacing="0" border="0">
      <c:choose>
        <c:when test="${application}">      
          <tr>
            <td class="FormLabel">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="4"/>
            		<fmt:message key="dash.home.DisplayCategory.AppTotal"/>
            	</s:a>
            </td>
            <td class="FormLabelRight">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="4"/>
            		${summary.applicationCount}
            	</s:a>
            </td>
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
              <td class="FormLabel" colspan="2">
              	<s:a action="/ResourceHub">
              		<s:param name="ff" value="4"/>
              		<fmt:message key="dash.home.DisplayCategory.AppTotal"/>
              	</s:a>
              </td>
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
            <td class="FormLabel">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="1" />
            		<fmt:message key="dash.home.DisplayCategory.PlatformTotal"/>
            	</s:a>
            </td>
            <td class="FormLabelRight">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="1"/>
            		${summary.platformCount}
            	</s:a>
            </td>
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
            <td class="FormLabel">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="1"/>
            		<fmt:message key="dash.home.DisplayCategory.PlatformTotal"/>
            	</s:a>
            </td>
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
            <td class="FormLabel">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="2"/>
            		<fmt:message key="dash.home.DisplayCategory.ServerTotal"/>
            	</s:a>
            </td>
            <td class="FormLabelRight">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="2"/>
            		${summary.serverCount}
            	</s:a>
            </td>
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
            <td class="FormLabel">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="2"/>
            		<fmt:message key="dash.home.DisplayCategory.ServerTotal"/>
            	</s:a>
            </td>
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
            <td class="FormLabel">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="3"/>
            		<fmt:message key="dash.home.DisplayCategory.ServiceTotal"/>
            	</s:a>
            </td>
            <td class="FormLabelRight">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="3"/>
            		${summary.serviceCount}
            	</s:a>
            </td>
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
            <td class="FormLabel">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="3"/>
            		<fmt:message key="dash.home.DisplayCategory.ServiceTotal"/>
            	</s:a>
            </td>
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
          <td class="FormLabel">
          	<s:a action="/ResourceHub">
          		<s:param name="ff" value="5"/>
          		<s:param name="g" value="1"/>
          		<fmt:message key="dash.home.DisplayCategory.group.ClusterTotal"/>
          	</s:a>
          </td>
          <td class="FormLabelRight">
          	<s:a action="/ResourceHub">
          		<s:param name="ff" value="5"/>
          		<s:param name="g" value="1"/>
          		${summary.compatGroupCount}
          	</s:a>
          </td>
        </tr>
      </c:if>

  
      <c:choose>
        <c:when test="${groupMixed}">      
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="5"/>
            		<s:param name="g" value="2"/>
            		<fmt:message key="dash.home.DisplayCategory.group.mixedTotal"/>
            	</s:a>
            </td>
            <td class="FormLabelRight">
            	<s:a action="/ResourceHub">
            		<s:param name="ff" value="5"/>
            		<s:param name="g" value="2"/>
            		${summary.groupCountAdhocGroup + summary.groupCountAdhocPSS + summary.groupCountAdhocApp}
            	</s:a>
            </td>
            		
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
    <td class="BlockContent"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td class="BlockBottomLine"><img src='<s:url value="/images/spacer.gif" />' width="1" height="1" border="0"/></td>
  </tr>
</table>
</div>
