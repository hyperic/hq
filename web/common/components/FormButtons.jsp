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


<tiles:importAttribute name="addToList" ignore="true"/>
<tiles:importAttribute name="cancelOnly" ignore="true"/>
<tiles:importAttribute name="noReset" ignore="true"/>
<tiles:importAttribute name="noCancel" ignore="true"/>

<script  type="text/javascript">
  var isButtonClicked = false;
  
  function checkSubmit() {
    if (isButtonClicked) {
      alert('<fmt:message key="error.PreviousRequestEtc"/>');
      return false;
    }
  }
</script>

<!-- FORM BUTTONS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="buttonTable">
  <tr>
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="10" border="0"/></td>
  </tr>
  <tr>
    <td colspan="3" class="ToolbarLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="3"><html:img page="/images/spacer.gif" width="1" height="10" border="0"/></td>
  </tr>
  <tr align=left valign=bottom>
<c:choose>
  <c:when test="${not empty addToList}">
    <td width="50%">&nbsp;</td>
    <td><html:img page="/images/spacer.gif" width="50" height="1" border="0"/></td>
    <td width="50%">
  </c:when>
  <c:otherwise>
    <td width="20%">&nbsp;</td>
		<td><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    <td width="80%">
  </c:otherwise>
</c:choose>
          <input type="hidden" name="temp" value="temp" id="formButtonHiddenSubmitArea"/>
<c:if test="${empty cancelOnly}">
          <a class="buttonGreen" href="javascript:hyperic.form.mockLinkSubmit('ok.x', '1', 'formButtonHiddenSubmitArea');"><span><fmt:message key="button.ok"/></span></a>
<c:if test="${empty noReset}">
          <a class="buttonGray" href="javascript:hyperic.form.mockLinkSubmit('reset.x', '1', 'formButtonHiddenSubmitArea');"><span><fmt:message key="button.reset"/></span></a>
</c:if>          
</c:if>
<c:if test="${empty noCancel}">
          <a class="buttonGray" href="javascript:hyperic.form.mockLinkSubmit('cancel.x', '1', 'formButtonHiddenSubmitArea');"><span><fmt:message key="button.cancel"/></span></a>
</c:if>
    </td>
  </tr>
</table>
<!-- /  -->
