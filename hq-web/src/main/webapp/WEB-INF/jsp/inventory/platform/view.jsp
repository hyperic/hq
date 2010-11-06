<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<b>Name</b> <c:out value="${platform.name}" />
<b>FQDN</b> <c:out value="${platform.fqdn}" />
<a id="getJSON" href="#">get JSON</a>
<a id="getXML" href="#">get XML</a>
<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.4.3/jquery.min.js"></script>
<script>
$(document).ready(function() {
	$("#getJSON").click(function() {
		$.ajax({
			type:"GET", 
			url:"/app/inventory/platform/<c:out value="${platform.id}" />",
			contentType: "application/json",
			success: function(data) {
				console.log(data);
			}
		});
	});
	
	$("#getXML").click(function() {
		alert("YE!");
		$.ajax({
			type:"GET", 
			url:"/app/inventory/platform/<c:out value="${platform.id}" />",
			beforeSend: function(req) {
				req.setRequestHeader("Accept", "application/application+xml");
			},
			success: function(data) {
				console.log(data);
			}
		});
	});
});
</script>