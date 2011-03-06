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

<script>
	dojo.ready(function() {
		<c:if test="${empty cancelOnly}">
			dojo.connect(dojo.byId("okButton"), "onclick", function() {
				hyperic.form.mockLinkSubmit("ok.x", "1", "formButtonHiddenSubmitArea");
			});
			
			<c:if test="${empty noReset}">
				dojo.connect(dojo.byId("resetButton"), "onclick", function() {
					hyperic.form.mockLinkSubmit("reset.x", "1", "formButtonHiddenSubmitArea");
				});
			</c:if>
		</c:if>
		<c:if test="${empty noCancel}">
			dojo.connect(dojo.byId("cancelButton"), "onclick", function() {
				hyperic.form.mockLinkSubmit("cancel.x", "1", "formButtonHiddenSubmitArea");
			});
		</c:if>
	});
</script>

<!-- FORM BUTTONS -->
<div class="formButtonContainer">
	<input type="hidden" name="temp" value="temp" id="formButtonHiddenSubmitArea" />
		
	<c:if test="${empty cancelOnly}">
		<input id="okButton" type="button" class="button42" value="<fmt:message key="button.ok" />" />
			
		<c:if test="${empty noReset}">
			<input id="resetButton" type="button" class="button42 reset" value="<fmt:message key="button.reset" />" />
		</c:if>
	</c:if>
		
	<c:if test="${empty noCancel}">
		<input id="cancelButton" type="button" class="button42 cancel" value="<fmt:message key="button.cancel" />" />
	</c:if>
</div>