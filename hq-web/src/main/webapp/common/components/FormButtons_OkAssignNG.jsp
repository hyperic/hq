<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
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


<%-- @param okAssignOnly a flag indicating to only display the okAssign, 
            and no ok button --%>

<tiles:importAttribute name="cancelOnly" ignore="true"/>
<tiles:importAttribute name="okAssignBtn" ignore="true"/>
<tiles:importAttribute name="okAssignOnly" ignore="true"/>
<tiles:importAttribute name="cancelAction" ignore="true"/>
<tiles:importAttribute name="resetAction" ignore="true"/>

<jsu:script>
  	var isButtonClicked = false;

  	function checkSubmit() {
    	if (isButtonClicked) {
      		alert('<fmt:message key="error.PreviousRequestEtc"/>');
      		return false;
    	}
  	}  
</jsu:script>

<c:if test="${not empty cancelAction}">
	<c:url var="cancelRedirect" value="${cancelAction}.action">
	  <c:if test="${not empty userId}">
		<c:param name="u" value="${userId}"/>
	  </c:if>
	  <c:if test="${not empty roleId}">
		<c:param name="r" value="${roleId}"/>
	  </c:if>
	</c:url>
</c:if>
<c:if test="${not empty resetAction}">
	<c:url var="resetRedirect" value="${resetAction}.action">
	  <c:if test="${not empty userId}">
		<c:param name="u" value="${userId}"/>
	  </c:if>
	  <c:if test="${not empty roleId}">
		<c:param name="r" value="${roleId}"/>
	  </c:if>
	</c:url>
</c:if>

<!-- FORM BUTTONS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="buttonTable">
  <tr>
    <td colspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="10" border="0"/></td>
  </tr>
  <tr>
    <td colspan="2" class="ToolbarLine"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td colspan="2"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="10" border="0"/></td>
  </tr>
  <tr align=left valign=bottom>
    <td width="20%">&nbsp;</td>
    <td width="80%">
      <table width="100%" cellpadding="0" cellspacing="7" border="0">
        <tr>
<c:if test="${empty cancelOnly}">
 <c:if test="${empty okAssignOnly}">
          <td><input type="image" src='<s:url value="/images/fb_ok.gif" />' border="0" titleKey="FormButtons.ClickToOk" 
                name="okButton" id="okButton" value="submit"
				onmouseover="imageSwap(this, imagePath + 'fb_ok', '_over');" 
				onmouseout="imageSwap(this, imagePath +  'fb_ok', '');" 
				onmousedown="imageSwap(this, imagePath +  'fb_ok', '_down')" tabindex="11"/></td>
          <td><img src='<s:url value="/images/spacer.gif"/>' width="10" height="1" border="0"/></td>
 </c:if>
 <c:choose>
  <c:when test="${not empty okAssignBtn}">
        <td><input type="image" src='<s:url value="/images/%{okAssignBtn}.gif" />' border="0" titleKey="FormButtons.ClickToAssignToRoles" 
                name="okButton" id="okButton" value="submit"
                onmouseover="imageSwap(this, imagePath + '${okAssignBtn}', '_over');" 
                onmouseout="imageSwap(this, imagePath + '${okAssignBtn}', '');" 
                onmousedown="imageSwap(this, imagePath + '${okAssignBtn}', '_down')" tabindex="11"/></td>
  </c:when>
  <c:otherwise>
        <td><input type="image" src='<s:url value="/images/fb_okassign.gif" />' border="0" titleKey="FormButtons.ClickToAssignToRoles" 
                name="okButton" id="okButton" value="submit"
                onmouseover="imageSwap(this, imagePath + 'fb_okassign', '_over');" 
                onmouseout="imageSwap(this, imagePath + 'fb_okassign', '');" 
                onmousedown="imageSwap(this, imagePath + 'fb_okassign', '_down')" tabindex="11"/></td>
  </c:otherwise>
 </c:choose>
		  <td><input type="image" src='<s:url value="/images/fb_reset.gif" />' border="0" titleKey="FormButtons.ClickToReset" 
				name="ngReset" id="ngReset" onclick="javascript:return resetForm()" value="reset"
                onmouseover="imageSwap(this, imagePath + 'fb_reset', '_over');" 
                onmouseout="imageSwap(this, imagePath + 'fb_reset', '');" 
                onmousedown="imageSwap(this, imagePath + 'fb_reset', '_down')" tabindex="12"/></td>
</c:if>
          <td><input type="image" src='<s:url value="/images/fb_cancel.gif" />' border="0" titleKey="FormButtons.ClickToCancel" 
                name="ngCancel" id="ngCancel" onclick="javascript:return cancelForm()" value="cancel"
				onmouseover="imageSwap(this, imagePath + 'fb_cancel', '_over');" 
                onmouseout="imageSwap(this, imagePath + 'fb_cancel', '');" 
                onmousedown="imageSwap(this, imagePath + 'fb_cancel', '_down')" tabindex="13"/></td>
		  <td width="100%"><img src='<s:url value="/images/spacer.gif"/>' width="1" height="1" border="0"/></td>
        </tr>
      </table>
    </td>
  </tr>
</table>

<script>
	function cancelForm(){
		window.location = '${cancelRedirect}';
		return false;
	}
	function resetForm(){
		window.location = '${resetRedirect}';
		return false;
	}
</script>
<!-- /  -->
