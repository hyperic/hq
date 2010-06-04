<div class="panelHeader">
	<span class="refreshToolbar">
		<span class="lastUpdated"></span>
	</span>
	<span class="navLink">
		<% if (associatedPlatform) { %>
			${linkTo(l.getFormattedMessage("link.goto.inventory.page", associatedPlatform.name), [resource:associatedPlatform])}
		<% } else if (resource) { %>
			${linkTo(l.getFormattedMessage("link.goto.inventory.page", resource.name), [resource:resource])}
		<% } %>
	</span>
</div>
<div id="chartSettings" style="padding: 10px; background-color: rgb(255, 238, 255); -moz-border-radius: 7px;">
	<strong>${l.getFormattedMessage('text.performance.data.for', resource.name)}</strong><br/><br/>
	<span>${l['label.date.range']}</span>
	<select id="rangeSelection">
  		<option value="3600000">${l['text.one.hour']}</option>
	  	<option value="14400000">${l['text.four.hours']}</option>
  		<option value="43200000" selected="true">${l['text.twelve.hours']}</option>
	  	<option value="86400000">${l['text.one.day']}</option>
  		<option value="172800000">${l['text.two.days']}</option>
	  	<option value="604800000">${l['text.one.week']}</option>
  		<option value="2419200000">${l['text.one.month']}</option>
	</select>
	<% if (host && metrics) { %>
		<span style="margin-left: 15px;">${l.getFormattedMessage('label.compare.with.parent', '<a href="javascript:performance_SelectNode(' + host.id + ');">' + host.name + '</a>')}</span>
		<select id="metricSelection">
  			<option value="0">${l['text.none']}</option>
  			<option value="-1">${l['text.all.metrics']}</option>
			<% for (m in metrics) { %>
  				<option value="${m['id']}">${m['name']}</option>
  			<% } %>
		</select>
	<% } %>
</div>
<div style="padding:15px 45px 0;font-size:11px;">${l['label.availability']}</div>
<div id="availability" style="padding: 0 30px;"></div>
<div id="charts">
  	<% for (metric in resourceMetrics) { %>
  		<div id="chart${metric.id}" class="chartcontainer" style="height:200px;"></div>
  	<% } %>
</div>

<script type="text/javascript">
	function performance_SelectNode(id) {
		jQuery("#tree-inventory").trigger("selectNode", id);
	};

	function percentFormatter(val, axis) {
    	return val.toFixed(axis.tickDecimals) + "${l['text.percentage']}";
  	}

  	function percentageFormatter(val, axis) {
    	return (val * 100).toFixed(axis.tickDecimals) + "${l['text.percentage']}";
  	}

  	function KBformatter(val, axis) {
    	if (val > 1000) {
      	return (val/1000).toFixed(axis.tickDecimals) + "${l['text.mb']}";
    	} else {
      	return val.toFixed(axis.tickDecimals) + "${l['text.kb']}";
    	}
  	}

  	function Bformatter(val, axis) {
    	if (val > 1000000) {
     		return (val/1000000).toFixed(axis.tickDecimals) + "${l['text.mb']}";
    	} else if (val > 1000) {
     		return (val/1000).toFixed(axis.tickDecimals) + "${l['text.kb']}";
    	} else {
     		return val.toFixed(axis.tickDecimals) + "${l['text.byte']}";
    	}
  	}

  	function msformatter(val, axis) {
    	if (val > 1000) {
      	return (val/1000).toFixed(axis.tickDecimals) + "${l['text.second']}";
    	} else {
      	return val.toFixed(axis.tickDecimals) + "${l['text.millisecond']}";
    	}
  	}

  	function getFormatter(units) {
    	if (typeof(units) != "undefined") {
      	if (units == "percent") {
        		return percentFormatter;
      	} else if (units == "percentage") {
        		return percentageFormatter;
      	} else if (units == "KB") {
        		return KBformatter;
      	} else if (units == "B") {
        		return  Bformatter;
      	} else if (units == "ms") {
        		return msformatter;
      	}
    	}
    	
    	return null;
  	}

	function timeFormatter(val, axis) {
		var timestamp = new Date(val);
		var range = jQuery("#rangeSelection").val();
		
		if (range <= 43200000) {
			return timestamp.format('h:MMt');
		} else if (range <= 172800000) {
			return timestamp.format('ddd, h:MMt');
		} else if (range <= 604800000) {
			return timestamp.format('ddd, m/d');
		}
		
		return timestamp.format('mmm dd');
	}
	
  	function getMax(unit) {
    	if (unit == "percent") {
      	return 100;
    	} else if (unit == "percentage") {
	      return 1;
    	}
    
    	return undefined;
  	}

  	function getChartOptions(res) {
    	var y1units = res.y1units;
    	var y2units = res.y2units;
    	var y1max = getMax(y1units);
    	var y2max = getMax(y2units);

    	return {
      	lines:    {show: true},
      	points:   {show: false},
      	legend:   {show: true, position: "nw", labelBoxBorderColor: "#000000"},
      	yaxis:    {min: 0, max: y1max, labelWidth: 40, tickFormatter: getFormatter(y1units)},
      	y2axis:   {min: 0, max: y2max, labelWidth: 40, tickFormatter: getFormatter(y2units)},
      	xaxis:    {mode: "time", twelveHourClock: true, tickFormatter: timeFormatter },
      	grid:     {color:"#000000",
                   hoverable: true,
                   autoHighlight: true,
                   backgroundColor: "#FFFAFF"}
    	};
  	}

  	function drawChart(id) {
    	var utcoffset = new Date().getTimezoneOffset() * 60000;
    	var metricVal = null;
    	
    	if (jQuery("#metricSelection").size() > 0) {
	    	metricVal = jQuery("#metricSelection").val();
	    }
	    
    	var rangeVal = jQuery("#rangeSelection").val();
    	var params = {
    		'mid': id,
    		'utcoffset': utcoffset,
    		'range': rangeVal
    	};
    	
    	if (metricVal) {
    		params['compare'] = metricVal;
    	}
    	
    	jQuery.ajax({url: "/hqu/vsphere/performance/data.hqu",
    	           data: params,
                   dataType: "json",
                   success: function(result) {
                   	 jQuery.plot(jQuery("#chart" + id), result.data, getChartOptions(result));
                   }
    	});
  	}

	function drawAvailability() {
		jQuery.ajax({
			'url': '/hqu/vsphere/performance/availability.hqu',
			'data': {
				'mid': ${availMetric.id},
				'range': jQuery("#rangeSelection").val(),
				'utcoffset': new Date().getTimezoneOffset() * 60000
			},
			'dataType': 'json',
			'success': function(data) {
				jQuery('#availability').empty().healthcontrol(data);
			}
		});
	}
	
  	function drawCharts() {
    	<% for (metric in resourceMetrics) { %>
    		drawChart(${metric.id});
    	<% } %>
  	}

  	var resizeTimer = null;
  
  	jQuery(document).ready(function() {
  		var metric = jQuery.bbq.getState('m');
  		
  		if (metric) {
  			jQuery("#metricSelection").val(metric);
  		}
  		
    	jQuery("#metricSelection").change(function(event) {
    		drawCharts();
    		
    		var metric = jQuery(this).val();
    		
    		jQuery.bbq.pushState({ 'm': metric });
    	});
    	
    	var range = jQuery.bbq.getState('rg');
    	
    	if (range) {
    		jQuery("#rangeSelection").val(range);
    	}
    	
    	jQuery("#rangeSelection").change(function(event) {
    		drawAvailability();
    		drawCharts();
    		
    		var range = jQuery(this).val();
    		
    		jQuery.bbq.pushState({ 'rg': range });
    	});
    	jQuery(window).bind('resize', function() {
      		if (resizeTimer) clearTimeout(resizeTimer);
      		// Resizing can be slow, wait 500ms before redrawing the charts.
      		resizeTimer = setTimeout(drawCharts, 500);
    	});
    	<% if (availabilityJSON) { %>
    	jQuery.fn.healthcontrol.formatTimestamp = function(date) {
    		return timeFormatter(date.getTime());
    	};
    	drawAvailability();
    	<% } %>
    	drawCharts();
    	
    	function showTooltip(x, y, contents) {
        	jQuery('<div id="tooltip">' + contents + '</div>').css({
	            position: 'absolute',
    	        display: 'none',
        	    top: y + 5,
            	left: x + 5,
	            border: '1px solid #fdd',
    	        padding: '2px',
        	    'background-color': '#fee',
            	opacity: 1,
            	'z-index': 10
	        }).appendTo("body").fadeIn(200);
    	};

	    var previousPoint = null;
    	
    	jQuery(".chartcontainer").bind("plothover", function (event, pos, item) {
			if (item) {
                if (previousPoint != item.datapoint) {
               	    previousPoint = item.datapoint;
                    
                   	jQuery("#tooltip").remove();
	
					var x = item.datapoint[0].toFixed(),
                        y = item.datapoint[1].toFixed(2);
                    var datetime = new Date(Number(x));
                    
	                showTooltip(item.pageX, item.pageY, item.series.label + ", " + y + " at " + datetime.format('ddd, mmm dd, h:MMt'));
               	}
	        } else {
               	jQuery("#tooltip").remove();
	            previousPoint = null;            
    	    }
    	});
  	});
</script>