<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8">
		<title>Flexible Hierarchy Demo</title>
		<link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.5/themes/smoothness/jquery-ui.css" />
		<script src="http://code.jquery.com/jquery-1.4.4.min.js"></script>
		<script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.5/jquery-ui.min.js"></script>
		<script src="./resources/js/jstree/jquery.jstree.js"></script>
	</head>
	<body>
		<div id="tabs">
			<ul>
				<li><a href="#tabs-1">Resources</a></li>
				<li><a href="#tabs-2">Groups</a></li>
				<li><a href="#tabs-3">Applications</a></li>
			</ul>
			<div id="tabs-1">
				<p>
					<div id="resources" style="width:50%;"></div>
				</p>
			</div>
			<div id="tabs-2">
				<p>Morbi tincidunt, dui sit amet facilisis feugiat, odio metus gravida ante, ut pharetra massa metus id nunc. Duis scelerisque molestie turpis. Sed fringilla, massa eget luctus malesuada, metus eros molestie lectus, ut tempus eros massa ut dolor. Aenean aliquet fringilla sem. Suspendisse sed ligula in ligula suscipit aliquam. Praesent in eros vestibulum mi adipiscing adipiscing. Morbi facilisis. Curabitur ornare consequat nunc. Aenean vel metus. Ut posuere viverra nulla. Aliquam erat volutpat. Pellentesque convallis. Maecenas feugiat, tellus pellentesque pretium posuere, felis lorem euismod felis, eu ornare leo nisi vel felis. Mauris consectetur tortor et purus.</p>
			</div>
			<div id="tabs-3">
				<p>Mauris eleifend est et turpis. Duis id erat. Suspendisse potenti. Aliquam vulputate, pede vel vehicula accumsan, mi neque rutrum erat, eu congue orci lorem eget lorem. Vestibulum non ante. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Fusce sodales. Quisque eu urna vel enim commodo pellentesque. Praesent eu risus hendrerit ligula tempus pretium. Curabitur lorem enim, pretium nec, feugiat nec, luctus a, lacus.</p>
				<p>Duis cursus. Maecenas ligula eros, blandit nec, pharetra at, semper at, magna. Nullam ac lacus. Nulla facilisi. Praesent viverra justo vitae neque. Praesent blandit adipiscing velit. Suspendisse potenti. Donec mattis, pede vel pharetra blandit, magna ligula faucibus eros, id euismod lacus dolor eget odio. Nam scelerisque. Donec non libero sed nulla mattis commodo. Ut sagittis. Donec nisi lectus, feugiat porttitor, tempor ac, tempor vitae, pede. Aenean vehicula velit eu tellus interdum rutrum. Maecenas commodo. Pellentesque nec elit. Fusce in lacus. Vivamus a libero vitae lectus hendrerit hendrerit.</p>
			</div>
		</div>
		<script>
			$(function() {
				$( "#tabs" ).tabs();
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
									url += "/resources/4";	
								} else {
									url += n.data().jstree.uri + "/relationships";
								}
								
								console.log(url);
								
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
	        		"contextmenu" : {
	        			"items" : {
	        				"create" : null,
	        				"test" : {
	        					"label" : "TEST",
	        					"action" : function() {alert("TEST");}
	        				},
        					"rename" : {
	        					// The item label
	        					"label"				: "Rename",
	        					// The function to execute upon a click
	        					"action"			: function (obj) { 
	        						alert(obj);
	        					},
	        					// All below are optional 
	        					"_disabled"			: false,		// clicking the item won't do a thing
	        					"_class"			: "class",	// class is applied to the item LI node
	        					"separator_before"	: false,	// Insert a separator before the item
	        					"separator_after"	: true,		// Insert a separator after the item
	        					// false or string - if does not contain `/` - used as classname
	        					"icon"				: false,
	        					"submenu"			: { 
	        						/* Collection of objects (the same structure) */
	        					}
	        				}
	        			}
	        		},
					"plugins": [ "themes", "json_data", "contextmenu" ]
				});
			});
		</script>
	</body>
</html>