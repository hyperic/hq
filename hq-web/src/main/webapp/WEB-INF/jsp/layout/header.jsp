<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>

<div id="headerLogo">
	<a href="<html:rewrite action="/Dashboard" />">
		<img alt="<fmt:message key="header.Dashboard" />" src="<spring:url value="/static/images/hqlogo.jpg"/>" border="0" />
	</a>
</div>
<div id="headerLinks">
	<ul>
		<sec:authorize access="hasRole('ROLE_USER')">
			<li>
				<span><fmt:message key="header.Welcome"/></span>
				<a href="<html:rewrite action="/admin/user/UserAdmin" />?mode=view&u=${sessionScope.webUser.id}">
	            	${sessionScope.webUser.firstName}
				</a>
			</li>
			<li>
				<a id="signOutLink" href="<spring:url value="/j_spring_security_logout" />" title="<fmt:message key="header.SignOut" />">
					<fmt:message key="header.SignOut" />
				</a>
			</li>
		</sec:authorize>
		<li>
			<a id="screencastLink" href="http://www.hyperic.com/demo/screencasts.html" target="_blank" title="<fmt:message key="header.Screencasts" />">
				<fmt:message key="header.Screencasts" />
			</a>
		</li>
		<li>
			<a id="helpLink" href="http://support.hyperic.com/confluence/display/DOC/" target="_blank" title="<fmt:message key="header.Help" />">
				<fmt:message key="header.Help" />
			</a>
		</li>
	</ul>
</div>
<sec:authorize access="hasRole('ROLE_USER')">
	<div id="headerTabs">
		<ul>
			<li id="dashboardTab">
				<a href="<html:rewrite action="/Dashboard" />">
					<fmt:message key="header.dashboard"/>
				</a>
			</li>
	        <li id="resourceTab">
	        	<a href="<html:rewrite action="/ResourceHub" />">
	        		<fmt:message key="header.resources"/>
	        	</a>
	           	<ul>
	               	<li>
	               		<a href="<html:rewrite action="/ResourceHub" />">
	               			<fmt:message key="header.Browse"/>
	               		</a>
	               	</li>
	                	
	               	
	               	<li>
	               		<fmt:message key=".dashContent.recentResources"/>
			        </li>
				</ul>
			</li>
	        <li id="analyzeTab">
				<fmt:message key="header.analyze"/>
	        </li>
		    <li id="adminTab">
		    	<a href="<html:rewrite action="/Admin" />">
		    		<fmt:message key="header.admin"/>
		    	</a>
		   	</li>
		</ul>
	</div>
</sec:authorize>