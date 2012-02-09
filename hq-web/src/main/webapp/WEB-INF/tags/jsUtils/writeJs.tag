<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:forEach items="${fn:split(jsIncludesList, '|')}" var="jsInclude">
<script type="text/javascript" src="<c:url value="${jsInclude}" />"></script>
</c:forEach>
<script type="text/javascript">
	${jsContentBlocks}
</script>
<script type="text/javascript">
	hqDojo.ready(function() {
		${jsOnLoadContentBlocks}
	});
</script>