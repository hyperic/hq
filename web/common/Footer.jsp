<%@ taglib uri="struts-html" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
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


<script src="<html:rewrite page="/js/effects.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/footer.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/window.js"/>" type="text/javascript"></script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td rowspan="99" class="PageTitle"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td colspan="2"><html:img page="/images/spacer.gif" width="1" height="60" alt="" border="0" styleId="footerSpacer"/></td>
  </tr>
  <tr> 
    <td rowspan="99" class="PageTitle" valign="top"><html:img page="/images/footer_corner.gif" width="8" height="8" alt="" border="0"/></td>
    <td width="100%"><html:img page="/images/spacer.gif" width="1" height="8" alt="" border="0"/></td>
  </tr>
  <tr class="PageTitle"> 
    <td>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr>           
          <!--<td class="FooterBold" nowrap>-->
          <tiles:insert definition=".footer.current.time"/>
          <!--</td>  -->
          <td class="FooterRegular" width="30">&nbsp;</td>
          <td class="FooterRegular" nowrap>
	          <div id="aboutAnchor" style="position:relative;">
		          <a name="aboutLink" href="javascript:about('<html:rewrite page="/common/"/>')"><fmt:message key="footer.HQ"/>
		          <fmt:message key="footer.version"/> <c:out value="${HQVersion}"/></a> <c:out value="${HQBuild}"/>
	          </div>
          </td>
          <td class="FooterRegular" width="30"></td>
          <td class="FooterSmall" nowrap></td>
          <td class="FooterRegular" width="100%" align="right"><fmt:message key="footer.Copyright"/></td>
          <td class="FooterBold" nowrap><a href="http://www.hyperic.com" target="_blank">www.hyperic.com</a></td>
          <td class="FooterBold" width="15">&nbsp;</td>
        </tr>
      </table>
    </td>
  </tr>
</table>

<div id="about" class="dialog" style="position: absolute; top: 0px; left: 0px; visibility: hidden;">
<table cellpadding="2" cellspacing="0" border="0" width="305">
  <tr class="PageTitleBar">
    <td width="1%" class="PageTitle" style="background-color: #EBEDF2"><html:img page="/images/spacer.gif" width="1" height="32" alt="" border="0"/></td>
    <td width="66%" class="PageTitle" style="background-color: #EBEDF2"><fmt:message key="about.Title"/></td>
    <td class="PageTitle" align="right" style="background-color: #EBEDF2"><html:link href="" onclick="window.open(genHelp + 'About+Hyperic+HQ','help','width=800,height=650,scrollbars=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/title_pagehelp.gif" width="20" height="20" alt="" border="0" hspace="10"/></html:link></td>
  </tr>
  <tr>
    <td class="DisplayLabel" rowspan="3">&nbsp;</td>
    <td valign="top" class="DisplaySubhead" colspan="2"><html:img page="/images/spacer.gif" width="1" height="5" border="0"/><br/>
    <fmt:message key="footer.version"/>
    <c:out value="${HQVersion}"/><br/>&nbsp;</td>
  </tr>
  <tr>
    <td valign="top" class="DisplayContent" colspan="2"><span class="DisplayLabel"><fmt:message key="footer.Copyright"/></span><fmt:message key="about.Copyright.Content"/><br/>
    <br/>&nbsp;<br/></td>
  </tr>
  <tr>
    <td valign="top" class="DisplayContent" colspan="2"><fmt:message key="about.MoreInfo.Label"/><br/>
    <html:link href="#" onclick="window.open('http://support.hyperic.com');"><fmt:message key="about.MoreInfo.LinkSupport"/></html:link><br/>
    <html:link href="#"  onclick="window.open('http://forums.hyperic.org');"><fmt:message key="about.MoreInfo.LinkForums"/></html:link><br/>
    &nbsp;</td>
  </tr>
  <tr>
    <td colspan="3">&nbsp;</td>
    </tr>
</table>
</div>

<script language="JavaScript" type="text/javascript">
  setFoot();
</script>

