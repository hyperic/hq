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
                        <!-- <div id="amazon_services" class="yui-buttongroup"> 
                            <input id="aws_ec2" type="radio" name="aws_service" value="Elastic Compute Cloud (EC2)" checked>
                            <input id="aws_s3" type="radio" name="aws_service"  value="Simple Storage Service (S3)">
                            <input id="aws_sqs" type="radio" name="aws_service" value="Simple Queue Service (SQS)">
                            <input id="aws_sdb" type="radio" name="aws_service" value="Simple DB (SDB)">
                            <input id="aws_fps" type="radio" name="aws_service" value="Flexible Payment Service (FPS)">
                        </div> -->
                        

                        <div id="amazon_services" class="yui-navset">
                            <ul class="yui-nav">
                                <li class="selected"><a href="#aws_overview"><em>Amazon Overview</em></a></li>
                                <li><a href="#aws_ec2"><em>Elastic Compute Cloud (EC2)</em></a></li>
                                <li><a href="#aws_s3"><em>Simple Storage Service (S3)</em></a></li>
                                <li><a href="#aws_sqs"><em>Simple Queue Service (SQS)</em></a></li>
                                <li><a href="#aws_sdb"><em>Simple DB (SDB)</em></a></li>
                                <li><a href="#aws_fps"><em>Flexible Payment Service (FPS)</em></a></li>
                            </ul>            
                            <div class="yui-content">
                                <!-- amazon summary subtab --> 
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
                                <!-- EC2 tab --> 
                                <div>
                                    <div class="title">Elastic Compute Cloud (EC2)</div>
                                    <div class="legend">These charts display real-time health status and <strong>one week</strong> of health history for  <a href="http://aws.amazon.com/ec2" target="amazon" title="Amazon Elastic Compute Cloud Service">Amazon Elastic Compute Cloud Service</a>.<br/> How we monitor <a href="/help/ec2.html" target="help">Elastic Compute Cloud</a>.</div>

                                    <div class="section" id="aws_ec2_health_section">
                                       <h3>Health</h3>
                                       <div class="secLegend">
                                          <span class="greenAvailS"> <a href="/help/ec2.html#health" target="help">Healthy</a> </span>
                                          <span class="yellowAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Issues</a> </span>

                                          <span class="redAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Failure</a></span>
                                       </div>
                                       <div class="both"></div><hr/>
                                    </div>
                                    <div id="aws_ec2_health"></div> 
                                    <div class="section" id="aws_ec2_performance_section">
                                       <h3>Performance</h3><div class="both"></div><hr/>

                                    </div>
                                    <div id="aws_ec2_chartCont"></div>
                                    <div class="both"></div>
                                    <div class="section" id="aws_ec2_metrics_section">
                                        <h3>Metrics</h3><div class="both"></div><hr/>
                                    </div>
                                    <div id="aws_ec2_table"></div>
                                </div>
                                <!-- S3 tab --> 
                                <div>
                                    <div class="title">Simple Storage Service (S3)</div>
                                    <div class="legend">These charts display real-time health status and <strong>one week</strong> of health history for <a href="http://aws.amazon.com/s3" target="amazon" title="Amazon Simple Storage Service">Amazon Simple Storage Service</a>.<br/> How we monitor <a href="/help/s3.html" target="help">Simple Storage Service</a>.</div>

                                    <div class="section" id="aws_s3_health_section">
                                       <h3>Health</h3>
                                       <div class="secLegend">
                                          <span class="greenAvailS"> <a href="/help/s3.html#health" target="help">Healthy</a> </span>
                                          <span class="yellowAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Issues</a> </span>

                                          <span class="redAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Failure</a></span>
                                       </div>
                                       <div class="both"></div><hr/>
                                    </div>
                                    <div id="aws_s3_health"></div> 
                                    <div class="section" id="aws_s3_performance_section">
                                       <h3>Performance</h3><div class="both"></div><hr/>

                                    </div>
                                    <div id="aws_s3_chartCont"></div>
                                    <div class="both"></div>
                                    <div class="section" id="aws_s3_metrics_section">
                                        <h3>Metrics</h3><div class="both"></div><hr/>
                                    </div>
                                    <div id="aws_s3_table"></div>
                                </div>
                                <!-- SQS tab --> 
                                <div>
                                    <div class="title">Simple Queue Service (SQS)</div>
                                    <div class="legend">These charts display real-time health status and <strong>one week</strong> of health history for <a href="http://aws.amazon.com/sqs" target="amazon" title="Amazon Simple Queue Service.">Amazon Simple Queue Service.</a>. <br/> How we monitor <a href="/help/sqs.html" target="help">Simple Queue Service</a>. </div>

                                    <div class="section" id="aws_sqs_health_section">
                                       <h3>Health</h3>
                                       <div class="secLegend">
                                          <span class="greenAvailS"> <a href="/help/s3.html#health" target="help">Healthy</a> </span>
                                          <span class="yellowAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Issues</a> </span>

                                          <span class="redAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Failure</a></span>
                                       </div>
                                       <div class="both"></div><hr/>
                                    </div>
                                    <div id="aws_sqs_health"></div> 
                                    <div class="section" id="aws_sqs_performance_section">
                                       <h3>Performance</h3><div class="both"></div><hr/>

                                    </div>
                                    <div id="aws_sqs_chartCont"></div>
                                    <div class="both"></div>
                                    <div class="section" id="aws_sqs_metrics_section">
                                        <h3>Metrics</h3><div class="both"></div><hr/>
                                    </div>
                                    <div id="aws_sqs_table"></div>
                                </div>
                                <!-- SDB tab --> 
                                <div>
                                    <div class="title">Simple DB (SDB)</div>
                                    <div class="legend">These charts display real-time health status and <strong>one week</strong> of health history for <a href="http://aws.amazon.com/sdb" target="amazon" title="Amazon Simple DB Service">Amazon Simple DB Service</a>. <br/>How we monitor <a href="/help/sdb.html" target="help">Simple DB</a>. </div>
                                    <div class="section" id="aws_sdb_health_section">

                                       <h3>Health</h3>
                                       <div class="secLegend">
                                          <span class="greenAvailS"> <a href="/help/s3.html#health" target="help">Healthy</a> </span>
                                          <span class="yellowAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Issues</a> </span>
                                          <span class="redAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Failure</a></span>

                                       </div>
                                       <div class="both"></div><hr/>
                                    </div>
                                    <div id="aws_sdb_health"></div> 
                                    <div class="section" id="aws_sdb_performance_section">
                                       <h3>Performance</h3><div class="both"></div><hr/>
                                    </div>
                                    <div id="aws_sdb_chartCont"></div>
                                    <div class="both"></div>

                                    <div class="section" id="aws_sdb_metrics_section">
                                        <h3>Metrics</h3><div class="both"></div><hr/>
                                    </div>
                                    <div id="aws_sdb_table"></div>
                                </div>
                                <!-- FPS tab --> 
                                <div>
                                    <div class="title">Flexible Payment Service (FPS)</div>
                                    <div class="legend">These charts display real-time health status and <strong>one week</strong> of health history for <a href="http://aws.amazon.com/fps" target="amazon" title="Amazon Flexible Payment Service">Amazon Flexible Payment Service</a>. <br/> How we monitor <a href="/help/fps.html" target="help">Flexible Payment Service</a>. </div>

                                    <div class="section" id="aws_fps_health_section">
                                       <h3>Health</h3>
                                       <div class="secLegend">
                                          <span class="greenAvailS"> <a href="/help/s3.html#health" target="help">Healthy</a> </span>
                                          <span class="yellowAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Issues</a> </span>

                                          <span class="redAvailS"> <a href="/help/aws.html#health" target="help">Service&nbsp;Failure</a></span>
                                       </div>
                                       <div class="both"></div><hr/>
                                    </div>
                                    <div id="aws_fps_health"></div> 
                                    <div class="section" id="aws_fps_performance_section">
                                       <h3>Performance</h3><div class="both"></div><hr/>

                                    </div>
                                    <div id="aws_fps_chartCont"></div>
                                    <div class="both"></div>
                                    <div class="section" id="aws_fps_metrics_section">
                                        <h3>Metrics</h3><div class="both"></div><hr/>
                                    </div>
                                    <div id="aws_fps_table"></div>
                                </div>
                            </div>
                        </div>
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
        <script type="text/javascript">
        var cloudTabs = new YAHOO.widget.TabView("clouds");
        var amazonTabs = new YAHOO.widget.TabView("amazon_services");
        // var amazonButtons = new YAHOO.widget.ButtonGroup("amazon_services") 
        </script>
<!-- end hqu plugin -->