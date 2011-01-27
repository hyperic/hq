<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<!DOCTYPE html>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html;charset=UTF-8" />
		<title>
			<fmt:message>
				<tiles:insertAttribute name="title"/>
			</fmt:message>
		</title>
		<link rel="icon" href="<spring:url value="/static/images/favicon.ico" />" />
		<link rel="stylesheet" type="text/css" href="<spring:url value="/static/css/layout.css" />" />
		<link rel="stylesheet" type="text/css" href="<spring:url value="/static/css/type.css" />" />
		<link rel="stylesheet" type="text/css" href="<spring:url value="/static/css/color.css" />" />
	</head>
	<body>
   		<div id="header">
   			<tiles:insertAttribute name="header" />
   		</div>
   		<div id="content">
			<tiles:insertAttribute name="content" />
		</div>
		<sec:authorize access="hasRole('ROLE_USER')">
			<div id="footer">
				<tiles:insertAttribute name="footer" />
			</div>
		</sec:authorize>
	</body>
</html>