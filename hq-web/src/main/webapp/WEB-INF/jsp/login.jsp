<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ page import="org.hyperic.hq.common.shared.HQConstants"%>
<%@ page import="org.hyperic.hq.context.Bootstrap"%>
<%@ page import="org.hyperic.hq.common.shared.ServerConfigManager"%>
<%@ page import="java.util.Properties"%>
<%
ServerConfigManager serverConfig = (ServerConfigManager)Bootstrap.getBean(ServerConfigManager.class); 
Properties serverProps = serverConfig.getConfig();
boolean ldapInUse = "LDAP".equals(serverProps.getProperty(HQConstants.AUTHENTICATION_TYPE));
String realm = serverProps.getProperty("KERBEROS_REALM");
String kdc = serverProps.getProperty("KERBEROS_KDC");

if (ldapInUse || (realm != null && realm.length() != 0 && kdc != null && kdc.length() != 0)) {
    request.setAttribute("orgAuth", "Use my organization authentication");
}

%>
<div id="loginPanel">
	<form id="loginForm" name="loginForm" action="<spring:url value="/j_spring_security_check" />" method="POST">
		<div class="fieldsetTitle"><fmt:message key="login.signin.message" /></div>
		<div class="fieldsetNote"><fmt:message key="login.signin.instructions" /></div>
		<fieldset>
			<c:if test="${not empty param.authfailed}">
				<p>
					<div class="msgPanel msgError">
						<c:choose>
							<c:when test="${not empty errorMessage}">
								${errorMessage}
							</c:when>
							<c:otherwise>
								<fmt:message key="login.error.login" />								
							</c:otherwise>
						</c:choose>
					</div>
				</p>
			</c:if>
			<div class="fieldRow">
				<label for="j_username"><fmt:message key="login.field.username" /></label> 
				<input style="width: 75%;" id="usernameInput" type="text" id="j_username" name="j_username" value="" />
			</div>
			<div class="fieldRow">
				<label for="j_password"><fmt:message
						key="login.field.password" /></label> <input style="width: 75%;"
					id="passwordInput" type="password" id="j_password"
					name="j_password" autocomplete="off" />
			</div>

			<div>
				<c:choose>
					<c:when test="${not empty orgAuth}">
						<br/>
						<table >
							<tr>
								<td width="2px"><input type="checkbox" name="orgAuth"
									id="orgAuth" checked="checked"></td>
								<td valign="top"><label>${orgAuth}</label></td>
							</tr>
						</table>
					</c:when>
					<c:otherwise>
					</c:otherwise>
				</c:choose>

			</div>
			<div class="submitButtonContainer">
				<input id="submit" type="submit" name="submit" class="button42"
					value="<fmt:message key="login.signin" />" />
				<c:if test="${guestEnabled}">
					<div class="guestUserLinkContainer">
						<fmt:message key="login.or" />
						&nbsp;<a href="#" id="guestLoginLink" class="guestUser"><fmt:message
								key="login.signInAsGuest" /></a>
					</div>
				</c:if>
			</div>
		</fieldset>
	</form>
</div>
<script>
	hqDojo.ready(function() {
		var username = hqDojo.byId("usernameInput");
		var password = hqDojo.byId("passwordInput");

		hqDojo.connect(username, "onfocus", function(e) { hqDojo.addClass(e.target.parentNode, "active"); });
		hqDojo.connect(username, "onblur", function(e) { hqDojo.removeClass(e.target.parentNode, "active"); });

		hqDojo.connect(password, "onfocus", function(e) { hqDojo.addClass(e.target.parentNode, "active"); });
		hqDojo.connect(password, "onblur", function(e) { hqDojo.removeClass(e.target.parentNode, "active"); });

		username.focus();

		<c:if test="${guestEnabled}">
			hqDojo.connect(hqDojo.byId("guestLoginLink"), "onclick", function() {
				var username = hqDojo.byId("usernameInput");
				var password = hqDojo.byId("passwordInput");

				username.value = "${guestUsername}";
				document.forms["loginForm"]["submit"].click();
				username.disabled = true;
				password.disabled = true;
			});
		</c:if>
	});
</script>