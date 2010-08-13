<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>



		<div id="escalationScheme">
			
			<span class="errorMessage"><c:if test="${not empty errorMsg }"> <c:out value="${errorMsg}"></c:out></c:if></span>
			<form:form method="post" modelAttribute="escalationForm" action="/app/admin/escalation">
				<fieldset>
					<legend>
						<fmt:message key="alert.config.escalation.scheme"/>
					</legend>
					<ul>
						<li>
							<label for="escalationName"><fmt:message key="common.header.EscalationName"/></label>
							<div class="fieldRow" id="escalationName">
								<form:input path="escalationName"/>
							</div>
						</li>
						<li>
							<label for="description"><fmt:message key="common.label.Description"/></label>
							<div class="fieldRow" id="description">
								<form:input path="description"/>
							</div>
						</li>
						<li>
							<label for="pauseAllowed"><fmt:message key="alert.config.escalation.acknowledged"/></label>
							<div id="pauseAllowed">
								<ul>
									<li>
										
										<form:radiobutton id="pauseAllowedTrue" path="pauseAllowed" value="true"/>
										<fmt:message key="alert.config.escalation.allow.pause"/>
										<form:select path="maxPauseTime">
											<option value="9223372036854775807"><fmt:message key="alert.config.props.CB.Enable.UntilFixed"/></option>
											<option value="300000">5<fmt:message key="admin.settings.Minutes"/></option>
											<option value="600000">10<fmt:message key="admin.settings.Minutes"/></option>
											<option value="7200000">2<fmt:message key="admin.settings.Hours"/></option>
										</form:select>
									</li>
									<li>
										
										<form:radiobutton id="pauseAllowedFalse" path="pauseAllowed" value="false" />
										<fmt:message key="alert.config.escalation.allow.continue"/>
									</li>
								</ul>
							
							</div>
						</li>
						
						<li>
							<label for="notifyAll"><fmt:message key="alert.config.escalation.state.change"/></label>
							<div id="notifyAll">
								<ul>
									<li>
										<form:radiobutton path="notifyAll" value="false" />
										<fmt:message key="alert.config.escalation.state.change.notify.previous"/>
									</li>
									<li>
										<form:radiobutton path="notifyAll" value="true" />
										<fmt:message key="alert.config.escalation.state.change.notify.all"/>
									</li>
								</ul>
							</div>
						</li>
						
						<li>
							<label for="repeat"><fmt:message key="alert.config.escalation.state.ended"/></label>
							<div id="repeat">
								<ul>
									<li>
										<form:radiobutton path="repeat" value="false" />
										<fmt:message key="alert.config.escalation.state.ended.stop"/>
									</li>
									<li>
										<form:radiobutton path="repeat" value="true" />
										<fmt:message key="alert.config.escalation.state.ended.repeat"/>
									</li>
								</ul>
							</div>
						</li>
						
					</ul>
				</fieldset>
				

	    	<input type="submit" value="<fmt:message key='common.label.Next'/>" />
    		<a href="/app/admin/escalations"><fmt:message key='common.label.Cancel'/></a>
    		
			</form:form>
			
			
		
		</div>
		


		