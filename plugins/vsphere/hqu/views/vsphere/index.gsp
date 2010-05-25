<link type="text/css" href="/${urlFor(asset:'css')}/ui.all.css" rel="stylesheet"/>
<link type="text/css" href="/${urlFor(asset:'css')}/jquery.auto-complete.css" rel="stylesheet"/>
<link type="text/css" href="/${urlFor(asset:'css')}/jquery.hyperic.treecontrol.css" rel="stylesheet"/>
<link type="text/css" href="/${urlFor(asset:'css')}/tablesorter/blue/style.css" rel="stylesheet"/>
<link type="text/css" href="/${urlFor(asset:'css')}/hq-vsphere.css" rel="stylesheet"/>

<div id="contentContainer" class="vsphere">
  	<div id="tabs-trees" class="ui-tabs" style="float:left; width:20%;">
    	<ul>
     		<li>
     			<a href="#tree-tab-1" title="${l['tab.label.inventory']}">${l['tab.label.inventory']}</a>
     		</li>
    	</ul>
    
    	<!-- TODO: auto-scale height to browser height -->
    	<div id="tree-tab-1" class="scroll-pane">
    		<input id="findResourceInput" type="text" value="${l['text.type.resource.name']}" />
    		<br/>
      		<div id="tree-inventory"></div>
    	</div>
  	</div>

  	<div id="tabs-content" style="float:right; width:78%;">
		<ul>
        	<li>
        		<a href="/hqu/vsphere/performance/index.hqu" title="${l['tab.label.performance']}">${l['tab.label.performance']}</a>
        	</li>
			<li>
        		<a href="/hqu/vsphere/summary/index.hqu" title="${l['tab.label.summary']}">${l['tab.label.summary']}</a>
        	</li>
		</ul>
    
    	<!-- TODO: auto-scale height to browser height -->    
    	<div id="tab-summary" class="scroll-pane"></div>
    	<div id="tab-performance" class="scroll-pane"></div>
  	</div>
  	<div style="clear:both;">&nbsp;</div>
</div>

<script src="/${urlFor(asset:'js')}/jquery-1.4.2.min.js" type="text/javascript"></script>
<script type="text/javascript">
  	jQuery.noConflict();
</script>
<script src="/${urlFor(asset:'js')}/jquery-ui-1.7.2.custom.min.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.auto-complete.min.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.tablesorter.min.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.flot.min.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.timers-1.2.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.ba-bbq.1.2.1.min.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/jquery.hyperic.treecontrol.js" type="text/javascript"></script>
<script src="/${urlFor(asset:'js')}/date.format.js" type="text/javascript"></script>
<script type="text/javascript">
  	// Global resource id for which resource is currently being viewed.
  	var resourceId = null;

  	function setAjaxParameters(tabs, load) {
    	if(typeof(resourceId) != "undefined") {
     		var extraData = {'id':resourceId, 'load':load };

     		tabs.tabs('option', 'ajaxOptions', { 
     			data: extraData,
     			success: function (event) {
     				jQuery("span.lastUpdated").text("${l['text.last.updated']} " + (new Date()).format("longTime"));
     			} 
     		});
    	}
  	};

  	jQuery(document).ready(function() {
    	// Initialize tab containers
    	jQuery("#tabs-trees").tabs();
    	// Must lazy load performance content so that canvas elements have dimensions.
    	jQuery("#tabs-content").tabs({
     		select: function(event, ui) {
      		var tabs = jQuery('#tabs-content').tabs();
        
        		if (ui.panel.id == "tab-performance") {
       			setAjaxParameters(tabs, 'false');
        		} else {
        			setAjaxParameters(tabs, 'true');
        		}
        
        		return true;
     		},
     		show: function(event, ui) {
        		if (ui.panel.id == 'tab-performance') {
        			var tabs = jQuery('#tabs-content').tabs();
        			var selected = tabs.tabs('option', 'selected');
          
        			setAjaxParameters(tabs, 'true');
        			tabs.tabs('load' , selected);
        		}
     		}
    	});

    	// Initialize inventory tree
    	jQuery("#tree-inventory").treecontrol({
         treeId: "vTree",
     		url: "/hqu/vsphere/vsphere/inventory.hqu",
     		initialDataset: ${payload},
     		<% if (selectedId) { %>
	     		initialSelectId: ${selectedId},
     		<% } %>
     		refreshInterval: "300s",
     		selectedCallback: function(item) {
     			setSelectedResource(item.id);
     		}
    	});
    	
    	jQuery("input#findResourceInput").autoComplete({
    		ajax: "/hqu/vsphere/vsphere/findByName.hqu",
    		maxItems: 25,
    		minChars: 3,
    		postVar: "name",
    		autoFill: true,
    		onSelect: function (event, ui) {
    			jQuery("#tree-inventory").trigger("selectNode", ui.data.id);
    		},
    		onBlur: function (event, ui) {
    			jQuery(this).removeClass("dataentry").val("Type resource name");
    		},
    		onFocus: function (event, ui) {
    			jQuery(this).addClass("dataentry").val("");
    		}
    	});
    	
    	jQuery("span.refreshToolbar").live("click", refreshPanel);
  	});

	function refreshPanel() {
		setSelectedResource(resourceId);
	};
	
  	function setSelectedResource(id) {
    	resourceId = id;
    	
    	var tabs = jQuery('#tabs-content').tabs();
    	var selected = tabs.tabs('option', 'selected');
    
    	setAjaxParameters(tabs, 'true');
    	tabs.tabs('load' , selected);
  	};
</script>