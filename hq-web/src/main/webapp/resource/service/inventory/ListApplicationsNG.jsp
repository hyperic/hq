<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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


<tiles:importAttribute name="service"/>
<tiles:importAttribute name="applications"/>
<tiles:importAttribute name="selfAction"/>

<s:set var="applicationCount" value="#attr.applications.totalSize"/>

<c:url var="ssAction" value="${selfAction}">
  <c:if test="${not empty param.fs}">
    <c:param name="fs" value="${param.fs}"/>
  </c:if>
  <c:if test="${not empty param.pss}">
    <c:param name="pss" value="${param.pss}"/>
  </c:if>
  <c:if test="${not empty param.pns}">
    <c:param name="pns" value="${param.pns}"/>
  </c:if>
  <c:if test="${not empty param.scs}">
    <c:param name="scs" value="${param.scs}"/>
  </c:if>
</c:url>

<hq:pageSize var="pageSize"/>
<c:set var="widgetInstanceName" value="listApplications"/>

<div id="listDiv">
  <c:choose>
  <c:when test="${applicationCount > 0}">

  
  <display:table cellspacing="0" cellpadding="0" width="100%" 
                   action="${ssAction}"
                  orderValue="sos" order="${param.sos}" sortValue="scs" sort="${param.scs}" pageValue="pns" 
                  page="${param.pns}" pageSizeValue="pss" pageSize="${param.pss}" items="${applications}" >
    <display:column width="25%" property="name" sort="true" sortAttr="5"
                    defaultSort="true" title="resource.service.inventory.applicationMembership.ApplicationTH" 
                    href="viewResourceInventoryApplicationVisibility.action?mode=view&type=4" paramId="rid" paramProperty="id" />
       
    <display:column width="50%" property="description" 
                    title="common.header.Description" /> 

    <display:column width="25%" property="owner" sort="true" sortAttr="21" 
            title="resource.service.inventory.applicationMembership.OwnerTH" />
  </display:table>
  <tiles:insertDefinition name=".toolbar.list">
    <tiles:putAttribute name="noButtons" value="true"/>
    <tiles:putAttribute name="listItems" value="${applications}"/>
    <tiles:putAttribute name="listSize" value="${applicationCount}"/>
    <tiles:putAttribute name="pageSizeAction" value="${selfAction}" />
    <tiles:putAttribute name="pageNumAction" value="${selfAction}"/>    
    <tiles:putAttribute name="widgetInstanceName" value="${widgetInstanceName}"/>
    <tiles:putAttribute name="defaultSortColumn" value="5"/>
  </tiles:insertDefinition>

  </c:when>

  <c:otherwise>
    <table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
  		<tr class="ListRow">
  			<td class="ListCell" colspan="3"><i><fmt:message key="resource.service.inventory.applicationlist.zerolength"/></i></td>
  		</tr>
  	</table>
  </c:otherwise>
  </c:choose>


</div>

