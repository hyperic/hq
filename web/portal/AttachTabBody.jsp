<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>

<!-- get the eid and mode here for the parent portal action and use that action instead of mastheadattach -->
<div style="display:none;">
<div style="padding:2px" id="SubTabSource">
<c:forEach var="attachment" items="${mastheadAttachments}">
    <c:choose>
    <c:when test="${param.id eq attachment.id}">
    <div style="padding:1px;border:1px solid rgb(255, 114, 20);margin-right:2px;width: 100px; float: left;text-align: center;"><a href="<html:rewrite page="/TabBodyAttach.do?id=${attachment.id}&mode=${param.mode}&eid=${param.eid}"/>"><c:out value="${attachment.view.description}"/></a></div>
    </c:when>
    <c:otherwise>
    <div style="padding:1px;border:1px solid gray;margin-right:2px;width: 100px; float: left;text-align: center;"><a href="<html:rewrite page="/TabBodyAttach.do?id=${attachment.id}&mode=${param.mode}&eid=${param.eid}"/>"><c:out value="${attachment.view.description}"/></a></div>
    </c:otherwise>
    </c:choose>
</c:forEach>
</div>
</div>
<c:choose>
<c:when test="${attachment ne null}">
	<div id=attachPointContainer style="padding:4px;">
		<c:url var="attachUrl" context="/hqu/${attachment.plugin.name}" value="${attachment.path}"/>
		<c:import url="${attachUrl}"/>
	</div>
</c:when>
<c:when test="${mastheadAttachments eq null}">
	<div style="padding: 100px 0px; color: gray; font-size: 14px;text-align:center;">
	  No views are available for this resource
	</div>
</c:when>
<c:otherwise>
	<div style="padding: 5px 0px 0px 59px; color: gray; font-size: 14px;">
	<img src="<html:rewrite page="/images/arrow_up.png"/>"/><span>Please choose a view from the list above.</span>
	</div>
</c:otherwise>
</c:choose>

<script type="text/javascript">
dojo.addOnLoad(function(){
    dojo.byId("SubTabTarget").innerHTML = dojo.byId("SubTabSource").innerHTML;
});
</script>
