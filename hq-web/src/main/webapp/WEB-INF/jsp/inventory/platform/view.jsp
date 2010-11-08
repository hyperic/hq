<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<style>
	#content strong {
		font-size: 1.15em;
		padding-right:10px;
	}
	
	#content .panel {
		background-color:white;
		padding:30px;
		-moz-border-radius:5px;
	}
	
	#content input[type='text'] {
		font-size:1.15em;
		padding:5px;
	}
</style>
<form id="form" action="<c:out value="${links.delete}" />" method="POST">
<input type="hidden" name="ids" value="${platform.id}" />
<input type="hidden" name="redirect" value="/app/inventory/platform/list" />
<div id="content" style="padding:50px;">
	<h1>View Platform (<c:out value="${platform.id}" />)</h1>
	<div class="panel">
		<p>
			<strong>Name</strong>
			<span><c:out value="${platform.name}" /></span>
		</p>
		<p>
			<strong>Fqdn</strong>
			<span><c:out value="${platform.fqdn}" /></span>
		</p>
		<br/>
		<p>
			<input id="editPlatform" type="button" value="Edit" onclick="document.location = '<c:out value="${links.edit}" />'" />
			<input id="deletePlatform" type="submit" value="Delete" />
			<a href="/app/inventory/platform/list">Close</a>
		</p>
	</div>
</div>
<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.4.3/jquery.min.js"></script>
<script>
	$(document).ready(function() {
		$("#deletePlatform").click(function(e) {
			return confirm("Are you sure you want to delete?");
		});
	});
</script>