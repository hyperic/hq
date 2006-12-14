<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
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

<script language="JavaScript" type="text/javascript">
  var genHelp = "<hq:help context="false"/>";
</script>

<script src="<html:rewrite page="/js/rico.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/"/>diagram.js" type="text/javascript"></script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td rowspan="2" width="34%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr>                     
          <td class="logo">
            <html:link page="/Dashboard.do">
          <c:choose>
            <c:when test="${applicationScope.largeLogo}">           
                <html:img page="/customer/${applicationScope.largeLogoName}" width="225" height="31" alt="" border="0"/>
                <br>
                <html:img page="/images/cobrand_logo.gif" width="225" height="25" alt="" border="0"/>
            </c:when>
            <c:otherwise>
                <html:img page="/images/logo_large.gif" width="223" height="56" alt="" border="0"/>
            </c:otherwise>
          </c:choose>
            </html:link>
          </td>
          <td width="100%" class="logo"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
          <td class="logo"><html:img page="/images/logo_Image_large.jpg" width="225" height="56" alt="" border="0"/></td>
        </tr>
      </table>
    </td>
    <td width="20%" class="MastheadBgTop">
      <table width="250" border="0" cellspacing="0" cellpadding="0">
        <tr> 
          <td><html:img page="/images/spacer.gif" width="9" height="18" alt="" border="0"/></td>
          <td class="MastheadContent" nowrap><c:out value="${sessionScope.webUser.username}"/> - <html:link page="/Logout.do" styleClass="MastheadLink">Logout</html:link></td>
          <td><html:img page="/images/spacer.gif" width="35" height="18" alt="" border="0"/></td>
          <td class="MastheadContent" nowrap>
          </td>
          <td><html:img page="/images/spacer.gif" width="15" height="18" alt="" border="0"/></td>
        </tr>
      </table>
    </td>
    <td width="46%" height="18" class="MastheadBgTop">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" height="18" style="margin-right:20px;">
        <tr> 
          <td width="9" height="18"><html:img page="/images/spacer.gif" width="1" height="14" alt="" border="0"/></td>
          <td width="100%" class="MastheadContent"><fmt:message key="header.RecentAlerts"/></td>
           <td id="loading" style="display:none" style="float:right;margin-right:10px;">
		    <html:img page="/images/ajax-loader.gif" border="0" width="16" height="16" />
		   </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr> 
    <td height="38" class="MastheadBgBottom">
      <table width="250" border="0" cellspacing="4" cellpadding="0">
        <tr> 
          <td rowspan="99"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
          <td><html:link page="/Dashboard.do"><html:img page="/images/toolbox_Dashboard.gif" onmouseover="imageSwap(this, imagePath + 'toolbox_Dashboard', '_on')" onmouseout="imageSwap(this, imagePath + 'toolbox_Dashboard', '')" width="66" height="12" alt="" border="0"/></html:link></td>
          <td><html:link page="/ResourceHub.do"><html:img page="/images/toolbox_BrowseResources.gif" onmouseover="imageSwap(this, imagePath + 'toolbox_BrowseResources', '_on')" onmouseout="imageSwap(this, imagePath + 'toolbox_BrowseResources', '')" width="113" height="12" alt="Resource Hub" border="0"/></html:link></td>
        </tr>
        <tr>           
          <td><html:link href="" onclick="window.open(genHelp,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/toolbox_Help.gif" onmouseover="imageSwap(this, imagePath + 'toolbox_Help', '_on');" onmouseout="imageSwap(this, imagePath + 'toolbox_Help', '')" width="28" height="12" alt="" border="0"/></html:link></td>
          <td><html:link page="/Admin.do"><html:img page="/images/toolbox_Administration.gif" onmouseover="imageSwap(this, imagePath + 'toolbox_Administration', '_on');" onmouseout="imageSwap(this, imagePath + 'toolbox_Administration', '')" width="88" height="12" alt="" border="0"/></html:link></td>         
        </tr>
      </table>
    </td>
    <td height="38" class="MastheadBgBottom">
      <script language="JavaScript1.2">
      <!--
      var refreshCount = 0;
      
      function refreshAlerts() {
        ajaxEngine.sendRequest( 'getRecentAlerts' );
        refreshCount++;

        if (refreshCount < 30) {
          setRefresh();
        } else {
          top.location = "<html:rewrite action="/Logout"/>";
        }

      }

      function setRefresh() {
        setTimeout( "refreshAlerts()", 60*1000 );
      }

      function initRecentAlerts() {
        ajaxEngine.registerRequest( 'getRecentAlerts', '<html:rewrite page="/common/RecentAlerts.jsp"/>');
        ajaxEngine.registerAjaxElement('recentAlerts');
      }

      onloads.push( initRecentAlerts );
      onloads.push( refreshAlerts );

      //-->
      </script>
      <div id="recentAlerts"></div>
    </td>
  </tr>
  <tr> 
    <td colspan="3" class="MastheadBottomLine"><html:img page="/images/spacer.gif" width="1" height="2" alt="" border="0"/></td>
  </tr>
</table>
