{
    var rtimer = null;

    function showProblemResponse(originalRequest) {
        var response = eval("(" + originalRequest.responseText + ")");
        var mList = response.problems;
        var table = document.getElementById('problemResourcesTable');
        var urlColon = ":"
        var resUrl = $('viewResUrl').href;

        for (var i = table.childNodes.length - 1; i > 2; i--) {
            table.removeChild(table.childNodes[i]);
        }

        for (i = 0; i < mList.length; i++) {

            var tr = document.createElement('tr');
            var td1 = document.createElement('td');
            var td2 = document.createElement('td');
            var td3 = document.createElement('td');
            var td4 = document.createElement('td');
            var newanchor = document.createElement("a");

            table.appendChild(tr);

            if (i % 2 == 0) {
                tr.setAttribute((document.all ? 'className' : 'class'), "tableRowOdd");
            } else {
                tr.setAttribute((document.all ? 'className' : 'class'), "tableRowEven");
            }

            tr.appendChild(td1);
            td1.setAttribute((document.all ? 'className' : 'class'), "resource");
            td1.setAttribute("id", (mList[i].resource));

            if (mList[i].resourceName) {
                td1.appendChild(newanchor);
                newanchor.appendChild(document.createTextNode(mList[i].resourceName));
                newanchor.setAttribute('href', (resUrl + mList[i].resourceType + urlColon + mList[i].resourceId));
            }

            tr.appendChild(td2);
            td2.setAttribute((document.all ? 'className' : 'class'), "availability");

            if (mList[i].availability) {
                switch (mList[i].availability) {
                    case "green":
                        td2.innerHTML = "<img src=images/icon_available_green.gif>";
                        break;
                    case "red":
                        td2.innerHTML = "<img src=images/icon_available_red.gif>";
                        break;
                    case "yellow":
                        td2.innerHTML = "<img src=images/icon_available_yellow.gif>";
                        break;
                    case "orange":
                        td2.innerHTML = "<img src=images/icon_available_orange.gif>";
                        break;
                    default:
                        td2.innerHTML = "<img src=images/icon_available_error.gif>";
                }
            }

            tr.appendChild(td3);
            td3.setAttribute((document.all ? 'className' : 'class'), "alerts");

            if (mList[i].alerts) {
                td3.appendChild(document.createTextNode(mList[i].alerts));
            }

            tr.appendChild(td4);
            td4.setAttribute((document.all ? 'className' : 'class'), "oob");

            if (mList[i].oob) {
                td4.appendChild(document.createTextNode(mList[i].oob));
            }
        }

        rTimer = setTimeout('requestProblemResources();', 60000);
        //Refresh in 60 seconds
    }

    function showFavoriteResponse(originalRequest) {
        var tmp = eval('(' + originalRequest.responseText + ')');

        var fList = tmp.favorites;
        var table = document.getElementById('favoriteTable');
        var tbody = document.createElement('tbody');

        if (fList) {

            for (var i = table.childNodes.length - 1; i > 2; i--) {
                table.removeChild(table.childNodes[i]);
            }

            for (i = 0; i < fList.length; i++) {

                var tr = document.createElement('tr');
                var td1 = document.createElement('td');
                var td2 = document.createElement('td');
                var td3 = document.createElement('td');
                var td4 = document.createElement('td');
                var td5 = document.createElement('td');
                var favAnchor = document.createElement("a");
                var urlColon = ":"
                var resUrl = $('viewResUrl').href;

                table.appendChild(tbody);
                tbody.appendChild(tr);

                if (i % 2 == 0) {
                    tr.setAttribute((document.all ? 'className' : 'class'), "tableRowOdd");
                } else {
                    tr.setAttribute((document.all ? 'className' : 'class'), "tableRowEven");
                }

                tr.appendChild(td1);
                td1.setAttribute((document.all ? 'className' : 'class'), "resourceName");
                td1.setAttribute("id", (fList[i].resourceName));

                if (fList[i].resourceName && favAnchor && fList[i].resourceId && fList[i].resourceTypeId) {
                    td1.appendChild(favAnchor);
                    favAnchor.appendChild(document.createTextNode(fList[i].resourceName));
                    favAnchor.setAttribute('href', (resUrl + fList[i].resourceTypeId + urlColon + fList[i].resourceId));
                } else {
                    td1.innerHTML = "&nbsp;";
                }

                tr.appendChild(td2);
                td2.setAttribute((document.all ? 'className' : 'class'), "resourceTypeName");
                td2.setAttribute("id", (fList[i].resourceTypeName));

                if (fList[i].resourceTypeName) {
                    td2.appendChild(document.createTextNode(fList[i].resourceTypeName));
                } else {
                    // XXX: use common.value.notavail
                    td2.innerHTML = "N/A";
                }

                tr.appendChild(td3);
                td3.setAttribute((document.all ? 'className' : 'class'), "throughput");
                td3.setAttribute("id", (fList[i].throughput));

                if (fList[i].throughput) {
                    td3.appendChild(document.createTextNode(fList[i].throughput));

                } else {
                    // XXX: use common.value.notavail
                    td3.innerHTML = "N/A";
                }

                tr.appendChild(td4);
                td4.setAttribute((document.all ? 'className' : 'class'), "availability");
                td4.setAttribute("id", (fList[i].availability));

                if (fList[i].availability) {
                    td4.appendChild(document.createTextNode(fList[i].availability));
                    switch (fList[i].availability) {
                        case "green":
                            td4.innerHTML = "<img src=/images/icon_available_green.gif>";
                            break;
                        case "red":
                            td4.innerHTML = "<img src=/images/icon_available_red.gif>";
                            break;
                        case "yellow":
                            td4.innerHTML = "<img src=/images/icon_available_yellow.gif>";
                            break;
                        case "orange":
                            td4.innerHTML = "<img src=/images/icon_available_orange.gif>";
                            break;
                        default:
                            td4.innerHTML = "<img src=/images/icon_available_error.gif>";
                    }

                } else {
                    // XXX: use common.value.notavail
                    td4.innerHTML = "N/A";
                }

                tr.appendChild(td5);
                td5.setAttribute((document.all ? 'className' : 'class'), "alerts");

                if (fList[i].alerts) {
                    td5.appendChild(document.createTextNode(fList[i].alerts));
                } else {
                    td5.innerHTML = "0";
                }
            }
        } else {
            $('noFaveResources').style.display = '';
        }
        //var rTimer = setTimeout(showFavoriteResponse,20000); //Refresh in 60 seconds
    }

    function reportError(originalRequest) {
        alert('Error ' + originalRequest.status + ' -- ' + originalRequest.statusText);
    }

    function showRecentAlerts(originalRequest) {
        var alertText = eval("(" + originalRequest.responseText + ")");
        var aList = alertText.criticalAlerts;
        var token = alertText.token;
        var alertTable;
        var alertFunc;
        if (alertText.token != null) {
            alertTable = document.getElementById('recentAlertsTable' + token);
            alertFunc = 'requestRecentAlerts' + token + '()';
        } else {
            alertTable = document.getElementById('recentAlertsTable');
            alertFunc = 'requestRecentAlerts()';
        }

        if (aList != 0) {
            $('noCritAlerts').style.display = 'none';

            for (var i = alertTable.childNodes.length - 1; i > 1; i--) {
                alertTable.removeChild(alertTable.childNodes[i]);
            }

            for (i = 0; i < aList.length; i++) {

                var tr = document.createElement('tr');
                var td1 = document.createElement('td');
                var td2 = document.createElement('td');
                var td3 = document.createElement('td');
                var td4 = document.createElement('td');
                var td5 = document.createElement('td');
                var alertAnchor = document.createElement("a");
                var checkBox = document.createElement("input");
                var urlAmp = "&a="
                var alertUrl = $('viewAlertUrl').href;

                alertTable.appendChild(tr);
                tr.setAttribute((document.all ? 'className' : 'class'), "ListRow");

                tr.appendChild(td1);
                td1.setAttribute((document.all ? 'className' : 'class'), "ListCellCheckbox");

                td1.innerHTML = "<input type=checkbox name=alerts value=" + aList[i].alertId + " onclick=\"ToggleSelection(this, widgetProperties, false);\" class=listMember>";

                tr.appendChild(td2);
                td2.setAttribute((document.all ? 'className' : 'class'), "ListCell");

                if (aList[i].cTime && aList[i].appdefKey && aList[i].alertId) {
                    td2.appendChild(alertAnchor);
                    alertAnchor.appendChild(document.createTextNode(aList[i].cTime));
                    alertAnchor.setAttribute('href', (alertUrl + aList[i].appdefKey + urlAmp + aList[i].alertId));
                }

                tr.appendChild(td3);
                td3.setAttribute((document.all ? 'className' : 'class'), "alertType");
                if (aList[i].alertDefName) {
                    td3.appendChild(document.createTextNode(aList[i].alertDefName));
                }

                tr.appendChild(td4);
                td4.setAttribute((document.all ? 'className' : 'class'), "resourceNameAlert");

                if (aList[i].resourceName) {
                    td4.appendChild(document.createTextNode(aList[i].resourceName));
                }
                tr.appendChild(td5);
                td5.setAttribute((document.all ? 'className' : 'class'), "resourceNameAlert");

                if (aList[i].fixed) {
                    td5.appendChild(document.createTextNode("Yes"));
                } else {
                    td5.appendChild(document.createTextNode("No"));
                }
            }
        } else {
            $('noCritAlerts').style.display = '';
        }

        rTimer = setTimeout(alertFunc, 60000);
        //Refresh in 60 seconds
    }

    function showAvailSummary(originalRequest) {
        var availText = eval("(" + originalRequest.responseText + ")");
        var availList = availText.availSummary;
        var browseUrl = $('browseUrl').href;
        var urlColon = ":";
        var urlParams = "&view=list&ft=";
        var token = availText.token;
        var noAvailTable;
        var availTable;
        var tbody = document.createElement('tbody');
        var availFunc;
        if (token != null) {
            availTable = document.getElementById('availTable' + token);
            noAvailTable = 'noAvailTable' + token;
            availFunc = 'requestAvailSummary' + token + '()';
        } else {
            availTable = document.getElementById('availTable');
            noAvailTable = 'noAvailTable';
            availFunc = 'requestAvailSummary()';
        }
        
        if (availList.length < 1) {
            $(noAvailTable).style.display = '';
        } else {

            for (var i = availTable.childNodes.length - 1; i > 1; i--) {
                availTable.removeChild(availTable.childNodes[i]);
            }

            for (var i = 0; i < availList.length; i++) {

                var tr = document.createElement('tr');
                var td1 = document.createElement('td');
                var td2 = document.createElement('td');
                var newanchor = document.createElement("a");
                var up = availList[i].numUp;
                var down = availList[i].numDown;

                availTable.appendChild(tbody);
                tbody.appendChild(tr);
                tr.setAttribute((document.all ? 'className' : 'class'), "ListRow");

                tr.appendChild(td1);
                td1.setAttribute((document.all ? 'className' : 'class'), "resourceTypeName");

                td1.appendChild(newanchor);
                newanchor.appendChild(document.createTextNode(availList[i].resourceTypeName));
                newanchor.setAttribute('href', (browseUrl + availList[i].appdefType + urlParams + availList[i].appdefType + urlColon + availList[i].appdefTypeId));
                tr.appendChild(td2);
                td2.setAttribute((document.all ? 'className' : 'class'), "availResourceStatus");

                if (down > '0') {
                    td2.innerHTML = '<span style=color:red;>' + down + '</span>' + " | " + up;
                } else {
                    td2.innerHTML = down + " | " + up;
                }
            }
        }

        rTimer = setTimeout(availFunc, 60000);
    }

    function showMetricsResponse(originalRequest) {

        var metricText = eval("(" + originalRequest.responseText + ")");
        var metricValues = metricText.metricValues;
        var resourceNameHeader = metricValues.resourceTypeName;
        var resourceLoadTypeHeader = metricValues.metricName;
        var urlColon = ":"
        var resUrl = $('viewResUrl').href;
        var metricTable;
        var noMetricTable;
        var tbody = document.createElement('tbody');
        var metricFunc
        var token = metricText.token;
        if (token != null) {
            metricTable = document.getElementById('metricTable' + token);
            noMetricTable = 'noMetricTable' + token;
            metricFunc = 'requestMetricsResponse' + token + '()';
        } else {
            metricTable = document.getElementById('metricTable');
            noMetricTable = 'noMetricTable';
            metricFunc = 'requestMetricsResponse()';
        }
        
        if (metricValues.values) {

            for (var i = metricTable.childNodes.length - 1; i > 1; i--) {
                metricTable.removeChild(metricTable.childNodes[i]);
            }

            // Create table headers
            var trHeader = document.createElement('tr');
            var th1 = document.createElement('th');
            var th2 = document.createElement('th');

            metricTable.appendChild(tbody);
            tbody.appendChild(trHeader);
            trHeader.setAttribute("class", "tableRowHeader");
            trHeader.appendChild(th1);
            th1.setAttribute("width", "90%");
            th1.setAttribute("class", "tableRowInactive");
            th1.style.textAlign = "left";
            th1.style.backgroundColor = "#DBE3F5";
            th1.style.padding = "3px";
            th1.style.borderBottom = "1px solid #D5D8DE";
            th1.appendChild(document.createTextNode(resourceNameHeader));
            trHeader.appendChild(th2);
            th2.setAttribute("width", "10%");
            th2.setAttribute("class", "tableRowInactive");
            th2.style.backgroundColor = "#DBE3F5";
            th2.style.padding = "3px";
            th2.style.textAlign = "left";
            th2.style.borderBottom = "1px solid #D5D8DE";
            th2.setAttribute("nowrap", true);
            th2.appendChild(document.createTextNode(resourceLoadTypeHeader));

            for (i = 0; i < metricValues.values.length; i++) {
                var newanchor = document.createElement("a");
                var tr = document.createElement('tr');
                var td1 = document.createElement('td');
                var td2 = document.createElement('td');

                tbody.appendChild(tr);
                tr.setAttribute((document.all ? 'className' : 'class'), "ListRow");

                tr.appendChild(td1);
                td1.setAttribute((document.all ? 'className' : 'class'), "resource");
                if (metricValues.values[i].resourceName) {
                    td1.appendChild(newanchor);
                    newanchor.appendChild(document.createTextNode(metricValues.values[i].resourceName));
                    newanchor.setAttribute('href', (resUrl + metricValues.values[i].resourceTypeId + urlColon + metricValues.values[i].resourceId));
                }

                tr.appendChild(td2);
                td2.setAttribute((document.all ? 'className' : 'class'), "metricName");
                td2.appendChild(document.createTextNode(metricValues.values[i].value));
            }

        } else {
            $(noMetricTable).style.display = '';
        }

        rTimer = setTimeout(metricFunc, 60000);
        //Refresh in 60 seconds
    }
}
