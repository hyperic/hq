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

 <%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="display" prefix="display" %>
<%@ taglib uri="jstl-c" prefix="c" %>

<div class="effectsPortlet">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.RecentResources"/>
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="dragDrop" value="true"/>
</tiles:insert>

<tiles:importAttribute name="resourceHealth"/>
<tiles:importAttribute name="performance"/>
<tiles:importAttribute name="throughput"/>
<tiles:importAttribute name="availability"/>
<tiles:importAttribute name="utilization"/>

<c:choose >
  <c:when test="${not empty resourceHealth}">   
  
    <display:table cellspacing="0" cellpadding="0" width="100%" action="/Dashboard.do"
                   var="resource" pageSize="-1" items="${resourceHealth}" >
                
        <display:column width="50%" href="/Resource.do?eid=${resource.resourceTypeId}:${resource.resourceId}" property="resourceName" title="dash.home.TableHeader.ResourceName"/>
        <display:column width="20%" property="resourceTypeName" title="dash.home.TableHeader.Type"/>
        <c:if test="${performance}">  
          <display:column width="10%" property="performance" title="resource.common.monitor.visibility.PerformanceTH" align="center" />
        </c:if>
        <c:if test="${throughput}">  
          <display:column width="10%" property="throughput" title="resource.common.monitor.visibility.UsageTH" align="center" > 
          <display:metricdecorator unit="${resource.throughputUnits}" defaultKey="resource.common.monitor.visibility.performance.NotAvail"/>
          </display:column>
        </c:if>
        
        <c:if test="${availability}">  
          <display:column width="10%" property="availability" title="resource.common.monitor.visibility.AvailabilityTH" align="center" >
            <display:availabilitydecorator value="${resource.availability}"
                                     monitorable="${resource.monitorable}"
                                  resourceTypeId="${resource.resourceTypeId}"
                                      resourceId="${resource.resourceId}"/>
          </display:column>
        </c:if>
        <c:if test="${utilization}">                  
          <display:column width="10%" property="alerts" title="dash.home.TableHeader.Alerts" align="center"/>          
        </c:if>
        
    </display:table>    
    <tiles:insert definition=".dashContent.seeAll"/>
    
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
