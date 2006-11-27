<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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


<tiles:importAttribute name="availablePortlets"/>
<tiles:importAttribute name="wide"/>
<tiles:importAttribute name="portlets"/>

<script>
<c:choose>
<c:when test="${wide}">
  function isWide(portlet) {
</c:when>
<c:otherwise>
  function isNarrow(portlet) {
</c:otherwise>
</c:choose>
    <c:forEach var="portlet" items="${portlets}">
      if (portlet == '<c:out value="${portlet}"/>')
        return true;
    </c:forEach>
    return false;
  }
</script>

<c:choose>
<c:when test="${not empty availablePortlets }">
  <div id="addContentsPortlet<c:out value="${wide}"/>" class="effectsPortlet">
</c:when>
<c:otherwise>
  <div id="addContentsPortlet<c:out value="${wide}"/>" class="effectsPortlet" style="visibility: hidden">
</c:otherwise>
</c:choose>
<html:form action="/dashboard/AddPortlet">
<html:hidden property="wide" value="${wide}"/>
  <table cellpadding="0" cellspacing="0" border="0" width="100%">
    <tr valign="top">
      <td colspan="3" class="ToolbarLine"><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
    </tr>
    <tr valign="top">
      <td colspan="3"><html:img page="/images/spacer.gif" width="20" height="1" border="0"/></td>
    </tr>
    <tr>
      <td colspan="3" class="FormLabel"><fmt:message key="dash.home.FormLabel.AddContent"/></td>
    </tr>
    <tr>
      <td valign="center">
        <select name="portlet">
          <option value="bad" SELECTED><fmt:message key="dash.home.AddContent.select"/></option>
          <c:forEach var="portlet" items="${availablePortlets}" >                                                
             <option value="<c:out value="${portlet}"/>"><fmt:message key="${portlet}" /></option>
          </c:forEach>           
        </select>
      </td>
      <td>&nbsp;</td>
      <td width="100%"><html:image page="/images/dash_movecontent_add.gif" border="0" titleKey="FormButtons.ClickToOk" property="ok" onmouseover="imageSwap(this, imagePath + 'dash_movecontent_add-on', '');" onmouseout="imageSwap(this, imagePath +  'dash_movecontent_add', '');" onmousedown="imageSwap(this, imagePath +  'dash_movecontent_add-off', '')" /></td>
    </tr>
  </table>
</html:form>
</div>
