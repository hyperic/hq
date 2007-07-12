<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
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

<script src="<html:rewrite page="/js/rico.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/popup.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/"/>diagram.js" type="text/javascript"></script>
<script language="JavaScript" type="text/javascript">
  var help = "<hq:help/>";
          
 function getUpdateStatus(opt) {
   if (opt=="<fmt:message key="header.Acknowledge"/>") {
     var pars =  "update=true";
     var updateUrl = 'Dashboard.do?';
     var url = updateUrl + pars;
     //window.location = url;
       new Ajax.Request( url, {method: 'post'} );
       $('hb').innerHTML = '<html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/>'
      }
     menuLayers.hide();
   }


</script>
<table width="100%" border="0" cellspacing="0" cellpadding="0" style="border-top:2px solid #3399ff;border-bottom:2px solid #3399ff;" height="56">

<tr>
<td rowspan="2">
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr>
            <td class="logo" style="border-top:1px solid #60a5ea;border-bottom:1px solid #60a5ea;">
                <html:link action="/Dashboard">
                    <c:choose>
                        <c:when test="${applicationScope.largeLogo}">
                            <html:img page="/customer/${applicationScope.largeLogoName}" width="225" height="31" alt=""
                                      border="0"/>
                            <br>
                            <html:img page="/images/cobrand_logo.gif" width="225" height="25" alt="" border="0"/>
                        </c:when>
                        <c:otherwise>
                            <html:img page="/images/logo_large2.gif" width="223" height="54" alt="" border="0"/>
                        </c:otherwise>
                    </c:choose>
                </html:link>
            </td>
            <td class="logo" style="border-top:1px solid #60a5ea;border-bottom:1px solid #60a5ea;border-right:1px solid #60a5ea;">
                <html:img page="/images/logo_Image_large4.jpg" width="175" height="54" alt="" border="0"/>
            </td>
        </tr>
    </table>
</td>
<td valign="top" width="100%">
    <table border="0" cellpadding="0" cellspacing="0" height="100%" width="100%">
        <tr>
            <td class="MastheadBgTop" colspan="5" nowrap style="border-bottom:1px solid #ffffff;" height="34">

                <c:if test="${not empty HQUpdateReport}">
                 <div style="position:absolute;top:10px;right:30px;" id="hb">
		        <html:img page="/images/transmit.gif" border="0" width="16" height="16" onmouseover="menuLayers.show('update', event)" onmouseout="menuLayers.hide()"/></div>
                </c:if>

                 <div style="display:none;position:absolute;top:10px;right:10px;" id="loading">
		        <html:img page="/images/ajax-loader.gif" border="0" width="16" height="16" /></div>
		        <div style="clear:both;"></div>
                <table border="0" cellspacing="0" cellpadding="0">
                    <tr>

                        <td class="MastheadContent" nowrap style="font-weight:bold;padding-right:35px;padding-left:9px;">
                            <c:out value="${sessionScope.webUser.username}"/>
                            &nbsp;-&nbsp;
                            <html:link action="/Logout"><fmt:message key="admin.user.generalProperties.Logout"/></html:link>
                        </td>
                        <td height="18" class="MastheadBgTop" nowrap>
                        <table border="0" cellspacing="0" cellpadding="0" height="20">
                           <tr>

                        <td class="MastheadContent" nowrap style="font-weight:bold;padding-left:9px;">
                            <fmt:message key="header.RecentAlerts"/>:
                        </td>
                        <td>
                            <div id="recentAlerts" style="font-weight:bold;color:#ffffff;overflow:hidden;width:100%"></div>
                        </td>
                       </tr>
                    </table>
                </td>
          </tr>
         </table>
       </td>
    </tr>
    <tr>
            <td class="MastheadBgBottom" style="padding-top:0px" align="left" colspan="5">
                <table border="0" cellspacing="0" cellpadding="0" id="navigationTbl" height="100%">
                    <tr>
                        <td style="padding-left:10px;" class="mainNavText" nowrap onmouseover="this.style.backgroundColor='#ffffff';" onmouseout="this.style.backgroundColor='#DBE3F6';">
                            <html:link page="/Dashboard.do"><span
                                    style="padding-right:10px;"><fmt:message key="dash.home.PageTitle"/></span></html:link>
                        </td>
                        <td style="width:2px;background-color:#ffffff;"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
                        <td class="mainNavText" nowrap onmouseover="this.style.backgroundColor='#ffffff';" onmouseout="this.style.backgroundColor='#DBE3F6';">
                            <html:link page="/ResourceHub.do"><span style="padding-right:10px;padding-left:10px;"><fmt:message key="resource.hub.ResourceHubPageTitle"/></span></html:link>
                        </td>
                        <td style="width:2px;background-color:#ffffff;"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>

                    <c:if test="${not empty mastheadAttachments}">
                      <c:forEach var="attachment" items="${mastheadAttachments}">
                        <td class="mainNavText" nowrap onmouseover="this.style.backgroundColor='#ffffff';" onmouseout="this.style.backgroundColor='#DBE3F6';">
                            <html:link action="/mastheadAttach" paramId="id" paramName="attachment" paramProperty="id"><span style="padding-right:10px;padding-left:10px;"><c:out value="${attachment.view.description}"/></span></html:link>
                        </td>
                          <td style="width:2px;background-color:#ffffff;"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>

                      </c:forEach>
                    </c:if>
                        <td class="mainNavText" nowrap onmouseover="this.style.backgroundColor='#ffffff';" onmouseout="this.style.backgroundColor='#DBE3F6';">
                            <html:link action="/Admin"><span style="padding-right:10px;padding-left:10px;"><fmt:message key="admin.admin.AdministrationTitle"/></span></html:link>
                        </td>
                        <td style="width:2px;background-color:#ffffff;"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>

                        <td class="mainNavText" onmouseover="this.style.backgroundColor='#ffffff';" onmouseout="this.style.backgroundColor='#DBE3F6';">
                        <a href="." onclick="toggleMenu('recent');return false;"><span
                                id="recentImg" style="padding-right:10px;padding-left:10px;"><fmt:message key=".dashContent.recentResources"/></span></a>
                            <div style="clear: all;"></div>
                            <tiles:insert definition=".toolbar.recentResources"/>
                        </td>
                        <td style="width:2px;background-color:#ffffff;"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
                         <td class="mainNavText" nowrap onmouseover="this.style.backgroundColor='#ffffff';" onmouseout="this.style.backgroundColor='#DBE3F6';">
                            <html:link href=""
                                       onclick="helpWin=window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes');helpWin.focus();return false;">
                                <span style="padding-right:10px;padding-left:10px;"><fmt:message key="common.label.Help"/></span></html:link>
                        </td>
                    </tr>

                </table>
            </td>
        </tr>
    </table>
    </td>
</tr>

</table>
<c:if test="${not empty HQUpdateReport}">
<div id="update" class="menu" style="border:1px solid black;padding-top:15px;padding-bottom:15px;font-weight:bold;font-size:12px;">
<c:out value="${HQUpdateReport}" escapeXml="false"/>
    <form name="updateForm" action="">
        <div style="text-align:center;padding-left:15px;padding-right:15px;"><input type="button" value="<fmt:message key="header.RemindLater"/>" onclick="getUpdateStatus(this.value);"><span style="padding-left:15px;"><input type="button" value="<fmt:message key="header.Acknowledge"/>" onclick="getUpdateStatus(this.value);"></span>
        </div>

    </form>
</div>
</c:if>

<script language="JavaScript1.2">
      <!--
      var refreshCount = 0;
      var autoLogout = true;
                                                                                    
      function refreshAlerts() {
        refreshCount++;

        new Ajax.Request('<html:rewrite page="/common/RecentAlerts.jsp"/>',
                         {method: 'get', onSuccess:showRecentAlertResponse});
      }

      function showRecentAlertResponse(originalRequest) {
        if (originalRequest.responseText.indexOf('recentAlertsText') > 0) {
          $('recentAlerts').innerHTML = originalRequest.responseText;
        }
        else {
          refreshCount = 31;
        }

        if (refreshCount < 30) {
          setTimeout( "refreshAlerts()", 60*1000 );
        } else if (autoLogout) {
          top.location.href = "<html:rewrite action="/Logout"/>";
        }
      }

      onloads.push( refreshAlerts );
      //-->
      </script>
