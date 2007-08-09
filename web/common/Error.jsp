<%@ page language="java" %>
<%@ page isErrorPage="true" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="org.hyperic.util.StringUtil" %>
<%@ page import="org.hyperic.hq.auth.shared.SessionTimeoutException" %>
<%@ page import="org.hyperic.hq.auth.shared.SessionNotFoundException" %>
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


<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="struts-html" prefix="html" %>
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

/* if the bizapp session is invalid, so should ours be */
if (root != null &&
    root instanceof SessionNotFoundException ||
    root instanceof SessionTimeoutException) {
    session.invalidate();
    // XXX: include a "session timed out" page
}
%>

<c:set var="exception">
<%= exception %>
</c:set>

<%
int randomNum=(int)(Math.random()*1000);
%>

<c:if test="${param.errorMessage}">
	<div id="errorMessage<%= randomNum %>" style="visibility:hidden"><fmt:message key="${param.errorMessage}"/></div>
</c:if>

<c:catch> 
  <c:if test="${not empty exception}"> 
      <div id="exception<%= randomNum %>" style="visibility:hidden"><%=StringUtil.getStackTrace(exception)%></div>
    <c:if test="${not empty root}"> 
      <div id="root<%= randomNum %>" style="visibility:hidden"><%=StringUtil.getStackTrace(root)%></div>
    </c:if> 
  </c:if> 
</c:catch>

<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">
<script type="text/javascript">
/*--- start declaration/initialization ---*/
var exDiv = document.getElementById("exception<%= randomNum %>");
if (exDiv!=null) {
	exDiv.style.display = "none";
	var exText<%= randomNum %> = exDiv.innerHTML;
}
else
	var exText<%= randomNum %> = "";

var rootDiv = document.getElementById("root<%= randomNum %>");
if (rootDiv!=null) {
	rootDiv.style.display = "none";
	var rootText<%= randomNum %> = rootDiv.innerHTML;
}
else
	var rootText<%= randomNum %> = "";

var errorDiv = document.getElementById("errorMessage<%= randomNum %>");
if (errorDiv!=null) {
	errorDiv.style.display = "none";
	var errorText<%= randomNum %> = errorDiv.innerHTML;
}
else
	var errorText<%= randomNum %> = "";
/*--- end declaration/initialization ---*/

document.write(
"<td>\n" + 
"<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" + 
"	<tr>\n" + 
"		<td class=\"ErrorBlock\"><img src=\"<html:rewrite page="/images/"/>tt_error.gif\" width=\"10\" height=\"11\" hspace=\"5\" border=\"0\"/></td>\n" + 
"		<td class=\"ErrorBlock\" width=\"100%\"><fmt:message key="errors.jsp.problem"/> <a href=\"javascript:displayStackTrace<%= randomNum %>()\"><fmt:message key="errors.jsp.ClickHere"/></a> <fmt:message key="errors.jsp.ToSee"/><br><fmt:message key="errors.jsp.contactsupport"/>  - <fmt:message key="errors.jsp.callhyperic"/> - <fmt:message key="errors.jsp.email"/> <a href=\'mailto:<fmt:message key="errors.jsp.support"/>?subject=<fmt:message key="error.Error.Title"/>&body=" + exText<%= randomNum %> + "\'><fmt:message key="errors.jsp.support"/></a></td>\n" +
"	</tr>\n" +
"</table>\n" + 
"</td>\n" + 
"<tr>\n"
);





function displayStackTrace<%= randomNum %>() {
	errorPopup = open("","errorPopup<%= randomNum %>","width=750,height=600,resizable=yes,scrollbars=yes,left=200,top=10");
	errorPopup.document.open();
	errorPopup.document.write("<html><title><fmt:message key="errors.jsp.problem"/></title>");
	errorPopup.document.write("<body>\n" + 
	"<link rel=stylesheet href=\"<html:rewrite page="/css/win.css"/>\" type=\"text/css\">" +
	"<a name=\"top\"></a>\n" + 
	"<a href=\"javascript:window.close()\">close window</a><br><br><br>\n" + 
	"<div align='center'>\n" + 
	"<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
	"    <tr>\n" + 
	"      <td>\n" + 
	"				<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
	"				  <tr><td class=\"BlockTitle\" width=\"100%\">Exception:</td></tr>\n" +
	"				</table>\n" +
	"			 </td>\n" + 
	"    </tr>\n" + 
	"    <tr>\n" + 
	"      <td class=\"BlockContent\"><blockquote>\n" + exText<%= randomNum %> + "</blockquote></td>\n" + 
	"    </tr>\n" + 
	"		 <tr><td class=\"BlockBottomLine\"><img src=\"<html:rewrite page="/images/"/>spacer.gif\" width=\"1\" height=\"1\" border=\"0\"></td></tr>\n" + 
	"    <tr>\n" + 
<c:if test="${not empty root}">
	"      <td>\n" + 
	"				<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
	"				  <tr><td class=\"BlockTitle\" width=\"100%\">Root cause:</td></tr>\n" +
	"				</table>\n" +
	"			 </td>\n" + 
	"    </tr>\n" + 
	"    <tr>\n" + 
	"      <td class=\"BlockContent\"><blockquote>\n" + rootText<%= randomNum %> + "</blockquote></td>\n" + 
	"    </tr>\n" + 
	"		 <tr><td class=\"BlockBottomLine\"><img src=\"<html:rewrite page="/images/"/>spacer.gif\" width=\"1\" height=\"1\" border=\"0\"></td></tr>" +
</c:if>
    "\n");

	if (errorDiv!=null) {
	errorPopup.document.write(
	"    <tr>\n" + 
	"        <td class=\"BlockContent\">\n" + 
	"            <b>"+ errorText<%= randomNum %> +"</b>\n" + 
	"        </td>\n" + 
	"    </tr>\n" + 
	"		 <tr><td class=\"BlockBottomLine\"><img src=\"<html:rewrite page="/images/"/>spacer.gif\" width=\"1\" height=\"1\" border=\"0\"></td></tr>\n"
	);
	}

	errorPopup.document.write(
	"</table>\n" + 
	"</div>\n" +
	"<br><br><br><a href=\"javascript:window.close()\">close window</a>\n" + 
	"</body>\n</html>"
	);
	
	errorPopup.document.close(); 
}
</script>
