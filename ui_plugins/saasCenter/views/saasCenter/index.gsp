<style type="text/css">
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
	background: url(/images/saas/highlight_middle.png);
}

/*for ie6 since it sucks and cant do div:hover*/

.rle-over {


/*background: url(/images/saas/highlight.png) no-repeat;*/
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

            <div id="clouds" class="yui-navset">
                <ul class="yui-nav">
                    <li class="selected"><a href="#overview"><em>Overview</em></a></li>
                    <li><a href="#amazon"><em>Amazon Web Services</em></a></li>
                    <li><a href="#google"><em>Google App Engine</em></a></li>
                    <li><a href="#salesforce"><em>Salesforce</em></a></li>
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
                    <!-- amazon tab --> 
                    <div>
                        <div class="title">Amazon Web Services Health Summary</div>
                        <div class="legend">These charts display real-time health status and <strong>one week</strong> of health history for key <a href="http://www.amazonaws.com/" target="amazon" title="Amazon Web Service Home">Amazon Web Services</a>.
                           Click a Service in the left panel for detailed service health status, metrics, and more history. <a href="/help/aws.html" target="help">More information</a> on how we monitor Amazon Web Services.
                        </div>
                        
                        <div class="clear" style="margin-bottom:20px"></div>
                        <div class="secLegend">
                           <span class="greenAvailS"> <a href="/help/aws.html#health" target="help">Healthy</a> </span>
                           <span class="yellowAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Issues</a> </span>
                           <span class="redAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Failure</a></span>
                        
                        </div>
                        <div class="both"></div>
                        <div id="aws_summary"></div>
                    </div>
                    <!-- google tab --> 
                    <div>
                        <div class="title">Google App Engine Health Summary</div>
                        <div class="legend">These charts display real-time health status and <strong>one week</strong> of health history for key Google App Engine services.
                           Click a Service in the left panel for detailed service health status, metrics, and more history. <br/><a href="/help/appengine.html" target="help">More information</a> on CloudStatus Google App Engine Support.
                           <ul>
                             <li>App Engine <a href="http://code.google.com/appengine/docs">documentation</a></li>
                        
                             <li>Google Groups hosts <a href="http://groups.google.com/group/google-appengine">discussion</a> and <a href="http://groups.google.com/group/google-appengine-downtime-notify">downtime</a> notifications</li>
                             <li>Get the <a href="http://support.hyperic.com/display/hypcomm/Google+App+Engine">App Engine HQ Plugin</a> for monitoring your own App Engine apps.</li>
                           </ul>
                        
                        </div>
                        <div class="clear" style="margin-bottom:20px"></div>
                        <div class="secLegend">
                           <span class="greenAvailS"> <a href="/help/appengine.html#health" target="help">Healthy</a> </span>
                           <span class="yellowAvailS"> <a href="/help/appengine.html#health" target="help">Service&nbsp;Issues</a> </span>
                        
                           <span class="redAvailS"> <a href="/help/appengine.html#health" target="help">Service&nbsp;Failure</a></span>
                        </div>
                        <div class="both"></div>
                        <div id="appengine_summary"></div>
                    </div>
                    <!-- salesforce tab --> 
                    <div>    
                        <div class="title">Salesforce Health Summary</div>
                        <div class="legend">These charts display real-time health status and <strong>one week</strong> of health history for key Salesforce services.</div>
                        <div class="clear" style="margin-bottom:20px"></div>
                        <div class="secLegend">
                           <span class="greenAvailS"> <a href="/help/appengine.html#health" target="help">Healthy</a> </span>
                           <span class="yellowAvailS"> <a href="/help/appengine.html#health" target="help">Service&nbsp;Issues</a> </span>

                           <span class="redAvailS"> <a href="/help/appengine.html#health" target="help">Service&nbsp;Failure</a></span>
                        </div>
                        <div class="both"></div>
                        <div id="salesforce_summary"></div>
                    </div>
                </div>
            </div>
        </div>
        <div id="z" style="display: none"></div>
        <script type="text/javascript">
        var cloudTabs = new YAHOO.widget.TabView('clouds');
        var tabs = [
            {
                id:     'aws',
                title:  'Amazon Web Services',
                legend: 'These charts display real-time health status and <strong>one week</strong> of health history for key <a href="http://www.amazonaws.com/" target="amazon" title="Amazon Web Service Home">Amazon Web Services</a>.',
                rle:    ''
            }
        ];

        // var d = document;
        var refInt = 90; //Set the pages Refresh Interval - for update timer and the pages coundown timer - in seconds
        var id1 = 149; //For ID Generator
        // var objIdx = -1; //For indexing the collection of widget references
        var objs = [];
        hyperic.widget.tempNode = dojo.byId('z');

        // var cprop = {expires:1}; //cookies should expire after 1d

        dojo11.addOnLoad(
            function() {
                loadData();
                // setInterval(loadData, refInt * 1000);
            }
        );

        /**
         * Called by the onload event and the refresh timer. 
         * This is the startup function for the page.
         */
        function loadData() {
            //Show the status update message
            // d.status.startUpdate();
            dojo11.xhrGet( {
                url : '/cloud.js?' + new Date().getTime(), //prevent caching
                handleAs : 'json',
                load : function (resp) {
                    buildPage(resp);
                    // d.status.endUpdate();
                }
            } );
        }

        /**
         * Called by the loaddata sucess callback. Builds each part 
         * of the page from the data retuned from the server
         */
        function buildPage(resp) {
            
            console.log(resp);

            //Remove all the old widgets before creating the new ones
            for (i = 0; i < objs.length; i++) {
                objs[i].cleanup();
                objs[i] = null;
            }
            objs = [];
            objIdx = -1;
            hyperic.widget.tooltip.cleanup();

            dojo.byId('overallSummary').innerHTML = '';
            // create the cloudstatus overview widgets
            var f = hyperic.widget.tempNode;
            for(var j in resp.providers) {
                if(typeof(resp.providers[j]) !== 'function') {
                    tmp_id = 'overall_' + resp.providers[j].code.toLowerCase() + '_summary';
                    f.innerHTML = '<div id="' + tmp_id + '"><h1 class="title" onclick="changeTabs(\''+ resp.providers[j].code.toLowerCase() +'\')">' + resp.providers[j].name + '</h1></div>';
                    dojo.byId('overallSummary').appendChild(f.firstChild);
                    for (var i in resp.providers[j].strips) {
                        if(typeof(resp.providers[j].strips[i]) !== 'function')
                        {
                            if(resp.providers[j].strips[i].stripType == 'health')
                            {
                                objs[++objIdx] = new hyperic.widget.Health(tmp_id, resp.providers[j].strips[i], true);
                            }
                            else
                            {
                                charts_container_id = (++id1) + '_charts';
                                f.innerHTML = '<div id="' + charts_container_id + '"></div>';
                                dojo.byId(tmp_id).appendChild(f.firstChild);
                                for(chart in resp.providers[j].strips[i].charts) {
                                    if(typeof(resp.providers[j].strips[i].charts[chart]) !== 'function')
                                    {
                                        objs[++objIdx] = new hyperic.widget.Chart(charts_container_id, resp.providers[j].strips[i].charts[chart], 'cso', chart + '_' + j + '_' + i , 'dashboard');
                                        // if(activeTab.id == 'cso'){
                                        //     objs[objIdx].showChart('cso');
                                        // }
                                    }
                                }
                                f.innerHTML = '<div style="clear: both;"></div>';
                                dojo.byId(charts_container_id).appendChild(f.firstChild);
                                delete charts_container_id;
                            }
                        }
                    }
                    tmp_id = null;
                }
            }
            delete f;
            // for(var j in resp.page.svcSummaryTab) {
            //     if(typeof(resp.page.svcSummaryTab[j]) !== 'function')
            //     {
            //         //clear out the attach points for the new widgets
            //         dojo.byId(j.toLowerCase() + '_summary').innerHTML = '';
            //         for (var i in resp.page.svcSummaryTab[j]) {
            //             if(typeof(resp.page.svcSummaryTab[j][i]) !== 'function')
            //             {
            //                 objs[++objIdx] = new hyperic.widget.Health(j.toLowerCase() + '_summary', resp.page.svcSummaryTab[j][i], true);
            //             }
            //         }
            //     }
            // }
            // // dojo.byId('sumHealth').innerHTML = '';
            // // var data = resp.page.svcSummaryTab.summary;
            // // for (i = 0; i < data.length; i++) {
            // //     objs[++objIdx] = new hyperic.widget.Health('sumHealth', data[i], true);
            // // }
            // //create each detail tab contents
            // data = resp.page.detailedDataTab;
            // // console.log(data);
            // for (i in data) {
            //     if(!0[i]){
            //         //clear out each sections iHTML.
            //         //health
            //         if(data[i].health)
            //         {
            //             //clear the node for the current section
            //             dojo.byId(i.toLowerCase() + '_health').innerHTML = '';
            //             objs[++objIdx] = new hyperic.widget.Health(i.toLowerCase() + '_health', data[i].health, false);
            //         }
            //         else
            //         {
            //             dojo.byId(i.toLowerCase() + '_health_section').style.display = 'none';
            //         }
            //         //charts
            //         //limit the charts to a max of 6
            //         var length = data[i].charts.length < 6 ? data[i].charts.length : 6;
            //         if(length > 0)
            //         {
            //             //clear the node for the current section
            //             dojo.byId(i.toLowerCase() + '_chartCont').innerHTML = '';
            //             for (var j = 0; j < length; j++) {
            //                 var chart_type = null;
            //                 if(data[i].charts[j].style !== undefined && data[i].charts[j].style == 'skinny') 
            //                 {
            //                     chart_type = 'skinny';
            //                 }
            //                 else
            //                 {
            //                     // (display chart full-width if only one chart is displayed, 
            //                     // or if the chart is the last on the page with an odd number of charts)
            //                     if(length==1 || (j == length-1 && length % 2)) {
            //                         chart_type = 'single';
            //                     }
            //                     else
            //                     {
            //                         chart_type = 'double';
            //                     }
            //                 }
            //                 // console.log(i + ': ' + data[i].charts[j].chartName + ' ('+data[i].charts[j].style+'): ' + chart_type);
            //                 objs[++objIdx] = new hyperic.widget.Chart(
            //                     i.toLowerCase() + '_chartCont', // chart container id
            //                     data[i].charts[j], // chart data
            //                     i.toLowerCase(), // tab id
            //                     j + 1, // chart position
            //                     chart_type); // chart type 
            //             }
            //         }
            //         else // if there are no charts to display, hide the chart header.
            //         {
            //             dojo.byId(i.toLowerCase() + '_performance_section').style.display = 'none';
            //         }
            //         //table
            //         if(data[i].table.length > 0)
            //         {
            //             //clear the node for the current section
            //             dojo.byId(i.toLowerCase() + '_table').innerHTML = '';
            //             for (var k = 0; k < data[i].table.length; k++) {
            //                 objs[++objIdx] = new hyperic.widget.Table(i.toLowerCase() + '_table', data[i].table[k]);
            //             }
            //         }
            //         else
            //         {
            //             dojo.byId(i.toLowerCase() + '_metrics_section').style.display = 'none';
            //         }
            //     }
            // }
            // //change the default tab to the last selected (cso)
            // if(activeTab.id == 'cso'){
            //     return;
            // }
            // dojo.byId('cso').className = 'tab';
            // dojo.byId('cso_tab').style.display = 'none';
            // dojo.byId(activeTab.id).className = 'activeTab';
            // dojo.byId(activeTab.id+'_tab').style.display = '';
            // dojo.publish('tabchange', [activeTab.id]);
        }

        // cloudTabs.addTab( new YAHOO.widget.Tab({
        //     label: 
        //     content: 
        //     active:
        // })
        </script>
<!-- end hqu plugin -->