<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %> 
<%@ taglib uri="hq" prefix="hq" %> 
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

<hq:navMapSupported var="navMapSupported"/>
<c:if test="${navMapSupported}">

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr valign="top"> 
   <c:choose>
      <c:when test="${xlib}">
    <td class="PageTitleSmallText" colspan="2" style="padding-top: 6px;">
<script src="<html:rewrite page="/js/"/>effects.js" type="text/javascript"></script>
<c:set var="imageWidth" value="800"/>
<hq:navMap areasVar="mapAreas" areasSizeVar="mapAreasSize" imageWidth="${imageWidth}"/>

<map name="diagram">
<c:forEach var="mapArea" varStatus="status" items="${mapAreas}">
<c:url var="mapAreaUrl" value="/ResourceNav.do">

<%-- always set the default page, if the end page is not controllable,
     send the user to the default page.
--%>

<c:param name="defaultPage" value="/Resource.do"/>
<c:if test="${not empty currResourceUrl}">
    <c:param name="currentResType" value="${currResourceType}"/>
    <c:param name="currentMode" value="${currResourceMode}"/>
</c:if>
<c:forEach var="entityId" items="${mapArea.entityIds}">
<c:param name="eid" value="${entityId.appdefKey}"/>
</c:forEach>

  <c:choose>
    <c:when test="${mapArea.hasAutogrouptype}" >
      <c:param name="ctype" value="${mapArea.autogrouptype}" />
    </c:when>
    <c:when test="${mapArea.hasCtype}" >
      <c:param name="ctype" value="${mapArea.ctype}"/>
    </c:when>
  </c:choose>
</c:url>
<area shape="RECT" coords="<c:out value='${mapArea.x1}'/>,<c:out value='${mapArea.y1}'/>,<c:out value='${mapArea.x2}'/>,<c:out value='${mapArea.y2}'/>" href="<c:out value='${mapAreaUrl}'/>" alt="<c:out value='${mapArea.alt}'/>">
</c:forEach>
</map>

      <span onclick="toggleDiagram('diagramDiv');"><html:img imageName="navMapIcon" border="0" alt="" page="/images/icon_navmap.png"/></span>
      <div style="clear: all"></div>
      <div id="diagramDiv" style="position: absolute; display: none;">
        <span>
          <html:img imageName="navMapImage" page="/resource/NavMapImage?treeVar=${treeVar}&imageWidth=${imageWidth}" alt="" border="0" usemap="#diagram" />
        </span>
      </div>
    </td>
      </c:when>
      <c:otherwise>
    <td class="ErrorBlock" colspan="2">
      <fmt:message key="error.NoXLibInstalled"/>
    </td>
      </c:otherwise>
    </c:choose>
    <td class="PageTitleSmallText">&nbsp;</td>
  </tr>
</table>

</c:if>

