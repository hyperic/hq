<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
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

<c:set var="rssUrl" value="/rss/ViewControlActions.rss"/>

<tiles:importAttribute name="displayLastCompleted"/>
<tiles:importAttribute name="lastCompleted" ignore="true"/>

<tiles:importAttribute name="displayMostFrequent"/>
<tiles:importAttribute name="nextScheduled" ignore="true"/>

<tiles:importAttribute name="displayNextScheduled"/>
<tiles:importAttribute name="mostFrequent" ignore="true"/>
            
<div class="effectsPortlet">
<!-- Content Block Title -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.Control"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="dragDrop" value="true"/>
  <tiles:put name="rssBase" beanName="rssUrl" />
</tiles:insert>

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
                <td width="37%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ResourceName"/></td>
                <td width="13%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.Type"/></td>
                <td width="13%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.ControlAction"/></td>
                <td width="21%" class="ListHeaderInactiveSorted"><fmt:message key="dash.home.TableHeader.DateTime"/><html:img page="/images/tb_sortdown.gif" width="9" height="9" border="0"/></td>
                <td width="13%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.Status"/></td>
              </tr>  
              <c:forEach items="${lastCompleted}" var="resource">
                <tr class="ListRow">                                                   
                  <td class="ListCell"><html:link page="/ResourceControlHistory.do?eid=${resource.entityType}:${resource.entityId}"><c:out value="${resource.entityName}"/></html:link></td>
                  <td class="ListCell"><hq:resourceTypeName typeId="${resource.entityType}"/></td>
                  <td class="ListCell"><c:out value="${resource.action}"/></td>
                  <td class="ListCell"><hq:dateFormatter value="${resource.startTime}"/></td>
                  <td class="ListCell"><c:out value="${resource.status}"/></td>
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
                      <td width="21%" class="ListHeaderInactiveSorted" align="center"><fmt:message key="dash.home.TableHeader.ControlActions"/><html:img page="/images/tb_sortdown.gif" width="9" height="9" border="0"/></td>
                      <td width="42%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.FrequentActions"/></td>
                    </tr>
                    <c:forEach items="${mostFrequent}" var="resource">
                      <tr class="ListRow">
                        <td class="ListCell"><html:link page="/ResourceControlHistory.do?eid=${resource.type}:${resource.id}"><c:out value="${resource.name}"/></html:link></td>
                        <td class="ListCell" align="center"><c:out value="${resource.num}"/></td>
                        <td class="ListCell"><c:out value="${resource.action}"/></td>
                      </tr>
                    </c:forEach>              
                    <tiles:insert definition=".dashContent.seeAll"/>
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
