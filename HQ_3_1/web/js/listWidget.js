// NOTE: This copyright does *not* cover user programs that use HQ
// program services by normal system calls through the application
// program interfaces provided as part of the Hyperic Plug-in Development
// Kit or the Hyperic Client Development Kit - this is merely considered
// normal use of the program, and does *not* fall under the heading of
// "derived work".
// 
// Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
// This file is part of HQ.
// 
// HQ is free software; you can redistribute it and/or modify
// it under the terms version 2 of the GNU General Public License as
// published by the Free Software Foundation. This program is distributed
// in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
// even the implied warranty of MERCHANTABILITY or FITNESS FOR A
// PARTICULAR PURPOSE. See the GNU General Public License for more
// details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
// USA.


/*-- START listWidget.js --*/
function ReplaceButton(divId, tdId, tdState, imageId, btnFunction) {
	var td = document.getElementById(tdId);
    if (td == null)
        return;

	var oldDiv = document.getElementById(divId);
	
	var newDiv = document.createElement("DIV");
	newDiv.setAttribute("id", divId);
	
	var newImg;
	var imgState = "";
	if (tdState=="off")
		imgState = "_gray";
	
	var imgName = imagePath + "tbb_" + imageId;

	var inputName = btnFunction;
	
	imgPath = imgName + imgState + ".gif";
	newImg = getNewImage (imgPath, false, false, "0");
	
	if (tdState == "on") {
		var newInput = document.createElement("INPUT");
		newInput.setAttribute("type", "image");
		newInput.setAttribute("src", imgPath);
		newInput.setAttribute("name", inputName);
        if (btnFunction == 'delete' || btnFunction == 'remove') {
            newInput.setAttribute("onclick", "return confirm('Are you sure you want to delete selections?');");
        }
		newDiv.appendChild(newInput);
	}
	
	else {
		newDiv.appendChild(newImg);
	}
	
	if (td!=null)
		td.replaceChild(newDiv,oldDiv);
}

function ToggleButtons(widgetInstanceName, prefix, isRemove, form) {
    if (isRemove) {
        var imgName = "removefromlist";
        var btnFunction = "remove";
    }
	else {
        var imgName = "delete";
        var btnFunction = "delete";
    }
  
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (numSelected < 0) {
		numSelected = getNumChecked(form);
		setWidgetProperty(widgetInstanceName, "numSelected", numSelected);
	}
	
	if (numSelected >= 1) {
		ReplaceButton(prefix + "DeleteButtonDiv", prefix + "DeleteButtonTd",
                      "on", imgName, btnFunction);
		ReplaceButton(prefix + "DeleteAlertsButtonDiv", prefix + "DeleteAlertsButtonTd",
                      "on", "delete_alerts", "deleteAlerts");
		ReplaceButton(prefix + "GroupButtonDiv", prefix + "GroupButtonTd",
                      "on", "group", "group");
        if (goButtonLink!=null)
            ReplaceGoButton(true);

        // Enable all submit buttons
        for (var i = 0; i < form.elements.length; i++) {
            if (form.elements[i].type == 'submit' &&
                form.elements[i].className == 'CompactButtonInactive') {
                form.elements[i].disabled = false;
                form.elements[i].className = 'CompactButton';
            }
        }
    } else if (numSelected == 0) {
		ReplaceButton(prefix + "DeleteButtonDiv", prefix + "DeleteButtonTd",
                      "off", imgName, btnFunction);
		ReplaceButton(prefix + "DeleteAlertsButtonDiv", prefix + "DeleteAlertsButtonTd",
                      "off", "delete_alerts", "deleteAlerts");
		ReplaceButton(prefix + "GroupButtonDiv", prefix + "GroupButtonTd",
                      "off", "group", "group");
        if (goButtonLink != null)
            ReplaceGoButton(false);

        // Disable all submit buttons
        for (var i = 0; i < form.elements.length; i++) {
            if (form.elements[i].type == 'submit' &&
                form.elements[i].className == 'CompactButton') {
                form.elements[i].disabled = true;
                form.elements[i].className = 'CompactButtonInactive';
            }
        }
	}
}

function ToggleRecentAlertButton(form) {
    for (var i = 0; i < form.elements.length; i++) {
            if (form.elements[i].type == 'submit' &&
                form.elements[i].className == 'CompactButton') {
                form.elements[i].disabled = true;
                form.elements[i].className = 'CompactButtonInactive';
            }
        }
	}


function ToggleTwoButtons(widgetInstanceName, prefix, form, btnFunction) {
    if (btnFunction == "remove")
        var imgPrefix = "removeFrom";
    else if (btnFunction == "add")
        var imgPrefix = "addTo";
  
    var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
    
    if (numSelected < 0) {
        numSelected = getNumChecked(form);
        setWidgetProperty(widgetInstanceName, "numSelected", numSelected);
    }
    
    var mode = "off";
    if (numSelected >= 1)
        mode = "on";

    ReplaceButton(prefix + "chartSelectedMetricsDiv", prefix + "chartSelectedMetricsTd", mode, "chartselectedmetrics", "chart");
    ReplaceButton(prefix + "setBaselinesDiv", prefix + "setBaselinesTd", mode, "setBaselines", "userset");
    ReplaceButton(prefix + "enableAutoBaselinesDiv", prefix + "enableAutoBaselinesTd", mode, "enableAutoBaselines", "enable");
    ReplaceButton(prefix + imgPrefix + "FavoritesDiv", prefix + imgPrefix + "FavoritesTd", mode, imgPrefix + "Favorites", btnFunction);
}

function ToggleButtonsCompare(widgetInstanceName, prefix, form) {
    var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (numSelected < 0) {
		numSelected = getNumChecked(form);
		setWidgetProperty(widgetInstanceName, "numSelected", numSelected);
	}
	
	if (numSelected < 2) {
		ReplaceButton(prefix + "compareDiv", prefix + "compareTd", "off", "compareMetricsOfSelected", "compare");
	}
	
	else if (numSelected >= 2) {
            ReplaceButton(prefix + "compareDiv", prefix + "compareTd", "on", 
                "compareMetricsOfSelected", "compare");
            form.setAttribute("method","get");

	}
}

function ToggleButtonsRemoveGo(widgetInstanceName, prefix, form) {
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (numSelected < 0) {
		numSelected = getNumChecked(form);
		setWidgetProperty(widgetInstanceName, "numSelected", numSelected);
	}
	
	if (numSelected >= 1) {
		ReplaceButton(prefix + "DeleteButtonDiv", prefix + "DeleteButtonTd", "on", "disablecollection", "remove");
        ReplaceButton(prefix + "GoButtonDiv", prefix + "GoButtonTd", "on", "go", "ok");
        ReplaceButton(prefix + "IndButtonDiv", prefix + "IndButtonTd", "on", "go", "indBtn");
	} else if (numSelected == 0) {
		ReplaceButton(prefix + "DeleteButtonDiv", prefix + "DeleteButtonTd", "off", "disablecollection", "remove");
        ReplaceButton(prefix + "GoButtonDiv", prefix + "GoButtonTd", "off", "go", "ok");
        ReplaceButton(prefix + "IndButtonDiv", prefix + "IndButtonTd", "off", "go", "indBtn");
	}
}

function ToggleButtonsGroup(widgetInstanceName, prefix, form) {
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (numSelected < 0) {
		numSelected = getNumChecked(form);
		setWidgetProperty(widgetInstanceName, "numSelected", numSelected);
	}
	
	if (numSelected == 0) {
		ReplaceButton(prefix + "ApplySelectedDiv", prefix + "ApplySelectedTd", "off", "applyselectedtomembers", "apply");
        ReplaceButton(prefix + "RemoveSelectedDiv", prefix + "RemoveSelectedTd", "off", "removeselectedfrommembers", "apply");
        ReplaceButton(prefix + "GoButtonDiv", prefix + "GoButtonTd", "off", "go", "ok");
	}
	
	else if (numSelected >= 1) {
		ReplaceButton(prefix + "ApplySelectedDiv", prefix + "ApplySelectedTd", "on", "applyselectedtomembers", "apply");
        ReplaceButton(prefix + "RemoveSelectedDiv", prefix + "RemoveSelectedTd", "on", "removeselectedfrommembers", "apply");
        ReplaceButton(prefix + "GoButtonDiv", prefix + "GoButtonTd", "on", "go", "ok");
	}
}

function ToggleSelection(e, widgetProperties, isRemove) {
	if (isIE)
		e = event.srcElement;

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
	
	var form = e.form;
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (e.checked) {
		highlight(e);
		setWidgetProperty(widgetInstanceName, "numSelected", ++numSelected);
	}
	else {
		unhighlight(e);
		var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
		setWidgetProperty(widgetInstanceName, "numSelected", --numSelected);
		form.listToggleAll.checked = false;
	}
	
	ToggleButtons(widgetInstanceName, prefix, isRemove, form);
}

function ToggleSelectionTwoButtons(e, widgetProperties, btnFunction) {
  if (isIE)
    e = event.srcElement;

  widgetInstanceName = widgetProperties["name"];
  var prefix = widgetInstanceName;
  var form = e.form;

  ToggleRemoveGo(e, widgetProperties);
  ToggleTwoButtons(widgetInstanceName, prefix, form, btnFunction);
}

function ToggleAllSelectionTwoButtons(e, widgetProperties, subGroup,
                                      btnFunction) {
    // First toggle all buttons
    ToggleAllRemoveGo(e, widgetProperties, subGroup);

    if (isIE)
        e = event.srcElement;

    widgetInstanceName = widgetProperties["name"];
    
    ToggleTwoButtons(widgetInstanceName, widgetInstanceName, e.form,
                     btnFunction);
}

function ToggleAllSelectionFourButtons(e, widgetProperties, subGroup,
                                       btnFunction) {
    // First toggle all buttons
    ToggleAllRemoveGo(e, widgetProperties, subGroup);

    if (isIE)
        e = event.srcElement;

    widgetInstanceName = widgetProperties["name"];
    
    ToggleTwoButtons(widgetInstanceName, widgetInstanceName, e.form,
                     btnFunction);
}

function ToggleAllCompare(e, widgetProperties, subGroup) {
    subGroup = widgetProperties["subGroup"];

    if (isIE)
		e = event.srcElement;

    widgetInstanceName = widgetProperties["name"];
    var prefix = widgetInstanceName;
    var form = e.form;

    if (e.checked) {
        CheckAll(e, widgetInstanceName, subGroup);
    }
    else {
        ClearAll(e, widgetInstanceName, subGroup);
    }

    ToggleButtonsCompare(widgetInstanceName, prefix, form);
}

function ToggleSelectionCompare(e, widgetProperties) {
    if (isIE)
		e = event.srcElement;

    widgetInstanceName = widgetProperties["name"];
    var prefix = widgetInstanceName;
	
    var form = e.form;
    var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
    if (e.checked) {
        highlight(e);
        setWidgetProperty(widgetInstanceName, "numSelected", ++numSelected);
    }
    else {
        unhighlight(e);
        var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
        setWidgetProperty(widgetInstanceName, "numSelected", --numSelected);
    }
	
    ToggleButtonsCompare(widgetInstanceName, prefix, form);
}

function ToggleRemoveGo(e, widgetProperties) {
	if (isIE)
		e = event.srcElement;
    
    var subGroup = e.getAttribute(classStr);
    var nameAll = subGroup + 'All';
    var checkAll = document.getElementsByName(nameAll)[0];

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
	
	var form = e.form;
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (e.checked) {
		highlight(e);
		setWidgetProperty(widgetInstanceName, "numSelected", ++numSelected);
	}
	else {
		unhighlight(e);
		var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
		setWidgetProperty(widgetInstanceName, "numSelected", --numSelected);
		checkAll.checked = false;
	}
	
	ToggleButtonsRemoveGo(widgetInstanceName, prefix, form);
}

function ToggleGroup(e, widgetProperties) {
	if (isIE)
		e = event.srcElement;

    var subGroup = e.getAttribute(classStr);
    var nameAll = subGroup + 'All';
    var checkAll = document.getElementsByName(nameAll)[0];

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
	
	var form = e.form;
	var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
	
	if (e.checked) {
		highlight(e);
		setWidgetProperty(widgetInstanceName, "numSelected", ++numSelected);
	}
	else {
		unhighlight(e);
		var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
		setWidgetProperty(widgetInstanceName, "numSelected", --numSelected);
		checkAll.checked = false;
	}
	
	ToggleButtonsGroup(widgetInstanceName, prefix, form);
}

function ToggleAll(e, widgetProperties, isRemove, subGroup) {
    if (!subGroup)
        subGroup="listMember";

    if (isIE)
        e = event.srcElement;

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;

	if (e.checked) {
		CheckAll(e, widgetInstanceName, subGroup);
		ToggleButtons(widgetInstanceName, prefix, isRemove, e.form);
	}
	else {
		ClearAll(e, widgetInstanceName, subGroup);
		ToggleButtons(widgetInstanceName, prefix, isRemove, e.form);
	}
}

function ToggleAllRemoveGo(e, widgetProperties, subGroup) {
    if (isIE)
        e = event.srcElement;

    widgetInstanceName = widgetProperties["name"];
    var prefix = widgetInstanceName;
    
    if (e.checked) {
        CheckAll(e, widgetInstanceName, subGroup);
    }
    else {
        ClearAll(e, widgetInstanceName, subGroup);
    }

    ToggleButtonsRemoveGo(widgetInstanceName, prefix);
}

function ToggleAllChart(e, widgetProperties, btnFunction, subGroup) {
	if (isIE)
		e = event.srcElement;

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
  var form = e.form;
	
	if (e.checked) {
		CheckAll(e, widgetInstanceName, subGroup);
		ToggleTwoButtons(widgetInstanceName, prefix, form, btnFunction);
	}
	else {
		ClearAll(e, widgetInstanceName, subGroup);
		ToggleTwoButtons(widgetInstanceName, prefix, form, btnFunction);
	}
}

function ToggleAllGroup(e, widgetProperties, subGroup) {
	if (isIE)
		e = event.srcElement;

	widgetInstanceName = widgetProperties["name"];
	var prefix = widgetInstanceName;
	
	if (e.checked) {
		CheckAll(e, widgetInstanceName, subGroup);
		ToggleButtonsGroup(widgetInstanceName, prefix);
	}
	else {
		ClearAll(e, widgetInstanceName, subGroup);
		ToggleButtonsGroup(widgetInstanceName, prefix);
	}
}

function CheckAll(e, widgetInstanceName, subGroup) {
	var uList = e.form;
    var len = uList.elements.length;
	var numCheckboxes = getWidgetProperty(widgetInstanceName, "numSelected");

	for (var i = 0; i < len; i++) {
        var e = uList.elements[i];
        var eClass = e.getAttribute("class");

        eClass = eClass? eClass : e.getAttribute("className");
       
        if (eClass==subGroup && e.checked == false) {

            Check(e);
			numCheckboxes++;
		}
	}

	setWidgetProperty(widgetInstanceName, "numSelected", numCheckboxes);
}

function ClearAll(e, widgetInstanceName, subGroup) {
	var uList = e.form;
    var len = uList.elements.length;
  var numCheckboxes = getWidgetProperty(widgetInstanceName, "numSelected");
	
	for (var i = 0; i < len; i++) {
		var e = uList.elements[i];
        var eClass = e.getAttribute("class");

        eClass = eClass? eClass : e.getAttribute("className");

         if (eClass==subGroup && e.checked == true) {
			Clear(e);
      numCheckboxes--;
		}
	}
	
	setWidgetProperty(widgetInstanceName, "numSelected", numCheckboxes);
}

function getNumChecked(uList) {
    if (uList == null)
      return 0;

	var len = uList.elements.length;
	var numCheckboxes = 0;
	
	for (var i = 0; i < len; i++) {
		var e = uList.elements[i];
		if (e.getAttribute(classStr)=="listMember" && e.checked) {
			numCheckboxes++;
		}
	}
		
	return numCheckboxes;	
}

function testCheckboxes(functionName, widgetInstanceName, hiddenElementId, className) {
  var e = document.getElementById(hiddenElementId);
  var thisForm = e.form;

  var numChecked = getNumCheckedByClass(thisForm, className);
  setWidgetProperty(widgetInstanceName, "numSelected", numChecked);
  
  if (functionName == "ToggleButtonsCompare")
    ToggleButtonsCompare(widgetInstanceName, widgetInstanceName, thisForm);
}

function ReplaceGoButton(condition) {
  var goLink = document.getElementById("goButtonLink");
  var goImg = document.getElementById("goButtonImg");
  
  if (condition == true) {
    goLink.setAttribute("href",goButtonLink);
    goImg.setAttribute("src",imagePath + "dash-button_go-arrow.gif");
  }
  else {
    goLink.setAttribute("href", "#");
    goImg.setAttribute("src",imagePath + "dash-button_go-arrow_gray.gif");
  }
}

/*-- END listWidget.js --*/

