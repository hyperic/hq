<%@ page language="java" %>
<%@ page isErrorPage="true" %>
<%@ page import="org.hyperic.util.StringUtil" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><fmt:message key="error.Error.Title"/></title>
<link rel=stylesheet href="<html:rewrite page="/css/win.css"/>" type="text/css">
<script  src="<html:rewrite page="/js/functions.js"/>" type="text/javascript"></script>
<script  type="text/javascript">
  var help = "<hq:help/>";
</script>
</head>
<body>
<br>
<br>
<br>
<br>
<br>
<br>
<div align="center">
<table width="400" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockTitle" width="100%"><fmt:message key="error.Error.Tab"/></td>
          <td class="BlockTitle" align="right"><html:link href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/tt_help.gif" width="16" height="16" border="0"/></html:link></td>
        </tr>
      </table>	
    </td>
  </tr>
  <tr>
    <td class="BlockContent" colspan="2">
      <p>
      <fmt:message key="error.Error.ThePageRequestedEtc"/>  
      <fmt:message key="error.Error.YouCan"/>          
      <span id="stacktrace_link">
      <html:link href="javascript:displayStackTrace()"><fmt:message key="error.Error.StackTraceHereLink"/></html:link>
      <fmt:message key="errors.jsp.contactsupport"/>  - <fmt:message key="errors.jsp.callhyperic"/><br/><br/>
      </span>
      <fmt:message key="error.Error.ReturnTo"/>
      <html:link href="javascript:history.back(1)"><fmt:message key="error.Error.PreviousPageLink"/></html:link> 
      <html:link page="/Dashboard.do"><fmt:message key="error.Error.DashboardLink"/></html:link> 
      <html:link page="/ResourceHub.do"><fmt:message key="error.Error.ResourceHubLink"/></html:link> 
      </p>
    </td>
  </tr>
  <tr>
    <td class="BlockContent" colspan="2"><span id="display"></span></td>
  </tr>
  <tr>
    <td class="ErrorBlock" colspan="2"><b><html:errors/></b></td>
  </tr>
  <tr>
    <td class="BlockBottomLine" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
  </tr>
</table>

<%
// XXX: move this all into an action
/* get the exception from one of the many places it could be hiding */
if (exception == null)
    exception = (Exception)request.getAttribute("javax.servlet.error.exception");
if (exception == null)
    exception = (Exception)request.getAttribute("org.apache.struts.action.EXCEPTION");
if (exception == null)
    exception = (Exception)request.getAttribute("org.apache.struts.action.ActionErrors");

request.setAttribute(PageContext.EXCEPTION, exception);

/* guarantee that our exceptions aren't throwables */
Exception root = null;
try {
    if (exception != null) {
        if (exception instanceof ServletException) {
            ServletException se = (ServletException) exception;
            root = (Exception) se.getRootCause();
%>
<c:set var="root">
<%= root %>
</c:set>
<%
        }
    }
}
catch (ClassCastException ce) {
    // give up on having a printable root exception
}

%>
<c:set var="exception">
<%= exception %>
</c:set>

<c:if test="${param.errorMessage}">
	<div id="errorMessage" style="visibility:hidden"><fmt:message key="${param.errorMessage}"/></div>
</c:if>

<c:catch> 
  <c:if test="${not empty exception}"> 
      <div id="exception" style="visibility:hidden"><%=StringUtil.getStackTrace(exception)%></div>
    <c:if test="${not empty root}"> 
      <div id="root" style="visibility:hidden"><%=StringUtil.getStackTrace(root)%></div>
    </c:if> 
  </c:if> 
</c:catch>


<script type="text/javascript">
/*--- start declaration/initialization ---*/
var exDiv = document.getElementById("exception");

if (exDiv!=null) {
    exDiv.style.display = "none";
    var exText = exDiv.innerHTML;
}
else
    var exText = "";

var rootDiv = document.getElementById("root");
if (rootDiv!=null) {
    rootDiv.style.display = "none";
    var rootText = rootDiv.innerHTML;
}
else
    var rootText = "";

var errorDiv = document.getElementById("errorMessage");
if (errorDiv!=null) {
    errorDiv.style.display = "none";
    var errorText = errorDiv.innerHTML;
}
else
    var errorText= "";
/*--- end declaration/initialization ---*/

var link = document.getElementById("stacktrace_link");
var display = document.getElementById("display");

function displayStackTrace() {
  display.innerHTML = '<table width="100%" cellpadding="4" cellspacing="0" border="0"><tr><td class="BlockTitle" width="100%"><fmt:message key="error.Error.Exception"/></td></tr><tr><td>' + exText + '</td></tr></table>';

  if (rootText.length > 0) {
    display.innerHTML += '<table width="100%" cellpadding="4" cellspacing="0" border="0"><tr><td class="BlockTitle" width="100%"><fmt:message key="error.Error.RootCause"/></td></tr><tr><td>' + rootText + '</td></tr></table>';
  }

  link.innerHTML = '<html:link href="javascript:hideStackTrace()"><fmt:message key="error.Error.HideStackTraceLink"/></html:link>';
}

function hideStackTrace() {
  display.innerHTML = "";

  link.innerHTML = '<html:link href="javascript:displayStackTrace()"><fmt:message key="error.Error.StackTraceHereLink"/></html:link>';
}
</script>
</div>
</body>
</html>
