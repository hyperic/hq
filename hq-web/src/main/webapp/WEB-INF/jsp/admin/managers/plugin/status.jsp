<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="message">
	<fmt:message key="${messageKey}">
		<fmt:param value="${params[0]}"> </fmt:param>
		<fmt:param value="${params[1]}"> </fmt:param>
		<fmt:param value="${params[2]}"> </fmt:param>
		<fmt:param value="${params[3]}"> </fmt:param>
	</fmt:message>
</c:set>
<html><head></head><body><textarea>{success:${success},message:'${message}'}</textarea></body></html>