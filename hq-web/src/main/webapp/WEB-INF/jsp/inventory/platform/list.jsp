<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>

<style>
	#content ul {
		margin:0;
		list-style:none;
	}
	
	#content strong {
		font-size:1.25em;
	}
	
	#content span {
		font-size:1.15em;
	}
	
	#content strong,
	#content span {
		float:left;
	}
	
	#content li {
		clear:both;
	}
	
	#content .id {
		width:50px;
	}
	
	#content .name,
	#content .fqdn {
		width:250px;
	}
</style>
<div id="content" style="padding:50px;">
	<h1>All Platforms (<c:out value="${count}" />)</h1>
	<form id="form" action="/app/inventory/platform/list" method="POST">
		<!--p>
			<input type="text" id="searchInput" />&nbsp;<input type="button" id="searchButton" value="Search" />
		</p-->
		<input type="hidden" name="redirect" value="/app/inventory/platform/list" />
		<ul style="background-color:#555;-moz-border-radius:5px; color:white;padding-bottom:15px;">
			<li style="padding:20px;">
				<strong>&nbsp;</strong>
				<strong class="id">Id</strong>
				<strong class="name">Name</strong>
				<strong class="fqdn">Fqdn</strong>
			</li>
			<li style="height: 250px; overflow-y: auto;">
				<ul style="background-color:white;-moz-border-radius:0 0 5px 5px;">
					<c:forEach var="platform" items="${platforms}">
						<li>
							<span><input type="checkbox" name="ids" value="<c:out value="${platform.id}" />" /></span>
							<span class="id"><a href="/app/inventory/platform/<c:out value="${platform.id}" />/view"><c:out value="${platform.id}" /><a/></span>
							<span class="name"><a href="/app/inventory/platform/<c:out value="${platform.id}" />/view"><c:out value="${platform.name}" /></a></span>
							<span class="fqdn"><a href="/app/inventory/platform/<c:out value="${platform.id}" />/view"><c:out value="${platform.fqdn}" /></a></span>
						</li>
					</c:forEach>
				</ul>
			</li>
		</ul>
		<br/>
		<p>
			<input id="createPlatform" type="button" value="Create Platform" />
			<input id="deletePlatform" type="submit" value="Delete Platform" />
			<select id="pageSize">
				<option value="">ALL</option>
				<option value="5"<c:if test="${pageSize == 5}"> selected</c:if>>5</option>
				<option value="10"<c:if test="${pageSize == 10}"> selected</c:if>>10</option>
				<option value="15"<c:if test="${pageSize == 15}"> selected</c:if>>15</option>
				<option value="20"<c:if test="${pageSize == 20}"> selected</c:if>>20</option>
			</select>
			<input type="hidden" id="pageNumber" value="<c:out value="${page}" />" />
			<c:if test="${not empty links.previous}">
				<a href="<c:out value="${links.previous}" />">Prev</a>&nbsp;&nbsp;
			</c:if>
			<c:if test="${not empty links.next}">
				<a href="<c:out value="${links.next}" />">Next</a>
			</c:if>
		</p>
	</form>
</div>
<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.4.3/jquery.min.js"></script>
<script>
	$(document).ready(function() {
		$("#createPlatform").click(function(e) {
			document.location = "/app/inventory/platform/create";
		});
		
		$("#deletePlatform").click(function(e) {
			if (confirm("Are you sure you want to delete?")) {
				$("#form").attr("action", "/app/inventory/platform/delete");
				return true;
			}
			
			return false;
		});
		
		var refreshList = function() {
			var url = "/app/inventory/platform/list"
			/*
			if ($("#searchInput").val().trim().length > 0) {
				url += "/" + escape($("#searchInput").val());
			}
			*/
			if ($("#pageSize").val().trim().length > 0) {
				var page = ($("#pageNumber").val().trim().length > 0) ? $("#pageNumber").val() : 1;
				url += "?page=" + page + "&pageSize=" + $("#pageSize").val();
			}
			
			document.location = url;
		};
		
		$("#searchButton").click(function(e) {
			refreshList();
		});
		
		$("#pageSize").change(function(e) {
			refreshList();
		});
	});
</script>