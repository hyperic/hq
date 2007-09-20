<%@ page pageEncoding="UTF-8"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-logic-el" prefix="logic" %>
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
  var help = "<hq:help/>";
</script>

<html:html locale="true">
<head>
<title><fmt:message key="login.title"/></title>
<link rel=stylesheet href="<html:rewrite page="/css/win.css"/>" type="text/css">
<script language="JavaScript" src="<html:rewrite page="/js/functions.js"/>" type="text/javascript"></script>
<script language="JavaScript" type="text/javascript">
  if (top != self)
    top.location.href = self.document.location;

  var imagePath = '<html:rewrite page="/images/"/>';
  // takes care of jsession garbage, because sometimes jsession gets tacked on, like this:
  // var imagePath = "/images/;jsessionid=2FA16379595FE7804C33FEDB13FAB8D0";
  var semiIndex = imagePath.indexOf(";");
  if (semiIndex!= -1)
    imagePath = imagePath.substring(0, semiIndex);
</script>
</head>

<body>
<div align="center">
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<html:form action="/j_security_check">
<table width="1032" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td><html:img page="/images/spacer.gif" width="344" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="344" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="344" height="1" alt="" border="0"/></td>
  </tr>
  <tr>
    <td background="<html:rewrite page="/images/login_bgLeft.gif"/>"><html:img page="/images/login_logo.gif" width="203" height="103" alt="" border="0"/></td>
    <td>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr>
          <td>
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr>
		<td class="BlockTitle" width="100%"><fmt:message key="login.login"/></td>
                <td class="BlockTitle" align="right"><html:link href="" onclick="window.open(help + 'ui-1','help','width=800,height=650,scrollbars=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/tt_help.gif" width="16" height="16" border="0"/></html:link></td>
              </tr>
            </table>	
          </td>
        </tr>
          <!--[if gt IE 6]>
          <tr>
            <td>
              <table width="100%" cellpadding="0" cellspacing="0" border="0">
                <tr>
                  <td class="ErrorBlock"><html:img page="/images/tt_error.gif" width="10" height="11" alt="" border="0"/></td>
                  <td class="ErrorBlock" width="100%" style="font-weight:bold;"><fmt:message key="login.unsupportedBrowser"/></td>
                </tr>
                <tr>
                    <td class="ErrorBlock">&nbsp;</td>
                  <td class="ErrorBlock" style="padding-bottom:15px;padding-right:10px;"><fmt:message key="login.browserMessage"/></td>
                </tr>
              </table>
            </td>
          </tr>
          <![endif]--> 
          <c:if test='${loginStatus ne null}'>
          <tr>
            <td>
              <table width="100%" cellpadding="0" cellspacing="0" border="0">
                <tr>
                  <td class="ErrorBlock"><html:img page="/images/tt_error.gif" width="10" height="11" alt="" border="0"/></td>
                  <td class="ErrorBlock" width="100%"><fmt:message key="${loginStatus}"/></td>
                </tr>
              </table>
            </td>
          </tr>
          </c:if>
          <logic:messagesPresent>
          <tr>
            <td>
              <table width="100%" cellpadding="0" cellspacing="0" border="0">
                <tr>
                  <td class="ErrorBlock"><html:img page="/images/tt_error.gif" width="10" height="11" alt="" border="0"/></td>
                  <td class="ErrorBlock"><html:errors/></td>
                </tr>
              </table>
            </td>
          </tr>
          </logic:messagesPresent>
        <tr>
          <td>
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td width="30%" class="BlockLabel"><fmt:message key="login.username"/></td>
              <td width="70%" class="BlockContent"><html:text property="j_username" size="25"/></td>
            </tr>
            <tr>
              <td class="BlockLabel"><fmt:message key="common.label.Password"/></td>
              <td class="BlockContent"><input type="password" name="j_password" size="25" value=""></td>
            </tr>
            <tr>
              <td class="BlockLabel">&nbsp;</td>
              <td class="BlockContent">
                <table width="100%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td><input type="image" src="<html:rewrite page="/images/login_btn.gif"/>"  onmouseover="imageSwap(this, imagePath + 'login_btn', '_over');" onmouseout="imageSwap(this, imagePath +  'login_btn', '');" onmousedown="imageSwap(this, imagePath +  'login_btn', '_down')"/></td>
                    <td><html:img page="/images/spacer.gif" width="15" height="1" border="0"/></td>
                    <td width="100%"><html:link href="#"><img src="<html:rewrite page="/images/reset_btn.gif"/>"  onmouseover="imageSwap(this, imagePath + 'reset_btn', '_over');" onmouseout="imageSwap(this, imagePath +  'reset_btn', '');" onmousedown="imageSwap(this, imagePath +  'reset_btn', '_down')" onclick="document.LoginForm.reset()" width="59" height="17" alt="" border="0"/></html:link></td>
                  </tr>
                </table>
              </td>
            </tr>
            <tr>
              <td class="BlockContent" colspan="2"><html:img page="/images/spacer.gif" width="1" height="5" alt="" border="0"/></td>
            </tr>
            <tr>
              <td class="BlockContent" colspan="2">&nbsp;<center><fmt:message key="login.message"/></center></td>
            </tr>
            <tr>
              <td class="BlockBottomLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
            </tr>
          </table>
          </td>
         </tr>
        </table>
      </td>
      <td background="<html:rewrite page="/images/login_bgRight.gif"/> ">&nbsp;</td>
    </tr>
  </table>
</html:form>

<script language="JavaScript" type="text/javascript">
  <!--
    document.forms["LoginForm"].elements["j_username"].focus();
  // -->
</script>
</div>
</body>
</html:html>
