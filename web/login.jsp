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
		<link rel="icon" href="/images/4.0/icons/favicon.ico" />
		<link rel="stylesheet" type="text/css" href="/css/HQ_40.css" />
		<link rel="stylesheet" type="text/css" href="/js/dojo/1.1/dijit/themes/tundra/tundra.css" />
		<link rel="stylesheet" type="text/css" href="/js/dojo/1.1/dojo/resources/dojo.css" />
	</head>
	<body>
		<div id="header">
    		<div id="headerLogo" title="Home" onclick="location.href='<html:rewrite action="/Dashboard" />'">&nbsp;</div>
    		<!-- 
    		<div id="headerLinks">
        		<ul>
        			<li>
        				<html:link href="javascript:void(0)" onclick="tutorialWin=window.open('http://www.hyperic.com/demo/screencasts.html','tutorials','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes');tutorialWin.focus();return false;"><fmt:message key="header.Screencasts"/></html:link>
        			</li>
        			<li>
        				<html:link href="javascript:void(0)" onclick="helpWin=window.open((typeof help != 'undefined' ? help : '<hq:help/>'),'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes');helpWin.focus();return false;"><fmt:message key="header.Help"/></html:link>		
        			</li>
        		</ul>
    		</div>
    		-->
    	</div>
    	<div class="loginPanel">
    		<form name='f' action='/j_spring_security_check' method='POST'>
    			<div class="fieldsetTitle">
    				<fmt:message key="login.signin.message" />
    			</div>
				<div class="fieldsetNote">
					<fmt:message key="login.signin.instructions" />
				</div>
				<fieldset>
					<c:if test="${not empty param.authfailed}">
						<p>
							<div class="msgPanel msgError">
								<fmt:message key="login.info.bad" />
							</div>
						</p>
					</c:if>
					<table>
						<tr>
							<td>
								<label for="j_username"><fmt:message key="login.field.username" /></label>	
							</td>
							<td>
								<input type="text" id="j_username" name="j_username" value="" />	
							</td>
						</tr>
						<tr>
							<td>
								<label for="j_password"><fmt:message key="login.field.password" /></label>	
							</td>
							<td>
								<input type="password" id="j_password" name="j_password" />	
							</td>
						</tr>
						<tr>
							<td colspan="2" align="right">
								<input name="submit" type="submit" class="button42" value="<fmt:message key="login.signin" />" />
							</td>
						</tr>
					</table>
				</fieldset>
    		</form>
    	</div>
	</body>
</html>