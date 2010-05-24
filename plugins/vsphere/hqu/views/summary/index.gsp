<style>
	#summaryPanels > div > table {
		margin:0;
		width:100%;
	}
	
	#summaryPanels div table tr td:first-child {
		width:20%;
	}
	
	#summaryPanels > div {
		float:left;
		margin-top:10px;
		width:49.5%;
	}

	#summaryPanels > div:first-child {
		float:none;
		width:100%;
	}

	#summaryPanels > div:last-child {
		float:right;
	}

	#summaryPanels div:first-child > span {
		display:inline-block;
		font-size:1.15em;
		font-weight:bold;
		width:100%;
	}
	
	#summaryPanels div > span {
		font-size:1em;
		font-weight:normal;
	}
	
	#summaryPanels div > span.collapsed {
		cursor:pointer;
		background:url("/${urlFor(asset:'images')}/collapsed-panel.gif") left no-repeat;
		padding-left:20px;
	}
	
	#summaryPanels div > span.expanded {
		cursor:pointer;
		background:url("/${urlFor(asset:'images')}/expanded-panel.gif") left no-repeat;
		padding-left:20px;
	}
	
	#summaryPanels > div > div {
		background-color:#EEEEEE;
		padding:5px;
	}

	#summaryPanels > div:first-child div {
		background-color:#EFF1FF;
	}
	
	#summaryPanels > div > div:first-child {
		-moz-border-radius-topleft:7px;
		-moz-border-radius-topright:7px;
		-webkit-border-radius-topleft:7px;
		-webkit-border-radius-topright:7px;
	}

	#summaryPanels > div > div:last-child {
		-moz-border-radius-bottomleft:7px;
		-moz-border-radius-bottomright:7px;
		-webkit-border-radius-bottomleft:7px;
		-webkit-border-radius-bottomright:7px;
	}
		
	#summaryPanels > div > div.collapsed {
		-moz-border-radius:7px;
		-webkit-border-radius:7px;
	}
</style>

<div style="text-align: right;">
	<% if (resource) { %>
		${linkTo(l.getFormattedMessage("link.goto.inventory.page", resource.name), [resource:resource])}
	<% } else if (vm) { %>
		${linkTo(l.getFormattedMessage("link.goto.inventory.page", vm.name), [resource:vm])}
	<% } else { %>
		${linkTo(l.getFormattedMessage("link.goto.inventory.page", host.name), [resource:host])}
	<% } %>
</div>

<div id="summaryPanels">
  	<% if (resource) { %>
  		<div id="resourceSection">
			<div>
  				<span>${l.getFormattedMessage("label.resource.properties", resource.name)}</span>
				<table class="tablesorter" border="0" cellpadding="0" cellspacing="1">
					<% if (resourceProps.size() > 0) { resourceProps.each { k, v -> %>
      				<tr>
        					<td>${k}</td>
        					<td>
        						<% if (v) { %>
        							${v}
        						<% } else { %>
        							<i>${l['text.no.value.specified']}</i>
        						<% } %>
        					</td>
      				</tr>
      			<% } } else { %>
      				<tr>
      					<td><i>${l['text.no.properties.specified']}</i></td>
      				</tr>
      			<% } %>
	    		</table>
  			</div>
  		</div>
  	<% } // if (resource) %>
  	<% if (vm) { %>
  		<div id="vmSection"<% if (resource) { %> class="collapsable"<% } %>>
  			<div>
  				<span>${l['label.vm.information']}</span>
  				<table class="tablesorter" border="0" cellpadding="0" cellspacing="1">
  					<tr>
      				<td>${l['label.hostname']}</td>
		      		<td>${vm.name}</td>
   				</tr>
       			<tr>
      				<td>${l['label.guest.os']}</td>
	      			<td>${vmProps['Guest OS']}</td>
	   			</tr>
    	  			<tr>
      				<td>${l['label.vcpus']}</td>
      				<td>${vmProps['Virtual CPUs']}</td>
	   			</tr>
	      		<tr>
    	  				<td>${l['label.memory']}</td>
      				<td>${vmProps['Memory Size']} ${l['text.mb']}</td>
	   			</tr>
      			<tr>
	      			<td>${l['label.mac.address']}</td>
    	  				<td>${vmProps['MAC Address']}</td>
	   			</tr>
       			<tr>
	      			<td>${l['label.ip']}</td>
    	  				<td>${vmProps['IP Address']}</td>
	   			</tr>
  					<tr>
	      			<td>${l['label.vm.version']}</td>
    	  				<td>${vmProps['VM Version']}</td>
	   			</tr>
        			<tr>
	      			<td>${l['label.tools.version']}</td>
    	  				<td>${vmProps['Tools Version']}</td>
	   			</tr>
   			</table>
	  		</div>
  			<div>
  				<span>${l['label.config.details']}</span>
  				<table class="tablesorter" border="0" cellpadding="0" cellspacing="1">
  					<tr>
      				<td>${l['label.esx.host']}</td>
		      		<td>${host.name}</td>
   				</tr>
  					<tr>
      				<td>${l['label.resource.pool']}</td>
	      			<td>${vmProps['Resource Pool']}</td>
	   			</tr>
    	  			<tr>
      				<td>${l['label.config.file']}</td>
      				<td>${vmProps['Config File']}</td>
	   			</tr>
  				</table>
	  		</div>
  		</div>
  	<% } // if (vm) %>
  	<div id="hostSection"<% if (vm) { %> class="collapsable"<% } %><% if (vm && !resource) { %> style="width:100%;"<% } %>>
  		<div>
			<span>${l['label.host.information']}</span>
  			<table class="tablesorter" border="0" cellpadding="0" cellspacing="1">
  				<tr>
      			<td>${l['label.hostname']}</td>
	      		<td>${host.name}</td>
   			</tr>
  				<tr>
      			<td>${l['label.location']}</td>
	      		<td>${hostProps['Data Center']}</td>
   			</tr>
      		<tr>
      			<td>${l['label.location']}</td>
      			<td>${hostProps['Manufacturer']}</td>
	   		</tr>
      		<tr>
      			<td>${l['label.model']}</td>
      			<td>${hostProps['Model']}</td>
	   		</tr>
      		<tr>
      			<td>${l['label.vmware.version']}</td>
      			<td>${l.getFormattedMessage("text.vmware.version.and.build", hostProps['VMware Version'], hostProps['Build'])}</td>
	   		</tr>
  			</table>
  		</div>
  		<div>
  			<span>${l['label.processor.details']}</span>
			<table class="tablesorter" border="0" cellpadding="0" cellspacing="1">
				<tr>
   				<td>${l['label.type']}</td>
     				<td>${hostProps['Processor Type']}</td>
				</tr>
				<tr>
   				<td>${l['label.cpus']}</td>
     				<td>${l.getFormattedMessage("text.logical.processors", (Integer.parseInt(hostProps['Processor Sockets']) * Integer.parseInt(hostProps['Cores per Socket'])), hostProps['Processor Sockets'], hostProps['Cores per Socket'])}</td>
				</tr>
			</table>
		</div>
  		<div>
  			<span>${l['label.network.details']}</span>
  			<table class="tablesorter" border="0" cellpadding="0" cellspacing="1">
  				<tr>
      			<td>${l['label.ip']}</td>
	      		<td>${hostProps['IP Address']}</td>
   			</tr>
  				<tr>
      			<td>${l['label.gateway']}</td>
	      		<td>${hostProps['Default Gateway']}</td>
   			</tr>
  				<tr>
      			<td>${l['label.dns']}</td>
	      		<td>
	      			${hostProps['Primary DNS']}
	      			<% if (hostProps['Secondary DNS']) { %>
	      				<br/>${hostProps['Secondary DNS']}
	      			<% } %>
	      		</td>
   			</tr>
  			</table>
  		</div>
  	</div>
</div>

<script type="text/javascript">
  	jQuery(document).ready(function() {
    	jQuery('#controldialog').dialog({ draggable: false, modal: true, autoOpen: false });
    	
    	var collapsables = jQuery("div.collapsable");
    	
    	if (collapsables.size() == 2) {
    		var height1 = jQuery(collapsables.get(0)).height();
    		var height2 = jQuery(collapsables.get(1)).height();
    		
    		if (height1 < height2) {
    			var diff = height2 - height1;
    			var div = jQuery("div:last-child", collapsables.get(0));
    			
    			div.height(div.height() + diff);
    		} else if (height2 < height1) {
    			var diff = height2 - height1;
    			var div = jQuery("div:last-child", collapsables.get(1));
    			
    			div.height(div.height() + diff);
    		}
    	}
  	});
</script>