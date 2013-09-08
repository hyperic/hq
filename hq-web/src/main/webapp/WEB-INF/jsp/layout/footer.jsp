<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<div id="footerContent">
	<span id="currentTime">
		<c:set var="tmpTime"><%= java.lang.System.currentTimeMillis() %></c:set>
		<hq:dateFormatter value="${tmpTime}"/>
	</span>
	<span id="processUser">
		<c:set var="owner"><%= java.lang.System.getProperty("user.name") %></c:set>
		${owner}
	</span>
	<span id="aboutInfo">
		<a id="aboutLink"><fmt:message key="footer.HQ" /> <fmt:message key="footer.version" /> ${HQVersion}</a>
	</span>
	<span id="copyrightInfo">
		<fmt:message key="footer.Copyright" />
	</span>
	<span id="corporateSite">
		<a href="http://www.hyperic.com" target="_blank">www.hyperic.com</a>
	</span>
</div>
<div id="aboutDialogContent" class="dialog" style="display: none;">
  	<table cellpadding="2" cellspacing="0" border="0" width="305">
  		<tr>
  			<td class="displaylabel" rowspan="3">&nbsp;</td>
  			<td valign="top" class="displaysubhead" colspan="2" style="padding-top:5px;">
  				<br />
  				<fmt:message key="footer.version" /> <c:out value="${HQVersion}" />
  				<br />&nbsp;
  			</td>
  		</tr>
  		<tr>
  			<td valign="top" class="displaycontent" colspan="2"><span
  				class="displaylabel"><fmt:message key="footer.Copyright" /></span><fmt:message
  				key="about.Copyright.Content" /><br />
  			<br />
  			&nbsp;<br />
  			</td>
  		</tr>
  		<tr>
  			<td valign="top" class="displaycontent" colspan="2"><fmt:message
  				key="about.MoreInfo.Label" /><br />
  				<html:link href="https://www.vmware.com/support/pubs/vcenter-hyperic.html" target='_blank'>
  					<fmt:message key="about.MoreInfo.LinkSupport" />
  				</a><br />
  				<a href="http://forums.hyperic.org" target="about">
  					<fmt:message key="about.MoreInfo.LinkForums" />
  				</a><br />
  				&nbsp;
  			</td>
  		</tr>
  	</table>
</div>
<script src="<spring:url value="/js/footer.js" />" type="text/javascript"></script>
<script type="text/javascript">
	hqDojo.require("dijit.dijit");
  	hqDojo.require("dijit.Dialog");

  	hqDojo.ready(function(){
  		var aboutDialog = new hqDijit.Dialog({
        	id: 'about_popup',
            refocus: true,
            autofocus: false,
            title: "<fmt:message key="about.Title" />"
        }, "aboutDialogContent");
  		
  		hqDojo.connect(hqDojo.byId("aboutLink"), "onclick", function(e) {
  			hqDijit.byId("about_popup").show();
  		});
  		
  		var pinFooter = function() {
	  		var windowCoords = hqDojo.window.getBox();
	  		var contentCoords = hqDojo.position(hqDojo.byId("content"), true);
  			var footerCoords = hqDojo.position(hqDojo.byId("footer"), true);
  			var combinedContentHeight = contentCoords.y + contentCoords.h;
  			var diff = windowCoords.h - combinedContentHeight - footerCoords.h;
  			
  			if (diff > 0) {
  				hqDojo.style("footer", "marginTop", diff + "px");	  			
  			}
  		};
  		
  		hqDojo.connect(window, "onresize", pinFooter);
  		
  		pinFooter();
  	});
</script>
