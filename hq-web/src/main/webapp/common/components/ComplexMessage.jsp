<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
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
  
  Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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


<tiles:importAttribute name="complexMsg"/>

<c:forEach items="${complexMsg}" var="elt">

  <c:choose>

    <c:when test="${elt.url == null}">

      <c:choose>
        <c:when test="${elt.mouseover == null}">

          <%-- no url, no mouseover --%>
          <fmt:message key="${elt.text}">
            <c:forEach items="${elt.params}" var="p">
              <fmt:param value="${p}"/>
            </c:forEach>
          </fmt:message>

        </c:when>

        <c:otherwise>
          <%-- no url, has mouseover --%>
          <html:link href="#" onclick="return false;" styleClass="ListCellPopup1">
            <fmt:message key="${elt.text}">
              <c:forEach items="${elt.params}" var="p">
                <fmt:param value="${p}"/>
              </c:forEach>
            </fmt:message>
            <span><c:out value="${elt.mouseover}"/></span>
          </html:link>
        </c:otherwise>
      </c:choose>

    </c:when>

    <c:otherwise>

      <c:choose>
        <c:when test="${elt.mouseover == null}">

          <%-- no mouseover, has url --%>
          <html:link page="${elt.url}">
            <fmt:message key="${elt.text}">
            <c:forEach items="elt.params" var="p">
              <fmt:param value="${p}"/>
            </c:forEach>
            </fmt:message>
          </html:link>

        </c:when>

        <c:otherwise> 

          <%-- has url and mouseover --%>
          <html:link page="${elt.url}" styleClass="ListCellPopup3">
            <fmt:message key="${elt.text}">
            <c:forEach items="elt.params" var="p">
              <fmt:param value="${p}"/>
            </c:forEach>
            </fmt:message>
            <span><c:out value="${elt.mouseover}"/></span>
          </html:link>

        </c:otherwise>
      </c:choose>

   </c:otherwise>

  </c:choose>

</c:forEach>
