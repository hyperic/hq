<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<style>
	#content strong {
		font-size: 1.15em;
		padding-right:10px;
	}
	
	#content fieldset {
		background-color:white;
		padding:30px;
		-moz-border-radius:5px;
	}
	
	#content input[type='text'] {
		font-size:1.15em;
		padding:5px;
	}
</style>
<div id="content" style="padding:50px;">
	<h1>Edit Platform (<c:out value="${platform.id}" />)</h1>
	<form:form id="form" method="post" modelAttribute="platform" action="/app/inventory/platform/edit">
		<form:hidden path="id" />
		<fieldset>
			<p>
				<form:label path="name"><strong>Name</strong></form:label> 
				<form:input path="name" />
			</p>
			<p>
				<form:label path="fqdn"><strong>Fqdn</strong></form:label> 
				<form:input path="fqdn" />
			</p>
			<br/>
			<p>
				<input type="submit" value="Submit" />&nbsp;&nbsp;
				<a href="/app/inventory/platform/list">Cancel</a>
			</p>
		</fieldset>
	</form:form>
</div>