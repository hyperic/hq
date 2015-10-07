<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib tagdir="/WEB-INF/tags/jsUtils" prefix="jsu" %>
<%--
  NOTE: This copyright does *not* cover user programs that use Hyperic
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004-2011], VMware, Inc.
  This file is part of Hyperic.
  
  Hyperic is free software; you can redistribute it and/or modify
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
<tiles:importAttribute name="cancelAction" ignore="true"/>
<tiles:importAttribute name="resetAction" ignore="true"/>
<tiles:importAttribute name="resetParam" ignore="true"/>
<tiles:importAttribute name="resetValue" ignore="true"/>

<jsu:script>
  	var isButtonClicked = false;
  
  	function checkSubmit() {
    	if (isButtonClicked) {
      		alert('<fmt:message key="error.PreviousRequestEtc"/>');
      		return false;
    	}
  	}
</jsu:script>
<jsu:script onLoad="true">
  
	<c:if test="${empty cancelOnly}">
		// it is possible for this template to be used multiple times
		// on the same page, so query for all possible occurrences of the id
		// (although all ids on a page should really be unique to be XHTML-compliant).
			
		hqDojo.query(".button42").forEach(function(e){				
			if (e.id == 'okButton' && e.onclick == null) {
				e.onclick = function() {
					hyperic.form.mockLinkSubmit("ok.x", "1", "formButtonHiddenSubmitArea");
				};
			}

			<c:if test="${empty noReset}">
				if (e.id == 'resetButton' && e.onclick == null) {
					e.onclick = function() {
						hyperic.form.mockLinkSubmit("reset.x", "1", "formButtonHiddenSubmitArea");							
					};
				}
			</c:if>
		});
	</c:if>
	<c:if test="${empty noCancel}">
		hqDojo.query(".button42").forEach(function(e){
			if (e.id == 'cancelButton' && e.onclick == null) {
				e.onclick = function() {
					hyperic.form.mockLinkSubmit("cancel.x", "1", "formButtonHiddenSubmitArea");
				};
			}
		});
	</c:if>
</jsu:script>

<c:if test="${not empty cancelAction}">
	<c:url var="cancelRedirect" value="${cancelAction}.action">
	  <c:if test="${not empty userId}">
		<c:param name="u" value="${userId}"/>
	  </c:if>
	  <c:if test="${not empty roleId}">
		<c:param name="r" value="${roleId}"/>
	  </c:if>
	  <c:if test="${not empty param.rid}">
		<c:param name="rid" value="${param.rid}"/>
	  </c:if>
	  <c:if test="${not empty param.type}">
		<c:param name="type" value="${param.type}"/>
	  </c:if>
	  <c:if test="${not empty param.eid}">
		<c:param name="eid" value="${param.eid}"/>
	  </c:if>
	  <c:if test="${not empty param.appSvcId}">
		<c:param name="appSvcId" value="${param.appSvcId}"/>
	  </c:if>
	  <c:if test="${not empty param.aetid}">
		<c:param name="aetid" value="${param.aetid}"/>
	  </c:if>
	  <c:if test="${not empty param.ad}">
		<c:param name="ad" value="${param.ad}"/>
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
	  <c:if test="${not empty param.rid}">
		<c:param name="rid" value="${param.rid}"/>
	  </c:if>
	  <c:if test="${not empty param.type}">
		<c:param name="type" value="${param.type}"/>
	  </c:if>
	  <c:if test="${not empty param.eid}">
		<c:param name="eid" value="${param.eid}"/>
	  </c:if>
	  <c:if test="${not empty resetParam and not empty resetValue}">
		<c:param name="${resetParam}" value="${resetValue}"/>
	  </c:if>
	  <c:if test="${not empty param.appSvcId}">
		<c:param name="appSvcId" value="${param.appSvcId}"/>
	  </c:if>
	  <c:if test="${not empty param.aetid}">
		<c:param name="aetid" value="${param.aetid}"/>
	  </c:if>
	  <c:if test="${not empty param.ad}">
		<c:param name="ad" value="${param.ad}"/>
	  </c:if>
	</c:url>
</c:if>

<!-- FORM BUTTONS -->
<div class="formButtonContainer">
	<input type="hidden" name="temp" value="temp" id="formButtonHiddenSubmitArea" />
		
  <c:if test="${empty cancelOnly}">
		<input id="okButton" type="button" class="button42" value="<fmt:message key='button.ok' />" />
			
		<c:if test="${empty noReset}">
			<input id="ngReset" type="button" class="button42 reset" onclick="resetForm()" value="<fmt:message key='button.reset' />" />
		</c:if>
	</c:if>
		
	<c:if test="${empty noCancel}">
		<input id="ngCancel" type="button" class="button42 cancel" onclick="cancelForm()" value="<fmt:message key='button.cancel' />" />
	</c:if>
</div>

<script>
	function cancelForm(){
		window.location = '${cancelRedirect}';
	}
	function resetForm(){
		window.location = '${resetRedirect}';
	}
</script>
