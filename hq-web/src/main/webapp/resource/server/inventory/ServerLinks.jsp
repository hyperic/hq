<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2009], Hyperic, Inc.
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


<tiles:importAttribute name="resource" ignore="true" />

<c:if test="${not empty resource}">
    <c:set var="newServiceUrl"    
        value="/resource/service/Inventory.do?mode=new&eid=${resource.entityId.appdefKey}" />
    <hq:userResourcePermissions debug="false" resource="${Resource}"/>    
<table border="0"><tr><td class="LinkBox">
    <c:if test="${canModify}" >
    	<html:link action="/resource/server/Inventory">
    		<html:param name="mode" value="editConfig"/>
    		<html:param name="eid" value="${resource.entityId.appdefKey}"/>
    		<fmt:message key="resource.server.inventory.link.Configure"/>
    		<html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/>
    	</html:link><br/>
	</c:if>
    <c:if test="${canRemove}" >
    	<tiles:insert definition=".resource.common.quickDelete">
      		<tiles:put name="resource" beanName="resource"/>
	  		<tiles:put name="deleteMessage">
				<fmt:message key="resource.server.inventory.link.DeleteServer"/>
	  		</tiles:put>
    	</tiles:insert>
		<br>
	</c:if>
    <c:choose>
        <c:when test="${canCreateChild}" >
            <html:link page="${newServiceUrl}" ><fmt:message key="resource.server.inventory.link.NewService"/><html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/></html:link><br>
        </c:when>
        <c:otherwise>
            <fmt:message key="resource.server.inventory.link.NewService"/><html:img page="/images/tbb_new_locked.gif" alt="" border="0"/><br>
        </c:otherwise>
    </c:choose>   
    <tiles:insert definition=".resource.common.quickFavorites">
      <tiles:put name="resource" beanName="resource"/>
    </tiles:insert>
	<br />
	<html:link page="#" styleId="AddToGroupMenuLink"><fmt:message key="resource.group.AddToGroup.Title"/><html:img page="/images/title_arrow.gif" width="11" height="9" alt="" border="0"/></html:link>
</td></tr></table>

<tiles:insert definition=".resource.common.addToGroup">
	<tiles:put name="resource" beanName="resource"/>
</tiles:insert>

</c:if>
