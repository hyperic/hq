<%@ page pageEncoding="UTF-8"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
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
<!--[if lte IE 7]>
<link rel=stylesheet href="<html:rewrite page="/css/iecss.css"/>" type="text/css">
 <![endif]-->
<script type="text/javascript">

	var djConfig = {
		isDebug : false,
		locale : "<%=request.getLocale().toString().substring(0,2)%>"
	};

</script>
<script src="<html:rewrite page="/js/dojo/dojo.js"/>" type="text/javascript"></script>
<script type="text/javascript">
	dojo.require("dojo.widget.Menu2");
</script>
<script src="<html:rewrite page="/js/rico.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/popup.js"/>" type="text/javascript"></script>
<script src="<html:rewrite page="/js/"/>diagram.js" type="text/javascript"></script>
<script language="JavaScript" type="text/javascript">
    var help = "<hq:help/>";

    function getUpdateStatus(opt) {
        if (opt == "<fmt:message key="header.Acknowledge"/>") {
            var pars = "update=true";
            var updateUrl = 'Dashboard.do?';
            var url = updateUrl + pars;
            //window.location = url;
            new Ajax.Request(url, {method: 'post'});
            $('hb').innerHTML = '<html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/>'
        }
        menuLayers.hide();
    }
</script>
<div class="headerWrapper" style="background-image: url('<html:rewrite page="/images/HeaderBkgd6.gif"/>')">
    <div style="position:absolute;left:0px;top:1px;width:225px;height:60px;">
        <html:link action="/Dashboard">
            <c:choose>
                <c:when test="${applicationScope.largeLogo}">
                    <html:img page="/customer/${applicationScope.largeLogoName}" width="225" height="31" alt=""
                              border="0"/>

                    <html:img page="/images/cobrand_logo.gif" width="225" height="25" alt="" border="0"/>
                </c:when>
                <c:otherwise>
                    <html:img page="/images/newLogo18.gif" width="203" height="60" alt="" border="0"/>
                </c:otherwise>
            </c:choose>
        </html:link>
    </div>
    <div class="headRightWrapper">
        <div class="headTopNav">
            <div class="headAlertWrapper">
                <div class="recentText">
                    <fmt:message key="header.RecentAlerts"/>
                    :
                </div>
                <div id="recentAlerts"></div>
            </div>

            <div id="headUsrName">
                <c:choose>
                    <fmt:message key="header.User"/>
                    :
                    <c:when test="${useroperations['viewSubject']}">
                        <html:link page="/admin/user/UserAdmin.do?mode=view&u=${sessionScope.webUser.id}">
                            <c:out value="${sessionScope.webUser.username}"/>
                        </html:link>
                    </c:when>
                    <c:otherwise>
                        <c:out value="${sessionScope.webUser.username}"/>
                    </c:otherwise>
                </c:choose>
               <span style="padding-left:10px;font-size:0.95em;">
                <html:link action="/Logout">
                    <span style="color:#333333;">[</span>
                    <fmt:message key="admin.user.generalProperties.Logout"/>
                    <span style="color:#333333;">]</span>
                </html:link></span>
                <span style="padding-left:10px;font-size:0.95em;"> <html:link href=""
                                                                              onclick="helpWin=window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes');helpWin.focus();return false;">
                    <fmt:message key="common.label.Help"/>
                </html:link></span>
            </div>
        </div>

        <div class="headBotNav">
            <div dojoType="PopupMenu2" widgetId="submenu1">
                <div dojoType="MenuItem2" caption="<fmt:message key="header.Browse"/>"
                     onClick="location.href='<html:rewrite page="/ResourceHub.do"/>'"></div>
                <tiles:insert definition=".header.optional.tabs">
                    <tiles:put name="location" value="resources"/>
                </tiles:insert>
                <div dojoType="MenuItem2" caption="<fmt:message key=".dashContent.recentResources"/>"
                     submenuId="submenu3"></div>

            </div>

            <div dojoType="PopupMenu2" widgetId="submenu2">
                <tiles:insert definition=".header.optional.tabs">
                    <tiles:put name="location" value="tracking"/>
                </tiles:insert>
            </div>

            <div dojoType="PopupMenu2" widgetId="submenu3">
                <tiles:insert definition=".toolbar.recentResources"/>
            </div>

            <div dojoType="MenuBar2">
                <div dojoType="MenuBarItem2" caption="<fmt:message key="dash.home.PageTitle"/>"
                     onClick="location.href='<html:rewrite page="/Dashboard.do"/>'"></div>
                <div dojoType="MenuBarItem2" caption="<fmt:message key="dash.settings.Resources"/>"
                     submenuId="submenu1"></div>
                <div dojoType="MenuBarItem2" caption="<fmt:message key="header.Views"/>" submenuId="submenu2"></div>
                <div dojoType="MenuBarItem2" caption="<fmt:message key="admin.admin.AdministrationTitle"/>"
                     onClick="location.href='<html:rewrite page="/Admin.do"/>'"></div>
            </div>
            <div style="display:none;" id="loading">
                <html:img page="/images/ajax-loader.gif" border="0" width="16" height="16"/>
            </div>
            <c:if test="${not empty HQUpdateReport}">
                <div style="position:absolute;" id="hb">
                    <html:img page="/images/transmit2.gif" border="0" width="16" height="16"
                              onmouseover="menuLayers.show('update', event)" onmouseout="menuLayers.hide()"/>
                </div>
            </c:if>
        </div>

    </div>
</div>

<c:if test="${not empty HQUpdateReport}">
<div id="update" class="menu" style="z-index:15000000;border:1px solid black;padding-top:15px;padding-bottom:15px;font-weight:bold;font-size:12px;">
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
