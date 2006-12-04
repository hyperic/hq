{
var rtimer = null;

function requestProblemResources() {
	new Ajax.Request("/dashboard/ViewResourceHealth.do", {method: 'get', onComplete:showProblemResponse, onFailure :reportError});
}

function showProblemResponse(originalRequest) {
	var response = eval("(" + originalRequest.responseText + ")");
	var mList = response.problems.resourceName;
	var table = document.getElementById('problemResourcesTable');
	
	for(var i=table.childNodes.length-1; i>2; i--){
        table.removeChild(table.childNodes[i]);
    }

	for(i=0;i < mList.length; i++) {
	

	var tr  = document.createElement('tr');
	var td1 = document.createElement('td');
    var td2 = document.createElement('td');
    var td3 = document.createElement('td');
    var td4 = document.createElement('td');
    var newanchor = document.createElement("a");
	
    
		table.appendChild(tr);
	
		if(i%2==0) {
		tr.setAttribute((document.all ? 'className' : 'class'), "tableRowOdd");
		} else {
		tr.setAttribute((document.all ? 'className' : 'class'), "tableRowEven");
		}
		
		
		tr.appendChild(td1);
		td1.setAttribute((document.all ? 'className' : 'class'), "resource");
		td1.setAttribute("id", (mList[i].resource));
		
		if (mList[i].resource) {
		td1.appendChild(newanchor);
		newanchor.appendChild(document.createTextNode(mList[i].resource));
		newanchor.setAttribute('href', (mList[i].resource));
		}
		
		tr.appendChild(td2);
		td2.setAttribute((document.all ? 'className' : 'class'), "availability");
		
		if (mList[i].availability) {
			switch(mList[i].availability) {
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
				case "green":
				td2.innerHTML = "<img src=images/icon_available_green.gif>";
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
	
		rTimer = setTimeout('requestProblemResources();',60000); //Refresh in 60 seconds
	}



function showFavoriteResponse(originalRequest) {
var tmp = eval('(' + originalRequest.responseText + ')');

	var fList = tmp.favorites;
	var table = document.getElementById('favoriteTable');
	
	if (fList) {
	
		for(var i=table.childNodes.length-1; i>2; i--){
			table.removeChild(table.childNodes[i]);
		}
    
    

		for(i=0;i < fList.length; i++) {
	
	
		var tr  = document.createElement('tr');
		var td1 = document.createElement('td');
		var td2 = document.createElement('td');
		var td3 = document.createElement('td');
		var td4 = document.createElement('td');
		var td5 = document.createElement('td');
		var favAnchor = document.createElement("a");
		var urlColon = ":"
		var resUrl = $('viewResUrl').href;
		
			table.appendChild(tr);
		
			if(i%2==0) {
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
			td2.innerHTML = "&nbsp;";
			}
			
				tr.appendChild(td3);
				td3.setAttribute((document.all ? 'className' : 'class'), "throughput");
				td3.setAttribute("id", (fList[i].throughput));
				
			if (fList[i].throughput) {
				td3.appendChild(document.createTextNode(fList[i].throughput));
				
			} else {
			td3.innerHTML = "&nbsp;";
			}
			
				tr.appendChild(td4);
				td4.setAttribute((document.all ? 'className' : 'class'), "availability");
				td4.setAttribute("id", (fList[i].availability));
			
			if (fList[i].availability) {
				td4.appendChild(document.createTextNode(fList[i].availability));
				switch(fList[i].availability) {
					case "green":	
					td4.innerHTML = "<img src=images/icon_available_green.gif>";
					break;
					case "red":	
					td4.innerHTML = "<img src=images/icon_available_red.gif>";
					break;
					case "yellow":
					td4.innerHTML = "<img src=images/icon_available_yellow.gif>";
					break;
					case "orange":
					td4.innerHTML = "<img src=images/icon_available_orange.gif>";
					break;
					case "green":
					td4.innerHTML = "<img src=images/icon_available_green.gif>";
					default:
					td4.innerHTML = "<img src=images/icon_available_error.gif>";
				}
		
		} else {
 		td4.innerHTML = "&nbsp;";
 		}
		
		tr.appendChild(td5);
			td5.setAttribute((document.all ? 'className' : 'class'), "alerts");
			
 		if (fList[i].alerts) {
			td5.appendChild(document.createTextNode(fList[i].alerts));
		} else {
 		td5.innerHTML = "&nbsp;";
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
	var alertTable = document.getElementById('recentAlertsTable');
	
	
	if (aList != 0) {
	
	for(var i=alertTable.childNodes.length-1; i>1; i--){
        alertTable.removeChild(alertTable.childNodes[i]);
    }

	for(i=0;i < aList.length; i++) {
	

	var tr  = document.createElement('tr');
	var td1 = document.createElement('td');
    var td2 = document.createElement('td');
    var td3 = document.createElement('td');
    var td4 = document.createElement('td');
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
		td2.setAttribute((document.all ? 'className' : 'class'), "resourceName");
		
		if (aList[i].resourceName && aList[i].appdefKey && aList[i].alertId) {
		td2.appendChild(alertAnchor);
		alertAnchor.appendChild(document.createTextNode(aList[i].resourceName));
		//alertAnchor.setAttribute('href', (aList[i].resourceName));
		alertAnchor.setAttribute('href', (alertUrl + aList[i].appdefKey + urlAmp + aList[i].alertId));
		}
		
		tr.appendChild(td3);
		td3.setAttribute((document.all ? 'className' : 'class'), "ListCell");
		if (aList[i].alertDefName) {
		td3.appendChild(document.createTextNode(aList[i].alertDefName));
		}
		
		tr.appendChild(td4);
		td4.setAttribute((document.all ? 'className' : 'class'), "ListCell");
		
		if (aList[i].cTime) {
		td4.appendChild(document.createTextNode(aList[i].cTime));
		td4.setAttribute('align', 'center');
		}
 		
	}
	
	} else {
	$('noCritAlerts').style.display = '';
	}
	
		rTimer = setTimeout(requestRecentAlerts,60000); //Refresh in 60 seconds
	}
	
	function showAvailSummary(originalRequest) {
		var availText = eval("(" + originalRequest.responseText + ")");
		var availList = availText.availSummary;
		var availTable = document.getElementById('availTable');
		
		if (availList.length <1) {
	
			$('noAvailSummary').style.display = '';
			} else {
			
			for(var i=availTable.childNodes.length-1; i>1; i--){
				availTable.removeChild(availTable.childNodes[i]);
			}
		
			for(i=0;i < availList.length; i++) {
			
		
			var tr  = document.createElement('tr');
			var td1 = document.createElement('td');
			var td2 = document.createElement('td');
			var up = availList[i].numUp;
			var down = availList[i].numDown;
		   
			
				availTable.appendChild(tr);
				tr.setAttribute((document.all ? 'className' : 'class'), "ListRow");
				
				
				tr.appendChild(td1);
				td1.setAttribute((document.all ? 'className' : 'class'), "resourceTypeName");
				
				td1.appendChild(document.createTextNode(availList[i].resourceTypeName));
				
				
				
				tr.appendChild(td2);
				td2.setAttribute((document.all ? 'className' : 'class'), "availResourceStatus");
				
				if (down > '0') {
				td2.innerHTML = '<span style=color:red;>' + down + '</span>' + " / " + up;
				} else {
				td2.innerHTML = down + " / " + up;
				}
				
				//td2.appendChild(document.createTextNode(availList[i].numDown));
				//td2.appendChild(document.createTextNode(availList[i].numUp));
					
				
			}	
		}
		
		rTimer = setTimeout('showAvailSummary();',60000); //Refresh in 60 seconds
	}
	
	function showMetricsResponse(originalRequest) {
	
	var metricText = eval("(" + originalRequest.responseText + ")");
	var metricValues = metricText.metricValues;
	var metricTable = document.getElementById('metricTable');
	var resourceNameHeader = metricValues.resourceTypeName;
	var resourceLoadTypeHeader = metricValues.metricName;
	
	if (metricValues && metricValues != 0) {
	
	for(var i=metricTable.childNodes.length-1; i>1; i--){
        metricTable.removeChild(metricTable.childNodes[i]);
    }

	for (i=0; i<metricValues.values.length; i++) {
	//alert(metricValues.values.length);
	

	var tr  = document.createElement('tr');
	var td1 = document.createElement('td');
    var td2 = document.createElement('td');
    var oTextNode = $('resourceNameType').childNodes[0];
    var lTextNode = $('resourceLoadType').childNodes[0];
   	var oReplaceNode = oTextNode.replaceData(0,27,resourceNameHeader);
    var lReplaceNode = lTextNode.replaceData(0,35,resourceLoadTypeHeader);
   	
    
		metricTable.appendChild(tr);
		tr.setAttribute((document.all ? 'className' : 'class'), "ListRow");
		if (metricValues.resourceName && metricValues.metricName) {
		oReplaceNode;
		lReplaceNode;
		
		}
		
		tr.appendChild(td1);
		td1.setAttribute((document.all ? 'className' : 'class'), "resource");
		if (metricValues.values[i].resourceName) {
		td1.appendChild(document.createTextNode(metricValues.values[i].resourceName));
		}
		
		tr.appendChild(td2);
		td2.setAttribute((document.all ? 'className' : 'class'), "metricName");
		
	
		td2.appendChild(document.createTextNode(metricValues.values[i].value));
		
		}
	
	} else {
	$('noMetricValues').style.display = '';
	}
	
		rTimer = setTimeout(requestMetricsPortal,60000); //Refresh in 60 seconds
	}
	



}
