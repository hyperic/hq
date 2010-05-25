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

<% def resourceMetrics = resource.getEnabledMetrics() %>

<span>${l.getFormattedMessage("text.performance.data.for", resource.name)}</span>
<strong>${l['label.right.axis']}</strong>
<select id="metricSelection">
  	<option value="0">${l['text.none']}</option>
  	<% for (m in metrics) { %>
  		<option value="${m['id']}">${m['name']}</option>
  	<% } %>
</select>
<strong>${l['label.date.range']}</strong>
<select id="rangeSelection">
  	<option value="3600000">${l['text.one.hour']}</option>
  	<option value="14400000">${l['text.four.hours']}</option>
  	<option selected="true" value="43200000">${l['text.twelve.hours']}</option>
  	<option value="86400000">${l['text.one.day']}</option>
  	<option value="172800000">${l['text.two.days']}</option>
  	<option value="604800000">${l['text.one.week']}</option>
  	<option value="2419200000">${l['text.one.month']}</option>
</select>
<div id="charts">
  	<% for (metric in resourceMetrics) { %>
  		<div id="chart${metric.id}" style="height:200px;"></div>
  	<% } %>
</div>

<script type="text/javascript">
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
      	legend:   {show: true, position: "sw", labelBoxBorderColor: "#000000"},
      	yaxis:    {min: 0, max: y1max, labelWidth: 40, tickFormatter: getFormatter(y1units)},
      	y2axis:   {min: 0, max: y2max, labelWidth: 40, tickFormatter: getFormatter(y2units)},
      	xaxis:    {mode: "time" },
      	grid:     {color:"#000000",
                 	  clickable: true,
                 	  autoHighlight: true,
                    backgroundColor: "#FFFAFF"}
    	};
  	}

  	function drawChart(id) {
    	var utcoffset = new Date().getTimezoneOffset() * 60000;
    	var metricVal = jQuery("#metricSelection").val();
    	var rangeVal = jQuery("#rangeSelection").val();
    
    	jQuery.ajax({url: "/hqu/vsphere/performance/data.hqu?mid=" + id + "&compare=" + metricVal + "&utcoffset=" + utcoffset + "&range=" + rangeVal,
                   dataType: "json",
                   success: function(result) {
                   	 jQuery.plot(jQuery("#chart" + id), result.data, getChartOptions(result));
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
    	drawCharts();
    	jQuery("#metricSelection").change(drawCharts);
    	jQuery("#rangeSelection").change(drawCharts);
    	jQuery(window).bind('resize', function() {
      	if (resizeTimer) clearTimeout(resizeTimer);
      	// Resizing can be slow, wait 500ms before redrawing the charts.
      	resizeTimer = setTimeout(drawCharts, 500);
    	});
  	});
</script>