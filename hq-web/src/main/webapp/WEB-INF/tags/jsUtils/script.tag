<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="onLoad" required="false" type="java.lang.Boolean" %>
<c:choose>
	<c:when test="${onLoad}">
		<c:set var="jsOnLoadContentBlocks" scope="request">
			${jsOnLoadContentBlocks}
			<jsp:doBody />
		</c:set>	
	</c:when>
	<c:otherwise>
		<c:set var="jsContentBlocks" scope="request">
			${jsContentBlocks}
			<jsp:doBody />
		</c:set>
	</c:otherwise>
</c:choose>