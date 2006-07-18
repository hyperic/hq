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


/*-- START chart.js --*/
function ReplaceButton(divId, tdId, tdState, imageId, btnFunction) {
    var td = document.getElementById(tdId);
    var oldDiv = document.getElementById(divId);

    var newDiv = document.createElement("DIV");
    newDiv.setAttribute("id", divId);

    var newImg;
    var imgState = "";
    if (tdState=="off")
        imgState = "_gray";

    var imgName = imagePath + "tbb_" + imageId;

    var inputName = btnFunction;
    var inputValue = btnFunction;

    imgPath = imgName + imgState + ".gif";
    newImg = getNewImage (imgPath, false, false, "0");

    if (tdState == "on") {
        var newInput = document.createElement("INPUT");
        newInput.setAttribute("type", "image");
        newInput.setAttribute("src", imgPath);
        newInput.setAttribute("name", inputName);
        newDiv.appendChild(newInput);
    }

    else {
        newDiv.appendChild(newImg);
    }

    if (td!=null)
        td.replaceChild(newDiv,oldDiv);
}

function ToggleButtons(widgetInstanceName, prefix, form) {
    var btnFunction = "redraw";
    var imgName = "redrawselectedonchart";

    var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");

    if (numSelected < 0) {
        numSelected = getNumCheckedByClass(form, "listMember");
        setWidgetProperty(widgetInstanceName, "numSelected", numSelected);
    }

    if (numSelected == 0) {
        ReplaceButton(prefix + "RedrawDiv", prefix + "RedrawTd", "off", imgName, btnFunction);
    }

    else if (numSelected >= 1) {
        ReplaceButton(prefix + "RedrawDiv", prefix + "RedrawTd", "on", imgName, btnFunction);
    }
}

function ToggleSelection(e, widgetProperties, maxNum, messageStr) {
    if (isIE)
        e = event.srcElement;

    if (maxNum!=null) {
        var numChecked = getNumChecked(e.form, e.getAttribute("name"));
        if (numChecked > maxNum) {
            e.checked = false;
            alert(messageStr);
            return;
        }
    }

    widgetInstanceName = widgetProperties["name"];
    var prefix = widgetInstanceName;

    var form = e.form;
    var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");

    if (e.checked) {
        highlight(e);
        setWidgetProperty(widgetInstanceName, "numSelected", ++numSelected);
    } else {
        unhighlight(e);
        var numSelected = getWidgetProperty(widgetInstanceName, "numSelected");
        setWidgetProperty(widgetInstanceName, "numSelected", --numSelected);
    }

    ToggleButtons(widgetInstanceName, prefix, form);
}

function getNumChecked(uList, nameStr) {
    var len = uList.elements.length;
    var numCheckboxes = 0;

    for (var i = 0; i < len; i++) {
        var e = uList.elements[i];
        if (e.getAttribute("name")==nameStr && e.checked) {
            numCheckboxes++;
        }
    }

    return numCheckboxes;
}

function checkboxToggled(cb, hidden) {
    var checkbox = document.getElementsByName(cb)[0];
    var field = document.getElementsByName(hidden)[0];

    if (checkbox.checked) {
        field.value = "true";
    } else {
        field.value = "false";
    }
}

function testCheckboxes(widgetInstanceName) {
  var e = document.getElementById("privateChart");
  var thisForm = e.form;

  var numChecked = getNumCheckedByClass(thisForm, "metricList") + getNumCheckedByClass(thisForm, "resourceList");
  setWidgetProperty(widgetInstanceName, "numSelected", numChecked);
  
  ToggleButtons(widgetInstanceName, widgetInstanceName, thisForm);
}
/*-- END chart.js --*/
