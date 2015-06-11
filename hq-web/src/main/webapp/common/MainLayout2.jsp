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
<%@ page pageEncoding="UTF-8"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ page errorPage="/common/Error2.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

<tiles:importAttribute name="content" ignore="true" scope="request"/>
<tiles:importAttribute name="titleKey" ignore="true" scope="request"/>
<tiles:importAttribute name="title" ignore="true" scope="request"/>
<tiles:importAttribute name="addFullName" ignore="true" scope="request"/>

<html>
	<head>
		<tiles:insertAttribute name="head" />
		<title>
				<s:if test="%{#attr.title != null }">
					<fmt:message key="${title}" />
				</s:if>
				<s:elseif test="%{#attr.titleKey != null}">
					<fmt:message key="${titleKey}" />
				</s:elseif>
				<s:else>
					<fmt:message key="${portal.name}">
						<c:if test="${not empty TitleParam}">
							<fmt:param value="${TitleParam}" />
						</c:if>
						<c:if test="${not empty TitleParam2}">
							<fmt:param value="${TitleParam2}" />
						</c:if>
					</fmt:message>
				</s:else>
		</title>
		<jsu:importScript path="/js/requests.js" />
		<jsu:script>
        	var onloads = [];

            function initOnloads() {
        	    if (arguments.callee.done) return;

                arguments.callee.done = true;

                if(typeof(_timer)!="undefined") clearInterval(_timer);

                for ( var i = 0 ; i < onloads.length ; i++ )
             		onloads[i]();
	        };
    	</jsu:script>
		<jsu:script onLoad="true">	
			initOnloads();
		</jsu:script>    	
	</head>
	<body style="background-color: #FFFFFF;" class="tundra" debug="true">
		<tiles:insertAttribute name="header" />

		<div id="migContainer" style="padding-left:25px">
			<tiles:insertAttribute name="body" /> 
			<div id="footerContent" style="margin-top:0px;">
				<tiles:insertAttribute name="footer" />
			</div>
		</div>
	</body>
</html>
