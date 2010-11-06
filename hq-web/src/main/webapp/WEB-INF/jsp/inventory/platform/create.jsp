<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<form:form id="form" method="post" modelAttribute="platform">
	<form:label path="name">Name</form:label> <form:input path="name" />
	<form:label path="fqdn">FQDN</form:label> <form:input path="fqdn" />
	<p><button type="submit">Submit</button></p>
</form:form>