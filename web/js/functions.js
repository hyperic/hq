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


/*-- START functions.js --*/

/*------------------------------------ start BROWSER DETECTION ------------------------------------*/
var isIE = false;
var isMacIE = false;
var isNS = false;

if (navigator.appName.indexOf("Microsoft")!=-1) {
	isIE = true;
	if (navigator.platform.indexOf("Mac")!=-1)
		isMacIE = true;
}
else
	isNS = true;

/*------------------------------------ end BROWSER DETECTION ------------------------------------*/	

/* fixes IE-Netscape DOM incompatibility
e.getAttribute("class") - works in NS, not in IE
e.getAttribute("className") - works in IE, not in NS
*/
var classStr = "class";
if (isIE)
	classStr = "className";

/*------------------------------------ start WidgetProperties ------------------------------------*/
// call this at the top of the widget (so that it's called once
// per widget)
function initializeWidgetProperties(widgetInstanceName) {
     var widgetProperties = new Array();
     widgetProperties["name"] = widgetInstanceName;
     widgetProperties["numFromSelected"] = 0; //for add/remove widgets
	 widgetProperties["numToSelected"] = 0; //for add/remove widgets
	 widgetProperties["numSelected"] = 0; //for list widgets
	 widgetProperties["subGroup"] = "availableListMember"; //for list widgets
     pageData[widgetInstanceName] = widgetProperties;
}

// call from any js when you need all of a widget's properties
function getWidgetProperties(widgetInstanceName) {
	 return pageData[widgetInstanceName];
}

// call from any js when you need a property (ex: numFromSelected) for a
// particular widget
function getWidgetProperty(widgetInstanceName, propertyName) {
	var widgetProperties = pageData[widgetInstanceName];
	
	if (widgetProperties == null) {
	// this would only happen if the widget was not initialized
		initializeWidgetProperties(widgetInstanceName);
		widgetProperties = pageData[widgetInstanceName];
	}

     return widgetProperties[propertyName];
}

// call from any js when you need to set a property (ex:
// numFromSelected) for a particular widget
function setWidgetProperty(widgetInstanceName, propertyName, propertyValue) {
	var widgetProperties = pageData[widgetInstanceName];

	if (widgetProperties == null) {
		// this would only happen if the widget was not initialized
		intializeWidgetProperties(widgetInstanceName);
		widgetProperties = pageData[widgetInstanceName];
	}
	
	widgetProperties[propertyName] = propertyValue;
}

function clearIfAnyChecked(name) {
  var eArr = document.getElementsByTagName("input");

  for (i=0; i<eArr.length; i++) {
    if ((name != null) && (eArr[i].name != name)) {
        continue;
    }

    if (eArr[i].checked) {
      eArr[i].checked = false;
    }
  }
}

function ClearText(e) {
  if (isIE)
		e = event.srcElement;
    
  e.value="";
}

/*----------- start SHOW/HIDE DIV -----------*/
function hideDiv( _levelId) {
	var thisDiv = document.getElementById( _levelId );
	thisDiv.style.display = "none";
}

function showDiv( _levelId) {
	var thisDiv = document.getElementById( _levelId );
	thisDiv.style.display = "block";
}

function toggleDiv( _levelId) {
	var thisDiv = document.getElementById( _levelId );
	if ( thisDiv.style.display == "none") {
		thisDiv.style.display = "block";
	}
	else {
		thisDiv.style.display = "none";
	}
}
/*----------- end SHOW/HIDE DIV -----------*/

/*------------------------------------ end WidgetProperties ------------------------------------*/

function clickSelect(formName, inputName, inputValue) {
	var form = document.forms[formName];
	form[inputName].value = inputValue;
}

function clickAdd(formName) {
	var newInput = document.createElement("input");
	newInput.setAttribute("type", "hidden");
	newInput.setAttribute("name", "add.x");
	document.forms[formName].appendChild(newInput);
}

function clickRemove(formName, index) {
	var newInput = document.createElement("input");
	newInput.setAttribute("type", "hidden");
	newInput.setAttribute("name", "remove.x");
	newInput.setAttribute("value", index);
	document.forms[formName].appendChild(newInput);
   // $('remove.x').value=index;
}

function clickAIPlatformImport(formName) {
	var newInput = document.createElement("input");
	newInput.setAttribute("type", "hidden");
	newInput.setAttribute("name", "includeForImport.x");
	document.forms[formName].appendChild(newInput);
}

function clickAIPlatformIgnore(formName) {
	var newInput = document.createElement("input");
	newInput.setAttribute("type", "hidden");
	newInput.setAttribute("name", "ignoreForImport.x");
	document.forms[formName].appendChild(newInput);
}

function clickLink(formName, inputName, method) {
	var newInput = document.createElement("input");
	newInput.setAttribute("type", "hidden");
	newInput.setAttribute("name", inputName + ".x");
	document.forms[formName].appendChild(newInput);
	if (method) {
	   document.forms[formName].method = method;
	}
}

function getNumCheckedByClass(uList, className) {
    var len = uList.elements.length;
    var numCheckboxes = 0;

    for (var i = 0; i < len; i++) {
        var e = uList.elements[i];
        if (e.getAttribute(classStr)==className && e.checked) {
            numCheckboxes++;
        }
    }

    return numCheckboxes;
}

function getParent(e) {
	return e.parentNode.parentNode;
}

function getForm(e) {
	var form = "error";
	
	while (form == "error") {
		e = e.parentNode;
		if (e.tagName == "FORM")
			form = e;
	}
	
	return form;
}

function getDivName (e) {
	var divName = "error";
	
	while (divName == "error") {
		e = e.parentNode;
		if (e.tagName == "DIV")
			divName = e.getAttribute("id");
	}
	
	return divName;
}

function imageSwap(e, btnName, state) {
	var btn = e;
	btn.src = btnName + state + ".gif";
}

/* ---------------------------- start TOGGLE ---------------------------- */

function highlight(e) {
	var parent = getParent(e);

	if (parent) {
	    parent.origColor = parent.style.backgroundColor;
		parent.style.backgroundColor = "#EBEDF2";
	}
}

function unhighlight(e) {
	var parent = getParent(e);

	if (parent) {
        if (parent.origColor != null)
		  parent.style.backgroundColor = parent.origColor;
        else
		  parent.style.backgroundColor = "#F2F4F7";
	}
}

function Check(e) {
	e.checked = true;
	highlight(e);
}
	
function Clear(e) {
	e.checked = false;
	unhighlight(e);
}
/* ---------------------------- end TOGGLE ---------------------------- */

/*--------------------------- START getters for individual elements ------------------------------*/

function getNewImage (iSrc, iW, iH, iB) {
	var newImage = document.createElement("IMG");
	newImage.setAttribute("src", iSrc);
	if (iW)
    newImage.setAttribute("width", iW);
	if (iH)
    newImage.setAttribute("height", iH);
	newImage.setAttribute("border", iB);
	
	return newImage;
}

function goToSelectLocation (e, param, base) {
	 var sep = base.indexOf('?') >=0 ? '&' : '?';
	 window.location = base + sep + param + "=" + e.options[e.selectedIndex].value;
}

function goToLocationSelfAndElement(param,elementName,base) {
    var sep = base.indexOf("?") >=0 ? '&' : "?";
    var val = document.forms[0].elements[elementName].value;
    window.location = base + sep + param + "=" + val;
    return false;
}

/*--------------------------- END getters for individual elements ------------------------------*/

/*--------------------------- BEGIN helpers for CSS stuff ------------------------------*/
function hideFormElements() {
  for (i=0; i<document.forms.length; ++i) {
    for (j=0; j<document.forms[i].elements.length; ++j) {
      document.forms[i].elements[j].style.visibility = "hidden";
    }
  }
}

function showFormElements() {
  for (i=0; i<document.forms.length; ++i) {
    for (j=0; j<document.forms[i].elements.length; ++j) {
      document.forms[i].elements[j].style.visibility = "visible";
    }
  }
}

function whereAmI(elem, elems) { 
  for (var i = 0; i < elems.length; i++) {
    if (elems[i] == elem) { return i }
  }
}

function moveElementUp(elem, root) {
    var elems = root.getElementsByTagName("li");
    var pos = whereAmI(elem, elems);
    if (pos != 0) { 
      root.removeChild(elem);
      root.insertBefore(elem, elems[pos-1]);
    }
}

function moveElementDown(elem, root) {
    var elems = root.getElementsByTagName("li");
    var pos = whereAmI(elem, elems);
    if (pos < (elems.length - 1)) { 
      var after = elems[pos + 1];
      if ((after.id != '.dashContent.addContent.narrow') &&
          (after.id != '.dashContent.addContent.wide')) {
        root.removeChild(after);
        root.insertBefore(after, elem);
      }
    }
}
/*--------------------------- END helpers for CSS stuff ------------------------------*/

function accord(tab) {
  var thisDiv = document.getElementById( 'propertiesAccordion' );
  thisDiv.style.visibility = 'visible';

  if (tab == null)
    tab = 0;

  new Rico.Accordion( 'propertiesAccordion',
                     { panelHeight: 350,
                       expandedBg: '#60a5ea',
                       hoverBg: '#70b5fa',
                       collapsedBg: '#BBBBBB',
                       collapsedTextColor: '#FFFFFF',
                       borderColor: '#60a5ea',
                       onLoadShowTab: tab
                     } );
} 

function accord0() {
  accord(0);
}

function accord1() {
  accord(1);
}

function accord2() {
  accord(2);
}

function accord3() {
  accord(3);
}

function accord4() {
  accord(4);
}

function accord5() {
  accord(5);
}

function accord6() {
  accord(6);
}

function accord7() {
  accord(7);
}

function accord8() {
  accord(8);
}


/* Escalation Admin */

/* Display minutes or hours dependent on alert time */
function formatWaitTime(el, time, hour, minute) {
     var  actionWaitTime = time;
        if (el==null && time!=null) {
            if  (actionWaitTime > 3600000) {
                actionWaitTime =  (actionWaitTime / 3600000) + " " + hour;
                } else {
                actionWaitTime =  (actionWaitTime / 60000) + " " + minute;
             }
         return actionWaitTime;
       } else{
        var formtdWaitTime;
        var index = el.options[el.selectedIndex].value;
            if  (index > 3600000) {
                formtdWaitTime = (index / 3600000) + " " + hour;
                } else {
                formtdWaitTime =  (index / 60000) + " " + minute;
             }
   		return formtdWaitTime;
   	}
}



/*-- END functions.js --*/
