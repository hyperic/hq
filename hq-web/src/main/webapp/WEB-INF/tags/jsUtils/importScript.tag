<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ attribute name="path" required="true"%>
<c:if test="empty jsIncludesList">
	<c:set var="jsIncludesList" scope="request" value="${path}" />
</c:if>

<c:if test="${not fn:contains(jsIncludesList, path)}">
	<c:set var="jsIncludesList" value="${jsIncludesList}|${path}" scope="request" />
</c:if>