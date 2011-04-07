<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="message">
	<fmt:message key="${messageKey}"/>
</c:set>
<html><head></head><body><textarea>{success:${success},message:'${message}'}</textarea></body></html>