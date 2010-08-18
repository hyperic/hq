<%--

    NOTE: This copyright does *not* cover user programs that use HQ
    program services by normal system calls through the application
    program interfaces provided as part of the Hyperic Plug-in Development
    Kit or the Hyperic Client Development Kit - this is merely considered
    normal use of the program, and does *not* fall under the heading of
     "derived work".

     Copyright (C) [2009-2010], VMware, Inc.
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
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html;charset=UTF-8" />
		<title><fmt:message key="login.title" /></title>
		<link rel="icon" href="<html:rewrite page="/images/4.0/icons/favicon.ico" />" />
		<link rel="stylesheet" type="text/css" href="<html:rewrite page="/css/HQ_40.css" />" />
	</head>
	<body style="text-align:center;">
		<div id="header">
    		<div id="headerLogo" title="Home" onclick="location.href='<html:rewrite action="/Dashboard" />'">&nbsp;</div>
    		<div id="headerLinks">
        		<ul>
        			<li>
        				<a id="screencastLink" href="#"><fmt:message key="header.Screencasts"/></a>
        			</li>
        			<li>
        				<a id="helpLink" href="#"><fmt:message key="header.Help"/></a>		
        			</li>
        		</ul>
    		</div>
    	</div>
	<div id="content">
		<div class="loginPanel">
			<form name="loginForm" action="<html:rewrite page="/j_spring_security_check" />" method="POST">
				<div class="fieldsetTitle"><fmt:message key="login.signin.message" /></div>
				<div class="fieldsetNote"><fmt:message key="login.signin.instructions" /></div>
				<fieldset>
					<c:if test="${not empty param.authfailed}">
						<p>
							<div class="msgPanel msgError">
								<c:choose>
									<c:when test="${not empty errorMessage}">
										<c:out value="${errorMessage}" />
									</c:when>
									<c:otherwise>
										<fmt:message key="login.error.login" />								
									</c:otherwise>
								</c:choose>
							</div>
						</p>
					</c:if>
					<div class="fieldRow">
						<label for="j_username"><fmt:message key="login.field.username" /></label> 
						<input style="width: 75%;" id="usernameInput" type="text" id="j_username" name="j_username" value="" />
					</div>
					<div class="fieldRow">
						<label for="j_password"><fmt:message key="login.field.password" /></label> 
						<input style="width: 75%;" id="passwordInput" type="password" id="j_password" name="j_password" />
					</div>
					<div class="submitButtonContainer">
						<input type="submit" name="submit" class="button42" value="<fmt:message key="login.signin" />" />
						<c:if test="${guestEnabled}">
							<div class="guestUserLinkContainer">
								<fmt:message key="login.or" />&nbsp;<a href="#" id="guestLoginLink" class="guestUser"><fmt:message key="login.signInAsGuest" /></a>
							</div>
						</c:if>
					</div>
				</fieldset>
			</form>
		</div>
	</div>
	<script src="<html:rewrite page="/js/dojo/1.1.2/dojo/dojo.js" />" type="text/javascript"></script>
		<script>
			dojo.addOnLoad(function() {
				var username = dojo.byId("usernameInput");
				var password = dojo.byId("passwordInput");

				dojo.connect(username, "onfocus", function(e) { dojo.addClass(e.target.parentNode, "active"); });
				dojo.connect(username, "onblur", function(e) { dojo.removeClass(e.target.parentNode, "active"); });

				dojo.connect(password, "onfocus", function(e) { dojo.addClass(e.target.parentNode, "active"); });
				dojo.connect(password, "onblur", function(e) { dojo.removeClass(e.target.parentNode, "active"); });

				username.focus();

				dojo.connect(dojo.byId("screencastLink"), "onclick", function() {
					var tutorialWin = window.open("http://www.hyperic.com/demo/screencasts.html",
		                      "tutorials",
		                      "width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes");
					tutorialWin.focus();
					return false;
				});

				dojo.connect(dojo.byId("helpLink"), "onclick", function() {
					var helpWin = window.open((typeof help != "undefined" ? help : "<hq:help/>"), 
			                  "help",
			                  "width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes");
					helpWin.focus();
  					return false;
				});

				<c:if test="${guestEnabled}">
					dojo.connect(dojo.byId("guestLoginLink"), "onclick", function() {
						var username = dojo.byId("usernameInput");
						var password = dojo.byId("passwordInput");
						username.value = "<c:out value="${guestUsername}" />";
	
						document.forms["loginForm"]["submit"].click();

						username.disabled = true;
						password.disabled = true;
					});
				</c:if>
			});
		</script>
	</body>
</html>