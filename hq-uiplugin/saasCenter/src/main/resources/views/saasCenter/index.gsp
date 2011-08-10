<style type="text/css">
/*Widgets*/

/*Health Bars*/

.grey {
	background: url(/images/saas/gray.png) repeat-x;
	cursor:crosshair
}

.greyRight {
	background: url(/images/saas/grayRight.png) no-repeat
}

.greyLeft {
	background: url(/images/saas/grayLeft.png) no-repeat
}

.green {
	background: url(/images/saas/green.png) repeat-x
}

.greenRight {
	background: url(/images/saas/greencapright.png) no-repeat;
	_background: url(/images/saas/greencapright.gif) no-repeat
}

.greenLeft {
	background: url(/images/saas/greencapleft.png) no-repeat;
	_background: url(/images/saas/greencapleft.gif) no-repeat
}

.yellow {
	background: url(/images/saas/yellow.png) repeat-x;
	cursor:crosshair
}

.yellowRight {
	background: url(/images/saas/yellowcapright.png) no-repeat;
	_background: url(/images/saas/yellowcapright.gif) no-repeat
}

.yellowLeft {
	background: url(/images/saas/yellowcapleft.png) no-repeat
	_background: url(/images/saas/yellowcapleft.gif) no-repeat
}

.red {
	background: url(/images/saas/red.png) repeat-x;
	cursor:crosshair
}

.redRight {
	background: url(/images/saas/redcapright.png) no-repeat;
	_background: url(/images/saas/redcapright.gif) no-repeat
}

.redLeft {
	background: url(/images/saas/redcapleft.png) no-repeat;
	_background: url(/images/saas/redcapleft.gif) no-repeat
}

.redAvail {
	background: url(/images/saas/noavail.png) no-repeat;
	_background: url(/images/saas/noavail.gif) no-repeat
}

.yellowAvail {
	background: url(/images/saas/partavail.png) no-repeat;
	_background: url(/images/saas/partavail.gif) no-repeat
}

.greenAvail {
	background: url(/images/saas/avail.png) no-repeat;
	_background: url(/images/saas/avail.gif) no-repeat
}

.redAvailS {
	background: url(/images/saas/us.png) no-repeat
}

.yellowAvailS {
	background: url(/images/saas/pas.png) no-repeat
}

.greenAvailS {
	background: url(/images/saas/as.png) no-repeat
}

.rle-box {
	width: 733px;
	margin: 10px 0 10px 14px;
	padding: 4px;
	position: relative;
}

.rle-box:hover {
	background: url('/images/saas/highlight_middle.png');
}

/*for ie6 since it sucks and cant do div:hover*/

.rle-over {


/*background: url('/images/saas/highlight.png') no-repeat;*/
	background-color: #E9F5FB;
}

.rle-cont {
	background: url(/images/saas/rlebg.gif) repeat-x;
	width: 490px;
	height: 50px;
	float: left;
	cursor: default;
	margin: 5px 0 0 10px
}

.rle-data {
	width: 418px;
	height: 0;
	margin: 18px 0 0 15px;
	cursor:default;
	_cursor: pointer
}

.rle-now {
	width: 30px;
	height: 30px;
	float: right;
	margin: -8px 12px 0;
	_margin: -27px 8px 0 16px;
}

.rle-cs {
	float: right;
	margin-right: 22px;
	font-size: .9em
}

.rle-status {
	color: #777;
	font-size: .7em;
	width: 500px;
	margin-top: 20px;
	padding: 0;
}

.rle-box li {
	padding-left: 10px;
	list-style: none
}

.rle-box ul {
	margin: 0;
	padding: 0
}

.rle-status li {
	margin-left: 26px;
	padding: 0
}

.rle-rule {
	background: transparent url(/images/saas/rule.png) repeat-x scroll 0;
	_background: transparent url(/images/saas/rule.gif) repeat-x scroll 0;
	height: 7px;
	width: 416px;
	clear: both;
	margin: 0 0 0 16px;
	_margin: -6px 0 0 16px
}

.rle-box a {
	font-size: .7em;
}

.rle-legend {
	clear: both;
	font-size: .65em;
	color: #888;
	padding-top: 3px
}

.rle-bg {
	height: 14px;
	float: left
}

.ll {
	float: left;
	margin-left: 4px
}

.rl {
	float: right;
	margin-right: 55px
}

.roll-title {
	font-size: .75em;
	color: #555;
	font-weight: 700;
	margin-left: 10px;
	width: 500px
}

.rle-more {
	bottom: 4px;
	position: absolute;
	right: 4px
}


/*Status Indicator Widget*/

.status {
	color: #333;
	height: 0;
    padding-right: 10px;
    float: right;
	position: relative;
	top: 0;
	text-align: right;
	width: 250px
}

#update {
	background: transparent url(/images/saas/activity.gif) no-repeat;
	margin-left: 110px;
	height: 16px;
	padding: 1px 21px 0
}

#nt {
	color: #06A
}
/*TIMELINE*/
.timeline-ether-marker-bottom {
    width:          5em; 
    height:         1.5em; 
    border-left:    1px solid #aaa; 
    padding-left:   2px; 
    color:          #aaa;
}

.timeline-ether-marker-bottom-emphasized {
    width:          5em; 
    height:         2em; 
    border-left:    1px solid #aaa; 
    padding-left:   2px; 
    color:          black;
}

.timeline-ether-marker-top {
    width:          5em; 
    height:         1.5em; 
    border-left:    1px solid #aaa; 
    padding-left:   2px; 
    color:          #aaa;
}

.timeline-ether-marker-top-emphasized {
    width:          5em; 
    height:         2em; 
    border-left:    1px solid #aaa; 
    padding-left:   2px; 
    color:          black;
}


.timeline-ether-marker-right {
    width:          5em; 
    height:         1.5em; 
    border-top:     1px solid #aaa; 
    padding-top:    2px; 
    color:          #aaa;
}

.timeline-ether-marker-right-emphasized {
    width:          7em; 
    height:         1.5em; 
    border-top:     1px solid #aaa; 
    padding-top:    2px; 
    color:          black;
}
.timeline-ether-marker-left {
    width:          5em; 
    height:         1.5em; 
    border-top:     1px solid #aaa; 
    padding-top:    2px; 
    color:          #aaa;
}

.timeline-ether-marker-left-emphasized {
    width:          7em; 
    height:         1.5em; 
    border-top:     1px solid #aaa; 
    padding-top:    2px; 
    color:          black;
}
.timeline-duration-event {
    position: absolute;
    overflow: hidden;
    border: 1px solid blue;
}

.timeline-instant-event2 {
    position: absolute;
    overflow: hidden;
    border-left: 1px solid blue;
    padding-left: 2px;
}

.timeline-instant-event {
    position: absolute;
    overflow: hidden;
}

.timeline-event-bubble-title {
    font-weight: bold;
    border-bottom: 1px solid #888;
    margin-bottom: 0.5em;
}

.timeline-event-bubble-body {
}

.timeline-event-bubble-wiki {
    margin:     0.5em;
    text-align: right;
    color:      #A0A040;
}
.timeline-event-bubble-wiki a {
    color:      #A0A040;
}

.timeline-event-bubble-time {
    color: #aaa;
}

.timeline-event-bubble-image {
    float: right;
    padding-left: 5px;
    padding-bottom: 5px;
}

.timeline-container {
    position: relative;
    overflow: hidden;
}

.timeline-message-container {
    position:   absolute;
    top:        30%;
    left:       35%;
    right:      35%;
    z-index:    1000;
    display:    none;
}
.timeline-message {
    font-size:      120%;
    font-weight:    bold;
    text-align:     center;
}
.timeline-message img {
    vertical-align: middle;
}

.timeline-band {
    position:   absolute;
    background: #eee;
    z-index:    10;
}

.timeline-band-inner {
    position: relative;
    width: 100%;
    height: 100%;
}

.timeline-band-input {
    position:   absolute;
    width:      1em;
    height:     1em;
    overflow:   hidden;
    z-index:    0;
}
.timeline-band-input input{
    width:      0;
}

.timeline-band-layer {
    position:   absolute;
    width:      100%;
    height:     100%;
}

.timeline-band-layer-inner {
    position:   relative;
    width:      100%;
    height:     100%;
}

/*TIMEPLOT*/

.timeplot-container {
    overflow: hidden;
    position: relative;
    height: 200px;
    border: 1px solid #ccc;
    padding: 12px 14px;
}

.timeplot-copyright {
    position: absolute;
    top: 0px;
    left: 0px;
    z-index: 1000;
    cursor: pointer;
}

.timeplot-message-container {
    position:   absolute;
    top:        30%;
    left:       35%;
    right:      35%;
    max-width:  400px;
    z-index:    1000;
    display:    none;
}
.timeplot-message {
    font-size:      120%;
    font-weight:    bold;
    text-align:     center;
}
.timeplot-message img {
    vertical-align: middle;
}

.timeplot-div {
    position: absolute;
}

.timeplot-grid-label {
    font-size: 9px;
}

.timeplot-event-box {
    cursor: pointer;
}

.timeplot-event-box-highlight {
    border: 1px solid #FFB03B;
}

.timeplot-valueflag {
    display: none;
    border: 1px solid #FFB02D;
    padding: 2px 4px;
    text-align: center;
    background-color: #FFE57F;
    font-weight: bold;
    z-index: 1000;
}

.timeplot-valueflag-line {
    display: none;
    width: 14px;
    height: 14px;
    z-index: 1000;
}

.timeplot-timeflag {
    display: none;
    border: 1px solid #FFB02D;
    padding: 2px 4px;
    text-align: center;
    background-color: #FFE57F;
    font-weight: bold;
    z-index: 1000;
}

.timeplot-timeflag-triangle {
    display: none;
    width: 11px;
    height: 6px;
    z-index: 1001;
}

.timeplot-valueflag-pole {
    display: none;
    border-left: 1px solid #FFB02D;
    z-index: 999;
}

.timeplot-lens {
    display: none;
    border: 1px solid #FFB02D;
    z-index: 998;
}

/*Chart Widget*/

.chartS {
	height: 80px;
	width: 180px;
	cursor: default
}

.chart {
	height: 150px;
	width: 320px;
	cursor: default
}

.chartW {
	height: 150px;
	width: 700px;
	cursor: default
}

.chartT {
	height: 75px;
	width: 700px;
	cursor: default
}

.chartCont {
	font-size: .65em;
	font-family: verdana;
	float: left;
	margin: 0 0 0 8px;
	padding: 10px 0 0 10px;
	position: relative;
}

.cTitle {
	font-size: 1.1em
}

.xlegend {
	text-align: right
}

.ylegend {
	float: left;
	width: 2px
}

div.timeplot-container {
	border: 0
}

canvas {
	_border-left: 1px solid #444;
	_border-bottom: 1px solid #444
}

div.timeplot-valueflag,div.timeplot-timeflag {
	background-color: #73D2F7;
	border: 1px solid #2813F1
}

div.timeplot-valueflag-pole {
	border-left: 1px solid #2813F1
}

/*diplay none tag*/

.none,img.timeplot-copyright {
	display: none
}

/* IE 6 PNG FIX */

* html img, * html .png {
	position: relative;
	behavior: expression((this.runtimeStyle.behavior="none")&&(this.pngSet?this.pngSet=true:(this.nodeName == "IMG" && this.src.toLowerCase().indexOf('.png')>-1?(this.runtimeStyle.backgroundImage = "none",
this.runtimeStyle.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + this.src + "', sizingMethod='image')",
this.src = "transparent.gif"):(this.origBg = this.origBg? this.origBg :this.currentStyle.backgroundImage.toString().replace('url("','').replace('")',''),
this.runtimeStyle.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + this.origBg + "', sizingMethod='crop')",
this.runtimeStyle.backgroundImage = "none")),this.pngSet=true)
);
}

/*TOOLTIP WIDGET*/

#toolTip {
	position: absolute;
	z-index: 1000;
	width: 220px;
	background: #000;
	border: 1px solid #fff;
	text-align: left;
	padding: 5px;
	min-height: 1em;
	-moz-border-radius: 5px;
}

#toolTip p {
	margin: 0;
	padding: 0;
	color: #fff;
	font: 11px verdana;
}

#toolTip p em {
	display: block;
	margin-top: 3px;
	color: #f60;
	font-style: normal;
	font-weight: bold;
}

#toolTip p em span {
	font-weight: bold;
	color: #fff;
}

.hover {
	cursor: pointer
}

#overallSummary h1 {
    cursor: pointer;
}

#overallSummary h1:hover {
    color: red;
}

#migContainer .yui-nav .selected a:visited, #migContainer .yui-nav .selected a:hover, #migContainer .yui-nav .selected a {
    color: #FFF;
} 
</style>
<!-- hqu plugin -->
<!-- Dependencies -->
<!-- Sam Skin CSS for TabView -->
<link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.6.0/build/tabview/assets/skins/sam/tabview.css">

<!-- JavaScript Dependencies for Tabview: -->
<script type="text/javascript" src="http://yui.yahooapis.com/2.6.0/build/yahoo-dom-event/yahoo-dom-event.js"></script>
<script type="text/javascript" src="http://yui.yahooapis.com/2.6.0/build/element/element-beta-min.js"></script>

<!-- OPTIONAL: Connection (required for dynamic loading of data) -->
<script type="text/javascript" src="http://yui.yahooapis.com/2.6.0/build/connection/connection-min.js"></script>

<!-- Source file for TabView -->
<script type="text/javascript" src="http://yui.yahooapis.com/2.6.0/build/tabview/tabview-min.js"></script>

<!-- Sam Skin CSS for buttons -->
<link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.6.0/build/button/assets/skins/sam/button.css">

<!-- Source file for buttons-->
<script type="text/javascript" src="http://yui.yahooapis.com/2.6.0/build/button/button-min.js"></script>
<script type="text/javascript">document.navTabCat = "Resource";</script>
        <div class="yui-skin-sam">

            <div class="status">
               <div id="status" style="display:none">Updated <span id="ct">DateTime</span>. Updates in <span id="nt">59</span></div>
               <div id="update">Updating...</div>
            </div>
            <div id="cloudRangeGroup" class="yui-buttongroup">
                <input id="cloudRange1" type="radio" name="cloudRange" value="1hr">
                <input id="cloudRange2" type="radio" name="cloudRange" value="6hr">
                <input id="cloudRange3" type="radio" name="cloudRange" value="12hr">
                <input id="cloudRange4" type="radio" name="cloudRange" value="1d">
                <input id="cloudRange5" type="radio" name="cloudRange" value="1w" checked>
            </div>
            <div id="clouds" class="yui-navset">
                <ul class="yui-nav">
                    <li class="selected"><a href="#overview"><em>Overview</em></a></li>
                </ul>            
                <div class="yui-content">
                    <!-- overall summary tab --> 
                    <div> 
                        <div class="title">Outage Dashboard</div>
                          <div class="legend">
                            This dashboard displays the last week of health status for selected remote computing services. This view is dynamic. For services with recent outages, a health bar is shown. Given no recent outages in a provider's services, key indicator charts are shown. Click a Service in the left panel for detailed service health status, metrics, and more history.
                          </div>

                          <div class="both"></div>
                          <div id="overallSummary"></div>
                    </div>
                </div>
            </div>
        </div>
        <div id="z" style="display: none"></div>
        <script type="text/javascript">
        var cloudTabs = new YAHOO.widget.TabView('clouds');
        cloudTabs.getTab(0).tabid = 'overview';
        
        var cloudRangeGroup = new YAHOO.widget.ButtonGroup("cloudRangeGroup");

        cloudRangeGroup.subscribe('checkedButtonChange', changeCloudRange);

        var refInt = 90; //Set the pages Refresh Interval - for update timer and the pages coundown timer - in seconds
        var id1 = 149; //For ID Generator
        var tabWidgets = {};
        var dataRange = '1w';

        var providerTabs = {};
        hyperic.widget.tempNode = hqDojo.byId('z');

        // var cprop = {expires:1}; //cookies should expire after 1d
        function changeCloudRange(evt)
        {
            dataRange = evt.newValue.get('value');
            loadData();
        }

        hqDojo.ready(
            function() {
                document.status = hyperic.widget.StatusElement('ct', 'nt', 'status', 'update', refInt);
                loadData();
                var refresh = setInterval(loadData, refInt * 1000);
            }
        );

        var t = new Date().getTime();
        /**
         * Called by the onload event and the refresh timer. 
         * This is the startup function for the page.
         */
        function loadData() {
            //Show the status update message
            document.status.startUpdate();
            hqDojo.xhrGet( {
                // url : '/cloud1.js?' + new Date().getTime(), //prevent caching
                url : '/hqu/saasCenter/Saascenter/summaryData.hqu?time=' + t + '&range=' + dataRange + '?' + t,
                handleAs : 'json',
                load : function (resp) {
                    buildPage(resp);
                    document.status.endUpdate();
                }
            } );
        }
        
        function buildTab(data, tab)
        {
            // clear old tab contents
            tab.set('content','');
            
            var f = document.createElement("div");
            f.style.display = 'none';
            f.id = 'tmpZ';
            // f.innerHTML = '<!-- ' + longName + ' tab --><div><div class="title">' + longName + ' Health Summary</div><div class="legend">These charts display real-time health status and <strong>one week</strong> of health history for ' + provider.longName + '.</div><div style="clear: both;"></div><div id="' + tab.tabid + '_summary"></div></div>';
            document.body.appendChild(f);
            
            tabWidgets[tab.tabid] = [];

            for (var i in data) {
                if(typeof data[i] !== 'function') {
                    var id = tab.tabid + '-' + i.replace(/\\s+/g,'_').toLowerCase();
                    f.innerHTML = f.innerHTML + '<h2 class="title">' + i + '</h2><div id="'+id+'_health"></div><div id="'+id+'_chartCont"></div><div style="clear: both;"></div>';
                    //health
                    if(data[i].health)
                    {
                        tabWidgets[tab.tabid].push(new hyperic.widget.Health(id + '_health', data[i].health, false));
                    }
                    // charts
                    // limit the charts to a max of 6
                    var length = data[i].charts.length < 6 ? data[i].charts.length : 6;

                    if(length > 0) {
                        for (var j = 0; j < length; j++) {
                            var chart_type = null;
                            if(data[i].charts[j].style !== undefined && data[i].charts[j].style == 'skinny') 
                            {
                                chart_type = 'skinny';
                            }
                            else
                            {
                                // (display chart full-width if only one chart is displayed, 
                                // or if the chart is the last on the page with an odd number of charts)
                                if(length==1 || (j == length-1 && length % 2)) {
                                    chart_type = 'single';
                                }
                                else
                                {
                                    chart_type = 'double';
                                }
                            }
                            // console.log(i + ': ' + data[i].charts[j].chartName + ' ('+data[i].charts[j].style+'): ' + chart_type);
                            tabWidgets[tab.tabid].push(new hyperic.widget.CloudChart(
                                id + '_chartCont', // chart container id
                                data[i].charts[j], // chart data
                                tab.tabid, // tab id
                                id + '_' + j, // chart position
                                chart_type) // chart type
                                );
                        }
                    }
                }
            }
            
            tab.set('content',f.innerHTML);
            while(f.lastChild) {
              f.removeChild(f.lastChild);
            }
            document.body.removeChild(f);
        }

        function changeTabs(provider) {
            cloudTabs.set('activeIndex', providerTabs[provider]);
        }

        /**
         * Called by the loaddata sucess callback. Builds each part 
         * of the page from the data retuned from the server
         */
        function buildPage(resp) {
            
            //Remove all the old widgets before creating the new ones
            for(tab in tabWidgets) {
                if(typeof tabWidgets[tab] !== "function")
                {
                    for(widget in tabWidgets[tab])
                    {
                        if(typeof tabWidgets[tab][widget] !== "function")
                        {
                            tabWidgets[tab][widget].cleanup();
                            tabWidgets[tab][widget] = null;
                        }
                    }
                }
            }
            hyperic.widget.tooltip.cleanup();

            hqDojo.byId('overallSummary').innerHTML = '';

            // create the cloudstatus overview widgets
            var f = document.createElement("div");
            f.style.display = 'none';
            document.body.appendChild(f);

            tabWidgets['overview'] = [];

            providers = resp.page.dashboard.providers;
            for(var j in providers) {
                if(typeof providers[j] !== 'function') {
                    var tabid = providers[j].code;

                    if( typeof providerTabs[tabid] == 'undefined' )
                    {
                        var tab = new YAHOO.widget.Tab({
                            label: providers[j].longName
                        });

                        tab.tabid = tabid;
                        cloudTabs.addTab( tab );
                    }
                    else
                    {
                        var tab = providerTabs[tabid];
                    }

                    tmp_id = 'overall_' + tabid + '_summary';

                    f.innerHTML = '<div id="' + tmp_id + '"><h1 class="title" onclick="changeTabs(\\\''+ tabid +'\\\')">' + providers[j].longName + '</h1></div>';
                    hqDojo.byId('overallSummary').appendChild(f.firstChild);
                    for (var i in providers[j].strips) {
                        if(typeof(providers[j].strips[i]) !== 'function')
                        {
                            if(providers[j].strips[i].stripType == 'health')
                            {
                                tabWidgets['overview'].push(new hyperic.widget.Health(tmp_id, providers[j].strips[i], true));
                            }
                            else
                            {
                                charts_container_id = (++id1) + '_charts';
                                f.innerHTML = '<div id="' + charts_container_id + '"></div>';
                                hqDojo.byId(tmp_id).appendChild(f.firstChild);
                                for(chart in providers[j].strips[i].charts) {
                                    if(typeof(providers[j].strips[i].charts[chart]) !== 'function')
                                    {
                                        tabWidgets['overview'].push(new hyperic.widget.CloudChart(charts_container_id, providers[j].strips[i].charts[chart], 'overview', chart + '_' + j + '_' + i , 'dashboard'));
                                    }
                                }
                                f.innerHTML = '<div style="clear: both;"></div>';
                                hqDojo.byId(charts_container_id).appendChild(f.firstChild);
                                delete charts_container_id;
                            }
                        }
                    }
                    tmp_id = null;
                }
            }
            delete f;
            //create each detail tab contents
            // console.log(data);

            var tabs = cloudTabs.get('tabs');
            for(var tab in tabs) {
                if(typeof(tabs[tab]) !== 'function') {
                    providerTabs[tabs[tab].tabid] = tab;
                    if(tabs[tab].tabid != 'overview')
                    {
                        buildTab(resp.page.detailedDataTab[tabs[tab].tabid], tabs[tab]);
                    }
                }
            }
            
            activeTabId = cloudTabs.getTab(cloudTabs.get('activeIndex')).tabid;
            for(widget in tabWidgets[activeTabId])
            {
                if(typeof widget !== 'function' && tabWidgets[activeTabId][widget].isShowing === false)
                {
                    tabWidgets[activeTabId][widget].showChart();
                }
            }
            

            // for (var j in resp.page.detailedDataTab) {
            //     if(typeof resp.page.detailedDataTab[j] !== 'function') {
            //         buildTab(resp.page.detailedDataTab[j]);
            //     }
            // }



            // //change the default tab to the last selected (cso)
            // if(activeTab.id == 'cso'){
            //     return;
            // }
            // hqDojo.byId('cso').className = 'tab';
            // hqDojo.byId('cso_tab').style.display = 'none';
            // hqDojo.byId(activeTab.id).className = 'activeTab';
            // hqDojo.byId(activeTab.id+'_tab').style.display = '';
            // hqDojo.publish('tabchange', [activeTab.id]);
        }
        </script>
<!-- end hqu plugin -->