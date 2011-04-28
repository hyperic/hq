<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2007], Hyperic, Inc.
  This file is part of HQ.
  
  HQ is free software; you can redistribute it and/or modify
  it under the terms version 2 of the GNU General Public License as
  published by the Free Software Foundation. This program is distributed
  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE. See the GNU General Public License for more
  details.
  
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
  USA.
 --%>
<c:set var="location" scope="request">
 	<tiles:getAsString name="location"/>
</c:set>
 <c:choose>
	<c:when test="${location eq 'resources'}">
 	 	<c:set var="attachments" scope="request" value="${mastheadResourceAttachments}" />
 	</c:when>
 	<c:when test="${location eq 'tracking'}">
 	 	<c:set var="attachments" scope="request" value="${mastheadTrackerAttachments}" />
 	</c:when>
</c:choose>
<c:forEach var="attachment" items="${attachments}">
	<li>
 		<a href="<html:rewrite action="/mastheadAttach"><html:param name="typeId" value="${attachment.attachment.id}"/></html:rewrite>">
 			<c:out value="${attachment.HTML}"/>
 	 	</a>
 	</li>
</c:forEach>