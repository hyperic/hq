<%@ page language="java" %>
<%@ page isErrorPage="true" %>
<%@ page import="org.hyperic.util.StringUtil" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<c:if test="${empty portletErrorMessage}"> 
	<c:set var="portletErrorMessage">
		<s:actionerror />
	</c:set>
</c:if>

<c:if test="${empty portletErrorMessage}"> 
	<c:set var="portletErrorMessage">
		<s:property value="customActionErrorMessagesForDisplay" /> 
	</c:set>
</c:if>
	
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <title><fmt:message key="error.Error.Title"/></title>
		<link rel="stylesheet" href="<s:url value="/css/win.css"/>" type="text/css"/>
		<link rel="stylesheet" href="<s:url value="HQ_40.css"/>" type="text/css"/>
        <jsu:importScript path="/js/functions.js" />
        <jsu:script>
            var help = "<hq:help/>";
        </jsu:script>
    </head>
<body class="exception" style="background-color:#EEEEEE">
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
      <table width="100%" cellpadding="0" cellspacing="0" border="0" class="errorTitle">
        <tr>
          <td class="BlockTitle" width="100%"><fmt:message key="error.Error.Tab"/></td>
          <td class="BlockTitle" align="right"><a href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,left=80,top=80,resizable=yes'); return false;"><img src='<s:url value="/images/4.0/icons/help.gif" />'  width="16" height="16" border="0" /></a></td>
        </tr>
      </table>  
    </td>
  </tr>
  <tr>
    <td class="BlockContent" colspan="2">
      <p>
      <fmt:message key="error.Error.ThePageRequestedEtc"/>  
      <fmt:message key="error.Error.ReturnTo"/>
      <a href="javascript:history.back(1);"><fmt:message key="error.Error.PreviousPageLink"/></a> 
      <s:a action="Dashboard"><fmt:message key="error.Error.DashboardLink"/></s:a> 
      <s:a action="ResourceHub"><fmt:message key="error.Error.ResourceHubLink"/></s:a> 
      <fmt:message key="error.Error.Page"/>
      </p>
    </td>
  </tr>
  <tr>
    <td class="BlockContent" colspan="2"><span id="display"></span></td>
  </tr>
  <tr>
    <td class="ErrorBlock" colspan="2"><b><c:out value="${portletErrorMessage}" escapeXml="false"/></b></td>
  </tr>
  <tr>
    <td class="BlockBottomLine" colspan="2"><img src='<s:url value="/images/spacer.gif" />'  width="1" height="1" alt="" border="0" /></td>
  </tr>
</table>
<div id="display" style="visibility: hidden">
		<table width="100%" cellpadding="4" cellspacing="0" border="0">
		<tr>
		<td class="BlockTitle" width="100%"><fmt:message key="error.Error.Exception"/></td>
		</tr>
		<tr>
		<td><s:property value="%{exception}"/></td>
		</tr>
		</table>	
		<table width="100%" cellpadding="4" cellspacing="0" border="0">
		<tr>
		<td class="BlockTitle" width="100%"><fmt:message key="error.Error.RootCause"/></td>
		</tr>
		<tr>
		<td><s:property value="%{exceptionStack}"/></td>
		</tr>
		</table>
</div>		



<c:set var="root">
	<s:property value="%{exception}"/>
</c:set>


<c:set var="exception">
	<s:property value="%{exceptionStack}"/>
</c:set>

<c:if test="${param.errorMessage}">
    <div id="errorMessage" style="visibility:hidden"><fmt:message key="${param.errorMessage}"/></div>
</c:if>

<jsu:script>
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
        display.innerHTML += '<table width="100%" cellpadding="4" cellspacing="0" border="0"><tr><td class="BlockTitle" width="100%"></td></tr><tr><td>' + rootText + '</td></tr></table>';
      }
    
      link.innerHTML = '<s:url action="javascript:hideStackTrace()"><fmt:message key="error.Error.HideStackTraceLink"/></s:url>';
    }
    
    function hideStackTrace() {
      display.innerHTML = "";
    
      link.innerHTML = '<s:url action="javascript:displayStackTrace()"><fmt:message key="error.Error.StackTraceHereLink"/></s:url>';
    }
</jsu:script>
</div>
</body>
</html>