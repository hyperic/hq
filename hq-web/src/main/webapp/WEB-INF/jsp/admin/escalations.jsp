<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div id="escalationList">

<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>



<fmt:message key="inform.config.escalation.scheme.newescalation.escalationexplanation"/>


	<div id="escalations">
	
    <table width="100%" cellpadding="0" cellspacing="0" class="TableBottomLine">
    <thead>
    <tr class="BlockTitle">
    	<th><fmt:message key="admin.escalation.escalationName"/></th>
        <th><fmt:message key="admin.escalation.actions"/></th>
        <th><fmt:message key="admin.escalation.alerts"/></th>
        <th></th>
    </tr>
    </thead>
    <tbody id="escalations">
    	<c:if test="${ empty escalationListUIBeanList}"> 
    		<td colspan="4"><fmt:message key="admin.config.message.noEscalations"/></td>
    	
    	</c:if>
    	
    	
    	<c:forEach items="${escalations}" var="row" >
    	<tr>
    		<td><a href="/app/admin/escalation/${row.escId}" ><c:out value="${row.escName}"></c:out></a></td>
    		<td><c:out value="${row.actionNum}"></c:out></td>
    		<td><c:out value="${row.alertNum}"></c:out></td>  
    		<td>
    			<form:form action="/app/admin/escalation/${row.escId}" method="delete">
    				<input type="submit" value='<fmt:message key="resource.common.button.delete"/>'/>
    			</form:form>
    		</td>  	
    	</tr>
    	
    	</c:forEach>

    </tbody>
    </table>
    </div>
    
    <form:form method="get" action="/app/admin/escalation">
    	<input type="submit" value="<fmt:message key='admin.escalation.newEscalation'/>" />
    </form:form>

<tiles:insert page="/admin/config/AdminHomeNav.jsp"/>
</div>
