<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://struts.apache.org/tags-html-el" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
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
<table width="100%" border="0" cellspacing="0" cellpadding="0">
	<tr>
		<td class="FooterBold" nowrap="nowrap" style="padding-left: 30px;" width="20%"><tiles:insert definition=".footer.current.time" /></td>
		<td class="FooterRegular" nowrap="nowrap" align="center" width="60%">
			<div id="aboutAnchor">
			  <a name="aboutLink" href="javascript:aboutDia.show()"><fmt:message key="footer.HQ" /> <fmt:message key="footer.version" /> <c:out value="${HQVersion}" /></a>
			</div>
		</td>
		<td class="FooterRegular" nowrap="nowrap" width="10%"><fmt:message key="footer.Copyright" /></td>
		<td class="FooterBold" nowrap="nowrap" style="padding-right: 30px;" width="10%"><a href="http://www.hyperic.com" target="_blank">www.hyperic.com</a></td>
	</tr>
</table>

<div id="about" class="dialog" style="display: none;">
  <table cellpadding="2" cellspacing="0" border="0" width="305">
  	<tr>
  		<td class="DisplayLabel" rowspan="3">&nbsp;</td>
  		<td valign="top" class="DisplaySubhead" colspan="2" style="padding-top:5px;">
  			<br />
  			<fmt:message key="footer.version" /> <c:out value="${HQVersion}" />
  			<br />
  			Build <c:out value="${HQBuild}" />
            <br />&nbsp;
  		</td>
  	</tr>
  	<tr>
  		<td valign="top" class="DisplayContent" colspan="2"><span
  			class="DisplayLabel"><fmt:message key="footer.Copyright" /></span><fmt:message
  			key="about.Copyright.Content" /><br />
  		<br />
  		&nbsp;<br />
  		</td>
  	</tr>
  	<tr>
  		<td valign="top" class="DisplayContent" colspan="2"><fmt:message
  			key="about.MoreInfo.Label" /><br />
  		<html:link href="https://www.vmware.com/support/pubs/hyperic-pubs.html" target='_blank'>
  			<fmt:message key="about.MoreInfo.LinkSupport" />
  		</html:link><br />
  		<html:link href="http://forums.hyperic.org" target="about">
  			<fmt:message key="about.MoreInfo.LinkForums" />
  		</html:link><br />
  		&nbsp;</td>
  	</tr>
  </table>
</div>

<jsu:importScript path="/js/effects.js" />
<jsu:importScript path="/js/footer.js" />
<jsu:script>
	hqDojo.require("dijit.dijit");
  	hqDojo.require("dijit.Dialog");

  	var aboutDia = null;
</jsu:script>
<jsu:script onLoad="true">
	aboutDia = new hqDijit.Dialog({
	    id: 'about_popup',
        refocus: true,
        autofocus: false,
        opacity: 0,
        title: "<fmt:message key="about.Title" />"
    }, hqDojo.byId('about'));

  	setFoot();
</jsu:script>
<jsu:writeJs />
