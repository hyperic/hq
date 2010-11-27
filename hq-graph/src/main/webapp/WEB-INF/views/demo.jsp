<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8">
		<title>Flexible Hierarchy Demo</title>
		<link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.5/themes/smoothness/jquery-ui.css" />
		<script src="http://code.jquery.com/jquery-1.4.4.min.js"></script>
		<script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.5/jquery-ui.min.js"></script>
		<script src="./resources/js/jstree/jquery.jstree.js"></script>
		<script src="./resources/js/json2.js"></script>
	</head>
	<body>
		<div id="tabs">
			<ul>
				<li><a href="#tabs-2">Resource Types</a></li>
				<li><a href="#tabs-1">Resources</a></li>
			</ul>
			<div id="tabs-2">
				<p>
					<table border="0" cellspacing="0" cellpadding="0" width="100%">
						<tr>
							<td width="50%" valign="top">
								<button id="removeResourceTypeRelationship">Remove Relationship</button>
								<div id="resourcetypes" style="padding-top:17px;"></div>
							</td>
							<td width="50%">
								<button id="createResourceType">Create</button>
								<button id="deleteResourceType">Delete</button>
								<ul id="availableResourceTypes" class="selectable"></ul>
							</td>
						</tr>
					</table>					
				</p>
			</div>
			<div id="tabs-1">
				<p>
					<button id="createResource">Create</button>
					<button id="deleteResource">Delete</button>
					<div id="resources" style="width:100%;"></div>
				</p>
			</div>
		</div>
		<div id="createResourceDialog" title="Create Resource">
			<form id="createResourceForm">
				<input type="hidden" id="relationshipType" name="relationshipType" value="CONTAINS" />
				<p>
					<label for="type">Type</label>
					<select name="type" id="type" class="text ui-widget-content ui-corner-all"></select>
				</p>
				<p>
					<label for="name">Name</label>
					<input type="text" name="name" id="name" class="text ui-widget-content ui-corner-all" />
				</p>			
			</form>
		</div>
		<div id="createResourceTypeDialog" title="Create Resource Type">
			<form id="createRequestTypeForm">
				<p>
					<label for="name">Name</label>
					<input type="text" name="name" id="name" class="text ui-widget-content ui-corner-all" />
				</p>
				<fieldset id="propertyTypes">
					<legend>Property Types</legend>
					<p>
						<input type="button" id="addPropertyType" value="Add" />
					</p>
					<table id="propertyTypeFields" width="100%" border="0" cellpadding="0" cellspacing="3">
						<tr>
							<td width="30%">Name</td>
							<td width="30%">Description</td>
							<td width="30%">Default Value</td>
							<td width="5%">Optional?</td>
							<td width="5%">Secret?</td>
						</tr>
						<tr>
							<td>
								<input type="text" name="propertyTypes[0].name" id="propertyTypes[0].name" class="text ui-widget-content ui-corner-all" />
							</td>
							<td>
								<input type="text" name="propertyTypes[0].description" id="propertyTypes[0].description" class="text ui-widget-content ui-corner-all" />
							</td>
							<td>
								<input type="text" name="propertyTypes[0].defaultValue" id="propertyTypes[0].defaultValue" class="text ui-widget-content ui-corner-all" />
							</td>
							<td>
								<input type="checkbox" name="propertyTypes[0].optional" id="propertyTypes[0].optional" class="text ui-widget-content ui-corner-all" />
							</td>
							<td>
								<input type="checkbox" name="propertyTypes[0].secret" id="propertyTypes[0].secret" class="text ui-widget-content ui-corner-all" />
							</td>
						</tr>
					</table>
				</fieldset>
			</form>
		</div>
		<script>
			$(function() {
				var populateRTs = function() {
					$.ajax({
						"dataType": "json",
						"url": "http://localhost:8080/hq-graph/resourcetypes",
						"success": function(data) {
							$("#availableResourceTypes").empty();
							if (data.resourceTypes) {
								for (var x = 0; x < data.resourceTypes.length; x++) {
									$("#availableResourceTypes").append(
										$("<li class='jstree-draggable'></li>")
											.append(data.resourceTypes[x].name)
											.data(data.resourceTypes[x])
											.attr({
												"id" : "rt_" + data.resourceTypes[x].id
											})
									);
								}	
								$("#availableResourceTypes li").click(function() {
									$("#availableResourceTypes li").removeClass("ui-selected");
									$(this).addClass("ui-selected");
								});
							}
						}
					});
				};
				populateRTs();
				$("button").button();
				$( "#createResourceTypeDialog" ).dialog({
					autoOpen: false,
					height: 500,
					width: 1000,
					modal: true,
					buttons: {
						"Create": function() {
							var rt = {};
							
							rt.name = $("#createRequestTypeForm #name").val();
							
							var index = $("#propertyTypeFields tr").length - 1;
							
							rt.propertyTypes = [];
							
							for (var x = 0; x < index; x++) {
								var pt = {};
								
								pt.name = $(document.getElementById("propertyTypes[" + x + "].name")).val();
								pt.description = $(document.getElementById("propertyTypes[" + x + "].description")).val();
								pt.defaultValue = $(document.getElementById("propertyTypes[" + x + "].defaultValue")).val();
								pt.optional = document.getElementById("propertyTypes[" + x + "].optional").checked;
								pt.secret = document.getElementById("propertyTypes[" + x + "].secret").checked;
								rt.propertyTypes.push(pt);
							}
							
							$.ajax({
								"url" : "http://localhost:8080/hq-graph/resourcetypes",
								"contentType" : "application/json",
								"data" : JSON.stringify(rt),
								"type" : "POST",
								"success" : function(data) {
									$( "#createResourceTypeDialog" ).dialog( "close" );
									populateRTs();																		
								}
							})
						},
						"Cancel" : function() {
							$( this ).dialog( "close" );
						}
					}
				});
				$( "#createResourceDialog" ).dialog({
					autoOpen: false,
					height: 500,
					width: 1000,
					modal: true,
					buttons: {
						"Create": function() {
							var r = {};
							
							r.name = $("#createResourceForm #name").val();
							r.type = $("#createResourceForm #type").val();
							r.properties = {};
							
							var props = $("#createResourceForm #properties input");
							
							for (var x = 0; x < props.length; x++) {
								r.properties[$(props[x]).attr("name")] = $(props[x]).val();
							}
							
							var rName = $("#createResourceForm #relationshipType").val();
							
							$.ajax({
								"url" : "http://localhost:8080/hq-graph/resources",
								"contentType" : "application/json",
								"data" : JSON.stringify(r),
								"type" : "POST",
								"success" : function(data) {
									var target = $("#resources").jstree("get_selected").data().jstree;
									var url = "/hq-graph" + target.uri + "/relationships/" + data.id;
									
									$.ajax({
										"url" : url,
										"type" : "PUT",
										"data" : {
											"name" : rName
										},
										"success" : function(data) {
											$("#resources").jstree("refresh");
											$( "#createResourceDialog" ).dialog( "close" );
										}
									});
								}
							})
						},
						"Cancel" : function() {
							$( this ).dialog( "close" );
						}
					}
				});
				$("#addPropertyType").click(function() {
					var index = $("#propertyTypeFields tr").length - 1;
					$("#propertyTypeFields").append(
						$("<tr></tr>").append(
							$("<td></td>").append(
								$("<input type='text'></input>").attr({
									"id" : "propertyTypes[" + index +"].name",
									"name" : "propertyTypes[" + index +"].name",
									"class" : "text ui-widget-content ui-corner-all"
								})
							)
						).append(
							$("<td></td>").append(
								$("<input type='text'></input>").attr({
									"id" : "propertyTypes[" + index +"].description",
									"name" : "propertyTypes[" + index +"].description",
									"class" : "text ui-widget-content ui-corner-all"
								})
							)
						).append(
							$("<td></td>").append(
								$("<input type='text'></input>").attr({
									"id" : "propertyTypes[" + index +"].defaultValue",
									"name" : "propertyTypes[" + index +"].defaultValue",
									"class" : "text ui-widget-content ui-corner-all"
								})
							)
						).append(
							$("<td></td>").append(
								$("<input type='checkbox'></input>").attr({
									"id" : "propertyTypes[" + index +"].optional",
									"name" : "propertyTypes[" + index +"].optional",
									"class" : "text ui-widget-content ui-corner-all"
								})
							)
						).append(
							$("<td></td>").append(
								$("<input type='checkbox'></input>").attr({
									"id" : "propertyTypes[" + index +"].secret",
									"name" : "propertyTypes[" + index +"].secret",
									"class" : "text ui-widget-content ui-corner-all"
								})
							)
						)
					);
				});
				$("#createResourceType").click(function() {
					$("#createResourceTypeDialog").dialog("open");
				});
				$("#createResource").click(function() {
					var target = $("#resources").jstree("get_selected").data().jstree;
					var select = $("#createResourceForm #type");
					
					select.empty();
					
					var rt;
								
					for (var x in target.relatedResourceTypes) {
						rt = target.relatedResourceTypes[x];
						var option = $("<option value='" + rt.name + "'>" + rt.name + "</option>");
						
						if (x == 0) option.attr("selected", "selected");
						
						option.data(rt);
						select.append(option);
					}
					
					var populateProperties = function(target) {
						$("#createResourceForm #properties").remove();
	
						if (target.length > 0) {
							var rt = target.data();
								
							if (rt.propertyTypes.length > 0) {
								var p;
								var propsFrameset = $("<div></div>").attr("id", "properties");
									
								propsFrameset.append($("<strong></strong>").text("Properties"));
	
								for (var x in rt.propertyTypes) {
									p = rt.propertyTypes[x];
										
									propsFrameset.append(
										$("<p></p>").append(
											$("<label></label>").attr("for", p.name).text(p.name)
										).append(
											$("<input></input>").attr({
												"type" : "text",
												"name" : p.name,
												"value" : p.defaultValue
											})
										)
									);
								}
								
								$("#createResourceForm").append(propsFrameset);
							}
						}						
					};
					
					select.change(function() {
						populateProperties($("#createResourceForm #type option:selected"));
					});
					
					select.change();
					
					$("#createResourceDialog").dialog("open");
				});
				$("#deleteResourceType").click(function() {
					var types = $("#availableResourceTypes .ui-selected");
					
					if (types.length > 0 && confirm("Are you sure?")) {
						types.each(function(i, n) {
							var target = $(n).data();
							
							$.ajax({
								"async" : false,
								"url" : "/hq-graph" + target.uri,
								"type" : "delete",
		    					"success" : function (data) {
		    						$(n).remove();
		    						$("#resourcetypes").jstree("refresh");
		    					}							
							});
						});
					}
				});
				$("#removeResourceTypeRelationship").click(function() {
					var target = $("#resourcetypes").jstree("get_selected").data().jstree;
					
					$.ajax({
						"async" : false,
						"url" : "/hq-graph" + target.relationshipUri,
						"type" : "delete",
						"data" : {
    						"name" : target.relationshipName
    					},
    					"success" : function (data) {
    						$("#resourcetypes").jstree("remove");
    					}
					});
				});
				$("#deleteResource").click(function() {
					var target = $("#resources").jstree("get_selected").data().jstree;
					
					if (confirm("Are you sure?")) {
						$.ajax({
							"async" : false,
							"url" : "/hq-graph" + target.uri,
							"type" : "delete",
	    					"success" : function (data) {
	    						$("#resources").jstree("refresh");
	    					}							
						});
					}
				});
				$("#tabs").tabs();
				$("#resources").jstree({
					"themes": {
						"theme": "default",
						"dots": false
					},
				 	"json_data" : {
						"ajax" : {
							"url" : function (n) {
								var url = "http://localhost:8080/hq-graph";
								if (n == -1) {
									url += "/resources/root-relationships";	
								} else {
									url += n.data().jstree.uri + "/relationships";
								}
								return url;
							},
							"success" : function (data) { 
								var r;
								var result = [];
								if (data.resources) {
									for (var x = 0; x < data.resources.length; x++) {
										r = data.resources[x];
										var item = {
											"data": {
												"title": r.name
											},
											"state": "closed",
											"attr": { "id": r.id },
											"metadata": r
										};									
										result.push(item);										
									}
								} else {
									r = data;
									var item = {
										"data": {
											"title": r.name
										},
										"state": "closed",
										"attr": { "id": r.id },
										"metadata": r
										};									
									result.push(item);
								}
								return result;
							}
						}
	        		},
	        		"ui" : {
	        			"select_limit" : 1
	        		},
					"plugins": [ "themes", "json_data", "ui", "crrm" ]
				});
				$("#resourcetypes").jstree({
					"themes": {
						"theme": "default",
						"dots": false
					},
				 	"json_data" : {
						"ajax" : {
							"url" : function (n) {
								var url = "http://localhost:8080/hq-graph";
								if (n == -1) {
									url += "/resourcetypes/root-relationships";	
								} else {
									url += n.data().jstree.uri + "/relationships";
								}
								return url;
							},
							"success" : function (data) { 
								var r;
								var result = [];
								if (data.resourceTypes) {
									for (var x = 0; x < data.resourceTypes.length; x++) {
										r = data.resourceTypes[x];
										var item = {
											"data": {
												"title": r.name
											},
											"state": "closed",
											"attr": { "id": "rtr_" + r.id },
											"metadata": r
										};									
										result.push(item);										
									}
								} else {
									r = data;
									var item = {
										"data": {
											"title": r.name
										},
										"state": "closed",
										"attr": { "id": "rtr_" + r.id },
										"metadata": r
										};									
									result.push(item);
								}
								return result;
							}
						}
	        		},
	        		"dnd" : {
	        			"drag_check" : function (data) {
	        				return { 
	        					after : false, 
	        					before : false, 
	        					inside : true 
	        				};
	        			},
	        			"drag_finish" : function (data) { 
	        				var rt = $(data.o).data();
	        				var target = data.r.data().jstree;
	        				var url = "/hq-graph" + target.uri + "/relationships/" + rt.id;
	        				
	        				$.ajax({
	        					"url" : url,
	        					"type" : "put",
	        					"contentType" : "application/x-www-form-urlencoded",
	        					"data" : {
	        						"name" : "CONTAINS"
	        					},
	        					"success" : function (data) {
	        						$("#resourcetypes").jstree("refresh", data.r);
	        						$("#resourcetypes").jstree("open_node", data.r, false, true);
	        					}
	        				});
	        			}
	        		},
	        		"ui" : {
	        			"select_limit" : 1
	        		},
					"plugins": [ "themes", "json_data", "dnd", "ui", "crrm" ]
				});
			});
		</script>
		<style>
			.jstree-default.jstree-focused {
				background-color: #fff;
			}
			.selectable .ui-selecting { background: #ddd; }
			.selectable .ui-selected { background: #888; color: white; }
			.selectable { list-style-type: none; margin: 0; padding: 0; }
			.selectable li { margin: 3px; padding: 0.4em; height: 18px; }
		</style>
	</body>
</html>