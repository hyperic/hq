<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2007], Hyperic, Inc.
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

<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>
<script  type="text/javascript">
	dojo11.addOnLoad(function() {
		dojo11.query("li.tableRow").onmouseenter(function(e) {
				dojo11.addClass(e.currentTarget, "hover");
			}).onmouseleave(function(e) {
				dojo11.removeClass(e.currentTarget, "hover");
			});
		});
	});
</script>

<!-- PLATFORM CONTENTS -->
<ul style="width:100%;margin:0;padding:0;list-style:none;">
	<li class="ListHeaderDark ListHeaderInactiveSorted"><fmt:message key="resource.hub.PlatformTypeTH"/>s</li>
	<c:forEach var="entry" items="${platformTypes}" varStatus="loopStatus">
		<li class="tableRow<c:if test="${(loopStatus.index % 2) != 0}"> tableRowOdd</c:if>"
		    style="position:relative;">
			<div class="tableCell" style="">
				<html:link action="/ResourceHub">
      				<html:param name="ff" value="1" />
      				<html:param name="ft" value="1:${entry.id}" />
      				<c:out value="${entry.name}"/>
      			</html:link>
			</div>
			<div style="position:absolute;right:0;top:0;padding:2px;">
				<tiles:insert definition=".admin.config.DefaultsAction">
        			<tiles:put name="typeName" value="platform"/>
        			<tiles:put name="aetid" value="1:${entry.id}" />
      			</tiles:insert>
			</div>
		</li>
    </c:forEach>
</ul>
  	
<!-- Platform Services -->
<ul style="width:100%;margin:0;padding:0;list-style:none;margin-top:15px;">
	<li class="ListHeaderDark ListHeaderInactiveSorted"><fmt:message key="resource.hub.PlatformServiceTypeTH"/>s</li>
	<c:forEach var="platSvc" items="${platformServiceTypes}" varStatus="loopStatus">
		<li class="tableRow<c:if test="${(loopStatus.index % 2) != 0}"> tableRowOdd</c:if>"
		    style="position:relative;">
			<div class="tableCell indentArrowIcon" style="">
      			<html:link action="/ResourceHub">
      				<html:param name="ff" value="3" />
      				<html:param name="ft" value="3:${platSvc.id}" />
      				<c:out value="${platSvc.name}"/>
      			</html:link>
			</div>
			<div style="position:absolute;right:0;top:0;padding:2px;">
				<tiles:insert definition=".admin.config.DefaultsAction">
    	    		<tiles:put name="typeName" value="service"/>
	        		<tiles:put name="aetid" value="3:${platSvc.id}" />
      			</tiles:insert>
			</div>
		</li>
    </c:forEach>
	<c:forEach var="winSvc" items="${windowsServiceTypes}" varStatus="loopStatus">
		<li class="tableRow<c:if test="${(loopStatus.index % 2) != 0}"> tableRowOdd</c:if>"
		    style="position:relative;">
			<div class="tableCell indentArrowIcon" style="">
      			<html:link action="/ResourceHub">
      				<html:param name="ff" value="3" />
      				<html:param name="ft" value="3:${winSvc.id}" />
      				<c:out value="${winSvc.name}"/>
      			</html:link>
			</div>
			<div style="position:absolute;right:0;top:0;padding:2px;">
				<tiles:insert definition=".admin.config.DefaultsAction">
	        		<tiles:put name="typeName" value="service"/>
	        		<tiles:put name="aetid" value="3:${winSvc.id}" />
      			</tiles:insert>
			</div>
		</li>
    </c:forEach>
</ul>
<!-- SERVER CONTENTS -->
<!-- Platform Services -->
<ul style="width:100%;margin:0;padding:0;list-style:none;margin-top:15px;">
	<li class="ListHeaderDark ListHeaderInactiveSorted"><fmt:message key="resource.hub.ServerTypeTH"/>s</li>
    <c:forEach var="entry" items="${serverTypes}">
	    <c:set var="server" value="${entry.key}"/>
    	<c:set var="services" value="${entry.value}"/>
    	
    	<c:if test="${server.virtual == false}">
			<li class="tableRow<c:if test="${(loopStatus.index % 2) != 0}"> tableRowOdd</c:if>"
			    style="position:relative;">
				<div class="tableCell" style="">
      				<html:link action="/ResourceHub">
      					<html:param name="ff" value="2" />
      					<html:param name="ft" value="2:${server.id}" />
      					<c:out value="${server.name}"/>
      				</html:link>
      			</div>
	 			<div style="position:absolute;right:0;top:0;padding:2px;">
					<tiles:insert definition=".admin.config.DefaultsAction">
		       			<tiles:put name="typeName" value="server"/>
		       			<tiles:put name="aetid" value="2:${server.id}" />
	      			</tiles:insert>
				</div>
    		</li>
       		<c:forEach var="serviceType" items="${services}" varStatus="loopStatus">
				<li class="tableRow<c:if test="${(loopStatus.index % 2) != 0}"> tableRowOdd</c:if>"
				    style="position:relative;">
					<div class="tableCell indentArrowIcon" style="">
           				<html:link action="/ResourceHub">
           					<html:param name="ff" value="3" />
           					<html:param name="ft" value="3:${serviceType.id}" />
           					<c:out value="${serviceType.name}"/>
           				</html:link>
           			</div>
           			<div style="position:absolute;right:0;top:0;padding:2px;">
	   					<tiles:insert definition=".admin.config.DefaultsAction">
	       					<tiles:put name="typeName" value="service"/>
	       					<tiles:put name="aetid">3:<c:out value="${serviceType.id}"/></tiles:put>
	   					</tiles:insert>
	   				</div>
       			</li>
       		</c:forEach>
    	</c:if>
	</c:forEach>
</ul>
<br/><br/>
<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>