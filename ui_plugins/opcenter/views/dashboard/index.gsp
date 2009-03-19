<%= dojoInclude(["dojo.event.*",
                 "dojo.collections.Store",
                 "dojo.widget.ContentPane",
                 "dojo.widget.TabContainer",
                 "dojo.widget.FilteringTable"]) %>
<%= hquStylesheets() %>

<style type="text/css">

    .OpStyleRed {
        background-color: lightpink;
    }

    .OpStyleGreen {
        background-color: lightgreen;
    }

    .OpStyleYellow {
        background-color: lightgoldenrodyellow;
    }

    .OpStyleOrange {
        background-color: lightsalmon;
    }

    .OpStyleGray {
        background-color: lightgray;
    }

</style>

<!-- Summary header -->
<div id="OpsHeader">
    <div id="OpsHeaderFilters" style="float:left;margin-right:30px">
        <form id="FiltersForm" action="javascript:filtersChanged();">
            <table>
                <tr>
                    <td colspan="2" style="font-weight:bold;text-align:center">Display Filters</td>
                </tr>
                <tr>
                    <td>Status Type:</td>
                    <td>
                        <select id="StatusFilter" onchange="filtersChanged();">
                            <option value="0">All</option>
                            <option value="1">Down Resources</option>
                            <option value="2" selected="selected">All Alerts</option>
                            <option value="3">Alerts in Escalation</option>
                            <option value="4">Alerts without Escalation</option>

                        </select>
                    </td>
                </tr>
                <tr>
                    <td>Platform Filter:</td>
                    <td>
                        <input id="HostnameFilter" type="text" size="20" />
                    </td>
                </tr>
                <tr>
                    <td>Group Filter:</td>
                    <td>
                        <select onchange="filtersChanged();">
                            <option value="-1" selected="selected">None</option>
                            <%
                                groups.each {
                                    out.write('<option value="' + it.id + '">' + it.name + '</option>')
                                }
                            %>
                        </select>
                    </td>
                </tr>
            </table>
        </form>
    </div>

    <div id="OpsHeaderResourceTotals" style="float:left;margin-right:30px">
        <table>
            <tr><td colspan="2" style="text-align:center;font-weight:bold">Current View Totals</td></tr>
            <tr>
                <td valign="top">
                    <table border="1">
                        <tr><td colspan="2" style="text-align:center;font-weight:bold">Resources</td></tr>
                        <tr><td>Down Platforms</td><td class="OpStyleGray" style="width:50px"><div id="DownPlatforms">&nbsp;</div></td></tr>
                        <tr><td>Down Resources</td><td class="OpStyleGray" style="width:50px"><div id="DownResources">&nbsp;</div></td></tr>
                    </table>
                </td>
                <td valign="top">
                    <table border="1">
                        <tr><td colspan="5" style="text-align:center;font-weight:bold">Alerts</td></tr>
                        <tr>
                            <td>&nbsp;</td>
                            <td>Low</td>
                            <td>Medium</td>
                            <td>High</td>
                            <td>Total</td>
                        </tr>
                        <tr>
                            <td>Unfixed Alerts</td>
                            <td class="OpStyleYellow" style="width:50px"><div id="AlertsUnfixedLow">&nbsp;</div></td>
                            <td class="OpStyleOrange" style="width:50px"><div id="AlertsUnfixedMed">&nbsp;</div></td>
                            <td class="OpStyleRed" style="width:50px"><div id="AlertsUnfixedHigh">&nbsp;</div></td>
                            <td class="OpStyleGray" style="width:50px"><div id="AlertsUnfixed">&nbsp;</div></td>
                        </tr>
                        <tr>
                            <td>Alerts in Escalation</td>
                            <td class="OpStyleYellow" style="width:50px"><div id="AlertsInEscLow">&nbsp;</div></td>
                            <td class="OpStyleOrange" style="width:50px"><div id="AlertsInEscMed">&nbsp;</div></td>
                            <td class="OpStyleRed" style="width:50px"><div id="AlertsInEscHigh">&nbsp;</div></td>
                            <td class="OpStyleGray" style="width:50px"><div id="AlertsInEsc">&nbsp;</div></td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
    </div>

   <div id="OpsHeaderTableControls" style="float:right;magin-right:10px">
        <form id="TableControlsForm" action="javascript:dashboardTable_refreshTable();">
            <table>
                <tr>
                    <td colspan="2" style="font-weight:bold;text-align:center">Table Controls</td>
                </tr>
                <tr>
                    <td>
                        Items per page:
                    </td>
                    <td>
                        <select id="PageSize" onchange="updatePageSize();">
                            <option value="15">15</option>
                            <option value="30">30</option>
                            <option value="50" selected="selected">50</option>
                            <option value="75">75</option>
                            <option value="100">100</option>
                            <option value="32767">All</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td>
                        Refresh interval:
                    </td>
                    <td>
                        <select id="Refresh" onchange="updateRefreshInterval();">
                            <option value="60000" selected="selected">1 minute</option>
                            <option value="120000">2 minutes</option>
                            <option value="300000">5 minutes</option>
                            <option value="600000">10 minutes</option>
                            <option value="900000">15 minutes</option>
                            <option value="1800000">30 minutes</option>
                            <option value="2700000">45 minutes</option>
                            <option value="3600000">60 minutes</option>
                            <option value="9223372036854775807">None</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td colspan="2"><div id="OpHeaderLastUpdated" style="font-weight:bold">&nbsp;</div></td>
                </tr>
            </table>
        </form>
    </div>

    <div style="clear:both"></div>
</div>

<form id="dashboardTable_FixForm" name="dashboardTable_FixForm" method="POST" action="/alerts/RemoveAlerts.do">

    <!-- Item details -->
    <%= dojoTable(id:'dashboardTable', title:'Resource Details for All Hosts',
                  refresh:60, url:urlFor(action:'updateDashboard'),
                  schema:DASHBOARD_SCHEMA, numRows:50, pageControls:true)
    %>
    <div id="HQAlertCenterDialog" style="display:none;"></div>
    <div id="TableFooter">
        <div id="dashboardTable_FixedButtonDiv" style="margin-top:6px;float:left">
            <input type="button" id="dashboardTable_FixButton" value="FIXED"
                    class="CompactButtonInactive" disabled="disabled"
                    onclick="MyAlertCenter.processButtonAction(this)" />
            &nbsp;&nbsp;
            <input type="button" id="dashboardTable_AckButton" value="ACKNOWLEDGE"
                   class="CompactButtonInactive" disabled="disabled"
                onclick="MyAlertCenter.processButtonAction(this)" />
            <input type="hidden" name="buttonAction" value="" />
            <input type="hidden" name="output" value="json" />
            <input type="hidden" name="fixedNote" value="" />
            <input type="hidden" name="ackNote" value="" />
            <input type="hidden" name="fixAll" value="false" />
            <input type="hidden" name="pauseTime" value="" />
        </div>
    </div>
</form>

<script type="text/javascript">

    dojo11.require("dijit.dijit");
    dojo11.require("dijit.Dialog");
    dojo11.require("dijit.ProgressBar");

    var MyAlertCenter = null;
    dojo11.addOnLoad(function(){
        MyAlertCenter = new hyperic.alert_center("Operations Center");

        dojo11.connect("dashboardTable_refreshTable", function() { MyAlertCenter.resetAlertTable(dojo11.byId('dashboardTable_FixForm')); });
    });

    // Handle update to the page size
    function updatePageSize() {

        var form = document.getElementById("TableControlsForm");

        _hqu_dashboardTable_pageSize = form.elements[0].value;
        _hqu_dashboardTable_pageNum = 0;

        dashboardTable_refreshTable();
    }

    // Handle update to the table refresh interval
    function updateRefreshInterval() {

        var form = document.getElementById("TableControlsForm");

        var refreshInterval = form.elements[1].value;

        clearTimeout(_hqu_dashboardTable_refreshTimeout);
        if (refreshInterval != 9223372036854775807) {
            _hqu_dashboardTable_refreshTimeout = setTimeout("_hqu_dashboardTable_autoRefresh()", refreshInterval);
            dashboardTable_refreshTable();
        }
    }

    // Called when any filter is changed to reset the page number and refresh the table.
    function filtersChanged() {
        _hqu_dashboardTable_pageNum = 0;
        dashboardTable_refreshTable();
    }

    // Called prior to table refresh to get current filters
    function getDashboardFilters() {

        var form = document.getElementById("FiltersForm");

        var typeFilter = form.elements[0].value;
        var platformFilter = form.elements[1].value;
        var groupFilter = form.elements[2].value;

        var res = {};
        if (typeFilter != null) {
            res['typeFilter'] = typeFilter;
        }
        if (platformFilter != null) {
            res['platformFilter'] = platformFilter;
        }
        if (groupFilter != null) {
            res['groupFilter'] = groupFilter;
        }

        return res;
    }

    function formatValue(val) {
        if (val == undefined) {
            return "N/A";
        } else {
            return val;
        }
    }

    // Called post table refresh with current table data
    function refreshSummaryData(data) {

        var summaryInfo = data.summaryinfo[0];
        
        var updatedDate = new Date(summaryInfo.LastUpdated);
        document.getElementById("OpHeaderLastUpdated").innerHTML =
            "Updated at " + updatedDate.formatDate('hh:mm:ss') +
            ", population took " + summaryInfo.FetchTime + " ms.";

        document.getElementById("DownPlatforms").innerHTML = formatValue(summaryInfo.DownPlatforms);
        document.getElementById("DownResources").innerHTML = formatValue(summaryInfo.DownResources);

        document.getElementById("AlertsUnfixedLow").innerHTML  = formatValue(summaryInfo.AlertsUnfixedLow);
        document.getElementById("AlertsUnfixedMed").innerHTML  = formatValue(summaryInfo.AlertsUnfixedMed);
        document.getElementById("AlertsUnfixedHigh").innerHTML = formatValue(summaryInfo.AlertsUnfixedHigh);
        document.getElementById("AlertsUnfixed").innerHTML     = formatValue(summaryInfo.AlertsUnfixed);

        document.getElementById("AlertsInEscLow").innerHTML  = formatValue(summaryInfo.AlertsInEscLow);
        document.getElementById("AlertsInEscMed").innerHTML  = formatValue(summaryInfo.AlertsInEscMed);
        document.getElementById("AlertsInEscHigh").innerHTML = formatValue(summaryInfo.AlertsInEscHigh);
        document.getElementById("AlertsInEsc").innerHTML     = formatValue(summaryInfo.AlertsInEsc);

        document.getElementById("_hqu_dashboardTable_pageNumbers").innerHTML =
        "Page " + (_hqu_dashboardTable_pageNum + 1) + " of " + summaryInfo.NumPages;
    }

    dashboardTable_addUrlXtraCallback(getDashboardFilters);
    dashboardTable_addRefreshCallback(refreshSummaryData);

</script>