<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div id="escalationScheme">
	<a href="/app/admin/escalations"><fmt:message key="admin.escalation.link.escalationList"/> </a>
	<ul>
		<li>
			<fmt:message key="common.label.Name"/> 
			<c:out value="${escalationForm.escalationName}"/>
		</li>
		<li>
			<fmt:message key="common.label.Description"/> 
			<c:out value="${escalationForm.description}"/>		
		</li>
		<li>
		    <fmt:message key="alert.config.escalation.acknowledged"/>
		    <c:if test="${escalationForm.pauseAllowed}">
		    	<fmt:message key="alert.config.escalation.allow.pause"/>
		    	<c:out value="${}"/>
		    	<fmt:message key="alert.config.escalation.allow.pause.indefinitely"/>
		    </c:if>
		    <c:if test="${not escalationForm.pauseAllowed}">
		    	<fmt:message key="alert.config.escalation.allow.continue"/>
		    </c:if>
		</li>
		<li>
			<fmt:message key="alert.config.escalation.state.change"/>
		 	<c:if test="${escalationForm.notifyAll}">
			<fmt:message key="alert.config.escalation.state.change.notify.all"/>
			</c:if>
			<c:if test="${not escalationForm.notifyAll}">
			<fmt:message key="alert.config.escalation.state.change.notify.previous"/>
			</c:if>
		</li>
		<li>
			<fmt:message key="alert.config.escalation.state.ended"/>
			<c:if test="${escalationForm.repeat}">
			<fmt:message key="alert.config.escalation.state.ended.repeat"/>
			</c:if>
			<c:if test="${not escalationForm.repeat}">
			<fmt:message key="alert.config.escalation.state.ended.stop"/>
			</c:if>
			
		</li>
	</ul>
</div>