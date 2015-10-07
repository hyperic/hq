<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

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

<c:set var="rssUrl" value="RssViewControlActions.action"/>

<hq:constant var="PLATFORM" 
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_PLATFORM"/>
<hq:constant var="SERVER"
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVER"/>
<hq:constant var="SERVICE" 
    classname="org.hyperic.hq.appdef.shared.AppdefEntityConstants" 
    symbol="APPDEF_TYPE_SERVICE"/>

<tiles:importAttribute name="portlet" ignore="true"/>
<tiles:importAttribute name="adminUrl" ignore="true"/>
<tiles:importAttribute name="portletName" ignore="true"/>

<tiles:importAttribute name="displayLastCompleted" ignore="true"/>
<tiles:importAttribute name="lastCompleted" ignore="true"/>

<tiles:importAttribute name="displayMostFrequent" ignore="true"/>
<tiles:importAttribute name="nextScheduled" ignore="true"/>

<tiles:importAttribute name="displayNextScheduled" ignore="true"/>
<tiles:importAttribute name="mostFrequent" ignore="true"/>
            
<div class="effectsPortlet">
<!-- Content Block Title -->
	<tiles:insertDefinition name=".header.tab">
  		<tiles:putAttribute name="tabKey" value="dash.home.Control"/>
  		<tiles:putAttribute name="portletName" value="${portletName}" />
  		<tiles:putAttribute name="adminUrl" value="${adminUrl}" />
		<tiles:putAttribute name="rssBase" value="${rssUrl}" /> 		
	</tiles:insertDefinition>

<!-- each sub-section can be hidden or visible.  They can't be re-ordered. -->

<table width="100%" cellpadding="0" cellspacing="0" border="0" class="DashboardControlActionsContainer">
  <tr>
    <td>
      <c:if test="${displayLastCompleted}">  
        <!-- Recent Actions Contents -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0" class="portletLRBorder">
          <tr>
            <td class="Subhead"><fmt:message key="dash.home.Subhead.Recent"/></td>
          </tr>
        </table>
        <table width="100%" cellpadding="0" cellspacing="0" border="0" class="portletLRBorder">
          <c:choose>    
            <c:when test="${empty lastCompleted}">
              <tr class="ListRow">
                <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
              </tr>
            </c:when>
            <c:otherwise>     
              <tr>
                <td width="40%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ResourceName"/></td>
                <td width="15%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ControlAction"/></td>
                <td width="20%" class="ListHeaderInactiveSorted"><fmt:message key="dash.home.TableHeader.DateTime"/><img src='<s:url value="/images/tb_sortdown.gif" />' height="9" width="9" border="0" /></td>
                <td width="25%" class="ListHeaderInactive"><fmt:message key="resource.server.ControlHistory.ListHeader.Message"/></td>
              </tr>  
              <c:forEach items="${lastCompleted}" var="resource">
                <tr class="ListRow">                                                   
                  <td class="ListCell">
					<c:if test="${resource.entityType == PLATFORM }">
						<c:set var="controlActionLink" value="controlStatusHistoryPlatformController.action" scope="request" />
					</c:if>
					<c:if test="${resource.entityType == SERVER }">
						<c:set var="controlActionLink" value="controlStatusHistoryServerController.action" scope="request" />
					</c:if>
					<c:if test="${resource.entityType == SERVICE }">
						<c:set var="controlActionLink" value="controlStatusHistoryServiceController.action" scope="request" />
					</c:if>													
				   <c:url var="historyLink" value="${controlActionLink}">
					<c:param name="mode" value="history"/>
					<c:param name="rid" value="${resource.entityId}"/>
					<c:param name="type" value="${resource.entityType}"/>
					<c:param name="eid" value="${resource.entityType}:${resource.entityId}"/>
				   </c:url>
					<s:a action="%{#attr.historyLink}" >
						${resource.entityName} 
					</s:a>
                  </td>
                  <td class="ListCell"><c:out value="${resource.action}"/></td>
                  <td class="ListCell"><hq:dateFormatter value="${resource.startTime}"/></td>
                  <td class="ListCell">
                  <c:choose>
                    <c:when test="${not empty resource.message}">
                      <c:out value="${resource.message}"/>
                    </c:when>
                    <c:otherwise>
                      <fmt:message key="resource.common.control.NoErrors"/>
                    </c:otherwise>
                  </c:choose>
                  </td>
                </tr>    
              </c:forEach>
            </c:otherwise>
          </c:choose>
        </table>
      </c:if>
    </td>
  </tr>
  <tr>
    <td>
      <c:if test="${displayMostFrequent}">
        <!-- On-Demand Control Frequency Contents -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0" class="ToolbarContent">
          <tr>
            <td class="Subhead"><fmt:message key="dash.home.Subhead.Quick"/></td>
          </tr>
        </table>  
        <table width="100%" cellpadding="0" cellspacing="0" border="0" class="portletLRBorder">
          <c:choose>
            <c:when test="${empty mostFrequent}">
              <tr class="ListRow">
                <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
              </tr>
            </c:when>
            <c:otherwise>
              <tr class="ListRow">
                <td>
                  <table width="100%" cellpadding="0" cellspacing="0" border="0">
                    <tr>
                      <td width="37%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ResourceName"/></td>
                      <td width="21%" class="ListHeaderInactiveSorted" align="center"><fmt:message key="dash.home.TableHeader.ControlActions"/><img src='<s:url value="/images/tb_sortdown.gif" />' height="9" width="9" border="0" /></td>
                      <td width="42%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.FrequentActions"/></td>
                    </tr>
                    <c:forEach items="${mostFrequent}" var="resource">
                      <tr class="ListRow">
                        <td class="ListCell">
								<c:if test="${resource.type == PLATFORM }">
									<c:set var="controlActionLink" value="controlStatusHistoryPlatformController.action" scope="request" />
								</c:if>
								<c:if test="${resource.type == SERVER }">
									<c:set var="controlActionLink" value="controlStatusHistoryServerController.action" scope="request" />
								</c:if>
								<c:if test="${resource.type == SERVICE }">
									<c:set var="controlActionLink" value="controlStatusHistoryServiceController.action" scope="request" />
								</c:if>													
							   <c:url var="historyLink" value="${controlActionLink}">
								<c:param name="mode" value="history"/>
								<c:param name="rid" value="${resource.id}"/>
								<c:param name="type" value="${resource.type}"/>
								<c:param name="eid" value="${resource.type}:${resource.id}"/>
							   </c:url>
							<s:a action="%{#attr.historyLink}" >
								${resource.name}
							</s:a>                        	
                        </td>
                        <td class="ListCell" align="center"><c:out value="${resource.num}"/></td>
                        <td class="ListCell"><c:out value="${resource.action}"/></td>
                      </tr>
                    </c:forEach>              
                    <tiles:insertDefinition name=".ng.dashContent.seeAll"/>
                  </table>
                </td>
              </tr>
            </c:otherwise>
          </c:choose>
        </table>
      </c:if>
    </td> 
  </tr>
</table>
</div>
