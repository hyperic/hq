<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<!-- get the eid and mode here for the parent portal action and use that action instead of mastheadattach -->
<div style="display:none;">
<c:out value="${resourceViewTabAttachments}"></c:out> ---
<c:out value="${resourceViewTabAttachment.plugin.name}"></c:out>
<div style="padding:2px" id="SubTabSource">
<c:forEach var="attachment" items="${resourceViewTabAttachments}">
	<c:url var="attachmentUrl" value="/TabBodyAttach.do">
		<c:param name="id" value="${attachment.attachment.id}"/>
		<c:param name="mode" value="${param.mode}"/>
		<c:param name="eid" value="${param.eid}"/>
	</c:url>
	<c:choose>
    	<c:when test="${param.id eq attachment.attachment.id}">
    		<div style="padding:1px;border:1px solid rgb(255, 114, 20);margin:2px;width: 100px; float: left;text-align: center;">
    			<a href="${attachmentUrl}"><c:out value="${attachment.HTML}"/></a>
    		</div>
    	</c:when>
    	<c:otherwise>
    		<div style="padding:1px;border:1px solid gray;margin:2px;width: 100px; float: left;text-align: center;">
    			<a href="${attachmentUrl}"><c:out value="${attachment.HTML}"/></a>
    		</div>
    	</c:otherwise>
    </c:choose>
</c:forEach>
</div>
</div>
<c:choose>
<c:when test="${resourceViewTabAttachment ne null}">
	<div id="attachPointContainer" style="padding:4px;">
		<c:url var="attachUrl" context="/hqu/${resourceViewTabAttachment.plugin.name}" value="${resourceViewTabAttachment.path}">
			<c:param name="attachId" value="${param.id}" />
		</c:url>
		<c:import url="${attachUrl}"/>
	</div>
</c:when>
<c:when test="${empty resourceViewTabAttachments}">
	<div style="padding: 100px 0px; color: gray; font-size: 15px;text-align:center;">
	  No views are available for this resource
	</div>
</c:when>
<c:otherwise>
	<div class="viewSelectionNote">
	<img src="<html:rewrite page="/images/arrow_up_transparent.gif"/>"/><span>Please choose a view from the list above.</span>
	</div>
</c:otherwise>
</c:choose>
<jsu:script onLoad="true">
   	hqDojo.byId("SubTabTarget").innerHTML = hqDojo.byId("SubTabSource").innerHTML;
</jsu:script>