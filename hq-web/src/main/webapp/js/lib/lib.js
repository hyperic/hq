// NOTE: This copyright does *not* cover user programs that use HQ
// program services by normal system calls through the application
// program interfaces provided as part of the Hyperic Plug-in Development
// Kit or the Hyperic Client Development Kit - this is merely considered
// normal use of the program, and does *not* fall under the heading of
// "derived work".
// 
// Copyright (C) [2004-2010], VMware, Inc.
// This file is part of Hyperic.
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

/*-- START lib.js --*/

var init_lib = false;
var urlXtraVar = [];
var hyperic = {};
hyperic.URLS = {}; hyperic.widget = {}; hyperic.utils = {}; hyperic.html = {}; hyperic.data = {}; hyperic.i18n = {}; hyperic.config = {};

/**
 * init the library
 */
(function(){
  if(!init_lib){
        init_lib = true;
  }
  hyperic.config.uniqueIndex = 0;
})();

hyperic.html = {
    show : function(/*String*/ node){
        hqDojo.query("#" + node).style('display', '');
    },
    hide : function(/*String*/ node){
        hqDojo.query("#" + node).style('display', 'none');
    }
};

hyperic.form = {
    fieldFocus : function(/*DOMNode*/elem) {
        if(!elem.getAttribute('readonly')) {
            if(elem.parentNode.className == "fieldRow hint") {
                elem.parentNode.className = "fieldRow hint active";
            } else {
                elem.parentNode.className = "fieldRow active";
            }
        }
    },
    fieldBlur : function(elem) {
        if(elem.parentNode.className == "fieldRow hint active") {
            elem.parentNode.className = "fieldRow hint";
        } else {
            elem.parentNode.className = "fieldRow";
        }
    },
    /**
     * An href representing a button calls this in conjunction with a 
     * hidden form field to submit the encapsulating form.
     *
     * @param inputName the name to assign to the hidden input field
     * @param inputValue  the value  to assign to the hidden input field
     * @param inputNodeName the String ID of the hidden input field
    */
    mockLinkSubmit : function(inputName, inputValue, inputNodeName){
        var hiddenInputObj = hqDojo.byId(inputNodeName);
        hiddenInputObj.name=inputName;
        hiddenInputObj.value=inputValue;
        
        var myForm = hiddenInputObj.form;
        if (myForm.onsubmit) {
        	var onsubmitResult = myForm.onsubmit(); 
        	if ((onsubmitResult == undefined) || onsubmitResult) {
        		myForm.submit();
        	}
        } else {
        	myForm.submit();
        }
    },
    createEscalationPauseOptions : function(selectAttributes, maxPauseTime) {
		var myPauseSelect = hqDojo.clone(hyperic.data.escalation.pauseSelect);

		if (selectAttributes.id != null) {
			myPauseSelect.id = selectAttributes.id;
		}
		if (selectAttributes.name != null) {
			myPauseSelect.name = selectAttributes.name;
		}
		if (selectAttributes.className != null) {
			myPauseSelect.className = selectAttributes.className;
		}
		if (maxPauseTime == null) {
			maxPauseTime = Number.MAX_VALUE;
		}
		
		for (var i = myPauseSelect.length-1; i >= 0; i--) {
			var optionChild = myPauseSelect[i];
			if (parseFloat(optionChild.value) > parseFloat(maxPauseTime)) {
				myPauseSelect.removeChild(optionChild);
			} else {
				break;
			}
		}
		
		// Select the longest available pause time in the list
		if (myPauseSelect.options && myPauseSelect.options.length > 1) {
			myPauseSelect.options[myPauseSelect.options.length - 1].selected = true;
		}
		
		return myPauseSelect;
    }
};

/**
 * @deprecated
 */
hyperic.utils.key = {
    enterKeyHandler : function(evt) {
        if(!evt) { evt = window.event; }
        if(window.event) {
            evt.cancelBubble = true;
            evt.returnValue = false;
        } else {
            evt.preventDefault();
            evt.stopPropagation();            
        }
        if(evt.keyCode == 13) {
            hqDojo.publish('enter', [evt]);
        }
    },
    registerListener : function(/*DOMNode*/node, /*fp*/handler){
        if(handler && node) {
            hqDojo.connect(node, 'onkeyup', handler);
        }
    }
};


/**
 * Get an DOM Id that is unique to this document
 */
hyperic.utils.getUniqueId = function(/*String*/ prefix){
    return (('undefined' !== typeof(prefix)) ? prefix : "unique" ) + hyperic.config.uniqueIndex++ +"";
};


/**
 * Register a key listener and publish the event on the specified topic
 * @param node A DOM object 
 * @param modKeys an Object with 4 keys: keyCode, ctrlKey, altKey and shiftKey
 * @param topic String name of the topic
 * 
 * To cancel the listener just call the cancel method on the object
 * 
 * Example: addKeyListener(window, {keyCode: 83, ctrl :true}, 'search');
 * which registers a 'ctrl-s' key listener on the window
 */
hyperic.utils.addKeyListener = function(dojo, /*Node*/node, /*Object*/ keyComb, /*String*/topic){
	this.node = node;
    this.keyComb = keyComb;
    this.topic = topic;
    this.canceled = false;
    this.keyListener = function(e){
        if (e &&                                  // event exists and,                
        	e.keyCode == this.keyComb.keyCode &&  // event keyCode matches expected keyCode and,
        	!this.canceled &&                     // listener isn't cancelled and,
        	(!(this.keyComb.ctrl ||               // No modifier key required or,
        	   this.keyComb.alt  || 
        	   this.keyComb.shift) ||
            ((this.keyComb.ctrl && e.ctrlKey) ||  // event modifier matches expected modifier
             (this.keyComb.alt && e.altKey) ||
             (this.keyComb.shift && e.shiftKey)))) {
            this.publish(e);                      // publish event
        }
    };
    this.publish = function(e){
        e.preventDefault();
        e.stopPropagation();
        
        dojo.publish(this.topic, [e]);
    };
    this.cancel = function(){
        this.canceled = true;
        
        dojo.disconnect(node, "onkeyup", this, "keyListener");
    };

    dojo.connect(node, "onkeyup", this, "keyListener");

    return this;
};

hyperic.utils.addUrlXtraCallback = function(plugin_id, fn) {
    urlXtraVar[plugin_id] = urlXtraVar[plugin_id] || [];
    urlXtraVar[plugin_id].push(fn);
};

hyperic.utils.deleteResource = function(appdefentityId, msg) {
	var url = "/resource/hub/RemoveResource.do";
	
	if (confirm(msg)) {
		url += "?resources=" + escape(appdefentityId);
		url += "&delete.x=1&preventCache=" + new Date().getTime();
		document.location = url;
	}
}

hyperic.utils.passwd = {
    /**
     * Password strength meter
     * Params are keys in kwArgs
     * 
     * @param node [Node] - text node that contains the pw, has a .value property
     * @param password [String] (optinal)
     * @param updateNode [Node] - the node to update, has a .innerHTML property
     * @param minimumChars (optional) defaults to 6
     * 
     * @return the localize string representing very weak - strong
     */
    assignStrength : function(kwArgs){
        var desc = [];
        desc[0] = hyperic.i18n.html.vweak; //"Very Weak";
        desc[1] = hyperic.i18n.html.weak; //"Weak";
        desc[2] = hyperic.i18n.html.medium; //"Medium";
        desc[3] = hyperic.i18n.html.strong; //"Strong";

        var score   = 0;

        //if password bigger than 6 give 1 point
        if(password.length > 6) { score++; }

        //if password has both lower and uppercase characters give 1 point      
        if( password.match(/[a-z]/) && password.match(/[A-Z]/) ) { score++; }

        //if password has at least one number give 1 point
        if(password.match(/\d+/)) { score++; }

        //if password has at least one special caracther give 1 point
        if( password.match(/.[!,@,#,$,%,\^,&,*,?,_,~,-,(,)]/) ) { score++; }

        //if password bigger than 12 give another 1 point
        if(password.length > 12) { score++; }
        document.getElementById("passwordDescription").innerHTML = desc[score];
        document.getElementById("passwordStrength").className = "strength" + score;
    }
};

hyperic.widget.search = function(dojo, /*Object*/ urls, /*number*/ minStrLenth, /*Object*/ keyCode){
	this.opened     = false;
    this.minStrLen  = minStrLenth; 
    this.resourceURL= urls.resource;
    this.searchURL  = urls.search;
    this.keyCode    = keyCode;
    this.listeners  = [];
    /**
     * Connect all the events up and grab the nodes that we are going to need
     */
    this.create = function(){
        this.searchBox          = dojo.byId('searchBox');
        this.searchContainer    = dojo.byId('headerSearchBox');
        this.nodeSearchResults  = dojo.byId('headerSearchResults');
        this.nodeCancel         = dojo.byId('searchClose');
        this.nodeSearchButton   = dojo.byId("headerSearch");
        //Set up the key listeners for the search feature
        this.listeners.push( new hyperic.utils.addKeyListener(dojo, window.document, this.keyCode, 'search') );
        this.listeners.push( new hyperic.utils.addKeyListener(dojo, this.searchContainer, { keyCode: 13 }, 'enter') );
        this.listeners.push( new hyperic.utils.addKeyListener(dojo, dojo.byId('header'), { keyCode: 27 }, 'escape') );
        dojo.connect(this.nodeCancel, "onclick", function(e) {
        	dojo.style("headerSearchResults", "display", "none");
        	dojo.byId("searchBox").value = "";
        });
    };
    this.search = function(e){
        var string = e.target.value;
        if(this.searchBox.value.length >= this.minStrLen){
            this.searchStarted();
            hqDojo.xhrGet({
                url: this.searchURL,
                content: {
                	q: string
                },
                handleAs: "json",
                headers: { 
                	"Content-Type": "application/json" 
                },
                timeout: 5000, 
                load: function(response, args) {
                	var resURL = resourceURL + ((resourceURL.indexOf("?") == -1) ? "?" : "&") + "eid=";
    	            var usrURL = userURL + ((userURL.indexOf("?") == -1) ? "?" : "&") + "mode=view&u=";
    	            var template = "<li class='type'><a href='link' title='fullname'>text<\/a><\/li>";
    	            var count = 0;
    	            var res = "";
    	            var relink = new RegExp("link", "g");
    	            var retext = new RegExp("text", "g");
    	            var refulltext = new RegExp("fullname", "g");
    	            var retype = new RegExp("type", "g");
    	            var resources = response.resources;
    	            
    	            for (var i = 0; i < resources.length; i++) {
    	            	var length = resources[i].name.length;
    	                var fullname = resources[i].name;
    	                
    	                if (length >= 37){
    	                	resources[i].name = resources[i].name.substring(0,4) + "..." + resources[i].name.substring(length-28, length);
    	                }
    	                
    	                res += template.replace(relink, resURL+resources[i].id)
    	                	.replace(retext, resources[i].name)
    	                    .replace(retype, resources[i].resType)
    	                    .replace(refulltext, fullname);
    	                    
    	                count++;
    	            }
    	            
    	            hqDojo.byId("resourceResults").innerHTML = res;
    	            hqDojo.byId("resourceResultsCount").innerHTML = count;

    	            count = 0;
    	            res = "";
    	             
    	            var users = response.users;
    	             
    	            for (var i = 0; i < users.length; i++) {
    	                var fullname = users[i].name;
    	             
    	                res += template.replace(relink, usrURL+users[i].id)
    	                    .replace(retype, "user")
    	                    .replace(retext, users[i].name);
    	            
    	                count++;
    	            }
    	             
    	            hqDojo.byId("usersResults").innerHTML = res;
    	            hqDojo.byId("usersResultsCount").innerHTML = count;
    	            hqDojo.style("headerSearchResults", "display", "");
    	            hqDojo.removeClass("searchBox", "searchActive");
                },
                error: this.error
            });
        }else{
            this.searchEnded();
            this.nodeSearchResults.style.display = 'none';
        }
    };
    this.error = function(){
        this.searchEnded();
    };
    this.loadResults = function(response){
        this.searchEnded();
    };
    this.searchStarted = function(){
    	hqDojo.addClass("searchBox", "searchActive");
    };
    this.searchEnded = function(){
    	hqDojo.removeClass("searchBox", "searchActive");
    };

    return this;
};

function loadSearchData(response, evt) {
	var resURL = resourceURL + ((resourceURL.indexOf("?") == -1) ? "?" : "&") + "eid=";
    var usrURL = userURL + ((userURL.indexOf("?") == -1) ? "?" : "&") + "mode=view&u=";
    var template = "<li class='type'><a href='link' title='fullname'>text<\/a><\/li>";
    var count = 0;
    var res = "";
    var relink = new RegExp("link", "g");
    var retext = new RegExp("text", "g");
    var refulltext = new RegExp("fullname", "g");
    var retype = new RegExp("type", "g");
    var resources = response.resources;
    
    for(var i = 0; i < resources.length; i++) {
    	var length = resources[i].name.length;
        var fullname = resources[i].name;
        
        if(length >= 37){
        	resources[i].name = resources[i].name.substring(0,4) + "..." + resources[i].name.substring(length-28, length);
        }
        
        res += template.replace(relink, resURL+resources[i].id)
        	.replace(retext, resources[i].name)
            .replace(retype, resources[i].resType)
            .replace(refulltext, fullname);
            
        count++;
    }
    
    hqDojo.byId("resourceResults").innerHTML = res;
    hqDojo.byId("resourceResultsCount").innerHTML = count;

    count = 0;
    res = "";
     
    var users = response.users;
     
    for(var i = 0; i < users.length; i++) {
        var fullname = users[i].name;
     
        res += template.replace(relink, usrURL+users[i].id)
            .replace(retype, "user")
            .replace(retext, users[i].name);
    
        count++;
    }
     
    hqDojo.byId("usersResults").innerHTML = res;
    hqDojo.byId("usersResultsCount").innerHTML = count;
    hqDojo.byId('headerSearchResults').style.display = '';
    hqDojo.byId('searchBox').className = "";
}

/**
 *
 * @args kwArgs - keys 
 *
 *
 */
hyperic.widget.Menu = function(kwArgs) {
    var that = this;
    that.show = function(node){
    };
    that.onclick = function(evt) {
        if(!this._isVisible) {
            var x,y,node;
            if(window.event) { node = window.event.srcElement;  console.log(node);  }
            else { node = evt.target; }
            if(this._isSubMenu) {
                //put it on the right
                x=node.offsetLeft+node.clientWidth+12;
                y=node.clientHeight+node.offsetTop+4;
            }else {
                //put it underneath
                x=node.offsetLeft;
                y=node.clientHeight+node.offsetTop;
            }
            this.node.style.top = y+'px';
            this.node.style.left = x+'px';
            this.node.style.display = 'block';
            this._isVisible = true;
            if(this._isSubMuenu)
            {
                this.isFocused = true;
            }
        }
    };
    this.onUnHover = function() {
        if(this.child){
            if(!this.child.isFocused){
            }else{
                this.node.style.display = 'none';
                this.isFocused = false;
                this._isVisible = false;
            }
        }else{
           this.node.style.display = 'none';
           this.isFocused = false;
            this._isVisible = false;
        }

    };
    this.onHover = function() {
        this.isFocused = true;
    };
    this._init = function(kwArgs) {
        if(kwArgs.child) {
            this.child = kwArgs.child;
        }
        var that = this;
        if(kwArgs.menuNode) {
            this.node = kwArgs.menuNode;
            this.node.style.display='none';
            hqDojo.connect(this.node, 'onmouseenter', that, 'onHover');
            hqDojo.connect(this.node, 'onmouseleave', that, 'onUnHover');
        }
        if(kwArgs.toggleNode) {
            if(kwArgs.subMenu){
                this._isSubMenu = true;
                hqDojo.connect(kwArgs.toggleNode, 'onmouseover', that, 'onclick');
            }else{
                hqDojo.connect(kwArgs.toggleNode, 'onclick', that, 'onclick');
            }
        }
        
    }; 
    this.isFocused = false;
    this._isVisible = false;
    this._init(kwArgs);
};


/* OLD REPORTING */

function init_reporting(){
    hqDojo.ready(function(){
        var reportList = hqDojo.byId("reports");
        if(reportList){
            reportList.selectedIndex = 0;
            selectedChanged(reportList);
        }
    });
}

hyperic.hq = {};
hyperic.hq.reporting = {};
hyperic.hq.dom = {};
    
hyperic.hq.dom.validationTextboxProps = {
    id : "ValidationWidget1",
    required : true,
    size : 23,
    maxlength : 60,
    templateString : '<div class="fieldRow" dojoAttachPoint="fieldRowContainerNode">\n\t<label for="${this.widgetId}">\n\t\t<span class="fieldLabel ${this.fieldRequiredClass}"><img src="/images/icon_required.gif" height="9" width="9" border="0"><span dojoAttachPoint="fieldLabel">${this.label}</span></span>\n\t</label>\n\t\t<div class="fieldValue" dojoAttachPoint="fieldWrapper">\n\t\t<span style="float:${this.htmlfloat};">\n\t\t<input dojoAttachPoint="textbox" type="${this.type}" dojoAttachEvent="onblur;onfocus;onkeyup" id="${this.widgetId}" name="${this.name}" size="${this.size}" maxlength="${this.maxlength}" class="${this.className}" style="">\n\t\t\t<div dojoAttachPoint="invalidSpan" class="${this.invalidClass}">&nbsp;-&nbsp;${this.messages.invalidMessage}</div>\n\t\t<div dojoAttachPoint="missingSpan" class="${this.missingClass}">&nbsp;-&nbsp;${this.messages.missingMessage}</div>\n\t\t<div dojoAttachPoint="rangeSpan" class="${this.rangeClass}">&nbsp;-&nbsp;${this.messages.rangeMessage}</div>\n\t\t</span>\n</div>\n</div>'
};

hyperic.hq.dom.selectboxProps = {
   templateCssString : ".dojoComboBoxOuter {\n\tborder: 0px !important;\n\tmargin: 0px !important;\n\tpadding: 0px !important;\n\tbackground: transparent !important;\n\twhite-space: nowrap !important;\n}\n\n.dojoComboBox {\n\tborder: 1px inset #afafaf;\n\tmargin: 0px;\n\tpadding: 0px;\n\tvertical-align: middle !important;\n\tfloat: none !important;\n\twidth:172px;position: static !important;\n\tdisplay: inline !important;\n}\n\n/* the input box */\ninput.dojoComboBox {\n\tborder-right-width: 0px !important; \n\tmargin-right: 0px !important;\n\tpadding-right: 0px !important;\n}\n\n/* the down arrow */\nimg.dojoComboBox {\n\tborder-left-width: 0px !important;\n\tpadding-left: 0px !important;\n\tmargin-left: 0px !important;height:15px;\n}\n\n/* IE vertical-alignment calculations can be off by +-1 but these margins are collapsed away */\n.dj_ie img.dojoComboBox {\n\tmargin-top: 1px; \n\tmargin-bottom: 1px; \n}\n\n/* the drop down */\n.dojoComboBoxOptions {\n\tfont-family: Verdana, Helvetica, Garamond, sans-serif;\n\t/* font-size: 0.7em; */\n\tbackground-color: white;\n\tborder: 1px solid #afafaf;\n\tposition: absolute;\n\tz-index: 1000; \n\toverflow: auto;\n\tcursor: default;width:200px;\n}\n\n.dojoComboBoxItem {\n\tpadding-left: 2px;\n\tpadding-top: 2px;\n\tmargin: 0px;\n}\n\n.dojoComboBoxItemEven {\n\tbackground-color: #f4f4f4;\n}\n\n.dojoComboBoxItemOdd {\n\tbackground-color: white;\n}\n\n.dojoComboBoxItemHighlight {\n\tbackground-color: #63709A;\n\tcolor: white;\n}\n",
   templateString : '<div class="fieldRow"><label for="${this.widgetId}"><span class="fieldLabel ${this.fieldRequiredClass}"><img width="9" height="9" border="0" src="/images/icon_required.gif"/><span dojoAttachPoint="fieldLabel">${this.label}</span></span></label><div class="fieldValue" dojoAttachPoint="fieldWrapper"><span class=\"dojoComboBoxOuter\"\n\t><input style=\"display:none\"  tabindex=\"-1\" name=\"\" value=\"\" \n\t\tdojoAttachPoint=\"comboBoxValue\"\n\t><input style=\"display:none\"  tabindex=\"-1\" name=\"\" value=\"\" \n\t\tdojoAttachPoint=\"comboBoxSelectionValue\"\n\t><input type=\"text\" autocomplete=\"off\" class=\"dojoComboBox\"\n\t\tdojoAttachEvent=\"key:_handleKeyEvents; keyUp: onKeyUp; compositionEnd; onResize;\"\n\t\tdojoAttachPoint=\"textInputNode\"\n\t><img hspace=\"0\"\n\t\tvspace=\"0\"\n\t\tclass=\"dojoComboBox\"\n\t\tdojoAttachPoint=\"downArrowNode\"\n\t\tdojoAttachEvent=\"onMouseUp:handleArrowClick; onResize;\"\n\t\tsrc=\"${this.buttonSrc}\"></img></span>\n<div dojoattachpoint="validationMessage" class="errorMsg"></div></div></div></div>',
   mode : 'local',
   autoComplete : true,
   fieldRequiredClass : "required",
   forceValidOption : true,
   maxListLength : 10
};

hqDojo.require("dijit.dijit");
hqDojo.require("dijit.form.DateTextBox");
hyperic.hq.dom.createDatePicker = function(datePickerName){
	var section = hqDojo.create("div", {
			"style": "padding: 5px;"
	}, "reportOptions");
	
	var label = hqDojo.create("label", {
		"for": datePickerName,
		"class": "fieldLabel required"	
	}, section);
	
	var img = hqDojo.create("img", {
		"src": "/images/icon_required.gif",
		"height": 9,
		"width": 9,
		"border": 0
	}, label);
	
	var span = hqDojo.create("span", {}, label);
	
	span.innerHTML = datePickerName;

	var div = hqDojo.create("div", {}, section);
	var datePicker = new hqDijit.form.DateTextBox({
		"name": datePickerName,
		"value": new Date()
	}, div);
	
    hyperic.hq.reporting.manager.currentReportOptions.push(datePicker);
};

hqDojo.require("dijit.form.ValidationTextBox");
hyperic.hq.dom.createTextBox = function(textboxName){
	var section = hqDojo.create("div", {
		"style": "padding: 5px;"
	}, "reportOptions");

	var label = hqDojo.create("label", {
		"for": textboxName,
		"class": "fieldLabel required"	
	}, section);
	
	var img = hqDojo.create("img", {
		"src": "/images/icon_required.gif",
		"height": 9,
		"width": 9,
		"border": 0
	}, label);
	
	var span = hqDojo.create("span", {}, label);

	span.innerHTML = textboxName;
	
	var div = hqDojo.create("div", {}, section);
	var textbox = new hqDijit.form.ValidationTextBox({
		"name": textboxName,
		"required": true,
	    "size": 23,
	    "maxlength": 60
	}, div);
	
	hyperic.hq.reporting.manager.currentReportOptions.push(textbox);
    
	return textbox;
};

hqDojo.require("dijit.form.FilteringSelect");
hqDojo.require("dojo.data.ItemFileReadStore");
hyperic.hq.dom.createSelectBox = function(selectboxName, optionsArray){
	var section = hqDojo.create("div", {
		"style": "padding: 5px;"
	}, "reportOptions");

	var label = hqDojo.create("label", {
		"for": selectboxName,
		"class": "fieldLabel required"	
	}, section);
	
	var img = hqDojo.create("img", {
		"src": "/images/icon_required.gif",
		"height": 9,
		"width": 9,
		"border": 0
	}, label);
	
	var span = hqDojo.create("span", {}, label);

	span.innerHTML = selectboxName;

	decodeNamesInOptionsArray(optionsArray);
	optionsArray.unshift({
		id: "-1", 
		name: "All Resources"
	});

	var dataStore = new hqDojo.data.ItemFileReadStore({
		data: {
			identifier: "id",
			label: "name",
			items: optionsArray
		}
	});
	
	var div = hqDojo.create("div", {}, section);
	
	var comboBox = new hqDijit.form.FilteringSelect({
		"name": selectboxName,
		"store": dataStore,
		"value": -1,
        "searchAttr": "name"
    }, div);
	
	hyperic.hq.reporting.manager.currentReportOptions.push(comboBox);
};

/*
 * Bug fix HQ-4358:
 * When creating a combo box, the options name is encoded to HTML,
 * the encoded names are shown as is and not decoded.
 * This fix decodes the HTML encoded string by creating a temporary div,
 * insert to it the encoded string, which decodes the string, and then
 * return the encoded string from the div and empty the div.
 */
function decodeNamesInOptionsArray(optionsArray) {
	for ( var int = 0; int < optionsArray.length; int++) {
		optionsArray[int].name = decodeEntities(optionsArray[int].name);
	}
}
// Decodes an HTML encoded string
var decodeEntities = (function() {
	// Creates an empty div
	var element = document.createElement('div');
	function decodeHTMLEntities (str) {
		// Security patch: just in case
		str = str.replace(/<script[^>]*>([\S\s]*?)<\/script>/gmi, '');
		str = str.replace(/<\/?\w(?:[^"'>]|"[^"]*"|'[^']*')*>/gmi, '');
		// Decode: innerText for IE, textContent for FireFox & Chrome
		element.innerHTML = str;
		return element.innerText || element.textContent;
	}
	return decodeHTMLEntities;
})();

hyperic.hq.reporting.manager = { 
    currentReportOptions : [], 
    preSubmit : function() {
        var submit = this.validateReportOptions();
        if(submit){
            this.serializeReportOptions();
            //hqDojo.byId("ReportingForm").submit();
            var mp = hqDojo.byId("messagePanel");
            if(mp){
                mp.style.display="none";
            }
            return true;
        }else{
            return false;
        }
    },
    serializeReportOptions : function(){
        var obj = [];
        var temp = "";
        
        for(var i = 0; i < this.currentReportOptions.length; i++) {
        	temp += '"' + this.currentReportOptions[i].name + '":"';
        	
        	if (this.currentReportOptions[i].getValue().getTime) {
	        	temp += this.currentReportOptions[i].getValue().getTime();
	        } else {
	        	temp += this.currentReportOptions[i].getValue();
	        }
	        
	        temp += '"';
	        
            obj.push(temp);
            
            temp = "";
        }
        
        hqDojo.byId("jsonData").value = "{" + obj.join(",") + "}";
    },
    validateReportOptions : function(){
        var submit = true;
        var dates ={};
        
        for (var i =0; i < this.currentReportOptions.length; i++) {
            if (this.currentReportOptions[i].getValue === '') {
            	submit = false && submit;
            	break;
            } else if (this.currentReportOptions[i].name == "StartDate") { 
                dates.StartDate = this.currentReportOptions[i].getValue();
                dates.StartDateNode = this.currentReportOptions[i];
            } else if (this.currentReportOptions[i].name == "EndDate") {
                dates.EndDate = this.currentReportOptions[i].getValue();
                dates.EndDateNode = this.currentReportOptions[i];
            } 
        }
        if(dates.EndDate && dates.StartDate){
            if(dates.StartDate > dates.EndDate){
                submit = false && submit;
            }
        }
        return submit;
    }
};

function resetReportOptions(){
    hqDojo.empty("reportOptions");
    hyperic.hq.reporting.manager.currentReportOptions = [];
    //TODO iterate through and call destroy
}

function selectedChanged(selectNode){
    //get the changed object
    //send to the server
    //inner html the response after checking the validity
    var selected;
    var textNode;
    var textTargetNode = hqDojo.byId("reportDetails");
    if(selectNode){
        selected = selectNode.options[selectNode.selectedIndex];
        textNode = hqDojo.byId(selected.value);
        textTargetNode.innerHTML = textNode.innerHTML;
    }
    getReportOptions(selected.value);
}

function getReportOptions(reportName){
	hqDojo.xhrGet({
		url: "/reporting/ReportCenter.do?reportName=" + reportName,
		handleAs: "json",
		load: function(response, args) {
	        if(response) createInputFieldsFromJSON(response); 
	    } 
	});
}

function createInputFieldsFromJSON(descriptor){
    resetReportOptions();
    
    for (var i = 0; i < descriptor.length; i++) {
        var type = descriptor[i].descriptor.type;
        
        if (type !== undefined) {
            var o = descriptor[i].descriptor;
        
            if (type.indexOf("String") != -1) {
                hyperic.hq.dom.createTextBox(o.name);
            } else if (type.indexOf("Date") != -1) {
                hyperic.hq.dom.createDatePicker(o.name);
            } else if (type.indexOf("Group") != -1) {
                hyperic.hq.dom.createSelectBox(o.name, o.options);
            }
        } 
    }
}


/**
 * @deprecated used only for the struts header
 */
function activateHeaderTab(dojo){
    var l = document.location;
    if (document.navTabCat) {
        //This is a plugin
        if (document.navTabCat == "Resource") {
            dojo.addClass("resTab", "activeTab");
        } else if(document.navTabCat == "Admin") {
            dojo.addClass("adminTab", "activeTab");
        }
        return;
    }
    l = l+""; // force string cast
    if ( l.indexOf("Dash")!=-1 || 
         l.indexOf("dash")!=-1 ) {
    	dojo.addClass("dashTab", "activeTab");
    } else if( l.indexOf("Resou")!=-1 ||
               l.indexOf("resource")!=-1 || 
               l.indexOf("alerts/")!=-1 || 
               l.indexOf("TabBodyAttach.do")!=-1 ) {
    	dojo.addClass("resTab", "activeTab");
    } else if( l.indexOf("rep")!=-1 || 
               l.indexOf("Rep")!=-1 || 
               l.indexOf("masth")!=-1 ) {
    	dojo.addClass("analyzeTab", "activeTab");
    } else if( l.indexOf("admin.do")!=-1 || 
               l.indexOf("Admin.do")!=-1 ) {
    	dojo.addClass("adminTab", "activeTab");
    } else if ( l.indexOf("/admin/") != -1 ) {
    	dojo.addClass("adminTab", "active");
    } else if ( l.indexOf("/Management.do") != -1 ) {
    	dojo.addClass("managementTab", "activeTab");
    }
}

hyperic.widget = hyperic.widget || {};

/**
* Chart - timeplot chart manager functionality. Creates and manages dom and datasource for dashboard timeplot chart widgets
*
* @param node
* @param kwArgs
*/
hyperic.widget.Chart = function(node, kwArgs) {
    var that = this;
    that.subscriptions=[];	
    that.create = function(node, kwArgs) {
    	// display the metric name for a multiple metric single resource chart
    	var chartDisplayName = kwArgs.name;  
    	if(kwArgs.measurementName)
    	{
        	if(chartDisplayName.indexOf(kwArgs.measurementName) == -1)
            {
        		chartDisplayName += ': ' + kwArgs.measurementName + ", Units: " + kwArgs.measurementUnits;
            }     		
    	}

        var template = '<div class="chartCont"> <h3 class="cTitle">'
        					+ getShortLink(chartDisplayName, kwArgs.maxTitleLength, kwArgs.url) 
        					+ '</h3><div id="widget_chart"></div><div class="xlegend"></div></div>';
        
        that.template = template;
        that.node = hqDojo.byId(node);
        that.name = kwArgs.name;
        that.node.innerHTML = template;
        that.data = kwArgs.data;
        };
    that.showChart = function() {
        // if(arg == that.tabid){
            if (!that.isShowing) {
                //create chart
                var count = 0;
                for(var i in that.data) {
                    if(undefined !== that.data[i] && typeof(that.data[i]) !== 'function')
                    {
                        count++;
                    }
                }
                
                if(count > 1)
                {
                    that.dataSource = new Timeplot.DefaultEventSource();
                    var pi = [Timeplot.createPlotInfo( {
                        id : "plot1", 
                        dataSource    : new Timeplot.ColumnSource(that.dataSource, 1), 
                        valueGeometry : new Timeplot.DefaultValueGeometry( { 
                                                gridColor : "#000000", 
                                                axisLabelsPlacement : "left" }), 
                        timeGeometry  : new Timeplot.DefaultTimeGeometry( { 
                                                gridColor : new Timeplot.Color("#DDDDDD"), 
                                                axisLabelsPlacement : "bottom"} ), 
                        showValues    : true, 
                        lineColor     : "#00EB08", 
                        roundValues   : false, //00EB08 89EB0F
                        fillColor     : "#00B93A" //#E6FCCA
                        }
                    )];
                    that.chart = Timeplot.create(hqDojo.byId("widget_chart"), pi);
                    // that.chart.loadText(that.url, ",", that.dataSource);
                    that.chart.loadJSON(that.data, that.dataSource);                }
                else
                {
                    node_el = hqDojo.byId(node);
                    var message = SimileAjax.Graphics.createMessageBubble(node_el.ownerDocument);
                    message.containerDiv.className = "timeline-message-container";
                    node_el.appendChild(message.containerDiv);
                    
                    message.contentDiv.className = "timeline-message";
                    message.contentDiv.innerHTML = '<span style="color: #000; font-size: 12px;">No Data</span>';
                    message.containerDiv.style.display = "block";

                    that.dataSource = new Timeplot.DefaultEventSource();
                    var pi = [Timeplot.createPlotInfo( {
                        id            : "plot1", 
                        dataSource    : new Timeplot.ColumnSource(that.dataSource, 1), 
                        valueGeometry : new Timeplot.DefaultValueGeometry( {
                                                gridColor : "#000000", 
                                                axisLabelsPlacement : "left" } ), 
                        timeGeometry  : new Timeplot.DefaultTimeGeometry( {
                                                gridColor : new Timeplot.Color("#DDDDDD"), 
                                                axisLabelsPlacement : "bottom"}), 
                        showValues    : true, 
                        lineColor     : "#00EB08", 
                        roundValues   : false, //00EB08 89EB0F
                        fillColor     : "#00B93A" //#E6FCCA
                        }
                    )];
                    that.chart = Timeplot.create(hqDojo.byId("widget_chart"), pi);
                    that.chart.loadJSON({"2008-08-04T01:08:51-0700":[0],"2008-08-04T01:09:51-0700":[0],"2008-08-04T01:10:51-0700":[0],"2008-08-04T01:11:51-0700":[0],"2008-08-04T01:12:51-0700":[0]}, that.dataSource);
                }
                
                that.isShowing = true;
            }
            delete count;
        // }
    };

    this.cleanup = function(){
        // destroy all children of the chart container
        while(that.node.lastChild) {
          that.node.removeChild(that.node.lastChild);
        }
    
        that.node = null;
    };
    that.isShowing = false;
    this.create(node, kwArgs);
    this.showChart();
};

// set up the dashboard widget namespace
hyperic = hyperic || {}; 
/** 
 * @namespace
 */
hyperic.dashboard = hyperic.dashboard || {}; 


/**
 * common hyperic dashboard widget functions
 *
 * @author Anton Stroganov <anton@hyperic.com>
 * @class hyperic.dashboard.widget
 * @requires hyperic.dashboard.widget
 */
hyperic.dashboard.widget = {
    /**
     * catches clicks on widget, and passes them on to the handler function 
     * determined by the button's class.
     *
     * @param {Event} e
     * @see #clickHandler
     */
    clickHandler: function(e) {
        var action = e.target.className;
        if(this['click_' + action])
        {
            e.stopPropagation();
            e.preventDefault();
            hqDojo.stopEvent(e);

            this['click_' + action](e);
        }
    },
    
    /**
     * clicking the config button shows the config layer 
     * @param {Event} e
     * @see #clickHandler
     */
    click_config_btn: function(e,onEnd)
    {
        if(this.currentSheet != 'config')
        {
            this.swapSheets('config',onEnd);
        }
    },

    /**
     * clicking the config layer's cancel button swaps config and 
     * content layers back
     *
     * @param {Event} e
     * @see #clickHandler
     */
    click_cancel_btn: function(e,onEnd)
    {
        this.swapSheets(this.previousSheet,onEnd);
    },
    
    /**
     * clicking the config layer's save button should make an xhr request
     * to store the data, and swaps the content layer back in
     *
     * @param {Event} e
     * @see #clickHandler
     */
    click_save_btn: function(e)
    {
        throw new Error('You need to implement this widget\'s save config functionality!');
    },
    
    /**
     * cleanup code should happen in the child class, and then invoke this
     * to remove the dashboard portlet from dashboard page.
     */
    click_remove_btn: function(e)
    {
        removePortlet(this.config.portletName, this.config.portletLabel);
    },
    
    /**
     * swap any two layers with a dojo fade-out -> fade-in effect
     * invokes function passed in as 'onEnd' argument after the fade
     *
     * @param {Node} from
     * @param {Node} to
     * @param {function} onEnd
     */
    swapSheets: function(to,onEnd) {
        var fromSheet = this.sheets[this.currentSheet];
        var toSheet = this.sheets[to];
        var c = hqDojo.fx.chain([
            hqDojo.fadeOut({
                node : fromSheet, 
                onEnd: function() { 
                	hqDojo.style(fromSheet, 'display', 'none');
                    hqDojo.style(toSheet, 'display', 'block'); 
                } 
            }),
            hqDojo.fadeIn({
                node : toSheet
            })
        ]);

        if(onEnd) {
            hqDojo.connect(c,'onEnd',onEnd);
        }
        
        c.play();
        
        this.previousSheet = this.currentSheet;
        this.currentSheet = to;
    }
};


/**
 * moves the selected option from one select box to another
 * 
 * @param {Node} from: selectbox node to move option from 
 * @param {Node} to: selectbox node to move option to 
 * @see #addOptionToSelect
 */
function moveOption(from,to)
{
    if(from.selectedIndex != -1)
    {
        for(var opt = 0; opt < from.options.length; opt++)
        {
            if(from.options[opt].selected === true) {
                addOptionToSelect(to, new Option(from.options[opt].text,from.options[opt].value));
                from.remove(opt);
                // call moveOption recursively, because remove() reset the options indices in the 
                // source selectbox,
                // so if multiple options are selected, once the first one is moved, the 
                // rest are offset by 1, so we can't move them in the same for loop.
                moveOption(from,to);
                break;
            }
        }
    }
}

/**
 * add an option to a selectbox in a correct position.
 * to keep the box alphabetically sorted.
 *
 * @param {Node} selectbox node to add option to
 * @param {Node} new option to add do the selectbox
 */
function addOptionToSelect(select,option)
{
    var newLocation = null;
    // set a 'title' property on the option to use as a tooltip.
    option.title = option.text;
    if(select.options.length > 0)
    {
        for(var i = 0,j = select.options.length; i < j; i++)
        {
            if(select.options[i].text.toLowerCase() > option.text.toLowerCase())
            {
                newLocation = i;
                break;
            }
        }
    }
    try {  // standards compliant; doesn't work in IE
        if(null !== newLocation)
        {
            select.add(option, select.options[newLocation]);
        }
        else
        {
            select.add(option, null); // standards compliant; doesn't work in IE
        }
    }
    catch(ex) { // IE only
        if(null !== newLocation)
        {
            select.add(option,newLocation); // IE only
        }
        else
        {
            select.add(option); // IE only
        }
    }
}

/**
 * filter a selectbox to show only the options
 * with names that match the text given.
 *
 * @param {Node} selectbox node
 * @param {Text} search text
 */
function searchSelectBox(node,text) {
    hqDojo.forEach(
        node.options,
        function(opt)
        {
            // if(opt.text.match(exp))
            if(opt.text.toLowerCase().indexOf(text.toLowerCase()) !== -1)
            {
                if(opt.disabled === true)
                {
                    opt.disabled = false;
                    opt.style.display = '';
                }
            }
            else
            {
                if(opt.disabled === false)
                {
                    opt.disabled = true;
                    opt.style.display = 'none';
                }
            }
        });
}

/**
 * wraps an html select box in an object to allow easy
 * filtering of the selectbox
 *
 * @param {Node} selectbox node
 * @param {Array} optional data to populate the select with. Can be 
 *   an array of objects (needed to set the option "value" attributes to 
 *   specific values):
 *       [{text: "option 1", value:"0101"}, {text: "two", value:"bla"}, ...] 
 *   or an array of option text's (if you don't care about the 'value' 
 *   attributes)
 *       ["option 1","option 2", ...]
 */
hyperic.selectBox = function(select, data) {
    var that = this;
    that.select = select;

    that.data = {};
    that.length = 0;

    // if we have data passed in, populate the select and the data object
    if(typeof data !== 'undefined')
    {
        // normalize data
        if(data.length)
        {
            for(var i = 0; i < data.length; i++)
            {
                if(data[i].text && data[i].value)
                {
                    that.data[data[i].value] = {text: data[i].text, value: data[i].value};
                }
                else
                {
                    that.data[i] = {text: data[i], value: i};
                }
            }
        }
        else
        {
            for(var i in data)
            {
                if(typeof data[i] !== 'function' && data[i].text && data[i].value)
                {
                    that.data[data[i].value] = {text: data[i].text, value: data[i].value};
                }
            }
        }

        // initial select population
        for(i in that.data)
        {
            if(typeof that.data[i] !== 'function')
            {
                addOptionToSelect(that.select,new Option(that.data[i].text, that.data[i].value));
                that.data[i].hidden = false;
                that.length += 1;
            }
        }
    }

    /**
     * remove option with given index from the select and from the data array
     * 
     * @param {Number} index
     */
    that.remove = function(index) {
        delete that.data[that.select.options[index].value];
        that.select.remove(index);
        that.length -= 1;
    };

    /**
     * add new option to the select and its data to the data array
     * 
     * @param {Option} option
     */
    that.add = function(option) {
        if(typeof that.data[option.value] == 'undefined')
        {
            that.data[option.value] = {text: option.text, value: option.value, hidden: false};
            addOptionToSelect(that.select,option);
            that.length += 1;
        }
        else
        {
            console.log('option with value '+ option.value +' already exists, could not be added');
        }
    };

    that.steal = function(from)
    {
        if(from.select.selectedIndex != -1)
        {
            for(var opt = 0; opt < from.select.options.length; opt++)
            {
                if(from.select.options[opt].selected === true) {
                    that.add(new Option(from.select.options[opt].text,from.select.options[opt].value));
                    from.remove(opt);
                    // call steal recursively, because remove() reset the options indices in the 
                    // source selectbox,
                    // so if multiple options are selected, once the first one is moved, the 
                    // rest are offset by 1, so we can't move them in the same for loop.
                    that.steal(from);
                    break;
                }
            }
        }
    };

    /**
     * filter the selectbox for options that contain specified text
     * 
     * @param {String} text
     */
    that.search = function(text) {
        text = text.toLowerCase();
        // delete options that should no longer be shown
        if(that.select.options.length > 0)
        {
            for(var i = 0; i < that.select.options.length; i++)
            {
                if(that.select.options[i].text.toLowerCase().indexOf(text) == -1)
                {
                    that.data[that.select.options[i].value].hidden = true;
                    that.select.remove(i);
                    that.search(text);
                    return;
                }
            }
        }
        // re-add options that should be shown
        for(i in that.data)
        {
            // if an option is hidden, but matches the search string
            if(typeof that.data[i] !== 'function' && that.data[i].hidden && that.data[i].text.toLowerCase().indexOf(text) !== -1)
            {
                // add option to select
                addOptionToSelect(that.select,new Option(that.data[i].text, that.data[i].value));
                // mark it as not hidden
                that.data[i].hidden = false;
            }
        }
    };

    that.getAllValues = function() {
        var vals = [];
        for(i in that.data)
        {
            // get all values (even hidden ones, woo)
            if(typeof that.data[i] !== 'function')
            {
                vals.push(that.data[i].value);
            }
        }
        return vals;
    };

    that.reset = function() {
        that.length = 0;
        that.data = {};
        while(that.select.lastChild) {
            that.select.removeChild(that.select.lastChild);
        }
    };
};

/**
 *
 */
hyperic.dashboard.arcWidget = function(node, portletName, portletLabel, kwArgs){
    var that = this;

    that.config = {
        portletName: portletName,
        portletLabel: portletLabel
    };

    that.desc = {};

    that.args = kwArgs;
    that.url = kwArgs.url + '/';
    that.error = kwArgs.error;
    that.container = {};
    that.currentReport = null;
    that.queryParams = { get: "dashboard.html?action=listReports" };

    // the dom containers
    that.container.loading = hqDojo.query('.loading',node)[0];
    that.container.progress = hqDojo.query('.progress',node)[0];
    that.container.error_loading = hqDojo.query('.notfound',node)[0];
    that.container.content = hqDojo.query('.contents',node)[0];

    // the buttons and form elements
    that.remove_btn = hqDojo.query('.remove_btn',node)[0];
    that.refresh_btn = hqDojo.query('.refresh_btn',node)[0];
    that.select_btn = hqDojo.query('.reportSelect',node)[0];

    // the report stuff
    that.report_img = hqDojo.query('.reportImage',node)[0];
    that.report_legend = hqDojo.query('.reportLegend',node)[0];
    that.showLeg_btn = hqDojo.query('.showlegend_btn',node)[0];
    that.hideLeg_btn = hqDojo.query('.hidelegend_btn',node)[0];
    that.legend = hqDojo.query('.legend',node)[0];
    that.report_title = hqDojo.query('.reportTitle',node)[0];
    that.arcLink = hqDojo.query('.arcLink',node)[0];

    that.isLegendShowing = false;
    that.legendDia = '';
    that.legendDiv = 'legend_dialog';
    
    /**
     * The widget remove callback
     * @param e the click event
     */
    this.click_remove_btn = function(e) {
        hyperic.dashboard.widget.click_remove_btn.apply(that);
    };

    /**
     * The widget refresh callback - updates the report list
     * @param e the click event
     */
    this.click_refresh_btn = function(e) {
        that.updateIframe(that.url+that.queryParams.get);
    };

    this.toggleLegend = function() {
        if(that.isLegendShowing) {
            that.showLeg_btn.style.display = "block";
            that.hideLeg_btn.style.display = "none" ;
            that.legend.style.display = "none" ;
            that.isLegendShowing = !that.isLegendShowing;
        } else {
            /*
            that.showLeg_btn.style.display = "none";
            that.hideLeg_btn.style.display = "block" ;
            that.legend.style.display = "block" ;
            that.isLegendShowing = !that.isLegendShowing;
            */

            // Reload the image
            var now = new Date();
            that.report_legend.src = that.report_legend.src + '&' + now.getTime();
            that.legendDia.show();

            // Dim the underlay less
            var underlayId = that.legendDiv + '_underlay'
            hqDojo.byId(underlayId).style.background = '#707070';
            hqDojo.byId(underlayId).style.opacity = '0.1';
        }
    };
    /**
     *
     * @param e
     */
    this.select_change = function(e) {
        console.info("select changed");
        that.report_img.src = "";
        that.getImageURL(that.url + that.select_btn.options[that.select_btn.selectedIndex].value);
    };

    /**
     * Callback to process the creation or update of the iframe
     */
    this.getReportsCallback = function(data) {
        var response = document.arcReportList;
        if (response !== null) {
            var curSel = that.select_btn.options[that.select_btn.selectedIndex].value;
            var options = [];
            while (that.select_btn.length > 0) {
                that.select_btn.remove(0);
            }
            var index = 0;
            for (var d in response) {
                if (response[d] !== undefined && response[d].label !== undefined) {
                    if(that.currentReport == null) {
                        that.currentReport = response[d];
                    }
                    options[index] = { value: response[d].URI, text : response[d].label };
                    that.desc[response[d].URI] = response[d].Description;
                    index++;
                }
            }
            new hyperic.selectBox(that.select_btn, options);

            for (var i = 0; i < that.select_btn.options.length; i++) {
                if (that.select_btn.options[i].value == curSel) {
                    that.select_btn.options[i].selected = true;
                    break;
                }
            }
            if (that.select_btn.options.length == 0)
                that.container.error_loading.innerHTML = that.error;
        }

        console.info("completed select box");
        if(response.length > 0) {
            that.container.loading.style.display = "none";
            that.container.error_loading.style.display = "none";
            that.container.progress.style.display = "none";

            that.container.content.style.display = "block";
        } else {
            that.container.loading.style.display = "block";
            that.container.error_loading.style.display = "block";
            that.container.progress.style.display = "none";
        }
        
        console.info("completed callback");
        that.getImageURL(that.url + that.select_btn.options[that.select_btn.selectedIndex].value);
    };

    this.errorRemotingCallback = function(data){
        that.container.loading.style.display = "block";
        that.container.error_loading.style.display = "block";
        that.container.progress.style.display = "none";
    };

    /**
     * Evaluates the string specified by the response and returns the object evaluated or null
     * @param url
     */
    this._evalResponse = function(response) {
        if (response === undefined || response.length < 1) {
            return;
        }
        document.arcReportList = null;
        try {
            eval("document.arcReportList = " + response + "");
        } catch(e) {
            console.log("Error extracting the available reports");
        }
        return document.arcReportList;
    };

    /**
     *
     * @param url the arc server url
     * @param connectionParam the POST param for the api call
     */
    this.updateIframe = function(url) {
        that.container.loading.style.display = "block";
        that.container.progress.style.display = "block";
        that.container.error_loading.style.display = "none";
        that.container.content.style.display = "none";
        that.init_connection(that.url+that.queryParams.get);
    };

    /**
     * initialize the portlet
     */
    this.init = function(kwArgs) {
        var isInError = false;
        //the refresh is showing
        //set the arc link
        //that.container.error_loading.innerHTML = that.args.notFound;
        if(that.url == "/") {
            that.container.loading.style.display = "block";
            that.container.error_loading.style.display = "block";
            that.container.progress.style.display = "none";
        } else {
            that.arcLink.href = that.url;
            that.init_connection(that.url+that.queryParams.get);
            console.log("connecting the buttons");
            hqDojo.connect(that.refresh_btn,'onclick',that.click_refresh_btn);
            hqDojo.connect(that.select_btn,'onchange',that.select_change);
            hqDojo.connect(that.showLeg_btn,'onclick',that.toggleLegend);
            hqDojo.connect(that.hideLeg_btn,'onclick',that.toggleLegend);
            var pane = that.legend;
			that.legendDia = new hqDijit.Dialog({
				id: that.legendDiv,
				refocus: true,
				autofocus: false,
                opacity: 0,
				title: "Report Legend"
			},pane);
        }

        hqDojo.connect(that.remove_btn,'onclick',that.click_remove_btn);
    };

    /**
     * Creates the iFrame, only to be called by init()
     * @param uri the arc server uri
     */
    this.init_connection = function (uri) {
        var now = new Date();
        uri = uri + '&' + now.getTime();
        console.info(uri);
        hqDojo.io.script.get({
            handleAs : "html",
            url : uri,
            checkString : "document.arcReportList",
            load: function(data) {
                that.getReportsCallback(data);
            },
            error : function(data) {
                that.errorRemotingCallback(data);
            }
        });
    };

    this.getImageURL = function (uri, checkString) {
        console.info(uri);
        document.arcImageData = null;
        hqDojo.io.script.get({
            handleAs : "html",
            url : uri,
            checkString : "document.arcImageData",
            error : function(data) {
                that.errorRemotingCallback(data);
            }
        });
    };

    this.getImageURLCallback = function() {
        var response = document.arcImageData;
        //assign the urls to the img src prepending the base url
        if(response && response.length >= 0) {
            if(response[0].reportImageURL) {
                that.report_title.innerHTML = that.desc[that.select_btn.options[that.select_btn.selectedIndex].value];
                that.report_img.src = that.url + response[0].reportImageURL;
                that.report_img.style.height = "auto";

                // Hide the legend
                that.hideLeg_btn.style.display = "none" ;
                if(response[0].reportLegendURL) {
                    that.report_legend.src = that.url + response[0].reportLegendURL;
                    that.showLeg_btn.style.display = "block";
                }
                else {
                    that.showLeg_btn.style.display = "none";
                }
                that.isLegendShowing = false;
            }
            else {
                // Handle error
                // response[0].error
                // response[0].errorDetails
            }

        }
    };

    this.init(kwArgs);
    return that;
};

hyperic.dashboard.arcWidget.prototype = hyperic.dashboard.widget;

/**
 * chartWidget is a widget that displays a chart slideshow
 * 
 * @author Anton Stroganov <anton@hyperic.com>
 * @base hyperic.dashboard.widget
 * @constructor
 */
hyperic.dashboard.chartWidget = function(args) {
    var that = this;
    var node = hqDojo.byId(args.tabName);
    var portletName = args.portletName;
    var portletLabel = args.title;
    var baseUrl = args.url;
    var chartUrl = args.chartUrl;
    var ctypeChartUrl = args.ctypeChartUrl;
    
    that.sheets = {};
    that.sheets.loading = hqDojo.query('.loading',node)[0];
    that.sheets.error_loading = hqDojo.query('.error_loading',node)[0];
    that.sheets.instructions = hqDojo.query('.instructions',node)[0];
    that.sheets.config = hqDojo.query('.config',node)[0];
    that.sheets.content = hqDojo.query('.content',node)[0];
    that.currentSheet = 'loading';

    that.last_updated_div = hqDojo.query('.last_updated',node)[0];

    that.chartsearch = hqDojo.byId('chartsearch');
    that.chartselect = new hyperic.selectBox(hqDojo.byId('chartselect'));
    
    that.play_btn = hqDojo.query('.pause_btn',node)[0];
    
    that.chart = null;
    that.charts = [];
    that.chartCount = 0;
    that.cycleId = null;
    that.fetchChartsCycleId = null;
    that.currentChartId = null;
    that.needsResize = false;
    that.config = {
        portletName: portletName,
        portletLabel: portletLabel
    };

    /**
     * pause the chart playback before showing the config layer 
     * populate the config form from the config object
     * 
     * @param {Event} e
     * @see #playCharts
     * @see hyperic.dashboard.widget#click_config_btn
     * @see hyperic.dashboard.widget#clickHandler
     */
    this.click_config_btn = function(e)
    {
        that.pauseCharts();
        hyperic.dashboard.widget.click_config_btn.apply(this,[e]);
        
        var input_rotation = hqDojo.byId('chart_rotation');
        var input_interval = hqDojo.byId('chart_interval');
        var input_range = hqDojo.byId('chart_range');
        if(that.config.rotation == 'true')
        {
            input_rotation.checked = true;
        }
        for(var i = 0; i < input_interval.options.length; i++)
        {
            if(input_interval.options[i].value == that.config.interval)
            {
                input_interval.selectedIndex = i;
                break;
            }
        }
        for(var j = 0; j < input_range.options.length; j++)
        {
            if(input_range.options[j].value == that.config.range)
            {
                input_range.selectedIndex = j;
                break;
            }
        }
    };

    /**
     * sets the config values from the config form
     * TODO: makes request to store config on server
     * after saving and showing the content layer 
     * - resize chart if necessary
     * - restart chart slideshow playback if rotation is still on
     * 
     * @param {Event} e
     * @see #playCharts
     * @see hyperic.dashboard.widget#click_save_btn
     * @see hyperic.dashboard.widget#clickHandler
     */
    this.click_save_btn = function(e)
    {
        // if the time range has changed, reset the chart data and chart refresh cycle
        if(that.config.range && that.config.range != hqDojo.byId('chart_range').value)
        {
            for(var i = 0; i < that.charts.length; i++)
            {
                if(that.charts[i].interval)
                {
                    clearInterval(that.charts[i].interval);
                    that.charts[i].interval = null;
                }
                that.charts[i].data = null;
            }
        }

        that.config.interval = parseInt(hqDojo.byId('chart_interval').value,10);
        that.config.range = hqDojo.byId('chart_range').value;
        that.config.rotation = hqDojo.byId('chart_rotation').checked ? 'true' : 'false';

        hqDojo.xhrPost({
            url: baseUrl,
            content: { 
        		"tr" : that.config.range, 
        		"ivl" : that.config.interval, 
        		"rot" : that.config.rotation,
        		"_method" : "PUT" // need to work around issue using PUT directly
        	},
        	handle: "json",
        	load: function(data){
                that.config.interval = parseInt(data.ivl,10) || that.config.interval;
                that.config.range = data.tr || that.config.range;
                that.config.rotation = data.rot || that.config.rotation;
            },
            error: function(data){
                console.info("An error occurred saving charts config... ", data);
            },
            timeout: 2000
        });

        if(that.chartCount > 0)
        {
            that.swapSheets('content',
                function()
                {
                    if(that.needsResize)
                    {
                        that.chartResize();
                    }
                    if(that.config.rotation == 'true')
                    {
                        that.playCharts();
                    }
                });
        }
        else
        {
            that.swapSheets('instructions');
        }
    };

    /**
     * extends parent's behaviour to restart chart slideshow playback
     * after canceling config and showing the content layer 
     * 
     * @param {Event} e
     * @see #playCharts
     * @see hyperic.dashboard.widget#click_cancel_btn
     * @see hyperic.dashboard.widget#clickHandler
     */
    this.click_cancel_btn = function(e)
    {
        if(that.chartCount > 0)
        {
            that.swapSheets('content',
                function()
                {
                    if(that.needsResize)
                    {
                        that.chartResize();
                    }
                    if(that.config.rotation == 'true')
                    {
                        that.playCharts();
                    }
                });
        }
        else
        {
            that.swapSheets('instructions');
        }
    };

    /**
     * a play button click handler to start playback, 
     * and change play button to a pause button. 
     * 
     * @param {Event} e
     * @see #playCharts
     * @see hyperic.dashboard.widget#clickHandler
     */
    that.click_play_btn = function(e) {
        that.playCharts();
    };
    
    /**
     * a pause button click handler to stop playback, 
     * and change pause button to a play button. 
     * 
     * @param {Event} e
     * @see #pauseCharts
     * @see hyperic.dashboard.widget#clickHandler
     */
    that.click_pause_btn = function(e) {
        that.pauseCharts();
    };
    
    /**
     * extends parent's behaviour to pause the chart playback 
     * and cleanup the chart before removing the widget
     * 
     * @param {Event} e
     * @see #pauseCharts
     * @see hyperic.dashboard.widget#click_remove_btn
     * @see hyperic.dashboard.widget#clickHandler
     */
    this.click_remove_btn = function(e)
    {
        that.pauseCharts();
        if (that.chart != null) {
            that.chart.cleanup();
        }
        hyperic.dashboard.widget.click_remove_btn.apply(that);
    };
    
    /**
     * remove chart button click handler
     * confirm chart deletion with user, make an ajax request to remove chart from config, 
     * on success, cycle to next chart, 
     * delete chart from selectbox, 
     * remove chart refresh interval if set, 
     * and finally remove chart data from that.charts object
     * 
     * @param {Event} e
     * @see hyperic.dashboard.widget#clickHandler
     */
    this.click_chart_remove_btn = function(e)
    {
        // save chart id into a variable so if it cycles away from this chart during the request, we still know which one we're deleting
        var chartId = that.currentChartId;
        var chartIndex = that.chartselect.select.selectedIndex;
        if(confirm('Remove ' + that.charts[chartId].name + ' from saved charts?')) {
        	var urlToUse = chartUrl.replace("{rid}", that.charts[chartId].rid).replace("{mtid}", that.charts[chartId].mtid);
            hqDojo.xhrPost( {
                url: urlToUse,
                content: {
            		"_method" : "DELETE" // need to work around issue using PUT directly
            	},
            	handleAs: 'json',
                preventCache: true,
                load: function(data){
                    if(data.error)
                    {
                        console.info('An server error occurred deleting chart: ' + data);
                    }
                    else
                    {
                        if(that.chartCount > 1)
                        {
                            // cycle to next chart if we're still displaying the one that's about to get deleted.
                            if(that.currentChartId == chartId)
                            {
                                that.cycleCharts();
                            }
                        }
                        else
                        {
                            that.pauseCharts();
                            if(that.cycleId !== null) {
                                clearInterval(that.cycleId);
                                that.cycleId = null;
                            }

                            that.chart.cleanup();
                            that.chart = null;

                            that.swapSheets('instructions');

                            // check for new charts after a minute.
                            that.fetchChartsCycleId = setInterval(that.fetchAndPlayCharts, 60000);
                        }
                        // clear chart refresh data interval
                        if(that.charts[chartId].interval)
                        {
                            clearInterval(that.charts[chartId].interval);
                        }
                        // remove chart from selectbox
                        that.chartselect.remove(chartIndex);

                        // delete chart data
                        delete that.charts[chartId];
                        that.chartCount--;
                    }
                },
                error: function(data){
                    console.info("A connection error occurred deleting chart: ", data);
                },
                timeout: 30000
            });
        }
    }

    /**
     * an event handler to handle onKeyUp events on a search textbox.
     * It filters the chart selectbox of the widget to show only the charts
     * with names that match the text being typed in.
     * 
     * @param {Event} e
     */
    that.search = function(e)
    {
        that.chartselect.search(e.target.value);
    };
    
    /**
     * an event handler to handle the onfocus event on the search textbox.
     * It empties it to prepare it for user's input.
     * 
     * @param {Event} e
     */
    that.emptySearch = function(e)
    {
        if(e.target.value == '[ Live Search ]')
        {
            e.target.value = '';
        }
    };
    
    /**
     * an event handler to handle the onblur event on the search textbox.
     * It resets the search textbox to instruction value if it's empty.
     * 
     * @param {Event} e
     */
    that.resetSearch = function(e) 
    {
        if(e.target.value === '')
        {
            e.target.value = '[ Live Search ]';
        }
    };
    
    /**
     * an event handler to handle the onclick event on the chart selectbox.
     * pauses chart playback and swaps the current chart for the newly 
     * selected chart.
     * 
     * @param {Event} e
     */
    that.select = function(e)
    {
        that.pauseCharts();
        var chartId = null;
        
        // in firefox, the target will be the option
        if(e.target.tagName.toLowerCase() == 'option')
        {
            chartId = e.target.value;
        }
        else // in IE, the target is the selectbox
        {
            chartId = e.target.options[e.target.selectedIndex].value;
        }
        if(that.currentChartId != chartId)
        {
            that.cycleCharts(chartId);
        }
    };

    /**
     * swaps a chart for the next chart in the list. 
     * NB: use 'that.' rather than 'this.' keyword here, because when
     * the browser window invokes this function, 'this.' is set to 
     * the window context, rather than the object context.
     *
     * @see #playCharts
     * @see #pauseCharts
     */
    that.cycleCharts = function(chartId)
    {
        var next = 0;
        if(typeof chartId == 'undefined' || chartId < 0)
        {
            if(that.chartselect.select.selectedIndex !== -1 && that.chartselect.select.selectedIndex != that.chartselect.select.options.length-1)
            {
                // console.info('next defined by next element from select box selected index ' + that.chartselect.selectedIndex + ' + 1');
                next = that.chartselect.select.options[that.chartselect.select.selectedIndex+1].value;
                that.chartselect.select.selectedIndex = that.chartselect.select.selectedIndex+1;
            }
            else
            {
                next = that.chartselect.select.options[0].value;
                that.chartselect.select.selectedIndex = 0;
            }
        }
        else
        {
            // console.info('next defined by passing in argument ' + chartId);
            next = chartId;
        }

        if(that.chart != null)
        {
            that.chart.cleanup();
        }

        if(that.charts[next].data)
        {
            that.chart = new hyperic.widget.Chart('chart_container', that.charts[next]);
            that.currentChartId = next;
            chartId = null;
            that.last_updated_div.innerHTML = 'Updated: ' + that.charts[next].last_updated.formatDate('h:mm t');
        }
        else
        {
            that.fetchChartData(next).addCallback(
                function()
                {
                    // console.log(that.charts);
                    // console.info("next chart to display is " + next);
                    // add a callback to refresh the chart data in a minute

                    that.charts[next].interval = setInterval(function(){that.fetchChartData(next);},36000);

                    // console.log('fetched data; next chart id is ' + next);
                    if(that.charts[next].data)
                    {
                        that.chart = new hyperic.widget.Chart('chart_container', that.charts[next]);
                    }
                    else
                    {
                        that.chart = new hyperic.widget.Chart('chart_container', {data: {'0': [0]}, name: that.charts[next].name});
                    }
                    that.last_updated_div.innerHTML = 'Updated: ' + that.charts[next].last_updated.formatDate('h:mm t');
                    that.currentChartId = next;
                    chartId = null;
                });
        }
    };
    
    /**
     * display the first chart if no chart is showing, 
     * and start the chart cycle a chart for the next chart in the list. 
     *
     * @see #pauseCharts
     * @see #cycleCharts
     */
    that.playCharts = function() {
        // console.log('starting to play');
        that.cycleCharts();
        if(that.cycleId === null) {
            /* 
             * in setInterval, we call an anonymous function instead of that.cycleCharts directly
             * because setInterval passes the time elapsed into the function it calls, and that confuses
             * the hell out of cycleCharts, which expects a chartId as an argument. By using an anonymous
             * function we quietly discard the argument and call cycleCharts without an argument.  
             */
            that.cycleId = setInterval(function() { that.cycleCharts(); }, parseInt(that.config.interval,10)*1000);

            // display pause button when playing
            that.play_btn.src = '/images/4.0/icons/control_pause.png';
            that.play_btn.className = 'pause_btn';
            that.play_btn.alt = 'pause slideshow';
        }
    };
    
    /**
     * if charts are playing, clear the interval to pause the playback 
     *
     * @see #playCharts
     * @see #cycleCharts
     */
    that.pauseCharts = function() {
        if(that.cycleId !== null) {
            clearInterval(that.cycleId);
            that.cycleId = null;
            
            // display play button when pausing
            that.play_btn.src = '/images/4.0/icons/control_play.png';
            that.play_btn.className = 'play_btn';
            that.play_btn.alt = 'play slideshow';
        }
    };

    /**
     * fetch the chart list with chart names and id's from the server
     */
    that.fetchAndPlayCharts = function()
    {
        // preset defaults
        that.config.interval = 60;
        that.config.range = '1h';
        that.config.rotation = 'true';
 
        if(that.fetchChartsCycleId !== null) {
            clearInterval(that.fetchChartsCycleId);
            that.fetchChartsCycleId = null;
            that.swapSheets('loading');
        }
        hqDojo.xhrGet( {
            url: baseUrl,
            handleAs: 'json',
            headers: { "Content-Type": "application/json"},
            preventCache: true,
            load: function(data){
	            that.config.interval = parseInt(data.ivl,10) || that.config.interval;
	            that.config.range = data.tr || that.config.range;
	            that.config.rotation = data.rot || that.config.rotation;

	            if (data.payload && data.payload.length > 0)
                {
                    that.charts = data.payload.sort(
                        function(a,b) { 
                            a = a.name.toLowerCase(); 
                            b = b.name.toLowerCase();
                            return a > b ? 1 : (a < b ? -1 : 0);
                        });

                    that.chartCount = that.charts.length;

                    that.populateChartSelect();
                    that.swapSheets('content',
                        function()
                        {
                            that.chartContainerResize();
                            that.playCharts();

                            if(that.config.rotation != 'true' || that.charts.length == 1)
                            {
                                that.pauseCharts();
                            }
                        });
                }
                else
                {
                    that.swapSheets('instructions',
                        function()
                        {
                            // try again after a minute.
                            that.fetchChartsCycleId = setInterval(that.fetchAndPlayCharts, 60000);
                        });
                }
            },
            error: function(data){
                that.swapSheets('error_loading',
                    function()
                    {
                        // try again after a minute.
                        that.fetchChartsCycleId = setInterval(that.fetchAndPlayCharts, 60000);
                    });
                console.info("An error occurred fetching charts: ", data);
            },
            timeout: 30000
        });
    };

    that._fetchingChart = false;
    
    that.fetchChartData = function(chart)
    {
    	if (that._fetchingChart) {
    		return new hqDojo.Deferred();
    	}
    	
    	that._fetchingChart = true;
    	
    	var urlToUse = unescape(chartUrl).replace("{rid}", that.charts[chart].rid).replace("{mtid}", that.charts[chart].mtid);
    	var ctype = that.charts[chart].ctype;
    	
    	if (ctype != null) {
    		urlToUse = unescape(ctypeChartUrl).replace("{rid}", that.charts[chart].rid).replace("{mtid}", that.charts[chart].mtid).replace("{ctype}", ctype);
    	}
    
    	return hqDojo.xhrGet( {
            url: urlToUse,
            handleAs: 'json',
            headers: { "Content-Type": "application/json"},
            preventCache: true,
            load: function(data){
                // that.charts[chart].data = data;
                if(!data.error && data.payload && data.payload.length > 0)
                {
                    that.charts[chart].data = {};
                    
                    var payload = data.payload[0];
                    
                    // loop through the 'data' object and round all values to 3 decimal places
                    for(var i in payload.data) {
                        if(typeof payload.data[i] != 'function')
                        {
                            that.charts[chart].data[i] = [];
                            for(var j in payload.data[i]) {
                                if(typeof payload.data[i][j] != 'function')
                                {
                                    that.charts[chart].data[i][j] = payload.data[i][j].toFixed(3);
                                }
                            }
                        }
                    }
                    that.charts[chart].measurementName = payload.measurementName;
                    that.charts[chart].measurementUnits = payload.measurementUnits;
                    that.charts[chart].last_updated = new Date();
                    that.charts[chart].maxTitleLength = (that.sheets.content.offsetWidth - 150) * 1.5;
                }
    
	        	that._fetchingChart = false;
    		},
            error: function(data){
                console.info("An error occurred fetching charts config ", data);
            },
            timeout: 30000
        });
    };

    /**
     * populate the available and selected alert selectboxes
     * 
     * @see hyperic.dashboard.widget#addOptionToSelect
     */
    that.populateChartSelect = function()
    {
        for(var i = 0; i < that.charts.length; i++)
        {
            that.chartselect.add(new Option(that.charts[i].name,i));
        }
    };
    
    /**
     * destroy the old chart, resize the chart container, and re-create the chart
     * invoked on window resize to ensure that chart always fits the widget.
     */
    that.chartResize = function()
    {
        if(that.currentSheet == 'content')
        {
            if (that.chart != null) {
                that.chart.cleanup();
            }
            that.chartContainerResize();
            that.chart = new hyperic.widget.Chart('chart_container', that.charts[that.currentChartId]);
            that.needsResize = false;
        }
        else
        {
            that.needsResize = true;
        }
    };

    that.chartContainerResize = function()
    {
        if(that.currentSheet == 'content')
        {
            hqDojo.query('#chart_container',that.sheets.content)[0].style.width = that.sheets.content.offsetWidth - 150;
        }
    };

    if(that.chartsearch && that.chartselect)
    {
        // connect the onclick event of the whole widget to the clickHandler
        // function of this object, inherited from hyperic.dashboard.widget.
        hqDojo.connect(node,'onclick',hqDojo.hitch(that,'clickHandler'));

        // set up the event handlers for the live search box
        hqDojo.connect(that.chartsearch,'onfocus', that.emptySearch);
        hqDojo.connect(that.chartsearch,'onblur', that.resetSearch);
        hqDojo.connect(that.chartsearch,'onkeyup',that.search);

        // set up the event handler for the select box
        hqDojo.connect(that.chartselect.select,'onclick',that.select);

        // set up the event handler for the remove button
        hqDojo.connect(hqDojo.byId('chart_remove_btn'),'onclick',that.click_chart_remove_btn);

        // handle resizing of the window
        hqDojo.connect(window,'onresize',hqDojo.hitch(that, that.chartResize));

        that.fetchAndPlayCharts();

        if(that.config.rotation == 'false')
        {
            this.pauseCharts();
        }
    }
};

// set the hyperic.dashboard.widget as the ancestor of the chartWidget class.
hyperic.dashboard.chartWidget.prototype = hyperic.dashboard.widget;

/**
 * summaryWidget is a widget that displays alert summaries
 * $
 * @author Anton Stroganov <anton@hyperic.com>
 * @base hyperic.dashboard.widget
 * @constructor
 */
hyperic.dashboard.summaryWidget = function(args) {
    var that = this;
    var node = hqDojo.byId(args.tabName);
    var portletName = args.portletName;
    var portletLabel = args.title;
    var baseUrl = args.url;
    
	that.cycleId = null;

    that.sheets = {};
    that.sheets.loading = hqDojo.query('.loading',node)[0];
    that.sheets.error_loading = hqDojo.query('.error_loading',node)[0];
    that.sheets.instructions = hqDojo.query('.instructions',node)[0];
    that.sheets.config = hqDojo.query('.config',node)[0];
    that.sheets.content = hqDojo.query('.content',node)[0];
    that.currentSheet = 'loading';

    that.last_updated = new Date();
    that.last_updated_div = hqDojo.query('.last_updated',node)[0];

    that.alert_groups = {"data": {}, "count": 0};
    that.selected_alert_groups = [];
    that.alert_group_status = {};

    that.available_alert_groups = new hyperic.selectBox(hqDojo.byId('available_alert_groups'));
    that.enabled_alert_groups = new hyperic.selectBox(hqDojo.byId('enabled_alert_groups'));
    that.groupsearch = hqDojo.byId('groupsearch');

	that.enable_alert_btn = hqDojo.query('.enable_alert_btn',that.sheets.config)[0];
	that.disable_alert_btn = hqDojo.query('.disable_alert_btn',that.sheets.config)[0];

    that.tables = {
        lcol: hqDojo.query('.lcol table tbody',node)[0],
        rcol: hqDojo.query('.rcol table tbody',node)[0]
    };

    that.config = {
        portletName: portletName,
        portletLabel: portletLabel
    };

    /**
     * an event handler to handle onKeyUp events on the search textbox.
     * filters the available and enabled alert selectboxes of the widget
     * to show only the alert groups with names that match the given text 
     * 
     * @param {Event} e
     */
    that.search = function(e)
    {
        that.available_alert_groups.search(e.target.value);
        that.enabled_alert_groups.search(e.target.value);
    };

    /**
     * an event handler to handle the onfocus event on the search textbox.
     * It empties it to prepare it for user's input.
     * 
     * @param {Event} e
     */
    that.emptySearch = function(e)
    {
        if(e.target.value == '[ Group Search ]')
        {
            e.target.value = '';
        }
    };
    
    /**
     * an event handler to handle the onblur event on the search textbox.
     * It resets the search textbox to instruction value if it's empty.
     * 
     * @param {Event} e
     */
    that.resetSearch = function(e) 
    {
        if(e.target.value === '')
        {
            e.target.value = '[ Group Search ]';
        }
    };

    /**
     * an event handler to handle the onclick event on the enable alert button.
     * moves the selected alert from the available alerts selectbox to the 
     * enabled alerts selectbox.
     * 
     * @param {Event} e
     * @see #moveOption
     * @see #disableAlert
     */
    that.click_enable_alert_btn = function(e)
    {
        // if(that.available_alert_groups.selectedIndex != -1)
        // {
        //     that.selected_alert_groups.push(that.available_alert_groups.options[that.available_alert_groups.selectedIndex].value);
        // }
        var proposedCount = 0;
        //count up the currently selected items that are being added
        for(var i = 0; i < that.available_alert_groups.select.options.length; i++){
            if(that.available_alert_groups.select.options[i].selected)
                proposedCount++;
        }
        //if the current proposed additions is less than the max allow the add
        that.enabled_alert_groups.steal(that.available_alert_groups);

		if(that.available_alert_groups.length == 0)
		{
		    that.enable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_select_disabled.gif" alt="select">';
			that.enable_alert_btn.disabled = true;
		}
		if(that.disable_alert_btn.disabled === true)
		{
		    that.disable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_deselect.gif" alt="select">';
			that.disable_alert_btn.disabled = false;
		}
    };
    
    /**
     * an event handler to handle the onclick event on the disable alert button.
     * moves the selected alert from the enabled alerts selectbox to the 
     * available alerts selet.
     * 
     * @param {Event} e
     * @see #moveOption
     * @see #enableAlert
     */
    that.click_disable_alert_btn = function(e)
    {
        // remove the selected index from the this.enabled_alert_groups array
        // if(that.enabled_alert_groups.selectedIndex != -1)
        // {
        //     that.selected_alert_groups.splice(
        //         that.selected_alert_groups.indexOf(
        //             that.enabled_alert_groups.options[
        //                 that.enabled_alert_groups.selectedIndex
        //                 ].value
        //         ),
        //         1
        //     );
        // }
        that.available_alert_groups.steal(that.enabled_alert_groups);

		if(that.enable_alert_btn.disabled === true)
		{
		    that.enable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_select.gif" alt="select">';
			that.enable_alert_btn.disabled = false;
		}

		if(that.enabled_alert_groups.length == 0)
		{
		    that.disable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_deselect_disabled.gif" alt="select">';
			that.disable_alert_btn.disabled = true;
		}
    };

    /**
     * config button handler
     * pause the chart refresh cycle while in config sheet
     */
    this.click_config_btn = function(e)
    {
        that.pauseRefreshCycle();
        hyperic.dashboard.widget.click_config_btn.apply(this, [e,that.reset_config]);
    }

    /**
     * cancel button handler
     * reset the available/enabled alert group selectboxes
     * and swap the config layer and show content layer again
     */
    this.click_cancel_btn = function(e)
    {
        if(that.selected_alert_groups.length > 0)
        {
            that.swapSheets('content');
        }
        else
        {
            that.swapSheets('instructions');
        }
        that.startRefreshCycle(portlets_reload_time);
    };

    that.reset_config = function() {
        // reset the searchbox
        that.groupsearch.value = '';
        that.resetSearch({target: that.groupsearch});

        that.enabled_alert_groups.reset();
        that.available_alert_groups.reset();
        that.populateAlertGroups();
        
        // reset enable/disable alert buttons.
        if(that.enable_alert_btn.disabled === true && that.available_alert_groups.length > 0)
        {
            that.enable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_select.gif" alt="select">';
            that.enable_alert_btn.disabled = false;
        }
        else if(that.enable_alert_btn.disabled === false && that.available_alert_groups.length == 0)
        {
            that.enable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_select_disabled.gif" alt="select">';
            that.enable_alert_btn.disabled = true;
        }
        if(that.disable_alert_btn.disabled === true && that.enabled_alert_groups.length > 0)
        {
            that.disable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_deselect.gif" alt="select">';
            that.disable_alert_btn.disabled = false;
        }
        else if(that.disable_alert_btn.disabled === false && that.enabled_alert_groups.length == 0)
        {
            that.disable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_deselect_disabled.gif" alt="select">';
            that.disable_alert_btn.disabled = true;
        }
    };

    /**
     * save button handler
     * store the data on server, then rebuild the selected_alert_groups local array
     * fetch the updated alert group status,
     * repaint the tables and
     * swap the config layer and show content layer again
     */
    that.click_save_btn = function(e)
    {
        that.selected_alert_groups = that.enabled_alert_groups.getAllValues();

        hqDojo.xhrPost( {
            url: baseUrl,
            content: {
        		"rid" : that.selected_alert_groups,
        		"_method" : "PUT" // need to work around issue using PUT directly
        	},
            handleAs: 'json',
            preventCache: true,
            load: function(data){
        		that.selected_alert_groups = data.rid
        		that.alert_groups.data = data.data;
                that.alert_group_status = data.payload;

                that.fetchAlertGroupStatus().addCallback(function(){

                    that.repaintAlertGroups();
                    if(that.selected_alert_groups.length > 0)
                    {
                        that.swapSheets('content');
                    }
                    else
                    {
                        that.swapSheets('instructions');
                    }
                });
            },
            error: function(data){
                console.info("An error occurred saving alerts config... " + data);

                if(that.selected_alert_groups.length > 0)
                {
                    that.swapSheets('content');
                }
                else
                {
                    that.swapSheets('instructions');
                }
            },
            timeout: 2000
        });
        that.startRefreshCycle(portlets_reload_time);
    };
    
    /**
     * populate the available and selected alert selectboxes
     * 
     * @see #addOptionToSelect
     */
    that.populateAlertGroups = function()
    {
        for(var i in that.alert_groups.data)
        {
            var to = null;
            var alertOption = new Option(that.alert_groups.data[i],i);
            for(var j = 0; j < that.selected_alert_groups.length; j++)
            {
                if(that.selected_alert_groups[j] == i)
                {
                    to = that.enabled_alert_groups;
                    break;
                }
            }
            to = to || that.available_alert_groups;
            
            to.add(alertOption);
        }

        if(that.available_alert_groups.length == 0)
		{
		    that.enable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_select_disabled.gif" alt="select">';
			that.enable_alert_btn.disabled = true;
		}
        if(that.enabled_alert_groups.length == 0)
		{
		    that.disable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_deselect_disabled.gif" alt="select">';
			that.disable_alert_btn.disabled = true;
		}
    };

    /**
     * destroy current alert tables, and call #paintAlertGroups() to re-paint them
     */
    that.repaintAlertGroups = function() 
    {
        for(var i in that.tables)
        {
            while(that.tables[i].rows.length > 1) {
                that.tables[i].removeChild(that.tables[i].lastChild);
            }
        }
        that.paintAlertGroups();
    };

    /**
     * populate the html tables with the alerts based on the data in the #alert_group_status array
     */
    that.paintAlertGroups = function()
    {
        var groups = that.selected_alert_groups;
        var half = Math.ceil(groups.length/2);
        var status = {
            'red'    : 'Failure',
            'green'  : 'OK',
            'yellow' : 'Warning',
            'gray'   : 'No Data'
        };

        for(var i = 0; i < groups.length; i++)
        {
            var table = (i < half) ? that.tables.lcol : that.tables.rcol;
            var row = table.rows[0].cloneNode(true);
            
            // HQ-1491 says remove alternating row colors; leaving code in here in case this decision is reversed.
            // row.className = ((i < half ? i : i - half) % 2 == 0) ? 'even' : 'odd';
            row.style.display = '';
            row.id = 'alertGroup:' + groups[i];
            var data = that.alert_group_status[groups[i]] || ['gray','gray'];
            var name = that.alert_groups.data[groups[i]];
            var aObj = document.createElement("a");
     	 	aObj.href = "/Resource.do?eid=5:" + groups[i];
     	 	
     	 	if (name.length > 20) {
     	 		var abbrObj = document.createElement("abbr");
     	 		abbrObj.title = name;
     	 		abbrObj.innerHTML = name.substring(0,20).escapeHTML() + "&hellip;";
     	 		aObj.appendChild(abbrObj);
     	 	} else {
     	 		aObj.innerHTML = name.escapeHTML();
            }
     	 	row.childNodes[0].appendChild(aObj);
            row.childNodes[1].innerHTML = '<img src="/images/4.0/icons/'+data[0]+'.gif" alt="'+ status[data[0]] +'">';
            row.childNodes[2].innerHTML = '<a href="/alerts/Alerts.do?mode=list&eid=5:' + groups[i] + '"><img src="/images/4.0/icons/'+data[1]+'.gif" alt="'+ status[data[1]]+'" border="0"></a>';
            table.appendChild(row);
            data = name = null;
        }
    };

    /**
     * fetch the alert group status from server for currently selected alert groups
     * the server should return a json object of the following form:
     * { 
     *    "id" : ['resouce alert status','group alert status'],
     *    ...
     * }
     * the status is a letter code ('r' for red,'g' for green,'y' for yellow, or 'd' for data unavailable);
     */
    that.fetchAlertGroupStatus = function()
    {
        return hqDojo.xhrGet( {
            url: baseUrl,
            handleAs: 'json',
            headers: { "Content-Type": "application/json"},
            preventCache: true,
            load: function(data){
        		that.selected_alert_groups = data.rid
        		that.alert_groups.data = data.data;
                that.alert_group_status = data.payload;
                // that.alert_group_status = {
                //                 '1': ['red','green'],
                //                 '2': ['green','yellow'],
                //                 '3': ['green','green'],
                //                 '4': ['green','red'],
                //                 '5': ['green','yellow'],
                //                 // '6': ['g','g'],
                //                 '7': ['green','green'],
                //                 '8': ['green','green'],
                //                 '9': ['green','green'],
                //                 '10': ['green','green']
                //             };
            },
            error: function(data){
            	that.swapSheets('error_loading');
                console.info("An error occurred fetching alert groups status... ", data);
            },
            timeout: 45000
        });
    };

    that.fetchAlertGroupStatusCallback = function()
    {
    	if (that.currentSheet != 'error_loading') {
	        if(that.selected_alert_groups.length > 0)
	        {
	            that.last_updated = new Date();
	            that.last_updated_div.innerHTML = 'Updated: ' + that.last_updated.formatDate('h:mm t');
	            that.swapSheets('content',
	                function()
	                {
	            		that.repaintAlertGroups();
	                });
	        }
	        else
	        {
	            that.swapSheets('instructions');
	        }
    	}
    }

	var fib1 = 0;
	var fib2 = 1;
    that.startRefreshCycle = function(interval)
    {
        that.cycleId = setInterval(
        		function() {
                	if (that.currentSheet == 'error_loading') {
                		//If the last call to the server did not return, we don't want
                		//to keep sending request to the server every N seconds, we will
                		//send requests in intervals determined by Fibonacci series. 
                		//First we will wait 1 minute and make another call, if that call fails
                		//we will wait 2 minuts, 3, 5, 8... 30 minutes is the max wait time
                		//between failed calls.
                		that.swapSheets('loading');
                		that.pauseRefreshCycle();
                		if (30 < fib2) {
                			that.startRefreshCycle(30*60*1000);
                		}
                		else {
                			var tmp = fib2;
                    		fib2 += fib1;
                    		fib1 = tmp;
                			that.startRefreshCycle(fib2*60*1000);
                		}
                	}
                	else {
                		if (0 != fib1) {
                			fib1 = 0;
                			fib2 = 1;
                			that.pauseRefreshCycle();
                			that.startRefreshCycle(portlets_reload_time);
                		}
                		
                	}
                	that.fetchAlertGroupStatus().addCallback(
        	                    function() {
        	                    	that.fetchAlertGroupStatusCallback();
        	                    }
        	            );
        		}, 
        		interval);
    };
    
    that.pauseRefreshCycle = function() {
        clearInterval(that.cycleId);
        delete that.cycleId;
    }
    
    if(that.available_alert_groups && that.enabled_alert_groups)
    {
        // connect the onclick event of the whole widget to the clickHandler
        // function of this object, inherited from hyperic.dashboard.widget.
        hqDojo.connect(node,'onclick',hqDojo.hitch(that,'clickHandler'));

        // set up the event handlers for the live search box
        hqDojo.connect(that.groupsearch,'onfocus', that.emptySearch);
        hqDojo.connect(that.groupsearch,'onblur', that.resetSearch);
        hqDojo.connect(that.groupsearch,'onkeyup',that.search);
        
        // preload 'disabled' view buttons
        var disabled_select = new Image();
        disabled_select.src = '/images/4.0/buttons/arrow_select_disabled.gif';
        var disabled_deselect = new Image();
        disabled_deselect.src = '/images/4.0/buttons/arrow_deselect_disabled.gif';
        
        that.fetchAlertGroupStatus().addCallback(
        	function() {
        		that.fetchAlertGroupStatusCallback();
	            // periodically refresh the status
	            that.startRefreshCycle(portlets_reload_time);
	        });
    }
};

// set the hyperic.dashboard.widget as the ancestor of the summaryWidget class.
hyperic.dashboard.summaryWidget.prototype = hyperic.dashboard.widget;

hyperic.group_manager = function(args) {
	var that = this;
	
	that.dialogs = {};
	that.message_area = {};
	that.button_area = {};
	
	that.init = function() {		
		if(!that.dialogs.AddToExistingGroup){
	    	var pane = hqDojo.byId("add_to_existing_group_dialog");
			that.dialogs.AddToExistingGroup = new hqDijit.Dialog({
				id: "Add_to_Existing_Group_Dialog",
				refocus: true,
				autofocus: false,
				title: "Group Manager"
				}, pane);
			
			that.message_area.AddToExistingGroup = hqDojo.byId("AddToExistingGroupStatus");
			that.button_area.AddToExistingGroup = hqDojo.byId("AddToExistingGroupButton");
			that.button_area.AddToNewGroup = hqDojo.byId("AddToNewGroupButton");
			
			that.dialogs.AddToExistingGroup.toggleAll = function(checkAllBox) {
				var checkedState = checkAllBox.checked;
				var uList = checkAllBox.form;
			    var len = uList.elements.length;

				for (var i = 0; i < len; i++) {
			        var e = uList.elements[i];
			       
			        if (e.className.indexOf("selectableGroup") >= 0) {
			        	e.checked = checkedState;
			        	
			        	if (e.checked) {
			        		highlight(e);
			        	} else {
			        		unhighlight(e);
			        	}
					}
				}
				that.dialogs.AddToExistingGroup.toggleButtons(checkAllBox);
			}

			that.dialogs.AddToExistingGroup.toggleButtons = function(myCheckBox) {
				var myList = myCheckBox.form;
				var checkAllBox = hqDojo.byId("AddToExistingGroup_CheckAllBox");

				if (myCheckBox.id != checkAllBox.id) {
					checkAllBox.checked = false;
					
					if (myCheckBox.checked) {
						highlight(myCheckBox);
					} else {
						unhighlight(myCheckBox);
					}
				}
				
				if (getNumCheckedByClass(myList, "selectableGroup") > 0) {
					that.button_area.AddToExistingGroup.className = "CompactButton";
					that.button_area.AddToExistingGroup.disabled = false;
					that.button_area.AddToNewGroup.className = "CompactButtonInactive";
					that.button_area.AddToNewGroup.disabled = true;
				} else {	
					that.button_area.AddToExistingGroup.className = "CompactButtonInactive";	
					that.button_area.AddToExistingGroup.disabled = true;
					that.button_area.AddToNewGroup.className = "CompactButton";
					that.button_area.AddToNewGroup.disabled = false;
				}
			}
		}
	}
	
	that.processAction = function(myForm) {
		var formArray = Form.serialize(myForm, true);
		var eidArray = null;
			
		if (formArray.resources) {
			// from Browse Resources page
			if (typeof formArray.resources == "string") {
				// one resource selected
				document.AddToExistingGroupForm.eid.value = formArray.resources;
			} else {
				// multiple resources selected
				document.AddToExistingGroupForm.eid.value = formArray.resources.join();
			}
			eidArray = document.AddToExistingGroupForm.eid.value.split(",");
		} else {
			// from Resource Tools menu
			eidArray = formArray.eid.split(",");			
		}

		var entityType = parseInt(eidArray[0].split(":")[0]);
		if (entityType == 5) {
			// adding existing groups to existing groups not supported,
			// so send directly to the Add New Group page
			that.addNewGroup(eidArray.join());
		} else {
			that.prepareAddResourcesToGroups(eidArray);
		}
		return false;
	}
	
	that.addNewGroup = function(eid) {
		var eidArray = eid.split(",");
		var entityType = eidArray[0].split(":")[0];
		var myForm = document.AddToExistingGroupForm;
		
		myForm.removeChild(myForm.eid);
				
		var a = new Array(eidArray.length+2);
		a[0] = {name: "group.x" , value: "1"};
		a[1] = {name: "ff", value: entityType};

		for (var i=0; i<eidArray.length; i++) {
			a[i+2] = {name: "resources", value: eidArray[i]};
		}
		
		for (var j=0; j<a.length; j++) {
			var e = document.createElement("input");
			e.type = "hidden";
			e.name = a[j].name;
			e.value = a[j].value;
			myForm.appendChild(e);			
		}
		myForm.submit();
	}
	
	that.addToGroup = function(eid) {
		var eidArray = eid.split(",");
		
		if (eidArray.length > 1) {
			that.prepareAddResourcesToGroups(eidArray);
		} else {
			that.addResourceToGroup(eid);
		}
	}
	
	that.addResourceToGroup = function(eid) {
		var entityType = parseInt(eid.split(":")[0]);
		var entityId = parseInt(eid.split(":")[1]);
		var url = "/resource/{0}/Inventory.do";
		url += "?mode=addGroups";
		url += "&rid=" + entityId;
		url += "&type=" + entityType;
		url += "&preventCache=" + new Date().getTime();
		
		switch(entityType) {
			case 1:
				url = url.replace("{0}", "platform");
				break;
			case 2:
				url = url.replace("{0}", "server");
				break;
			case 3:
				url = url.replace("{0}", "service");
				break;
			case 4:
				url = url.replace("{0}", "application");
				break;
			case 5:
				url = url.replace("{0}", "group").replace("addGroups", "addResources");
				break;
			default:
				alert("Unable to process your request.");
				that.dialogs.AddToGroupMenu.hide();
				return;
		}
				
		window.location.href = url;
	}

	that.prepareAddResourcesToGroups = function(eidArray) {
		that.message_area.AddToExistingGroup.style.display = "none";
		that.button_area.AddToExistingGroup.className = "CompactButtonInactive";
		that.button_area.AddToExistingGroup.disabled = true;
		that.button_area.AddToNewGroup.className = "CompactButton";
		that.button_area.AddToNewGroup.disabled = false;
		
		var tbody = hqDojo.byId("AddToExistingGroupTableBody");
        for (var i = tbody.childNodes.length-1; i >= 0; i--) {
            tbody.removeChild(tbody.childNodes[i]);
        }
        
		hqDojo.byId("AddToExistingGroupTableFooter").style.display = "";
		that.dialogs.AddToExistingGroup.show();
		that.getGroupsNotContaining(eidArray);	
	}
	
	that.addResourcesToGroups = function(myForm) {
		var formArray = Form.serialize(myForm, true);

		that.button_area.AddToExistingGroup.className = "CompactButtonInactive";	
		that.button_area.AddToExistingGroup.disabled = true;
		that.displayConfirmation(that.message_area.AddToExistingGroup,
								'Please wait. Processing your request...');

		hqDojo.xhrPost( {
            url: args.postUrl,
            content: {
				eid: formArray.eid.split(","),
				groupId: formArray.group.toString().split(",")
			},
            handleAs: 'json',
            load: function(data) {
				if (data && data.error) {
					console.debug(data.error_message, data);
					that.displayError(that.message_area.AddToExistingGroup, data.error_message);
				} else {
				    var successText = "The requested groups have been assigned.";
				    that.displayConfirmation(that.message_area.AddToExistingGroup, successText);
		        	setTimeout('MyGroupManager.dialogs.AddToExistingGroup.hide()', 2000);
				}
			},
            error: function(data) {
	    		var errorText = "An error occurred processing your request.";
            	console.debug(errorText, data);
            	that.displayError(that.message_area.AddToExistingGroup, errorText);
            }
        });
	}
	
	that.getGroupsNotContaining = function(eids) {    
		hqDojo.xhrPost( {
            url: args.associationsUrl,
            content: {
				eid: eids,
				"_method": "PUT"
			},
			handleAs: 'json',
            preventCache: true,
            load: function(data) {            	
            	var tbody = hqDojo.byId("AddToExistingGroupTableBody");
            	var tfoot = hqDojo.byId("AddToExistingGroupTableFooter");

            	tfoot.style.display = "none";
            	            	
            	for (var i=0; i<data.groups.length; i++) {
            		var tr = document.createElement("tr");
            		var td1 = document.createElement("td");
            		var td2 = document.createElement("td");
            		var td3 = document.createElement("td");
            		var checkBox = document.createElement("input");
            		
            		tr.className = (i%2 == 0) ? "tableRowOdd" : "tableRowEven";
            		td1.className = "ListCellCheckbox";
            		checkBox.type = "checkbox";
            		checkBox.className = "selectableGroup";
            		checkBox.name = "group";
            		checkBox.id = "group_" + data.groups[i].id;
            		checkBox.value = data.groups[i].id;
            		checkBox.onclick = new Function("MyGroupManager.dialogs.AddToExistingGroup.toggleButtons(this);");
            		
            		td2.className = "tableCell";
            		td2.innerHTML = data.groups[i].name.escapeHTML() + "&nbsp;";
            		td3.className = "tableCell";
            		
            		if (data.groups[i].description) {
            			td3.innerHTML = data.groups[i].description.escapeHTML();
            		}
            		td3.innerHTML += "&nbsp;";
            		
            		td1.appendChild(checkBox);
            		tr.appendChild(td1);
            		tr.appendChild(td2);
            		tr.appendChild(td3);
            		tbody.appendChild(tr);
            	}
            },
            error: function(data) {
	    		var errorText = "An error occurred processing your request.";
            	console.debug(errorText, data);
            	that.displayError(that.message_area.AddToExistingGroup, errorText);
            }
        });
		
	}

	that.displayConfirmation = function(msg_area, msg) {
		msg_area.className = 'confirmationPanel';
		msg_area.innerHTML = msg;
		msg_area.style.display = '';   	
    }
    
    that.displayError = function(msg_area, msg) {
    	msg_area.className = 'errorPanel';
    	msg_area.innerHTML = msg;
    	msg_area.style.display = '';
    }
    
	that.init();
}

hyperic.indicator_charts_manager = function(props, charts) {
	var that = this;
	that.name = props.name;
	that.view = props.view;
	that.eid = props.eid;
	if (props.ctype) {
		that.ctype = props.ctype;
	}
	that.displaySize = props.displaySize;
	that.baseUrl = props.baseUrl;
	that.labels = props.labels;
	that.charts = charts;
	that.chartsStartTime = null;
	that.chartsEndTime = null;

	that.getChartLink = function(chartArgs) {		
		var url = "/resource/common/monitor/Visibility.do"
				+ "?m=" + chartArgs.metricId
				+ "&eid=" + chartArgs.entityId;

		if (chartArgs.ctype) {
			url += "&ctype=" + chartArgs.ctype
				+ "&mode=chartSingleMetricMultiResource";
		} else if (chartArgs.entityType == 5) {
			url += "&mode=chartSingleMetricMultiResource";
		} else {
			url += "&mode=chartSingleMetricSingleResource";
		}
		  
		return url;
	}
	
	that.getChartImg = function(chartArgs) {
		var url = "/resource/HighLowChart?imageWidth=647&imageHeight=100"
				+ "&tid=" + chartArgs.metricId
				+ "&eid=" + chartArgs.entityId
				+ "&unitUnits=" + chartArgs.unitUnits
				+ "&unitScale=" + chartArgs.unitScale
				+ "&index=" + chartArgs.index
				+ "&now=" + chartArgs.timeToken;

		if (chartArgs.ctype) {
			url += "&ctype=" + chartArgs.ctype;
		}
		  
		return url;
	}
	
	that.drawChart = function(chartArgs) {
		var rootObj = hqDojo.byId("root");
		var liNode = document.createElement("li");
		liNode.id = chartArgs.metricId;

		var liInnerHtml = 
			'<table width="650" border="0" cellpadding="2" bgcolor="#DBE3F5">' +
			'<tr>' +
			'  <td>' +
			'    <table width="100%" border="0" cellpadding="0" cellspacing="1" style="margin-top: 1px; margin-bottom: 1px;">' +
		    '    <tr>' +
		    '      <td class="BoldText">' +
		    '        <img src="/images/icon_chart.gif"/>' +
		    '        <a href="' + that.getChartLink(chartArgs) + '" target="_top">' + chartArgs.metricLabel + '</a>' +
		    '      </td>' +
		    '      <td colspan="3" align="right">' +
		    '        <a href="javascript:' + that.name + '.moveMetricUp(\'' + chartArgs.metricId + '\')"><img src="/images/dash_icon_up.gif" border="0"/></a>' +
		    '        <a href="javascript:' + that.name + '.moveMetricDown(\'' + chartArgs.metricId + '\')"><img src="/images/dash_icon_down.gif" border="0"/></a>' +
		    '        <a href="javascript:' + that.name + '.removeMetric(\'' + chartArgs.metricId + '\')"><img src="/images/dash-icon_delete.gif" border="0"/></a>' +
		    '      </td></tr>' +
		    '    <tr>' +
		    '      <td style="font-size: 10px; padding-left: 12px;">(' + chartArgs.metricSource + ')</td>' +
		    '      <td nowrap="nowrap" width="13%"><span class="BoldText">' + that.labels.low + '</span>: ' + chartArgs.minMetric + '</td>' +
		    '      <td nowrap="nowrap" width="14%"><span class="BoldText">' + that.labels.avg + '</span>: ' + chartArgs.avgMetric + '</td>' +
		    '      <td nowrap="nowrap" width="15%"><span class="BoldText">' + that.labels.peak + '</span>: ' + chartArgs.maxMetric + '</td>' +
		    '    </tr>' +
		    '    </table>' +
		    '  </td>' +
		    '</tr>' +
		    '</table>';
			    
		if (chartArgs.error) {
			liInnerHtml += '<table><tr><td class="ErrorBlock" colspan="2">' + chartArgs.error + '</td></tr></table>';
			liNode.innerHTML = liInnerHtml;
		} else {
			// create IMG
			var chartNode = document.createElement("img");
			chartNode.border = "0";
			chartNode.width = "647";
			chartNode.height = "100";
			chartNode.src = that.getChartImg(chartArgs);

			// add to DOM
			liNode.innerHTML = liInnerHtml;
			liNode.appendChild(chartNode);
			rootObj.appendChild(liNode);
		}
	}
	
	that.loadChart = function(chartIndex) {
		if (chartIndex <= that.charts.length-1) {
			if (chartIndex == 0) {
				that.chartsStartTime = new Date();
		  	}

			that.drawChart(that.charts[chartIndex]);

			// load each chart individually in a separate javascript thread
			// to improve rendering performance
			if (chartIndex < that.charts.length-1) {
				setTimeout(that.name + ".loadChart(" + (chartIndex+1) + ")", 10);
			} else if (chartIndex == that.charts.length-1) {
				setTimeout(that.name + ".fetchChart(" + that.displaySize + ")", 10);
			}
		}
	}

	that.logLoadTime = function() {
		console.log("chartsLoadTime(ms)=" + (that.chartsEndTime.getTime()-that.chartsStartTime.getTime()));
	}

	that.fetchChart = function(displaySize) {
	    var url = that.baseUrl + '?action=fresh&output=json'
	    		+ '&eid=' + that.eid 
	    		+ '&view=' + that.view;
		
	    if (that.ctype) {
	      url += '&ctype=' + that.ctype;
	    }
	    
		if (displaySize > 0) { 
			url += '&displaySize=' + (displaySize+1);

		    hqDojo.xhrGet({
		        url: url,
		        handleAs: "json",
		        preventCache: true,
		        load: function(data) {
		          that.drawChart(data);
		          that.fetchChart(data.displaySize);
		        },
		        error: function(data){
		          console.debug("Could not fetch chart:");
		          console.debug(data);
		        }
		      });	    	
		} else {
			hqDojo.byId("slowScreenSplash").style.display = "none";
			that.chartsEndTime = new Date();
			that.logLoadTime();
		}
	}
  
	that.removeMetric = function(metric) {
    	var url = that.baseUrl + '?action=remove'
    			+ '&metric=' + metric 
    			+ '&eid=' + that.eid 
    			+ '&view=' + that.view;
    			
	    if (that.ctype) {
			url += '&ctype=' + that.ctype;
		}
    	
        hqDojo.xhrGet({
            url: url,
            handleAs: "text",
            preventCache: true,
            timeout: 5000,
            load: function(data, ioArgs) {
              console.log(data);
              console.log('removed metric');
              new Effect.Fade(metric);
            },
            error: function(data){
              console.debug("could not remove metric:");
              console.debug(data);
              new Effect.Pulsate(metric);
            }
          });
	}

	that.moveMetricUp = function(metric) {
    	var url = that.baseUrl + '?action=moveUp'
    			+ '&metric=' + metric 
    			+ '&eid=' + that.eid 
    			+ '&view=' + that.view;

	    if (that.ctype) {
			url += '&ctype=' + that.ctype;
		}
    	
        hqDojo.xhrGet({
            url: url,
            handleAs: "text",
            preventCache: true,
            timeout: 5000,
            load: function(data, ioArgs) {
              console.log(data);
              console.log('moved metric up');
              var root = hqDojo.byId('root');
              var elem = hqDojo.byId(metric);
              moveElementUp(elem, root);
            },
            error: function(data){
              console.debug("could not move metric up:");
              console.debug(data);
              new Effect.Pulsate(metric);
            }
          });
	}

	that.moveMetricDown = function(metric) {
    	var url = that.baseUrl + '?action=moveDown'
    			+ '&metric=' + metric 
    			+ '&eid=' + that.eid 
    			+ '&view=' + that.view;

	    if (that.ctype) {
			url += '&ctype=' + that.ctype;
		}
    	
        hqDojo.xhrGet({
            url: url,
            handleAs: "text",
            preventCache: true,
            timeout: 5000,
            load: function(data, ioArgs) {
              console.log(data);
              console.log('moved metric down');
              var root = hqDojo.byId('root');
              var elem = hqDojo.byId(metric);
              moveElementDown(elem, root);
            },
            error: function(data){
              console.debug("could not move metric down:");
              console.debug(data);
              new Effect.Pulsate(metric);
            }
          });
	}

}

hyperic.alert_center = function(title_name) {
	var that = this;
	that.title_name = title_name;
	that.current = {dialog: null};
	that.dialogs = {};
	
	that.initDialog = function() {
    	var alertCenterDiv = hqDojo.byId("HQAlertCenterDialog");

    	if (!that.dialogs.AckAlert) {
	    	var ackDiv = document.createElement("div");
	    	ackDiv.innerHTML =
	          	'<div style="width:400px">' +
	          	'<div id="AlertCenterAckStatus" style="display:none"></div>' +
				'<table cellspacing="0" cellpadding="0">' +
				'<tr><td colspan="2">' +
      	        '	<span class="BoldText">Reason for Acknowledgement for Selected Alerts:</span><br/>' +
      	        '	<textarea id="AckNoteTextArea" style="width:400px; height:100px"></textarea>' +
      	        '</td></tr>' +
      	        '<tr id="AlertCenterAckButtonActive">' +
      	        '	<td><span id="AlertCenterEscalationOption"><input type="checkbox" id="AlertCenterPauseCheckbox" value="true" />'+
      	        '		Pause escalation for&nbsp;<select id="AlertCenterPauseSelect"></select></span></td>' +
      	        '	<td align="right">' +
      	    	'		<table border="0" cellspacing="0" cellpadding="0"><tr>'+
      	        '			<td class="buttonLeft"></td>' +
      	    	'			<td class="buttonRight" valign="middle" nowrap="nowrap" style="padding-top: 6px; padding-bottom: 6px;">' +
      	    	'				<span id="button"><a href="javascript:MyAlertCenter.acknowledgeAlerts();">ACKNOWLEDGE</a></span>' +
      	    	'			</td></tr></table>' +
      	    	'	</td></tr>' +
      	        '<tr id="AlertCenterAckButtonInActive" style="display:none">' +
      	        '	<td align="center">' +
      	    	'		<table border="0" cellspacing="0" cellpadding="0"><tr>'+
      	        '			<td class="buttonLeft"></td>' +
      	    	'			<td class="buttonRight" valign="middle" nowrap="nowrap" style="padding-top: 6px; padding-bottom: 6px;">' +
      	    	'   			<span class="InactiveText">ACKNOWLEDGE</span>' +
      	    	'			</td></tr></table>' +
      	    	'	</td></tr>' +
      	    	'</table>' +
      	    	'</div>';
	    	alertCenterDiv.appendChild(ackDiv);
	    	
	    	that.dialogs.AckAlert = new hqDijit.Dialog({
				id: "Alert_Center_Ack_Alert_Dialog",
				refocus: true,
				autofocus: false,
				title: that.title_name
				}, ackDiv);

	    	that.dialogs.AckAlert.data = {
		    	form: null,
		    	subgroup: null,
	    		button_area: {
	    			active: hqDojo.byId("AlertCenterAckButtonActive"),
	    			inactive: hqDojo.byId("AlertCenterAckButtonInActive")
	    		},
	    		message_area: {
	    			request_status: hqDojo.byId("AlertCenterAckStatus")
	    		},
		    	note: hqDojo.byId("AckNoteTextArea"),
		    	pause: hqDojo.byId("AlertCenterPauseCheckbox"),
		    	pauseTime: hqDojo.byId("AlertCenterPauseSelect")
		    }
	    	
	    	// restart auto refresh after dialog closes
	    	hqDojo.connect(that.dialogs.AckAlert, "hide", this, "delayAutoRefresh");
    	}
	    
		if (!that.dialogs.FixAlert) {
			var fixedDiv = document.createElement("div");
			fixedDiv.innerHTML = 
	          	'<div style="width:400px">' +
				'<div id="AlertCenterFixedStatus" style="display:none"></div>' +
				'<div id="AlertCenterProgressStatus" style="display:none; margin-top:-5px; margin-bottom:10px">' +
				'	<div id="AlertCenterProgressBar"></div>' +
				'</div>' +
				'<table cellspacing="0" cellpadding="0">' +
				'<tr><td colspan="2">' +
      	        '	<span class="BoldText">Resolution for Fix for Selected Alerts:</span><br/>' +
      	        '	<textarea id="FixedNoteTextArea" style="width:400px; height:100px"></textarea>' +
      	        '</td></tr>' +
      	        '<tr id="AlertCenterFixedButtonActive">' +
      	        '	<td><input type="checkbox" id="FixAllCheckbox" value="true" />&nbsp;Fix all previous alerts</td>' +
      	    	'	<td align="right">' +
      	    	'		<table border="0" cellspacing="0" cellpadding="0"><tr>'+
      	    	'			<td class="buttonLeft"></td>' +
      	    	'			<td class="buttonRight" valign="middle" nowrap="nowrap" style="padding-top: 6px; padding-bottom: 6px;">' +
      	    	'				<span id="button"><a href="javascript:MyAlertCenter.fixAlerts();">FIXED</a></span>' +
      	    	'			</td></tr></table>' +
      	    	'	</td></tr>' +
      	        '<tr id="AlertCenterFixedButtonInActive" style="display:none">' +
      	        '	<td></td>' +
      	        '	<td align="center">' +
      	    	'		<table border="0" cellspacing="0" cellpadding="0"><tr>'+
      	        '			<td class="buttonLeft"></td>' +
      	    	'			<td class="buttonRight" valign="middle" nowrap="nowrap" style="padding-top: 6px; padding-bottom: 6px;">' +
      	    	'				<span class="InactiveText">FIXED</span>' +
      	    	'			</td></tr></table>' +
      	    	'	</td></tr>' +
      	    	'</table>' +
      	    	'</div>';
			alertCenterDiv.appendChild(fixedDiv);
	    	
	    	that.dialogs.FixAlert = new hqDijit.Dialog({
				id: "Alert_Center_Fix_Alert_Dialog",
				refocus: true,
				autofocus: false,
				title: that.title_name
				}, fixedDiv);
	    	
	    	that.dialogs.FixAlert.data = {
	    		form: null,
	    		subgroup: null,
	    		button_area: {
	    			active: hqDojo.byId("AlertCenterFixedButtonActive"),
	    			inactive: hqDojo.byId("AlertCenterFixedButtonInActive")
	    		},
	    		message_area: {
	    			request_status: hqDojo.byId("AlertCenterFixedStatus"),
	    			progress_status: hqDojo.byId("AlertCenterProgressStatus")
	    		},
	    		note: hqDojo.byId("FixedNoteTextArea"),
	    		fixAll: hqDojo.byId("FixAllCheckbox")
	    	}
	    	
	    	// restart auto refresh after dialog closes
	    	hqDojo.connect(that.dialogs.FixAlert, "hide", this, "delayAutoRefresh");
	    	
	    	// create progress bar				
			that.dialogs.FixAlert.data.progressBar = new hqDijit.ProgressBar({}, hqDojo.byId("AlertCenterProgressBar"));
		}
	}

	that.initData = function(myDialog, myForm) {
		that.current.dialog = myDialog;
		myDialog.data.form = myForm;
		myDialog.data.subgroup = myDialog.data.form.id.substring(0, myDialog.data.form.id.indexOf("_FixForm"));
	}
	
	that.startAutoRefresh = function(mySubgroup) {
		var adhocScript = "if (window._hqu_" + mySubgroup + "_autoRefresh) { ";
		adhocScript += "window._hqu_" + mySubgroup + "_autoRefresh(); ";
		adhocScript += " }";
		
		that.stopAutoRefresh(mySubgroup);
		eval(adhocScript);		
	}

	that.stopAutoRefresh = function(mySubgroup) {		
		var subgroup = null;
		if (typeof mySubgroup == "string") {
			subgroup = mySubgroup;
		} else {
			subgroup = that.current.dialog.data.subgroup;
		}
		
		if (subgroup != null) {
			var adhocScript = "if (window._hqu_" + subgroup + "_refreshTimeout) { ";
			adhocScript += "clearTimeout(window._hqu_" + subgroup + "_refreshTimeout); ";
			adhocScript += " }";
			eval(adhocScript);
		}
	}

	that.delayAutoRefresh = function(mySubgroup) {		
		var subgroup = null;
		if (typeof mySubgroup == "string") {
			subgroup = mySubgroup;
		} else {
			subgroup = that.current.dialog.data.subgroup;
		}
		
		if (subgroup != null) {
			var refreshDelay = 60000;
			var adhocScript = "if (window._hqu_" + subgroup + "_refreshTimeout) { ";
			adhocScript += "window._hqu_" + subgroup + "_refreshTimeout = setTimeout('window._hqu_" + subgroup + "_autoRefresh()', refreshDelay);";
			adhocScript += " }";
			
			that.stopAutoRefresh(subgroup);
			eval(adhocScript);
		}
	}

	that.fixOrAckAlert = function(myDialog, myNote, fixAll, pauseTime) {
		var myForm = myDialog.data.form;
		myNote.value = myDialog.data.note.value;
		if (fixAll != null && myDialog.data.fixAll.checked) {
			fixAll.value = myDialog.data.fixAll.value;
		}
		if (pauseTime != null && myDialog.data.pause.checked) {
			pauseTime.value = myDialog.data.pauseTime.value;
		}
		myDialog.data.button_area.active.style.display = "none";
		myDialog.data.button_area.inactive.style.display = "";
		that.displayConfirmation(
				myDialog.data.message_area.request_status, 
				'Please wait. Processing your request...');

		if (myForm.output && myForm.output.value == "json") {
			if (fixAll != null && myDialog.data.fixAll.checked) {
				that.xhrBatchSubmit(myDialog);
			} else {
				that.xhrSubmit(myForm);
			}
		} else {
			myForm.submit();
		}	
	}
	
	that.showDialog = function(myDialog) {
		that.current.dialog = myDialog;
		that.stopAutoRefresh();
		myDialog.data.message_area.request_status.style.display = "none";
		if (myDialog.data.message_area.progress_status != null) {
			myDialog.data.message_area.progress_status.style.display = "none";
		}
		myDialog.data.button_area.active.style.display = "";
		myDialog.data.button_area.inactive.style.display = "none";
		myDialog.data.note.value = "";
		if (myDialog.data.fixAll != null) {
			myDialog.data.fixAll.checked = false;
		}
		if (myDialog.data.pause != null) {
			var escalationSpan = hqDojo.byId("AlertCenterEscalationOption");		
			myDialog.data.pause.checked = true;
			myDialog.data.pause.onclick = function() { 
				hqDojo.byId("AlertCenterPauseSelect").disabled = !this.checked; 
			};

			if (myDialog.data.pauseTime.options.length == 0) {
				escalationSpan.style.display = "none";
			} else {
				escalationSpan.style.display = "";
			}
		}
		myDialog.show();	
	}
	
	that.fixAlerts = function() {
		that.fixOrAckAlert(that.dialogs.FixAlert, 
						   that.dialogs.FixAlert.data.form.fixedNote,
						   that.dialogs.FixAlert.data.form.fixAll);
	}
	
	that.acknowledgeAlerts = function() {
		that.fixOrAckAlert(that.dialogs.AckAlert, 
						   that.dialogs.AckAlert.data.form.ackNote,
						   null,
						   that.dialogs.AckAlert.data.form.pauseTime);
	}

	that.acknowledgeAlert = function(inputId) {		
		var myInput = hqDojo.byId(inputId);
		var myParam = {buttonAction: "ACKNOWLEDGE", output: "json"}
		
		myParam[myInput.name] = myInput.value;
		that.initData(that.dialogs.AckAlert, myInput.form);
		that.stopAutoRefresh();

		hqDojo.xhrPost( {
	    	url: myInput.form.action,
	    	content: myParam,
	    	handleAs: 'json',
	    	load: function(data) {
				if (data.actionToken) {
					// use new CSRF token for subsequent POST requests
					myInput.form.action = data.actionToken;
				}
	    		that.startAutoRefresh(that.dialogs.AckAlert.data.subgroup);
	    	},
	    	error: function(data){
	    		var errorText = "An error occurred processing your request.";
	    		alert(errorText);
	    		console.info(errorText, data);
			}
		});
	}
	
	that.xhrSubmit = function(myForm) {				
		hqDojo.xhrPost( {
	    	url: myForm.action,
	    	content: Form.serialize(myForm,true),
	    	handleAs: 'json',
	    	load: function(data){
				if (data.actionToken) {
					// use new CSRF token for subsequent POST requests
					myForm.action = data.actionToken;
				}
				that.current.dialog.hide();
	    		that.startAutoRefresh(that.current.dialog.data.subgroup);
	    	},
	    	error: function(data){
	    		var errorText = "An error occurred processing your request.";
	    		that.displayError(that.current.dialog.data.message_area.request_status, errorText);
	    		console.info(errorText, data);
			}
		});	    
	}
	
	that.xhrBatchSubmit = function(myDialog, alertIndex) {
		var myForm = myDialog.data.form;
		var formObj = Form.serialize(myForm,true);
		var batchFormObj = Form.serialize(myForm,true);
		var ealerts = formObj["ealerts"];
		
		if (typeof ealerts == "undefined") {
			that.xhrSubmit(myForm);
			return;
		}
		
		if (typeof ealerts == "string") {
			// convert to array if single alert selected
			ealerts = [ealerts];
		}

		if (alertIndex == null) {
			alertIndex = 0;
		}
				
		// show and update progress bar
		var currentProgress = alertIndex*2;
		myDialog.data.progressBar.update({maximum: ealerts.length*2, progress: ++currentProgress});
		myDialog.data.message_area.progress_status.style.display = "";
			
		// submit each selection separately			
		batchFormObj["ealerts"] = ealerts[alertIndex];
			
		hqDojo.xhrPost( {
			url: myForm.action,
			content: batchFormObj,
			sync: false,
			handleAs: 'json',
			load: function(data){
				// update progress bar
				myDialog.data.progressBar.update({progress: ++currentProgress});
					
				// submit next selection
				if (++alertIndex < ealerts.length) {
					if (data.actionToken) {
						// use new CSRF token for subsequent POST requests
						myForm.action = data.actionToken;
					}
					that.xhrBatchSubmit(myDialog, alertIndex);
				} else {
					that.current.dialog.hide();
					that.startAutoRefresh(that.current.dialog.data.subgroup);						
				}
			},
	    	error: function(data){
				var errorText = "An error occurred processing your request.";
				that.displayError(that.current.dialog.data.message_area.request_status, errorText);
				console.info(errorText, data);
	    	}
		});
	}

	that.resetAlertTable = function(myForm) {
		var subgroup = myForm.id.substring(0, myForm.id.indexOf("_FixForm"));
		var checkAllBox = hqDojo.byId(subgroup + "_CheckAllBox");
		checkAllBox.checked = false;
		that.toggleAll(checkAllBox, false);
		myForm.fixedNote.value = "";
		myForm.ackNote.value = "";
		myForm.fixAll.value = "false";
		myForm.pauseTime.value = "";
	}
	
	that.toggleAll = function(checkAllBox, doDelay) {
		var checkedState = checkAllBox.checked;
		var uList = checkAllBox.form;
	    var len = uList.elements.length;

		for (var i = 0; i < len; i++) {
	        var e = uList.elements[i];
	       
	        if (e.className.indexOf("fixableAlert") >= 0 
	        		|| e.className.indexOf("ackableAlert") >= 0) {
	        	e.checked = checkedState;
	        	
	        	if (e.checked) {
	        		highlight(e);
	        	} else {
	        		unhighlight(e);
	        	}
			}
		}
		that.toggleAlertButtons(checkAllBox, doDelay);
	}

	that.toggleAlertButtons = function(myCheckBox, doDelay) {
		if (doDelay == null) {
			doDelay = true;
		}
		var myList = myCheckBox.form;
		var subgroup = myList.id.substring(0, myList.id.indexOf("_FixForm"));
		var checkAllBox = hqDojo.byId(subgroup + "_CheckAllBox");
		var fixedButton = hqDojo.byId(subgroup + "_FixButton");
		var ackButton = hqDojo.byId(subgroup + "_AckButton");

		// delay refresh for X milliseconds if checkbox is clicked
		if (doDelay) {
			that.delayAutoRefresh(subgroup);
		}

		if (myCheckBox.id != checkAllBox.id) {
			checkAllBox.checked = false;
			
			if (myCheckBox.checked) {
				highlight(myCheckBox);
			} else {
				unhighlight(myCheckBox);
			}
		}
		
		if (getNumCheckedByClass(myList, "fixableAlert") > 0) {
			fixedButton.className = "CompactButton";
			fixedButton.disabled = false;	
			ackButton.className = "CompactButtonInactive";
			ackButton.disabled = true;
		} else if (getNumCheckedByClass(myList, "ackableAlert") > 0) {
			fixedButton.className = "CompactButton";
			fixedButton.disabled = false;	
			ackButton.className = "CompactButton";	
			ackButton.disabled = false;
		} else {
			fixedButton.className = "CompactButtonInactive";		
			fixedButton.disabled = true;	
			ackButton.className = "CompactButtonInactive";	
			ackButton.disabled = true;
		}
	}

	that.processButtonAction = function(myButton) {
		myButton.form.buttonAction.value = myButton.value;
		
		if (myButton.value == "FIXED") {
			that.initData(that.dialogs.FixAlert, myButton.form);
			that.showDialog(that.dialogs.FixAlert);
		} else if (myButton.value == "ACKNOWLEDGE") {
			that.initData(that.dialogs.AckAlert, myButton.form);
			that.redrawEscalationOptions(that.dialogs.AckAlert);
			that.showDialog(that.dialogs.AckAlert);
		}
	}
	
	that.redrawEscalationOptions = function(myDialog) {
	    var commonMaxPauseTime = Number.MAX_VALUE;
	    var myForm = myDialog.data.form;

		for (var i = 0; i < myForm.elements.length; i++) {
	        var e = myForm.elements[i];
	       
	        if (e.className.indexOf("ackableAlert") >= 0 && e.checked) {
	        	// checkbox id is in the format: {portalName}|{appdefKey}|{alertId}|{maxPauseTime}
	        	var alertParams = e.id.split("|");
	        	var maxPauseTime = parseFloat(alertParams[alertParams.length-1]);
	        	if (maxPauseTime < commonMaxPauseTime) {
	        		commonMaxPauseTime = maxPauseTime;
	        	}
			}
		}

		myDialog.data.pauseTime.options.length = 0;
		if (commonMaxPauseTime > 0 && commonMaxPauseTime < Number.MAX_VALUE) {			
			var tempSelect = hyperic.form.createEscalationPauseOptions({name: "tempName"}, commonMaxPauseTime);
			for (var i = 0; i < tempSelect.options.length; i++) {
				myDialog.data.pauseTime.options[i] = new Option(tempSelect.options[i].text, tempSelect.options[i].value);
				myDialog.data.pauseTime.options[i].selected = tempSelect.options[i].selected;
			}
		}
	}

	that.displayConfirmation = function(msg_area, msg) {
		msg_area.className = 'confirmationPanel';
		msg_area.innerHTML = msg;
		msg_area.style.display = '';   	
    }
    
    that.displayError = function(msg_area, msg) {
    	msg_area.className = 'errorPanel';
    	msg_area.innerHTML = msg;
    	msg_area.style.display = '';
    }
	
	that.initDialog();
}

hyperic.maintenance_schedule = function(args) {
    var that = this;
    var baseUrl = args.url;
    
    that.existing_schedule = {};
    that.appdefentity_id = args.appdefentityId;
    that.entity_id = that.appdefentity_id.split(":")[1];
    that.resource_name = unescape(args.resourceName);
    that.title_name = args.title + " - " + that.resource_name;
    that.dialog = null;
	that.buttons = {};
	that.inputs = {};
	that.canSchedule = true;
    that.selected_from_time = that.selected_to_time = new Date();
    that.server_time = new Date(); // default
        
    that.message_area = {
    	request_status : hqDojo.byId('maintenance_status_' + that.entity_id),
    	schedule_status : hqDojo.byId('existing_downtime_' + that.entity_id)
    };

    that.init = function() {
	    if(!that.dialog){
			if(that.title_name.length > 42) {
				that.title_name = that.title_name.substring(0,42) + "...";
			}
	    	var pane = hqDojo.byId('maintenance' + that.entity_id);
			pane.style.width = "450px";
			that.dialog = new hqDijit.Dialog({
				id: "maintenance_schedule_dialog_" + that.entity_id,
				refocus: true,
				autofocus: false,
				title: that.title_name
			},pane);
		}
        
        that.inputs.from_date = new hqDijit.form.DateTextBox({
    			name: "from_date",
    			value: that.selected_from_time,
    			constraints: {
                    datePattern: 'MM/dd/y'},
    			promptMessage: "A valid date in format mm/dd/yyyy is required.",
    			rangeMessage: hyperic.data.maintenance_schedule.error.startDateRange,
    			invalidMessage: hyperic.data.maintenance_schedule.error.datePattern,
    			required: true
    		}, "from_date");

        that.inputs.to_date = new hqDijit.form.DateTextBox({
    			name: "to_date",
    			value: that.selected_to_time,
    			constraints: {
                    datePattern: 'MM/dd/y'},
    			promptMessage: "A valid date in format mm/dd/yyyy is required.",
    			rangeMessage: hyperic.data.maintenance_schedule.error.endDateRange,
    			invalidMessage: hyperic.data.maintenance_schedule.error.datePattern,
    			required: true
    		}, "to_date");

        that.inputs.from_time = new hqDijit.form.TimeTextBox({
    			name: "from_time",
    			value: that.selected_from_time,
                rangeMessage: hyperic.data.maintenance_schedule.error.startTimeRange,
    			invalidMessage: hyperic.data.maintenance_schedule.error.timePattern,
    			required: true
    		}, "from_time");

        that.inputs.to_time = new hqDijit.form.TimeTextBox({
    			name: "to_time",
    			value: that.selected_to_time,
                rangeMessage: hyperic.data.maintenance_schedule.error.endTimeRange,
    			invalidMessage: hyperic.data.maintenance_schedule.error.timePattern,
    			required: true
    		}, "to_time");

		that.buttons.schedule_btn = new hqDijit.form.Button({
			label: hyperic.data.maintenance_schedule.label.schedule,
			name: "schedule_btn",
			id: "schedule_btn",
			type: 'button'
		}, "schedule_btn");
        hqDojo.connect(that.buttons.schedule_btn, 'onClick', that.schedule_action);
        
		that.buttons.clear_schedule_btn = new hqDijit.form.Button({
			label: hyperic.data.maintenance_schedule.label.clear,
			name: "clear_schedule_btn",
			id: "clear_schedule_btn",
			type: 'button'
		}, "clear_schedule_btn");
        hqDojo.connect(that.buttons.clear_schedule_btn, 'onClick', that.clear_schedule_action);
        
		hqDojo.byId("maintenance_cancel_link_" + that.entity_id).onclick = function(e) {
			hqDijit.byId("maintenance_schedule_dialog_" + that.entity_id).onCancel();
		};
    };

    that.schedule_action = function() {
		that.updateConstraints();
	
    	// validate with updated constraints
    	if(that.dialog.validate())
        {
    	    // create unix epoch datetime in GMT timezone
            var from_datetime = (that.inputs.from_date.getValue().getTime() + that.inputs.from_time.getValue().getTime() - that.inputs.from_time.getValue().getTimezoneOffset() * 60000);
            var to_datetime = (that.inputs.to_date.getValue().getTime() + that.inputs.to_time.getValue().getTime() - that.inputs.to_time.getValue().getTimezoneOffset() * 60000);

            return hqDojo.xhrPost( {
                url: baseUrl + "/schedule",
                content: {
            		startTime: from_datetime, 
            		endTime: to_datetime
            	},
                handleAs: 'json',
                load: function(data){
                    if(data && !data.error) 
                    {
                		that.server_time = new Date(data.serverTime);

                    	if((parseInt(data.startTime,10) != 0 && parseInt(data.endTime,10) != 0))
                    	{
	                        that.existing_schedule.from_time = parseInt(data.startTime,10);
	                        that.existing_schedule.to_time = parseInt(data.endTime,10);
	                        that.selected_from_time = new Date(that.existing_schedule.from_time);
	                        that.selected_to_time = new Date(that.existing_schedule.to_time);
	                        that.redraw(false, 
	                        		hyperic.data.maintenance_schedule.message.currentSchedule, 
	                        		hyperic.data.maintenance_schedule.message.success);
                    	}
                    }
                    else
                    {
                    	that.displayError(hyperic.data.maintenance_schedule.error.serverError);
                        console.info(data.error);
                    }                    	
                },
                error: function(data){
                	that.displayError(hyperic.data.maintenance_schedule.error.serverError);
                	console.info("An error occurred setting maintenance schedule for resource " + that.appdefentity_id, data);
                },
                timeout: 5000
            });
        }
    };
    
    that.clear_schedule_action = function() {
        return hqDojo.xhrPost( {
            url: baseUrl + "/schedule/" + that.appdefentity_id,
            content: {
        		"_method" : "DELETE" // need to work around issue using PUT directly
        	},
        	handleAs: 'json',
            load: function(data){
                if(data && !data.error)
                {
            		that.server_time = new Date(data.serverTime);
                	that.resetSchedule();
					that.redraw(false,
							hyperic.data.maintenance_schedule.message.noSchedule,
							hyperic.data.maintenance_schedule.message.success);
				}
                else
                {
                	that.displayError(hyperic.data.maintenance_schedule.error.serverError);                	
                }
            },
            error: function(data){
            	that.displayError(hyperic.data.maintenance_schedule.error.serverError);
            	console.info("An error occurred clearing maintenance schedule for resource " + that.appdefentity_id, data);
            },
            timeout: 5000
        });
    };
    
    that.getSchedule = function() {
        return hqDojo.xhrGet( {
            url: baseUrl + "/schedule/" + that.appdefentity_id,
            headers: { "Content-Type": "application/json"},
            handleAs: 'json',
            preventCache: true,
            load: function(data){
                if(data && !data.error)
                {
            		that.server_time = new Date(data.serverTime);
            		that.canSchedule = data.permission;
                	                	
            		if(parseInt(data.startTime,10) != 0 && parseInt(data.endTime,10) != 0)
                	{
                    	that.existing_schedule.from_time = parseInt(data.startTime,10);
                    	that.existing_schedule.to_time = parseInt(data.endTime,10);

                    	that.selected_from_time = new Date(that.existing_schedule.from_time);
                    	that.selected_to_time = new Date(that.existing_schedule.to_time);
                    	                    
                    	that.redraw(true, hyperic.data.maintenance_schedule.message.currentSchedule);
                    	
                        if(data.state == 'running')
                    	{
                    		that.displayWarning(hyperic.data.maintenance_schedule.message.runningSchedule);
                    	}
                	} 
        			else
        			{
                        that.resetSchedule();
                    	that.redraw(true, hyperic.data.maintenance_schedule.message.noSchedule);        			
        			}
                }
                else
                {
                    that.resetSchedule();
                	that.redraw(true, "&nbsp;");        			
                	that.displayError(hyperic.data.maintenance_schedule.error.serverError);
                }
            },
            error: function(data){
                that.resetSchedule();
            	that.redraw(true, "&nbsp;");
            	that.displayError(hyperic.data.maintenance_schedule.error.serverError);
            	console.info("An error occurred fetching maintenance schedule for resource " + that.appdefentity_id, data);
            },
            timeout: 5000
        });
    };
    
    that.resetSchedule = function() {
		var curdatetime = new Date();
    	that.existing_schedule = {};
	    that.selected_from_time = new Date(curdatetime.getFullYear(), curdatetime.getMonth(), curdatetime.getDate(), curdatetime.getHours()+1);
	    that.selected_to_time = new Date(that.selected_from_time.getTime() + (60*60000));    	
	    that.deleteConstraints();    	
    };
    
    that.deleteConstraints = function() {
    	for(var inputName in that.inputs)
    	{
    		delete that.inputs[inputName].constraints.min;
    		delete that.inputs[inputName].constraints.max;
    	}
    };
    
    that.updateConstraints = function() {
    	that.deleteConstraints();

    	if(that.inputs.from_date.isValid() && that.inputs.to_date.isValid()
    			&& that.inputs.from_time.isValid() && that.inputs.to_time.isValid())
    	{	
    		var curdatetime = new Date();
	    	var curdate = new Date(curdatetime.getFullYear(), curdatetime.getMonth(), curdatetime.getDate());
	    	var curtime = new Date(curdatetime.getTime()-curdate.getTime()+that.inputs.from_time.getValue().getTimezoneOffset() * 60000);
	    	that.inputs.from_date.constraints.min = curdate;
    		that.inputs.to_date.constraints.min = that.inputs.from_date.getValue();
    		
            if(that.existing_schedule.from_time)
            {
            	if(that.inputs.from_date.getValue().getTime() < curdate.getTime())
            	{
            		that.inputs.from_date.constraints.min = that.inputs.from_date.getValue();
            	}
            }
    		    		
    		if(that.inputs.from_date.getValue().getTime() == curdate.getTime())
    		{
    			if(that.existing_schedule.from_time)
    			{
    				if(that.selected_from_time.getTime() > curdatetime.getTime())
    				{
    					that.inputs.from_time.constraints.min = curtime;
    				}
    			}
    			else
    			{
    				that.inputs.from_time.constraints.min = curtime;
    			}
    		}
    		
    		if(that.inputs.from_date.getValue().getTime() == that.inputs.to_date.getValue().getTime())
    		{
				if((that.inputs.from_time.getValue().getTime() > curtime.getTime())
						|| (that.inputs.from_date.getValue().getTime() > curdate.getTime()))
				{
					that.inputs.to_time.constraints.min = new Date(that.inputs.from_time.getValue().getTime() + 60000);
				}
				else
				{
					that.inputs.to_time.constraints.min = new Date(curtime.getTime() + 60000);
				}
    		}
    	}
    };
    
    that.displayWarning = function(msg)
    {
		that.message_area.request_status.className = 'warningPanel';
		that.message_area.request_status.innerHTML = msg;
		that.message_area.request_status.style.display = '';   	
    };
    
    that.displayError = function(msg)
    {
		that.message_area.request_status.className = 'errorPanel';
		that.message_area.request_status.innerHTML = msg;
		that.message_area.request_status.style.display = '';
    	
		for(var buttonName in that.buttons)
    	{
    		if(buttonName != 'cancel_btn')
    		{
    			that.buttons[buttonName].domNode.style.display = 'none';
    		}
    	}		
    };
    
    that.redraw = function(show, scheduleStatus, confirmationStatus)
    {
    	that.deleteConstraints();
    	
    	that.inputs.from_date.setValue(that.selected_from_time);
		that.inputs.from_time.setValue(that.selected_from_time);
		that.inputs.to_date.setValue(that.selected_to_time);
		that.inputs.to_time.setValue(that.selected_to_time);
		
        // temporary hack because dojo displays the full timezone name
        // (pacific daylight time) in windows firefox
		var timezoneName = hqDojo.date.getTimezoneName(new Date());
        var tzSplit = timezoneName.split(" ");        
        if (tzSplit.length > 1) {            
            // Take the first letter of each word in the split             
            // May be 'pacific daylight time'            
        	timezoneName = "";
            for (var i=0; i<tzSplit.length; i++) {                
            	timezoneName += tzSplit[i][0];
            }
            timezoneName = timezoneName.toUpperCase();
        }       
		hqDojo.byId('maintenance_from_time_timezone').innerHTML = hyperic.data.maintenance_schedule.label.time + '&nbsp;(' + timezoneName + '):&nbsp;';
		hqDojo.byId('maintenance_to_time_timezone').innerHTML = hyperic.data.maintenance_schedule.label.time + '&nbsp;(' + timezoneName + '):&nbsp;';
		
        if(that.existing_schedule.from_time)
        {
        	that.buttons.clear_schedule_btn.domNode.style.display = '';
            that.buttons.schedule_btn.setLabel(hyperic.data.maintenance_schedule.label.reschedule);
            
            if(that.selected_from_time.getTime() < that.server_time.getTime())
            {
            	that.inputs.from_date.setAttribute('disabled', 'disabled');
            	that.inputs.from_time.setAttribute('disabled', 'disabled');
            }
        }
        else
        {
        	that.buttons.clear_schedule_btn.domNode.style.display = 'none';
            that.buttons.schedule_btn.setLabel(hyperic.data.maintenance_schedule.label.schedule);        	
        }
        
        if(!that.canSchedule) {
        	for(var inputName in that.inputs)
        	{
        		that.inputs[inputName].setAttribute('disabled', 'disabled');
        	}
        	for(var buttonName in that.buttons)
        	{
        		if(buttonName != 'cancel_btn')
        		{
        			that.buttons[buttonName].domNode.style.display = 'none';
        		}
        	}
        }
        
    	that.message_area.schedule_status.innerHTML = scheduleStatus;

    	if (confirmationStatus)
    	{
    		that.message_area.request_status.className = 'confirmationPanel';
    		that.message_area.request_status.innerHTML = confirmationStatus;
    		that.message_area.request_status.style.display = '';
    	}
    	else
    	{
    		that.message_area.request_status.style.display = 'none';
    	}

        if (show) 
        {
            that.dialog.show();
        }
        else
        {
        	setTimeout('maintenance_' + that.entity_id + '.dialog.hide()', 2000);
        }
    };
    
	try {
		that.init();
	} catch (e) {
		alert('The Schedule Downtime feature has been disabled because of the following error:\n\n' + e.message);
	}
};

hyperic.clone_resource_dialog = function(args) {
    var that = this;
    var baseUrl = args.url;
    
    that.dialog = null;
    that.title_name = args.titleName;
    that.data = {};
	that.buttons = {};
	that.platform_id = args.platformId || null;
    that.sheets = {};
    that.sheets.clone_instructions = hqDojo.byId('clone_instructions');
    that.sheets.clone_queue_status = hqDojo.byId('clone_queue_status');
    that.sheets.clone_error_status = hqDojo.byId('clone_error_status');
    that.currentSheet = 'clone_instructions';

    that.available_clone_targets = new hyperic.selectBox(hqDojo.byId('available_clone_targets'));
    that.selected_clone_targets = new hyperic.selectBox(hqDojo.byId('selected_clone_targets'));
    
    that.searchbox = hqDojo.byId('cln_search');
    
    that.init = function() {
	    if(!that.dialog){
			var pane = hqDojo.byId('clone_resource_dialog');
			pane.style.width = "450px";
			that.dialog = new hqDijit.Dialog({
				id: "clone_resource_dialog",
				refocus: true,
				autofocus: false,
				title: that.title_name
			},pane);
		}

		that.buttons.create_btn = new hqDijit.form.Button({
			label: "Queue for Cloning",
			name: "clone_btn",
			id: "clone_btn",
			type: 'submit'
		}, "clone_btn");
        hqDojo.connect(that.buttons.create_btn, 'onClick', that.clone_action);

		that.buttons.cancel_btn = new hqDijit.form.Button({
			label: "Cancel",
			name: "create_cancel_btn",
			id: "create_cancel_btn",
			type: 'cancel'
		}, "clone_cancel_btn");
		hqDojo.connect(that.buttons.cancel_btn, 'onClick', that.cancel_action);

        that.buttons.add_clone_btn = hqDojo.byId('add_clone_btn');
        that.buttons.remove_clone_btn = hqDojo.byId('remove_clone_btn');

		hqDojo.connect(
		    that.buttons.add_clone_btn, 
		    'onclick', 
		    function(e) { 
		        that.selected_clone_targets.steal(that.available_clone_targets);

        		if(that.buttons.remove_clone_btn.disabled === true)
        		{
        		    that.buttons.remove_clone_btn.innerHTML = '<img src="/images/arrow_left.gif" alt="deselect">';
        			that.buttons.remove_clone_btn.disabled = false;
        		}

        		if(that.available_clone_targets.length == 0)
        		{
        		    that.buttons.add_clone_btn.innerHTML = '<img src="/images/arrow_right_disabled.gif" alt="select">';
        			that.buttons.add_clone_btn.disabled = true;
        		}
		    }
		);
		hqDojo.connect(
		    hqDojo.byId('remove_clone_btn'), 
		    'onclick', 
		    function(e) { 
		        that.available_clone_targets.steal(that.selected_clone_targets);

        		if(that.buttons.add_clone_btn.disabled === true)
        		{
        		    that.buttons.add_clone_btn.innerHTML = '<img src="/images/arrow_right.gif" alt="select">';
        			that.buttons.add_clone_btn.disabled = false;
        		}

        		if(that.selected_clone_targets.length == 0)
        		{
        		    that.buttons.remove_clone_btn.innerHTML = '<img src="/images/arrow_left_disabled.gif" alt="deselect">';
        			that.buttons.remove_clone_btn.disabled = true;
        		}
		    }
		);

        // search box connections
        hqDojo.connect(that.searchbox,'onfocus', function(e) {if(e.target.value == '[ Resources ]') { e.target.value = ''; }});
        hqDojo.connect(that.searchbox,'onblur', function(e) {if(e.target.value == '') { e.target.value = '[ Resources ]'; }});
        hqDojo.connect(that.searchbox,'onkeyup',function(e) { that.available_clone_targets.search(e.target.value);});
        hqDojo.connect(that.searchbox,'onkeyup',function(e) { that.selected_clone_targets.search(e.target.value);});

		that.fetchData();
    };

    that.fetchData = function() {
        hqDojo.xhrGet( {
            url: baseUrl,
            preventCache: true,
            headers: { "Content-Type": "application/json"},
            handleAs: 'json',
            load: function(data){
                if(data && !data.error)
                {
                    that.data = data;
                    that.populateCloneTargets();
                }
            },
            error: function(data){
                console.info("An error occurred fetching alert groups status... ", data);
            },
            timeout: 2000
        });
    };

    that.populateCloneTargets = function()
    {
        for(var i in that.data)
        {
            if(i != that.platform_id)
            {
                that.available_clone_targets.add(new Option(that.data[i],i));
            }
        }
        if(that.available_clone_targets.length == 0)
		{
		    that.buttons.add_clone_btn.innerHTML = '<img src="/images/arrow_right_disabled.gif" alt="select">';
			that.buttons.add_clone_btn.disabled = true;
		}

		if(that.selected_clone_targets.length == 0)
		{
		    that.buttons.remove_clone_btn.innerHTML = '<img src="/images/arrow_left_disabled.gif" alt="deselect">';
			that.buttons.remove_clone_btn.disabled = true;
		}
    };

    that.cancel_action = function() {
        // hide the dialog
        that.dialog.onCancel();

        // reset the select boxes
        that.selected_clone_targets.reset();
        that.available_clone_targets.reset();
        that.populateCloneTargets();
        
        // reset sheets
        if (that.currentSheet == 'clone_error_status') {
        	that.swapSheets('clone_instructions');
        }
    };

    that.clone_action = function() {
        
        var clone_target_ids = [];
        for(var i = 0, j = that.selected_clone_targets.length; i < j; i++)
        {
            clone_target_ids.push(that.selected_clone_targets.select.options[i].value);
        }
        if(clone_target_ids.length > 0)
        {
        	hqDojo.xhrPost( {
                url: baseUrl,
                content: {
        			ctid: clone_target_ids
        		},
        		handleAs: 'json',
                load: function(data) {
                },
                error: function(data) {
                    console.info("An error occurred queueing platforms for cloning " + that.platform_id, data);
                }
            });
        	that.buttons.create_btn.disabled = true;
        	that.swapSheets('clone_queue_status');
        	setTimeout('clone_platform_' + that.platform_id + '.dialog.hide()', 5000);
        }
        else
        {
            that.swapSheets('clone_error_status');
        }
    };

    that.swapSheets = function(to) {
        var fromSheet = that.sheets[that.currentSheet];
        var toSheet = that.sheets[to];
        
        fromSheet.style.display = 'none';
        toSheet.style.display = 'block'; 

        that.currentSheet = to;
    }
    
	that.init();
};

var activeItem;
function listItemClicked(node){
    if(activeItem){
        
        activeItem.className=activeItem.getAttribute('oldClassName');
    }
    
    node.setAttribute('oldClassName', node.className);
    node.className="active "+node.getAttribute('oldClassName');
    activeItem = node;
}

document.onResize = function(e){
    var newSize = document.body.offsetWidth - 35 - 270 +'px';
    var node = hqDojo.byId("rightPanel");
    node.style.width = newSize;

};

function toggleDialog(){
    alert('open dialog');
}

/**
*/
function filterList(nodes,searchText) {
    var opt;
    for(var i in nodes.childNodes){ 
        opt = nodes.childNodes[i];  
        if(opt.nodeType == 1 && opt.tagName.toLowerCase() == "li"){
            if(opt.childNodes[0].nodeValue.toLowerCase().indexOf(searchText.toLowerCase()) !== -1){
                opt.style.display = '';
            }else {
                opt.style.display = 'none';
            }
        }
    }
}

function clearField(field, className){
    field.value = '';
    var appendClass = " "+ className;
    field.className += appendClass;
}

function setField(field, value){
    if(field.value == '')
    {
        field.value = value;
    }
}

Date.prototype.formatDate = function(format)
{
    var date = this;
    var short_months = ['Jan','Feb','Mar','Apr','May','Jun', 'Jul','Aug','Sep','Oct','Nov','Dec'];
    var months = ['January','February','March','April','May','June', 'July','August','September','October','November','December'];

    format= format || "MM/dd/yyyy";               
 
    var month = date.getMonth() + 1;
    var year = date.getFullYear();    
 
    format = format.replace("MM",month.toString().padL(2,"0"));        
 
    format = format.replace("M",month.toString());
 
    if (format.indexOf("yyyy") > -1)
    {
        format = format.replace("yyyy",year.toString());
    }
    else if (format.indexOf("yy") > -1)
    {
        format = format.replace("yy",year.toString().substr(2,2));
    }
 
    format = format.replace("dd",date.getDate().toString().padL(2,"0"));

    format = format.replace("d",date.getDate().toString());
 
    format = format.replace("b",short_months[date.getMonth()]);

    format = format.replace("B",months[date.getMonth()]);

    format = format.replace("z",hqDojo.date.getTimezoneName(date));

    var hours = date.getHours();       
    if (format.indexOf("t") > -1)
    {
       if (hours > 11)
       {
           format = format.replace("t","PM");
       }
       else
       {
           format = format.replace("t","AM");
       }
    }
    if (format.indexOf("HH") > -1)
    {
        format = format.replace("HH",hours.toString().padL(2,"0"));
    }
    if (format.indexOf("hh") > -1) {
        if (hours > 12) {hours -= 12;}
        if (hours == 0) {hours = 12;}
        format = format.replace("hh",hours.toString().padL(2,"0"));        
    }
    if (format.indexOf("h") > -1) {
        if (hours > 12) {hours -= 12;}
        if (hours == 0) {hours = 12;}
        format = format.replace("h",hours.toString());        
    }
    if (format.indexOf("mm") > -1)
    {
        format = format.replace("mm",date.getMinutes().toString().padL(2,"0"));
    }
    if (format.indexOf("ss") > -1)
    {
        format = format.replace("ss",date.getSeconds().toString().padL(2,"0"));
    }
    return format;
};

Date.prototype.getTimezoneName = function(){
	// similar to dojo.date.getTimezoneName, but fixes a bug
	// with Windows FF that displays the long name of the timezone
	var dateObject = this;
	var str = dateObject.toString(); // Start looking in toString
	var tz = ''; // The result -- return empty string if nothing found
	var match;

	// First look for something in parentheses -- fast lookup, no regex
	var pos = str.indexOf('(');
	if(pos > -1){
		tz = str.substring(++pos, str.indexOf(')'));
        var split = tz.split(" ");        
        if (split.length > 1) {            
            // Take the first letter of each word in the split             
            // May be 'pacific daylight time'            
            tz = "";
            for (var i=0; i<split.length; i++) {                
                tz += split[i][0];
            }
            tz = tz.toUpperCase();
        }		
	}else{
		// If at first you don't succeed ...
		// If IE knows about the TZ, it appears before the year
		// Capital letters or slash before a 4-digit year 
		// at the end of string
		var pat = /([A-Z\/]+) \d{4}$/;
		if((match = str.match(pat))){
			tz = match[1];
		}else{
		// Some browsers (e.g. Safari) glue the TZ on the end
		// of toLocaleString instead of putting it in toString
			str = dateObject.toLocaleString();
			// Capital letters or slash -- end of string, 
			// after space
			pat = / ([A-Z\/]+)$/;
			if((match = str.match(pat))){
				tz = match[1];
			}
		}
	}

	// Make sure it doesn't somehow end up return AM or PM
	return (tz == 'AM' || tz == 'PM') ? '' : tz; // String
};

hyperic.MetricsUpdater = function(eid,ctype,messages) {
    that = this;
    that.attributes = ["min", "average", "max", "last", "avail", "alertCount", "oobCount"];
    that.eid = eid || false;
    that.ctype = ctype || false;
    that.lastUpdate = 0;
    that.liveUpdate = true;
    that.refreshInterval = 0;
    that.refreshTimeout = null;
    that.refreshRates = {
        0   : messages['0'],
        60  : messages['60'],
        120 : messages['120'],
        300 : messages['300']
        };

    that.update = function() {
        var now = new Date();
        that.refreshTimeout = null;
        if (that.liveUpdate && (that.lastUpdate == 0 || (now - that.lastUpdate) >= that.refreshInterval)) {
            var url = '/resource/common/monitor/visibility/CurrentMetricValues.do?eid=' + that.eid;
            if(that.ctype)
            {
                url += '&ctype=' + that.ctype;
            }
            hqDojo.xhrGet( {
                url: url,
                handleAs: "json",
                timeout: 5000,
                load: function(data, ioArgs) {
                    that.lastUpdate = now;
                    that.refreshTimeout = setTimeout( that.update, parseInt(that.refreshInterval,10)*1000 );
                    for (var i = 0; i < data.objects.length; i++) {
                        that.setValues(data.objects[i]);
                    }
                    // Update the time
                    if (hqDojo.byId('UpdatedTime') !== null) {
                        hqDojo.byId('UpdatedTime').innerHTML = messages.LastUpdated + now.toLocaleString();
                    }
                },
                error: function(data){
                    console.info("An error occurred refreshing metrics:");
                    console.info(data);
                }
            });
        }
    };

    that.setValues = function(metricValues) {
        for (var i = 0; i < that.attributes.length; i++) {
            that.substitute(that.attributes[i], metricValues);
        }
    };

    that.substitute = function( attribute, metricValues) {
        var metric = metricValues.mid;
        var lastSpan = hqDojo.byId(attribute + metric);
        if (lastSpan !== null) {
            var html;
            if (attribute == "avail") {
                var img;
                switch(metricValues.last) {
                    case '100.0%':
                        img = 'green';
                        break;
                    case '0.0%':
                        img = 'red';
                        break;
                    case '-1.0%':
                        img = 'orange';
                        break;
                    default:
                        img = 'yellow';
                        break;
                }
                html = '<img src="/images/icon_available_'+img+'.gif" width="12" height="12" alt="" border="0" align="absmiddle">';
            }
            else {
                html = metricValues[attribute];
            }

            if (lastSpan.innerHTML != html) {
                var hl = new Effect.Highlight(lastSpan.parentNode);
                lastSpan.innerHTML = html;
                var p = new Effect.Pulsate(lastSpan);
            }
        }
    };
    
    that.setRefresh = function(refresh) {
        refresh = parseInt(refresh,10);
        if(typeof that.refreshRates[refresh] != 'undefined')
        {
            that.liveUpdate = (refresh === 0) ? false : true;
            that.refreshInterval = refresh;
            for(var i in that.refreshRates)
            {
                if(typeof that.refreshRates[i] !== 'function')
                {
                    if(i == refresh)
                    {
                        hqDojo.byId('refresh' + i).innerHTML = that.refreshRates[i];
                    }
                    else
                    {
                        hqDojo.byId('refresh' + i).innerHTML = '<a href="javascript:" onclick="metricsUpdater.setRefresh('+ i +');">' + that.refreshRates[i] + '</a>';
                    }
                }
            }
            if(that.refreshTimeout === null && that.refreshInterval !== 0)
            {
                that.refreshTimeout = setTimeout( that.update, parseInt(that.refreshInterval,10)*1000 );
            }
        }
    };

    // set default refresh rate and initialize the refresh rate links.
    that.setRefresh(120);
};

hyperic.MetricChart = function(formObj) {
	var that = this;
	that.chartForm = formObj;
	
	that.refresh = function() {
		that.chartForm.submit();
	}
	
	that.generateChartParameters = function() {
		var inputEls = that.chartForm.elements;
		var params = {};
		
		for (var x = 0; x < inputEls.length; x++) {
			var el = inputEls[x];
			
			if (el.type == 'checkbox' && !el.checked) continue;
	
			if (params[el.name]) {
				params[el.name][params[el.name].length] = el.value;
			} else {
				params[el.name] = [el.value];
			}
		}
		
		return params;
	}
	
	that.saveToDashboard = function() {
		that.chartForm.saveChart.value='true'; 
		var saveChartUrl = that.chartForm.action;
		var saveChartParams = that.generateChartParameters();
		
		new Ajax.Request(saveChartUrl, {
			method: 'post',
			parameters: saveChartParams,
			onSuccess: function(transport) {
				alert(hyperic.data.metric_chart.message.chartSaved);
			}
		});
		
		return false;
	}

	that.exportData = function(params) {
		var url = "/resource/MetricData?hq=" + new Date().getTime();
		
		for (var x in params) {
			url += "&" + x + "=" + params[x]; 
		}
		
		var instanceIds = "";
		var inputList = that.chartForm.elements;
		var numOfResources = 0;
		var numOfResourcesChecked = 0;
		for (var i = 0; i < inputList.length; i++) {
			if (inputList[i].type == "checkbox"
					&& inputList[i].name == "resourceIds") {
				numOfResources++;
				if (inputList[i].checked) {
					if (numOfResourcesChecked > 0) {
						instanceIds += ",";
					}
					instanceIds += inputList[i].value;
					numOfResourcesChecked++;
				}
			}
		}
		// only add this parameter if user has deselected some resources
		if (numOfResourcesChecked < numOfResources) {
			url += "&instanceIds=[" + instanceIds + "]";
		}
		window.location.href = url;
    }	
}

// see http://ejohn.org/blog/javascript-array-remove/ for explanation
Array.prototype.remove = function(from, to){
  this.splice(from, !to || 1 + to - from + (!(to < 0 ^ from >= 0) && (to < 0 || -1) * this.length));
  return this.length;
};

/**
 * The Health Widget
 * 
 * @param parentNodeId where to create the node
 * @param kwArgs the data 
 * @parma isDetail whether the node should have the show detail property
 */
hyperic.widget.Health = function(parentNodeId, kwArgs, isDetail) {
    var that = this;
    that.parendNodeId = parentNodeId;
    that.id = ++id1;
    that.isDetail = isDetail;
    that.connects = [];
    that.title = kwArgs.n;
    that.legend = "";
    that.data = kwArgs.d;
    that.statusMsg = kwArgs.sm;
    that.shortname = kwArgs.sn.toLowerCase();
    var range_start = new Date(kwArgs.startMillis);
    var range_end = new Date(kwArgs.endMillis);
    
    that.template = '<div class="rle-box" id="' + that.id + '_health"><div class="roll-title"> <span id="' + that.id + '_roll_title">' + kwArgs.n + '</span><span class="rle-cs">' + kwArgs.nm + '</span></div><div class="both"></div><div class="rle-cont"><div class="rle-data" id="' + that.id + '_data">' + '</div><div class="rle-now ' + kwArgs.cs + 'Avail" id="ec2_now">&nbsp;</div><div class="rle-rule"></div><div class="rle-legend" id="' + that.id + '_legend"><span class="ll">' + range_start.formatDate('M/d HH:mm z') + '</span><span class="rl">' + range_end.formatDate('M/d HH:mm z') + '</span></div></div>' + '<div style="clear:both"></div><div class="rle-more" id="' + that.id + '_more" style="display:none"><a href="javascript:void(0)" onclick="changeTabs(\'' + that.shortname + '\');">more detail</a></div><div class="rle-status" id="' + that.id + '_status"></div><div style="clear:both"></div></div></div>';
    that.create = function() {
        var f = hyperic.widget.tempNode;
        f.innerHTML = that.template;
        that.appendNode = hqDojo.byId(parentNodeId);
        that.titleNode = hqDojo.byId(that.id + '_roll_title');
        that.legendNode = hqDojo.byId(that.id + '_legend');
        that.node = hqDojo.byId(that.id + '_health');
        that.moreNode = hqDojo.byId(that.id + '_more');
        hqDojo.byId(that.id + '_data').innerHTML = that.createHealthData(that.data);
        hqDojo.byId(that.id + '_status').innerHTML = that.createStatus(that.statusMsg);
        that.connects[0] = hqDojo.connect(that.node, 'onmouseenter', that, 'onMouseOver');
        that.connects[1] = hqDojo.connect(that.node, 'onmouseleave', that, 'onMouseOut');
        hqDojo.byId(parentNodeId).appendChild(f.firstChild);
        };
    that.createStatus = function(data) {
        var ihtml = '';
        var datestring = '';
        if (data) {
            ihtml = '<ul>';
            for (var i = 0; i < data.length; i++) { 
                console.log(data[i]);
                datestring = new Date(data[i].timeMillis).formatDate('M/d HH:mm z');
                ihtml += '<li>' + datestring + ' : ';
                if(data[i].url) {
                    ihtml += '<a href="'+ data[i].url +'">' + data[i].msg + '</a>';
                }
                else
                {
                    ihtml += data[i].msg;
                }
                datestring = '';
                ihtml += '</li>'; 
            }
        ihtml += '</ul>';
        }
        return ihtml;
        };
    that.createHealthData = function(data) {
        var range_start = new Date(data[0].startMillis);
        var i_range_start = i_range_end = null;
    	//beggining cap
        var ihtml = '<div class="rle-bg ' + data[0].s + 'Left" style="width:4px" title="' + range_start.formatDate('M/d HH:mm z') + '"></div>';
        for (var i = 0; i < data.length; i++) {
            i_range_start = new Date(data[i].startMillis);
            i_range_end = new Date(data[i].endMillis);
        	//Scale to 98% to compensate for the fixed with of the 3px caps - (3px*2)/418px ~= 2%
            ihtml += '<div class="rle-bg ' + data[i].s + '" style="width:' + (data[i].w * 0.98) + '%" title="' + i_range_start.formatDate('M/d HH:mm') + ' &#8594; ' + i_range_end.formatDate('M/d HH:mm z') + '"></div>';
            i_range_start = i_range_end = null;
        }
        //end cap
        var range_end = new Date(data[data.length-1].endMillis);
        ihtml += '<div class="rle-bg ' + data[data.length-1].s + 'Right" style="width:4px" title="' + range_end.formatDate('M/d HH:mm z') + '"></div>';
        delete range_start;
        delete range_end;
        return ihtml;
        };
    that.onMouseOver = function() {
        //that.legendNode.innerHTML = that.legend;
        if(that.isDetail){ that.moreNode.style.display = 'block'; }
        };
    that.onMouseOut = function() {
        //that.legendNode.innerHTML = "&nbsp;";
        if(that.isDetail){ that.moreNode.style.display = 'none'; }
        };
    that.cleanup = function() {
        //null out references to DOM Nodes
        that.node = null;
        that.titleNode = null;
        that.legendNode = null;
        that.moreNode = null;
        that.appendNode = null;
        //disconnect all aggregated events
        hqDojo.disconnect(that.connects[0]);
        hqDojo.disconnect(that.connects[1]);
        };
    //init
    this.create();
};
    
/**
* Table - creates a Table with a title
*
* @param node
* @param kwArgs
*/
hyperic.widget.Table = function(node, kwArgs) {
    var t1 = '<div><div class="tTitle">' + kwArgs.label + '</div><table id="aws_table"><thead><tr><td class="tRight"></td>';
    var t2 = '</tr></thead><tbody>';
    var t3 = '</tbody></table><div>';
    this.create = function(node, kwArgs) {
        this.node = hqDojo.byId(node);
        var t = t1 + this.createHeader(kwArgs.header) + t2 + this.createBody(kwArgs.data) + t3;
        var f = hyperic.widget.tempNode;
        f.innerHTML = t;
        this.node.appendChild(f.firstChild);
        f.innerHTML = '';
        f = null;
        };
    this.createHeader = function(data) {
        var ret = '';
        for (var i = 0; i < data.length; i++) { ret += '<td>' + data[i] + '</td>'; }
        return ret;
        };
    this.createBody = function(data) {
        var ret = '';
        for (var i = 0; i < data.length; i++) {
            ret += i % 2 !== 0 ? '<tr>': '<tr class="alternate">';
            for (var j = 0; j < data[i].length; j++) {
                if (j === 0) { ret += '<td class="tRight">' + data[i][j] + '</td>'; }
                else { ret += '<td>' + data[i][j] + '</td>'; }
            }
            ret += '</td>';
        }
        return ret;
        };
    this.cleanup = function(){
        this.node = null;
        };
    //init
    this.create(node, kwArgs);
};

/**
 * @param kwArgs 
 *    - document (boolean) whether the 
 *    - tipElements (Array) the nodes to parse for
 *    - baseNodeId (String) the node in the document to start the parsing at
 *    -
 */
hyperic.widget.tooltip = { 
    tipElements : [],   // @Array: Allowable elements that can have the toolTip
    obj : {},                           // @Element: That of which you're hovering over
    tip : {},                           // @Element: The actual toolTip itself
    xPos : 0,                               // @Number: x pixel value of current cursor position
    yPos : 0,                               // @Number: y pixel value of current cursor position
    active : 0,                             // @Number: 0: Not Active || 1: Active
    connections : [],
    conIdx : -1,
    init : function(kwArgs) {
        if(kwArgs.tipElements)
            this.tipElements = kwArgs.tipElements;
        if ( !document.getElementById ||
            !document.createElement ||
            !document.getElementsByTagName ) {
            return;
        }
        var i,j;
        if(!hqDojo.byId('toolTip')){
            this.tip = document.createElement('div');
            this.tip.id = 'toolTip';
            document.getElementsByTagName('body')[0].appendChild(this.tip);
        }else{
            this.tip = hqDojo.byId('toolTip');
        }
        this.tip.style.top = '0';
        this.tip.style.display = 'none';
        var tipLen = this.tipElements.length;
        for ( i=0; i<tipLen; i++ ) {
            var current = {};
            if(kwArgs.document){
                current = document.getElementsByTagName(this.tipElements[i]);
            }else{
                current = hqDojo.byId(kwArgs.baseNodeId).getElementsByTagName(this.tipElements[i]);
            }   
            var curLen = current.length;
            for ( j=0; j<curLen; j++ ) {
                if(current[j].getAttribute('title')){
                    this.connections[++this.conIdx] = hqDojo.connect(current[j], 'mouseover', hyperic.widget.tooltip, "tipOver");
                    this.connections[++this.conIdx] = hqDojo.connect(current[j], 'mouseout', hyperic.widget.tooltip, "tipOut");
                    current[j].setAttribute('tip',current[j].getAttribute('title'));
                    current[j].removeAttribute('title');
                }
            }
        }
    },
    updateXY : function(e) {
        if ( document.captureEvents ) {
            hyperic.widget.tooltip.xPos = e.pageX;
            hyperic.widget.tooltip.yPos = e.pageY;
        } else if ( window.event.clientX ) {
            hyperic.widget.tooltip.xPos = window.event.clientX+document.documentElement.scrollLeft;
            hyperic.widget.tooltip.yPos = window.event.clientY+document.documentElement.scrollTop;
        }
    },
    //TODO there is no tID
    tipOut: function() {
        if ( window.tID ) {
            clearTimeout(window.tID);
        }
        if ( window.opacityID ) {
            clearTimeout(window.opacityID);
        }
        hyperic.widget.tooltip.tip.style.display = 'none';
    },
    checkNode : function() {
        var trueObj = this.obj;
        if ( inArray(this.tipElements, trueObj.nodeName.toLowerCase()) ) {
            return trueObj;
        } else {
            return trueObj.parentNode;
        }
    },
    tipOver : function(e) {
        hyperic.widget.tooltip.obj = e.target;
        window.tID = window.setTimeout("hyperic.widget.tooltip.tipShow()",10);
        hyperic.widget.tooltip.updateXY(e);
    },
    tipShow : function() {      
        var scrX = Number(this.xPos);
        var scrY = Number(this.yPos);
        var tp = parseInt(scrY+15);
        var lt = parseInt(scrX+10);
        var anch = this.checkNode();
        var addy = '';
        var access = '';
        if ( anch.nodeName.toLowerCase() == 'a' ) {
            addy = (anch.href.length > 25 ? anch.href.toString().substring(0,25)+"..." : anch.href);
            var access = ( anch.accessKey ? ' <span>['+anch.accessKey+']</span> ' : '' );
        } else {
            //addy = anch.firstChild.nodeValue;
        }
        this.tip.innerHTML = "<p>"+anch.getAttribute('tip')+"<em>"+access+addy+"</em></p>";
        if ( parseInt(document.documentElement.clientWidth+document.documentElement.scrollLeft) < parseInt(this.tip.offsetWidth+lt) ) {
            this.tip.style.left = parseInt(lt-(this.tip.offsetWidth+10))+'px';
        } else {
            this.tip.style.left = lt+'px';
        }
        if ( parseInt(document.documentElement.clientHeight+document.documentElement.scrollTop) < parseInt(this.tip.offsetHeight+tp) ) {
            this.tip.style.top = parseInt(tp-(this.tip.offsetHeight+10))+'px';
        } else {
            this.tip.style.top = tp+'px';
        }
        this.tip.style.display = 'block';
        this.tip.style.opacity = '.1';
        this.tipFade(10);
    },
    tipFade: function(opac) {
        var passed = parseInt(opac);
        var newOpac = parseInt(passed+10);
        if ( newOpac < 80 ) {
            this.tip.style.opacity = '.'+newOpac;
            this.tip.style.filter = "alpha(opacity:"+newOpac+")";
            window.opacityID = window.setTimeout("hyperic.widget.tooltip.tipFade('"+newOpac+"')",20);
        }
        else { 
            this.tip.style.opacity = '.80';
            this.tip.style.filter = "alpha(opacity:80)";
        }
    },
    cleanup: function() {
        for(var i = 0; i < this.connections.length; i++) {
            hqDojo.disconnect(this.connections[i]);
        }
        this.conIdx = -1;
    }
};

/* SaaS plugin js */
hyperic.widget.CloudChart = function(node, kwArgs, tabid, chartPos, chartType) {
    var that = this;
    that.create = function(node, kwArgs, tabid, chartPos) {
        var chart_class = '';
        if(chartType)
        {
            switch(chartType) {
                case 'single':
                    chart_class = 'chartW';
                    break;
                case 'dashboard':
                    chart_class = 'chartS';
                    break;
                case 'skinny':
                    chart_class = 'chartT';
                    break;
                default:
                    chart_class = 'chart';
                    break;
            }
        }
        that.containerId = tabid+'-chartCont-'+chartPos;
        var template = '<div class="chartCont" id="'+that.containerId+'"><div class="cTitle">' + kwArgs.chartName + '</div><div id="' + tabid + '_chart' + chartPos + '" class="'+chart_class+'"></div><div class="xlegend"></div></div>';
        that.template = template;
        that.tabid = tabid;
        that.node = node;
        var f = hqDojo.byId('z');
        that.chartName = kwArgs.chartName;
        //console.log("created chart: "+kwArgs.chartName);
        f.innerHTML = template;
        hqDojo.byId(node).appendChild(f.firstChild);
        that.url = kwArgs.url;
        that.data = kwArgs.data;
        that.chartPos = chartPos;
        //chartObjs[tabid] = that;

        cloudTabs.subscribe('activeTabChange', that.onTabChange);
        
        //TODO check if the tab that is currently selected is the one that is getting the chart.
        f=null;
    };
    that.onTabChange = function(evt) {
        if (!that.isShowing && evt.newValue.tabid == that.tabid){
            that.showChart();
        }
    };
    that.showChart = function() {
        //create chart
        that.dataSource = new Timeplot.DefaultEventSource();
        var chartOptions = null;
        var count = 0;
        for(var i in that.data) {
            if(undefined !== that.data[i] && typeof(that.data[i]) !== 'function')
            {
                count++;
            }
        }
        
        chartOptions = {
            id : "plot1", 
            dataSource : new Timeplot.ColumnSource(that.dataSource, 1), 
            valueGeometry : new Timeplot.DefaultValueGeometry( {
                axisLabelsPlacement : "left"
                }
            ), 
            timeGeometry : new Timeplot.DefaultTimeGeometry( {
                axisLabelsPlacement : "bottom" }
            ), 
            showValues : false, 
            lineColor : "#00EB08", 
            roundValues: false,
            fillColor : "#D0FFD2"
        };

        if(chartType != 'dashboard')
        {
            chartOptions.valueGeometry.gridColor = new Timeplot.Color("#000000");
            chartOptions.timeGeometry.gridColor  = new Timeplot.Color("#DDDDDD");
            chartOptions.showValues = true;
            chartOptions.fillColor = "#00B93A";
        }

        if(count == 0)
        {
            chartOptions.showValues = false;
        }

        that.chart = Timeplot.create(hqDojo.byId(that.tabid + "_chart" + that.chartPos), [Timeplot.createPlotInfo( chartOptions )]);

        if(count > 1)
        {
            // that.chart.loadText(that.url, ",", that.dataSource);
            that.chart.loadJSON(that.data, that.dataSource);
        }
        else
        {
            if(chartType != 'dashboard')
            {
                // display 'no data' message
                containerNode = hqDojo.byId(that.containerId);
                var message = SimileAjax.Graphics.createMessageBubble(containerNode.ownerDocument);
                message.containerDiv.className = "timeline-message-container";
                containerNode.appendChild(message.containerDiv);

                message.contentDiv.className = "timeline-message";
                message.contentDiv.innerHTML = '<span style="color: #000; font-size: 12px;">No Data</span>';
                message.containerDiv.style.display = "block";
            }

            // load fake data to make the chart draw a grid
            that.chart.loadJSON({"2008-08-04T01:08:51-0700":[0],"2008-08-04T01:09:51-0700":[0],"2008-08-04T01:10:51-0700":[0]}, that.dataSource);
        }
        // chart.loadText(that.url, ",", es);
        that.isShowing = true;
    };
    this.cleanup = function(){
        cloudTabs.unsubscribe('activeTabChange', that.onTabChange);

        n = hqDojo.byId(that.containerId);
        // destroy all children of the chart container
        while(n.lastChild) {
          n.removeChild(n.lastChild);
        }
        hqDojo.byId(that.node).removeChild(n);

        that.node = null;
    };
    //init
    that.isShowing = false;
    this.create(node, kwArgs, tabid, chartPos);
};

/** 
 * Status Element Functionality
 * @param ct The Current Time Node
 * @param nt the Countdown time Node
 * @param status the status node
 * @param update the update node
 *
 * The template - 
 *   <div class="status">
 *      <div id="status" style="display:none">Updated <span id="ct">DateTime</span>. Updates in <span id="nt">59</span></div>
 *      <div id="update">Updating...</div>
 *   </div>
 */
hyperic.widget.StatusElement = function(ct, nt, status, update, interval) {
    var that = this;
    that.ctNode = hqDojo.byId(ct);
    that.ntNode = hqDojo.byId(nt);
    that.sNode = hqDojo.byId(status);
    that.uNode = hqDojo.byId(update);
    that.interval = null;
    that.time = interval;
    that.refInterval = interval;
    that.startUpdate = function() {
        //show updating
        that.uNode.style.display = 'block';
        that.sNode.style.display = 'none';
        that.isUpdating = !that.isUpdating;
        };
    that.endUpdate = function() {
        //hide updating
        that.uNode.style.display = 'none';
        that.sNode.style.display = 'block';
        //reset timer with the inteval value
        that.time = that.refInterval;
        //then start the new clock
        that.startClock();
        //mark current date
        that.ctNode.innerHTML = new Date().formatDate('HH:mm:ss z');
        };
    that.startClock = function() {
        that.ntNode.innerHTML = that.time + '';
        clearInterval(that.interval);
        that.interval = window.setInterval(that.updateClock, 1000);
        };
    that.updateClock = function() {
        that.ntNode.innerHTML = that.time + '';
        --that.time;
        };
    that.cleanup = function() {
        };
    //init
    return that;
};


/* end SaaS plugin js */

function fitStringToSize(str,width) {
    var result = str;
    var span = document.createElement("span");
    span.style.visibility = 'hidden';
    span.style.padding = '0px';
    document.body.appendChild(span);

    // on first run, check if string fits into the length already.
    span.innerHTML = result;
    if(span.offsetWidth > width) {
        var posStart = 0, posMid, posEnd = str.length, posLength;

        // Calculate (posEnd - posStart) integer division by 2 and
        // assign it to posLength. Repeat until posLength is zero.
        while (posLength = (posEnd - posStart) >> 1) {
            posMid = posStart + posLength;
            //Get the string from the begining up to posMid;
            span.innerHTML = str.substring(0,posMid) + '&hellip;';

            // Check if the current width is too wide (set new end)
            // or too narrow (set new start)
            if ( span.offsetWidth > width ) posEnd = posMid; else posStart=posMid;
        }
        //Escape < and >, eliminate trailing space and a widow character if one is present.
        result = str.substring(0,posStart).replace("<","&lt;").replace(">","&gt;").replace(/(\s.)?\s*$/,'') + '&hellip;';
    }
    document.body.removeChild(span);
    return result;
}

function getShortLink(str,width,url)
{
    return '<a title="' + str.replace("\"","&#34;") + '" href="'+ url +'">' + fitStringToSize(str,width) + '<\/a>';
}

function getShortAbbr(str,width)
{
    return '<abbr title="' + str.replace("\"","&#34;") + '">' + fitStringToSize(str,width) + '<\/abbr>';
}
