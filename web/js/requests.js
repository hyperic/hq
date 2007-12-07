
{
    Ajax.Responders.register({
        onCreate: function() {
            if ($('loading') && Ajax.activeRequestCount > 0)
                Effect.Appear('loading', {duration: 0.50, queue: 'end'});
        },
        onComplete: function() {
            if ($('loading') && Ajax.activeRequestCount == 0)
                Effect.Fade('loading', {duration: 0.2, queue: 'end'});
        }
    });

    var rtimer = null;


    function showProblemResponse(originalRequest) {

        var probResp = eval("(" + originalRequest.responseText + ")");
        var mList = probResp.problems;
        var problemTable = document.getElementById('problemResourcesTable');
        var urlColon = ":"
        var resUrl = $('viewResUrl').href;
        var noProblemResources = $('noProblemResources');
        $('modifiedProblemTime').innerHTML = 'Updated: ' + refreshTime();

            if (mList && mList.length > 0) {

                var tbody = problemTable.getElementsByTagName('tbody')[0];
                var browser = BrowserDetect.browser;

                if (browser == "Safari") {
                    for (var i = tbody.childNodes.length; i > 1; i--) {
                    tbody.removeChild(tbody.childNodes[i]);
                    }
                } else {
                    for (var i = tbody.childNodes.length - 1; i > 1; i--) {
                    tbody.removeChild(tbody.childNodes[i]);
                    }
                }
                
                for (i = 0; i < mList.length; i++) {

                    var tr = document.createElement('tr');
                    var trTime = document.createElement('tr');
                    var td1 = document.createElement('td');
                    var td2 = document.createElement('td');
                    var td3 = document.createElement('td');
                    var td4 = document.createElement('td');
                    var td5 = document.createElement('td');
                    var newanchor = document.createElement("a");

                    tbody.appendChild(tr);

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

                    if (mList[i].alerts != '') {
                        td3.appendChild(document.createTextNode(mList[i].alerts));
                    } else {
                        td3.appendChild(document.createTextNode("0"));
                    }
                    
                    tr.appendChild(td4);
                    td4.setAttribute((document.all ? 'className' : 'class'), "oob");
                    td4.appendChild(document.createTextNode(mList[i].oob));

                    tr.appendChild(td5);
                    td5.setAttribute((document.all ? 'className' : 'class'), "latest");
                    td5.setAttribute("nowrap", "true");
                    td5.appendChild(document.createTextNode(mList[i].latest));

                }

            
        } else {
            $('noProblemResources').style.display = '';
        }



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

        var tbody = alertTable.getElementsByTagName('tbody')[0];

        var noCritAlerts = alertText.token != null ?
                           $('noCritAlerts' + token) : $('noCritAlerts');

        var ackInstruction = alertText.token != null ?
                           $('ackInstruction' + token) : $('ackInstruction');

        ackInstruction.style.display = 'none';

        if (aList.length != 0) {
            noCritAlerts.style.display = 'none';

            for (var i = tbody.childNodes.length; i > 0; i--) {
                tbody.removeChild(tbody.childNodes[i - 1]);
            }

            var alertUrl = $('viewAlertUrl').href;

            for (i = 0; i < aList.length; i++) {

                var tr = document.createElement('tr');
                var td1 = document.createElement('td');
                var td2 = document.createElement('td');
                var td3 = document.createElement('td');
                var td4 = document.createElement('td');
                var td5 = document.createElement('td');
                var td6 = document.createElement('td');
                var alertAnchor = document.createElement("a");
                var checkBox = document.createElement("input");
                var urlAmp = "&a="

                tbody.appendChild(tr);
                tr.setAttribute((document.all ? 'className' : 'class'), "ListRow");

                tr.appendChild(td1);
                td1.setAttribute((document.all ? 'className' : 'class'), "ListCellCheckbox");

                if (aList[i].fixed) {
                  td1.innerHTML = "&nbsp;";
                }
                else {
                  var checkbox = document.createElement('input');
                  checkbox.setAttribute("type", "checkbox");
                  checkbox.setAttribute("name", "ealerts");
                  checkbox.setAttribute("class", "listMember");
                  //checkbox.setAttribute("onclick", "ToggleSelection(this, widgetProperties, false);");
                  checkbox.onclick = new Function("ToggleSelection(this, widgetProperties, false)");
                  checkbox.setAttribute("value",
                                        aList[i].alertType + ":" + aList[i].alertId);
                  td1.appendChild(checkbox);
                }

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
                td4.setAttribute((document.all ? 'className' : 'class'), "resourceNameAlertLeft");

                if (aList[i].resourceName) {
                    td4.appendChild(document.createTextNode(aList[i].resourceName));
                }
                tr.appendChild(td5);
                td5.setAttribute((document.all ? 'className' : 'class'), "resourceNameAlert");

                td5.setAttribute("align", "center");
                if (aList[i].fixed) {
                    td5.appendChild(document.createTextNode("Yes"));
                } else {
                    td5.appendChild(document.createTextNode("No"));
                }

                tr.appendChild(td6);
                td6.setAttribute((document.all ? 'className' : 'class'), "resourceNameAlert");

                td6.setAttribute("align", "center");
                if (aList[i].acknowledgeable) {
                    var ackAnchor = document.createElement("a");
                    //ackAnchor .setAttribute("href", ".");
                     td6.appendChild(ackAnchor);
                     ackAnchor.setAttribute("text-decoration", "none");

                    var imgNode = document.createElement('img');

                    imgNode.setAttribute("src", "images/icon_ack.gif");
                    imgNode.setAttribute("border", "0");
                    imgNode.setAttribute("alt", "Acknowledge");
                    imgNode.setAttribute('id', 'ack_'+ aList[i].alertId);

                    ackAnchor.appendChild(imgNode);
                    ackAnchor.setAttribute("href", "javascript:acknowledgeAlert(this,'" + aList[i].appdefKey + "'," + aList[i].alertId + ");");

                    //imgNode.onclick = new Function("imgHide", "this.parentNode.innerHTML='';");
                    //ackAnchor.onclick = new Function("imgHide", "this.parentNode.innerHTML='&nbsp;';");


                    imgNode.onclick = new Function("imgHide", "this.src ='images/spacer.gif';this.border='0';");
                    ackInstruction.style.display = "";

                } else {
                    
                    imgNode = document.createElement('img');
                    imgNode.setAttribute("src", "images/spacer.gif");
                    td6.appendChild(imgNode);
                }
            }
        } else {
            noCritAlerts.style.display = '';

        }

        $('modifiedCritTime' + (token != null ? token : '')).innerHTML =
        'Updated: ' + refreshTime();
        //rTimer = setTimeout(alertFunc, 60000);
        // Refresh in 60 seconds
        unCheck();

        ToggleRecentAlertButton(document.FixAlertsForm);
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
            var tbody = availTable.getElementsByTagName('tbody')[0];
            var browser = BrowserDetect.browser;
            
            if (browser == "Safari") {

            for (var i = tbody.childNodes.length; i > 1; i--) {
                tbody.removeChild(tbody.childNodes[i]);
                }
            } else {

             for (var i = tbody.childNodes.length - 1; i > 1; i--) {
                tbody.removeChild(tbody.childNodes[i]);
                 }
            }

            for (var i = 0; i < availList.length; i++) {
                var tr = document.createElement('tr');
                var trTime = document.createElement('tr');
                var td1 = document.createElement('td');
                var td2 = document.createElement('td');
                var td3 = document.createElement('td');
                var td4 = document.createElement('td');
                var newanchor = document.createElement("a");
                var up = availList[i].numUp;
                var down = availList[i].numDown;
                var downgraphic = '<span style="padding-right:5px;"><img src=/images/icon_available_red.gif></span>';
                var upgraphic = '<span style="padding-right:5px;padding-left:5px;"><img src=/images/icon_available_green.gif></span>';

                tbody.appendChild(tr);
                tr.setAttribute((document.all ? 'className' : 'class'), "ListRow");

                tr.appendChild(td1);
                td1.setAttribute((document.all ? 'className' : 'class'), "resourceTypeName");

                td1.appendChild(newanchor);
                newanchor.appendChild(document.createTextNode(availList[i].resourceTypeName));
                newanchor.setAttribute('href', (browseUrl + availList[i].appdefType + urlParams + availList[i].appdefType + urlColon + availList[i].appdefTypeId));
                tr.appendChild(td2);
                tr.appendChild(td3);

                td2.setAttribute((document.all ? 'className' : 'class'), "availResourceStatus");
                td2.setAttribute('align', 'left');

                td3.setAttribute((document.all ? 'className' : 'class'), "availResourceStatus");
                td3.setAttribute('align', 'left');


                if (down > '0') {
                    td2.setAttribute('width', '50px');
                    td2.innerHTML = downgraphic + '<span style=color:red;>' + down + '</span>';
                } else {
                    td2.setAttribute('width', '25px');
                    td2.innerHTML = "&nbsp;";
                }

                if (up > '0') {
                    td3.setAttribute('width', '50px');
                    td3.innerHTML = upgraphic + '<span style=color:green;>' + up + '</span>';
                } else {
                    td3.setAttribute('width', '25px');
                    td3.innerHTML = "&nbsp;";
                }
            }
            tbody.appendChild(trTime);
            trTime.appendChild(td4);
            td4.setAttribute('colSpan', '3');
            td4.setAttribute((document.all ? 'className' : 'class'), "modifiedDate");

            if (token != null) {
                td4.setAttribute('id', 'availTime' + token);
                $('availTime' + token).innerHTML = 'Updated: ' + refreshTime();
            } else {
                td4.setAttribute('id', 'availTime');
                $('availTime').innerHTML = 'Updated: ' + refreshTime();
            }

        }

        //$('modifiedAvailTime').innerHTML = 'Updated: ' + refreshTime();
        //rTimer = setTimeout(availFunc, 60000);
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

        if (metricTable && metricValues.values) {


            var tbody = metricTable.getElementsByTagName('tbody')[0];
            var browser = BrowserDetect.browser;
            
            if (browser == "Safari") {
                for (var a = tbody.childNodes.length; a > 0; a--) {
                tbody.removeChild(tbody.childNodes[a]);
                }
            } else {
                for (var a = tbody.childNodes.length - 1; a > 0; a--) {
                tbody.removeChild(tbody.childNodes[a]);
                }
            }

            // Create table headers
            var trHeader = document.createElement('tr');
            var trTime = document.createElement('tr');
            var th1 = document.createElement('th');
            var th2 = document.createElement('th');

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
            th2.setAttribute("noWrap", true);
            th2.appendChild(document.createTextNode(resourceLoadTypeHeader));

            for (i = 0; i < metricValues.values.length; i++) {
                var newanchor = document.createElement("a");
                var tr = document.createElement('tr');
                var td1 = document.createElement('td');
                var td2 = document.createElement('td');
                var td3 = document.createElement('td');

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
            tbody.appendChild(trTime);
            trTime.appendChild(td3);
            td3.setAttribute('colSpan', '2');
            td3.setAttribute((document.all ? 'className' : 'class'), "modifiedDate");

            if (token != null) {

                td3.setAttribute('id', 'metricTime' + token);
                $('metricTime' + token).innerHTML = 'Updated: ' + refreshTime();
            } else {

                td3.setAttribute('id', 'metricTime');
                $('metricTime').innerHTML = 'Updated: ' + refreshTime();
            }

            //$('modifiedMetricTime').innerHTML = 'Updated: '+refreshTime();

        } else {
            $(noMetricTable).style.display = '';
        }

        //rTimer = setTimeout(metricFunc, 60000);
        //Refresh in 60 seconds
    }

    function showFavoriteResponse(originalRequest) {
       
        var faveText = eval('(' + originalRequest.responseText + ')');
        var fList = faveText.favorites;
        var table = document.getElementById('favoriteTable');
        $('modifiedFavoriteTime').innerHTML = 'Updated: ' + refreshTime();
        
        if (table) {
            if (fList && fList.length > 0) {
                var tbody = table.getElementsByTagName('tbody')[0];

                for (var d = tbody.childNodes.length - 1; d > 1; d--) {
                    tbody.removeChild(tbody.childNodes[d]);
                }

                for (i = 0; i < fList.length; i++) {

                    var tr = document.createElement('tr');
                    var trTime = document.createElement('tr');
                    var td1 = document.createElement('td');
                    var td2 = document.createElement('td');
                    var td3 = document.createElement('td');
                    var td4 = document.createElement('td');
                    var td5 = document.createElement('td');
                    var td6 = document.createElement('td');
                    var favAnchor = document.createElement("a");
                    var urlColon = ":"
                    var resUrl = $('viewResUrl').href;


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
        }
        //var rTimer = setTimeout(showFavoriteResponse,20000); //Refresh in 60 seconds

    }

}

function addOption(sel, val, txt, selected) {
        var o = document.createElement('option');
        var t = document.createTextNode(txt);

        o.setAttribute('value',val);

        if (selected) {
          o.setAttribute('selected', 'true');
        }
        sel.appendChild(o);
        o.appendChild(document.createTextNode(txt));

    }


function refreshTime() {
    var curDateTime = new Date()
    var curHour = curDateTime.getHours()
    var curMin = curDateTime.getMinutes()
    var curSec = curDateTime.getSeconds()
    var curAMPM = " AM"
    var curTime = ""
    if (curHour >= 12) {
        curHour -= 12
        curAMPM = " PM"
    }
    if (curHour == 0) curHour = 12
    curTime = curHour + ":"
            + ((curMin < 10) ? "0" : "") + curMin
            + curAMPM
    return curTime;
}

function refreshDate() {
    var today = new Date()
    var year = today.getYear()
    if (year < 1000) year += 1900

    var todayDate = (today.getMonth() + 1) + "/" +
                    today.getDate() + "/" + (year + "").substring(2, 4);
    return todayDate;
}

function reportError(originalRequest) {
    //alert('Error ' + originalRequest.status + ' -- ' + originalRequest.statusText);
}

function unCheck() {

        for(i=0; i<document.FixAlertsForm.elements.length; i++) {
            if(document.FixAlertsForm.elements[i].type=="checkbox") {
            document.FixAlertsForm.elements[i].checked=false;
            }
            $('listToggleAll').checked=false;
       }

}

var BrowserDetect = {
	initBrowser: function () {
		this.browser = this.searchString(this.dataBrowser) || "An unknown browser";
		this.version = this.searchVersion(navigator.userAgent)
			|| this.searchVersion(navigator.appVersion)
			|| "an unknown version";
		this.OS = this.searchString(this.dataOS) || "an unknown OS";
	},
	searchString: function (data) {
		for (var i=0;i<data.length;i++)	{
			var dataString = data[i].string;
			var dataProp = data[i].prop;
			this.versionSearchString = data[i].versionSearch || data[i].identity;
			if (dataString) {
				if (dataString.indexOf(data[i].subString) != -1)
					return data[i].identity;
			}
			else if (dataProp)
				return data[i].identity;
		}
	},
	searchVersion: function (dataString) {
		var index = dataString.indexOf(this.versionSearchString);
		if (index == -1) return;
		return parseFloat(dataString.substring(index+this.versionSearchString.length+1));
	},
	dataBrowser: [
		{ 	string: navigator.userAgent,
			subString: "OmniWeb",
			versionSearch: "OmniWeb/",
			identity: "OmniWeb"
		},
		{
			string: navigator.vendor,
			subString: "Apple",
			identity: "Safari"
		},
		{
			prop: window.opera,
			identity: "Opera"
		},
		{
			string: navigator.vendor,
			subString: "iCab",
			identity: "iCab"
		},
		{
			string: navigator.vendor,
			subString: "KDE",
			identity: "Konqueror"
		},
		{
			string: navigator.userAgent,
			subString: "Firefox",
			identity: "Firefox"
		},
		{
			string: navigator.vendor,
			subString: "Camino",
			identity: "Camino"
		},
		{		// for newer Netscapes (6+)
			string: navigator.userAgent,
			subString: "Netscape",
			identity: "Netscape"
		},
		{
			string: navigator.userAgent,
			subString: "MSIE",
			identity: "Explorer",
			versionSearch: "MSIE"
		},
		{
			string: navigator.userAgent,
			subString: "Gecko",
			identity: "Mozilla",
			versionSearch: "rv"
		},
		{ 		// for older Netscapes (4-)
			string: navigator.userAgent,
			subString: "Mozilla",
			identity: "Netscape",
			versionSearch: "Mozilla"
		}
	],
	dataOS : [
		{
			string: navigator.platform,
			subString: "Win",
			identity: "Windows"
		},
		{
			string: navigator.platform,
			subString: "Mac",
			identity: "Mac"
		},
		{
			string: navigator.platform,
			subString: "Linux",
			identity: "Linux"
		}
	]

};
BrowserDetect.initBrowser();
