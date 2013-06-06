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
<%@ page errorPage="/common/Error.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<html>
	<head>
		<tiles:insert attribute="head" />
		<title>
			<fmt:message key="${portal.name}">
				<c:if test="${not empty TitleParam}">
					<fmt:param value="${TitleParam}" />
				</c:if>
				<c:if test="${not empty TitleParam2}">
					<fmt:param value="${TitleParam2}" />
				</c:if>
			</fmt:message>
		</title>
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
		<tiles:insert attribute="header" />

		<div id="migContainer">
			<tiles:insert attribute='body' /> 
			<div id="footerContent" style="margin-top:0px;">
				<tiles:insert attribute="footer" />
			</div>
		</div>
	</body>
</html>
