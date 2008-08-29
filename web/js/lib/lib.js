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
        dojo.html.setStyle(node, 'display', '');
    },
    hide : function(/*String*/ node){
        dojo.html.setStyle(node, 'display', 'none');
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
            dojo.event.topic.publish('enter', [evt]);
        }
    },
    registerListener : function(/*DOMNode*/node, /*fp*/handler){
        if(handler && node) {
            dojo.event.connect(node, 'onkeyup', handler);
            /*
            if(dojo.isIE) {
                node.attachEvent("keyup", handler);
            } else {
                node.addEventListener("keyup", handler, false);
            }*/
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
hyperic.utils.addKeyListener = function(/*Node*/node, /*Object*/ keyComb, /*String*/topic){
	this.node = node;
    this.keyComb = keyComb;
    this.topic = topic;
    this.canceled = false;
    this.keyListener = function(e){
        if(!e) { e = window.event; }
        if(e.keyCode == this.keyComb.keyCode && !this.canceled){
            if(this.keyComb.ctrl || this.keyComb.alt || this.keyComb.shift){
                if((this.keyComb.ctrl && e.ctrlKey)
                		|| (this.keyComb.alt && e.altKey)
                		|| (this.keyComb.shift && e.shiftKey)){
                    this.publish(e);
                }else{
                    return;
                }
            }else{
                this.publish(e);
            }            
        }
    };
    this.publish = function(e){
        if(window.event){
            e.cancelBubble = true;
            e.returnValue = false;
        }else{
            e.preventDefault();
            e.stopPropagation();
        }
        dojo.event.topic.publish(this.topic, [e]);
    };
    this.cancel = function(){
        this.canceled = true;
        dojo.event.disconnect(node, "onkeyup", this, this.keyListener);
    };
    //dojo.connect(node, "onkeyup", this, this.keyListener);
    dojo.event.connect(node, "onkeyup", this, this.keyListener);
    return this;
};

hyperic.utils.addUrlXtraCallback = function(plugin_id, fn) {
    urlXtraVar[plugin_id] = urlXtraVar[plugin_id] || [];
    urlXtraVar[plugin_id].push(fn);
};

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

hyperic.widget.search = function(/*Object*/ urls, /*number*/ minStrLenth, /*Object*/ keyCode){
    dojo.require('dojo.io');
    dojo.require('dojo.lfx.html');
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
        this.listeners.push( new hyperic.utils.addKeyListener(window.document, this.keyCode, 'search') );
        this.listeners.push( new hyperic.utils.addKeyListener(this.searchContainer, {keyCode: 13}, 'enter') );
        this.listeners.push( new hyperic.utils.addKeyListener(dojo.byId('header'), {keyCode: 27}, 'escape') );
    };
    this.search = function(e){
        var string = e.target.value;
        if(this.searchBox.value.length >= this.minStrLen){
            this.searchStarted();
            dojo.io.bind( {
                url: this.searchURL+'?q='+string, 
                method: "post",
                handleAs: "json",
                timeout: 5000, 
                handle: loadSearchData,
                error: this.error,
                mimetype:'text/json'
            });
            
           
        }else{
            this.searchEnded();
            this.nodeSearchResults.style.display = 'none';
        }
    };
    this.error = function(){
        this.searchEnded();
        alert("foo");
    };
    this.loadResults = function(response){
        this.searchEnded();
        
    };
    this.toggleSearchBox = function() {
        if(this.opened) {
            this.nodeSearchResults.style.display = 'none';
            dojo.lfx.html.wipeOut([this.searchContainer], 400).play();
            //dojo.fx.wipeOut({node:this.searchContainer, duration: 400}).play();
            this.opened = false;
            this.searchEnded();
            this.searchBox.value = '';
        }
        else {
            window.scrollTo(0,0);
            dojo.lfx.html.wipeIn([this.searchContainer], 400).play();
            //dojo.fx.wipeIn({node:this.searchContainer, duration: 400}).play();
            this.opened = true;
            this.searchBox.focus();
        }
    };
    this.searchStarted = function(){
        this.searchBox.className = "searchActive";
    };
    this.searchEnded = function(){
        this.searchBox.className = "";
    };
    return this;
};

function loadSearchData(type, response, evt) {
    if(type == 'load'){
        var resURL = resourceURL+"?eid=";
        var template = "<li><a href='link' class='type'>text<\/a><\/li>";
        var count = 0;
        var res = "";
        var relink = new RegExp("link", "g");
        var retext = new RegExp("text", "g");
        var retype = new RegExp("type", "g");
        for(var i = 0; i < response.length; i++) {
            var length = response[i].name.length;
            if(length >= 37){
                response[i].name = response[i].name.substring(0,4) + "..." + response[i].name.substring(length-28, length);
            }
            res += template.replace(relink, resURL+response[i].eId).replace(retext, response[i].name).replace(retype, response[i].resType);
            count++;
        }
        dojo.byId("resourceResults").innerHTML = res;
        dojo.byId("resourceResultsCount").innerHTML = count;
        dojo.byId('headerSearchResults').style.display = '';
        dojo.byId('searchBox').className = "";
    }
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
            dojo11.connect(this.node, 'onmouseenter', that, 'onHover');
            dojo11.connect(this.node, 'onmouseleave', that, 'onUnHover');
        }
        if(kwArgs.toggleNode) {
            if(kwArgs.subMenu){
                this._isSubMenu = true;
                dojo11.connect(kwArgs.toggleNode, 'onmouseover', that, 'onclick');
            }else{
                dojo11.connect(kwArgs.toggleNode, 'onclick', that, 'onclick');
            }
        }
        
    }; 
    this.isFocused = false;
    this._isVisible = false;
    this._init(kwArgs);
};


/* OLD REPORTING */

function init_reporting(){
    dojo.require("dojo.widget.DropdownDatePicker"); 
    dojo.require("dojo.widget.HtmlWidget");
    dojo.require("dojo.widget.ValidationTextbox");
    dojo.require("dojo.io");
    dojo.require("dojo.json");
    dojo.require("dojo.event");
    
    dojo.event.connect(window, "onload", function(){
        var reportList = dojo.byId("reports");
        if(reportList){
            reportList.selectedIndex = 0;
            selectedChanged(reportList);
        }
    });
}

hyperic.hq = {};
hyperic.hq.reporting = {};
hyperic.hq.dom = {};

hyperic.hq.dom.datePickerProps = {
    displayWeeks : "6",
    inputWidth : "15em",
    formatLength : "full",
    templateString : '<div class="fieldRow" dojoAttachPoint="fieldRowContainerNode">\n\t<label for="${this.widgetId}">\n\t\t<span class="fieldLabel ${this.fieldRequiredClass}"><img src="/images/icon_required.gif" height="9" width="9" border="0"><span dojoAttachPoint="fieldLabel">${this.label}</span></span>\n\t</label>\n\t\t<div class="fieldValue" dojoAttachPoint="fieldWrapper">\n\t\t<span style=\"white-space:nowrap\"><input type=\"hidden\" name=\"\" value=\"\" dojoAttachPoint=\"valueNode\" /><input name=\"\" type=\"text\" value=\"\" style=\"vertical-align:middle;\" dojoAttachPoint=\"inputNode\" dojoAttachEvent=\"onclick:onIconClick\" readonly=\"readonly\" autocomplete=\"off\" /> <img src=\"${this.iconURL}\" alt=\"${this.iconAlt}\" dojoAttachEvent=\"onclick:onIconClick\" dojoAttachPoint=\"buttonNode\" style=\"vertical-align:middle; cursor:pointer; cursor:hand\" /></span>\n<div dojoattachpoint="validationMessage" class="errorMsg"></div></div>\n</div>',
    value : new Date(),
    StartDate : new Date(1-1-2000)
};
    
hyperic.hq.dom.validationTextboxProps = {
    id : "ValidationWidget1",
    type : 'text',
    required : true,
    missingClass : "",
    size : 23,
    maxlength : 60,
    missingMessage : "",
    requiredMessage : "this value is required",
    listenOnKeyPress : false,
    templateString : '<div class="fieldRow" dojoAttachPoint="fieldRowContainerNode">\n\t<label for="${this.widgetId}">\n\t\t<span class="fieldLabel ${this.fieldRequiredClass}"><img src="/images/icon_required.gif" height="9" width="9" border="0"><span dojoAttachPoint="fieldLabel">${this.label}</span></span>\n\t</label>\n\t\t<div class="fieldValue" dojoAttachPoint="fieldWrapper">\n\t\t<span style="float:${this.htmlfloat};">\n\t\t<input dojoAttachPoint="textbox" type="${this.type}" dojoAttachEvent="onblur;onfocus;onkeyup" id="${this.widgetId}" name="${this.name}" size="${this.size}" maxlength="${this.maxlength}" class="${this.className}" style="">\n\t\t\t<div dojoAttachPoint="invalidSpan" class="${this.invalidClass}">&nbsp;-&nbsp;${this.messages.invalidMessage}</div>\n\t\t<div dojoAttachPoint="missingSpan" class="${this.missingClass}">&nbsp;-&nbsp;${this.messages.missingMessage}</div>\n\t\t<div dojoAttachPoint="rangeSpan" class="${this.rangeClass}">&nbsp;-&nbsp;${this.messages.rangeMessage}</div>\n\t\t</span>\n</div>\n</div>',
    templateCssString : ".dojoValidateEmpty{}\n.dojoValidateValid{}\n.dojoValidateInvalid{}\n.dojoValidateRange{}\n"
};

hyperic.hq.dom.selectboxProps = {
   templateCssString : ".dojoComboBoxOuter {\n\tborder: 0px !important;\n\tmargin: 0px !important;\n\tpadding: 0px !important;\n\tbackground: transparent !important;\n\twhite-space: nowrap !important;\n}\n\n.dojoComboBox {\n\tborder: 1px inset #afafaf;\n\tmargin: 0px;\n\tpadding: 0px;\n\tvertical-align: middle !important;\n\tfloat: none !important;\n\twidth:172px;height:14px;position: static !important;\n\tdisplay: inline !important;\n}\n\n/* the input box */\ninput.dojoComboBox {\n\tborder-right-width: 0px !important; \n\tmargin-right: 0px !important;\n\tpadding-right: 0px !important;\n}\n\n/* the down arrow */\nimg.dojoComboBox {\n\tborder-left-width: 0px !important;\n\tpadding-left: 0px !important;\n\tmargin-left: 0px !important;height:15px;\n}\n\n/* IE vertical-alignment calculations can be off by +-1 but these margins are collapsed away */\n.dj_ie img.dojoComboBox {\n\tmargin-top: 1px; \n\tmargin-bottom: 1px; \n}\n\n/* the drop down */\n.dojoComboBoxOptions {\n\tfont-family: Verdana, Helvetica, Garamond, sans-serif;\n\t/* font-size: 0.7em; */\n\tbackground-color: white;\n\tborder: 1px solid #afafaf;\n\tposition: absolute;\n\tz-index: 1000; \n\toverflow: auto;\n\tcursor: default;width:200px;\n}\n\n.dojoComboBoxItem {\n\tpadding-left: 2px;\n\tpadding-top: 2px;\n\tmargin: 0px;\n}\n\n.dojoComboBoxItemEven {\n\tbackground-color: #f4f4f4;\n}\n\n.dojoComboBoxItemOdd {\n\tbackground-color: white;\n}\n\n.dojoComboBoxItemHighlight {\n\tbackground-color: #63709A;\n\tcolor: white;\n}\n",
   templateString : '<div class="fieldRow"><label for="${this.widgetId}"><span class="fieldLabel ${this.fieldRequiredClass}"><img width="9" height="9" border="0" src="/images/icon_required.gif"/><span dojoAttachPoint="fieldLabel">${this.label}</span></span></label><div class="fieldValue" dojoAttachPoint="fieldWrapper"><span class=\"dojoComboBoxOuter\"\n\t><input style=\"display:none\"  tabindex=\"-1\" name=\"\" value=\"\" \n\t\tdojoAttachPoint=\"comboBoxValue\"\n\t><input style=\"display:none\"  tabindex=\"-1\" name=\"\" value=\"\" \n\t\tdojoAttachPoint=\"comboBoxSelectionValue\"\n\t><input type=\"text\" autocomplete=\"off\" class=\"dojoComboBox\"\n\t\tdojoAttachEvent=\"key:_handleKeyEvents; keyUp: onKeyUp; compositionEnd; onResize;\"\n\t\tdojoAttachPoint=\"textInputNode\"\n\t><img hspace=\"0\"\n\t\tvspace=\"0\"\n\t\tclass=\"dojoComboBox\"\n\t\tdojoAttachPoint=\"downArrowNode\"\n\t\tdojoAttachEvent=\"onMouseUp:handleArrowClick; onResize;\"\n\t\tsrc=\"${this.buttonSrc}\"></img></span>\n<div dojoattachpoint="validationMessage" class="errorMsg"></div></div></div></div>',
   mode : 'local',
   autoComplete : true,
   fieldRequiredClass : "required",
   forceValidOption : true,
   maxListLength : 10
};

hyperic.hq.dom.createDatePicker = function(datePickerName){
    hyperic.hq.dom.datePickerProps.label = datePickerName;
    hyperic.hq.dom.datePickerProps.name = datePickerName;
    hyperic.hq.dom.id = datePickerName;
    var parentNode =  document.createElement('div');
    parentNode.id = dojo.dom.getUniqueId();
    dojo.byId("reportOptions").appendChild(parentNode); 
    var calendarWidget = dojo.widget.createWidget("dropdowndatepicker", hyperic.hq.dom.datePickerProps, parentNode);
    calendarWidget.inputNode.id = calendarWidget.widgetId;
    hyperic.hq.reporting.manager.currentReportOptions.push(calendarWidget);
};

hyperic.hq.dom.createTextBox = function(textboxName){
    hyperic.hq.dom.validationTextboxProps.label = textboxName;
    hyperic.hq.dom.validationTextboxProps.name = textboxName;
    var parentNode =  document.createElement('div');
    parentNode.id = dojo.dom.getUniqueId();
    dojo.byId("reportOptions").appendChild(parentNode); 
    var validationWidget = dojo.widget.createWidget("ValidationTextbox", hyperic.hq.dom.validationTextboxProps, parentNode);
    hyperic.hq.reporting.manager.currentReportOptions.push(validationWidget);
    return validationWidget;
};

hyperic.hq.dom.createSelectBox = function(selectboxName, optionsArray){
    this.option = function(name, value){
        return "<option value='" + value + '">' + name + "</option>";
    };
    var select = document.createElement('select');
    select.id = "temp";
    var option = document.createElement('option'); 
    option.value = "-1";
    option.innerHTML = "All Resources";
    select.appendChild(option);
    for(var i =0; i < optionsArray.length; i++){
        option = document.createElement('option');
        option.value = optionsArray[i].id;
        option.innerHTML = optionsArray[i].name;
        select.appendChild(option);
    }
    dojo.byId("reportOptions").appendChild(select);
    hyperic.hq.dom.selectboxProps.label = selectboxName;
    var selectWidget = dojo.widget.createWidget("ComboBox", hyperic.hq.dom.selectboxProps, select);
    selectWidget.textInputNode.id = selectWidget.widgetId;
    selectWidget.dataProvider.searchLimit = optionsArray.length + 1;
    selectWidget.domNode = selectWidget.textInputNode;
    hyperic.hq.reporting.manager.currentReportOptions.push(selectWidget);
};

hyperic.hq.reporting.manager = { 
    currentReportOptions : [], 
    preSubmit : function(){
        var submit = this.validateReportOptions();
        if(submit){
            this.serializeReportOptions();
            //dojo.byId("ReportingForm").submit();
            var mp = dojo.byId("messagePanel");
            if(mp){
                mp.style.display="none";
            }
            return true;
        }else{
            return false;
        }
    },
    serializeReportOptions : function(){
        var obj = "{";
        for(var i = 0; i < this.currentReportOptions.length; i++){
            if(this.currentReportOptions[i].getDate){
                obj += '"' + this.currentReportOptions[i].name + '":"' +  this.currentReportOptions[i].getDate().getTime() +'",'; 
            }else if(this.currentReportOptions[i].getState){
                var value = this.currentReportOptions[i].comboBoxSelectionValue.value;
                if(dojo.render.html.ie){
                    obj += '"' + this.currentReportOptions[i].label + '":"' + value +'",';
                }else{
                    obj += '"' + this.currentReportOptions[i].label + '":"' + value +'",';
                }
            }else if(this.currentReportOptions[i].textbox){
                obj += '"' + this.currentReportOptions[i].name + '":"' +  this.currentReportOptions[i].getValue() +'",';
            }
        }
        obj += "}";
        dojo.byId("jsonData").value = obj;
    },
    validateReportOptions : function(){
        var submit = true;
        var dates ={};
        for(var i =0; i < this.currentReportOptions.length; i++){
            if(this.currentReportOptions[i].getDate){
                if(this.currentReportOptions[i].label == "Start Date" ||
                    this.currentReportOptions[i].label == "StartDate"){ 
                    dates.StartDate = this.currentReportOptions[i].getDate();
                    dates.StartDateNode = this.currentReportOptions[i];
                }else{
                    dates.EndDate = this.currentReportOptions[i].getDate();
                    dates.EndDateNode = this.currentReportOptions[i];
                }
                if(this.currentReportOptions[i].inputNode.value === ''){
                    this.currentReportOptions[i].fieldWrapper.className += ' error';
                    this.currentReportOptions[i].validationMessage.innerHTML = "&nbsp;-&nbsp;this field is required ";
                    submit = false && submit;
                }else{
                    this.currentReportOptions[i].fieldWrapper.className = 'fieldValue';
                    this.currentReportOptions[i].validationMessage.innerHTML = "";
                    submit = true && submit;                        
                }
            }else if(this.currentReportOptions[i].getState){
                if(this.currentReportOptions[i].getValue() === '' && !this.currentReportOptions[i]._isValidOption()){
                    this.currentReportOptions[i].fieldWrapper.className += ' error';
                    this.currentReportOptions[i].validationMessage.innerHTML = "&nbsp;-&nbsp;this field is required ";
                    submit = false && submit;  
                }else{
                    this.currentReportOptions[i].fieldWrapper.className = 'fieldValue';
                    this.currentReportOptions[i].validationMessage.innerHTML = "";
                    submit = true && submit;
                }
            }else if(this.currentReportOptions[i].getValue && !this.currentReportOptions[i].getState){
                if(this.currentReportOptions[i].textbox.value === ''){
                    this.currentReportOptions[i].fieldWrapper.className += ' error';
                    this.currentReportOptions[i].validationMessage.innerHTML = "&nbsp;-&nbsp;this field is required ";
                    submit = false && submit;                       
                }else{
                    this.currentReportOptions[i].fieldWrapper.className = 'fieldValue';
                    this.currentReportOptions[i].validationMessage.innerHTML = "";
                    submit = true && submit;                        
                }
            }
        }
        if(dates.EndDate && dates.StartDate){
            if(dates.StartDate > dates.EndDate){
                    dates.EndDateNode.fieldWrapper.className += ' error';
                    dates.EndDateNode.validationMessage.innerHTML = '&nbsp;-&nbsp;The "End" date must be earlier than the "Start" date.';
                    submit = false && submit;
            }
        }
        return submit;
    }
};

function resetReportOptions(){
    dojo.byId("reportOptions").innerHTML = "";
    hyperic.hq.reporting.manager.currentReportOptions = [];
    //TODO iterate through and call destroy
}

function selectedChanged(selectNode){
    //get the changed object
    //send to the server
    //inner html the response after checking the validity
    var selected;
    var textNode;
    var textTargetNode = dojo.byId("reportDetails");
    if(selectNode){
        selected = selectNode.options[selectNode.selectedIndex];
        textNode = dojo.byId(selected.value);
        textTargetNode.innerHTML = textNode.innerHTML;
    }
    getReportOptions(selected.value);
}

function getReportOptions(reportName){
    var URL = "/reporting/ReportCenter.do?reportName=" + reportName;
    var request = new dojo.io.Request(URL, "text/plain", "XMLHTTPTransport");
    request.load = function(type, data, evt){
        if(data){ createInputFieldsFromJSON(data); } 
    };
    request.error = function(type, error){};
    dojo.io.bind(request);
}

function createInputFieldsFromJSON(jsonArray){
    resetReportOptions();
    var descriptor = dojo.json.evalJson(jsonArray);
    //for(var key in descriptor){
    var i = 0;
    while(i < descriptor.length){
        // var type = descriptor[key].type;
        var type = descriptor[i].descriptor.type;
        if(type !== undefined){
            var o = descriptor[i].descriptor;
            if(type.indexOf("String") != -1){
                hyperic.hq.dom.createTextBox(o.name);
            }else if(type.indexOf("Date") != -1){
                hyperic.hq.dom.createDatePicker(o.name);
            }else if(type.indexOf("Group") != -1){
                hyperic.hq.dom.createSelectBox(o.name, o.options);
            }
        } 
        i++; 
    }
}


/**
 * @deprecated used only for the struts header
 */
function activateHeaderTab(){
    var l = document.location;
    l = l+""; // force string cast
    if(l.indexOf("Dash")!=-1) {
        dojo.byId("dashTab").className = "activeTab";
    }
    if(l.indexOf("Resou")!=-1){
        dojo.byId("resTab").className = "activeTab";
    }
    if(l.indexOf("rep")!=-1 || l.indexOf("Rep")!=-1 || l.indexOf("masth")!=-1){
        dojo.byId("analyzeTab").className = "activeTab";
    }
    if(l.indexOf("admin")!=-1 || l.indexOf("Adm")!=-1){
        dojo.byId("adminTab").className = "activeTab";
    }
}

hyperic.widget = hyperic.widget || {};

/**
* Chart - timeplot chart manager functionality. Creates and manages dom and datasource for timeplot
*
* @param node
* @param kwArgs
*/
// hyperic.widget.Chart = function(node, kwArgs, tabid, chartPos, singleChart) {
  hyperic.widget.Chart = function(node, kwArgs) {
    var that = this;
    that.subscriptions=[];
    that.create = function(node, kwArgs) {
        
        var template = '<div class="chartCont"> <h3 class="cTitle">' + kwArgs.name + '</h3><div id="widget_chart"></div><div class="xlegend"></div></div>';
        that.template = template;
        // that.tabid = tabid;
        that.node = dojo11.byId(node);
        that.name = kwArgs.name;
        //console.log("created chart: "+kwArgs.name);
        that.node.innerHTML = template;
        // dojo11.byId(node).appendChild(f.firstChild);
        // that.url = kwArgs.url;
        that.data = kwArgs.data;
        // that.chartPos = chartPos;
        //chartObjs[tabid] = that;
        // that.node = dojo11.byId(tabid);
        // that.subscriptions[0] = dojo11.subscribe('tabchange', that, 'showChart');
        //TODO check if the tab that is currently selected is the one that is getting the chart.
        // f=null;
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
                        id : "plot1", dataSource : new Timeplot.ColumnSource(that.dataSource, 1), valueGeometry : new Timeplot.DefaultValueGeometry( {
                            gridColor : "#000000", axisLabelsPlacement : "left" }
                        ), timeGeometry : new Timeplot.DefaultTimeGeometry( {
                            gridColor : new Timeplot.Color("#DDDDDD"), axisLabelsPlacement : "bottom" }
                        ), showValues : true, lineColor : "#00EB08", roundValues:false, //00EB08 89EB0F
                        fillColor : "#00B93A" //#E6FCCA
                        }
                    )];
                    that.chart = Timeplot.create(dojo11.byId("widget_chart"), pi);
                    // that.chart.loadText(that.url, ",", that.dataSource);
                    that.chart.loadJSON(that.data, that.dataSource);                }
                else
                {
                    node_el = dojo11.byId(node);
                    var message = SimileAjax.Graphics.createMessageBubble(node_el.ownerDocument);
                    message.containerDiv.className = "timeline-message-container";
                    node_el.appendChild(message.containerDiv);
                    
                    message.contentDiv.className = "timeline-message";
                    message.contentDiv.innerHTML = '<span style="color: #000; font-size: 12px;">No Data</span>';
                    message.containerDiv.style.display = "block";

                    that.dataSource = new Timeplot.DefaultEventSource();
                    var pi = [Timeplot.createPlotInfo( {
                        id : "plot1", dataSource : new Timeplot.ColumnSource(that.dataSource, 1), valueGeometry : new Timeplot.DefaultValueGeometry( {
                            gridColor : "#000000", axisLabelsPlacement : "left" }
                        ), timeGeometry : new Timeplot.DefaultTimeGeometry( {
                            gridColor : new Timeplot.Color("#DDDDDD"), axisLabelsPlacement : "bottom" }
                        ), showValues : true, lineColor : "#00EB08", roundValues:false, //00EB08 89EB0F
                        fillColor : "#00B93A" //#E6FCCA
                        }
                    )];
                    that.chart = Timeplot.create(dojo11.byId("widget_chart"), pi);
                    that.chart.loadJSON({"2008-08-04T01:08:51-0700":[0],"2008-08-04T01:09:51-0700":[0],"2008-08-04T01:10:51-0700":[0],"2008-08-04T01:11:51-0700":[0],"2008-08-04T01:12:51-0700":[0]}, that.dataSource);
                }
                
                that.isShowing = true;
            }
            delete count;
        // }
    };

    this.cleanup = function(){
        // dojo11.unsubscribe(that.subscriptions[0]);
        
        // destroy all children of the chart container
        while(that.node.lastChild) {
          that.node.removeChild(that.node.lastChild);
        }
        // that.node.parentNode.removeChild(that.node);
        that.node = null;
        };
    //init
    that.isShowing = false;
    // this.create(node, kwArgs, tabid, chartPos);
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
    configSheet: null,
    contentSheet: null,
    config: {},

    /**
     * catches clicks on widget, and passes them on to the handler function 
     * determined by the button's class.
     *
     * @param {Event} e
     * @see #clickHandler
     */
    clickHandler: function(e) {
        if(this['click_' + e.target.className])
        {
            e.stopPropagation();
            e.preventDefault();
            dojo11.stopEvent(e);

            this['click_' + e.target.className](e);
        }
    },
    
    /**
     * clicking the config button shows the config layer 
     * @param {Event} e
     * @see #clickHandler
     */
    click_config_btn: function(e,onEnd)
    {
        this.swapSheets(this.contentSheet,this.configSheet,onEnd);
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
        this.swapSheets(this.configSheet,this.contentSheet,onEnd);
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
    swapSheets: function(from,to,onEnd) {
        var c = dojo11.fx.chain([
            dojo11.fadeOut({
                node : from, 
                onEnd: function() { 
                        dojo11.style(from,'display','none'); 
                        dojo11.style(to,'display','block'); 
                    } 
                }
            ),
            dojo11.fadeIn({
                node : to
                }
            )
            ]);

        if(onEnd)
        {
            dojo11.connect(c,'onEnd',onEnd);
        }
        c.play();
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
    dojo11.forEach(
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
        while(that.length > 0)
        {
            // remove takes an index, and our data array is zero-indexed.
            that.remove(that.length-1);
        }
    };
};

/**
 * chartWidget is a widget that displays a chart slideshow
 * 
 * @author Anton Stroganov <anton@hyperic.com>
 * @base hyperic.dashboard.widget
 * @constructor
 */
hyperic.dashboard.chartWidget = function(node, portletName, portletLabel) {
    var that = this;

    that.configSheet = dojo11.query('.config',node)[0];
    that.contentSheet = dojo11.query('.content',node)[0];
    
    that.chartsearch = dojo11.byId('chartsearch');
    that.chartselect = new hyperic.selectBox(dojo11.byId('chartselect'));
    
    that.play_btn = dojo11.query('.pause_btn',node)[0];
    
    that.chart = null;
    that.charts = [];
    that.cycleId = null;
    that.currentChartId = null;
    that.showing = 'content';
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
        hyperic.dashboard.widget.click_config_btn.apply(this);
        that.showing = 'config';
        
        var input_rotation = dojo11.byId('chart_rotation');
        var input_interval = dojo11.byId('chart_interval');
        var input_range = dojo11.byId('chart_range');
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
        if(that.config.range && that.config.range != dojo11.byId('chart_range').value)
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

        that.config.interval = parseInt(dojo11.byId('chart_interval').value,10);
        that.config.range = dojo11.byId('chart_range').value;
        that.config.rotation = dojo11.byId('chart_rotation').checked ? 'true' : 'false';

        dojo11.xhrGet( {
            url: "/api.shtml?v=1.0&s_id=chart&config=true&tr=" + that.config.range + "&ivl=" + that.config.interval + "&rot=" + that.config.rotation,
            handleAs: 'json',
            load: function(data){
                that.config.interval = parseInt(data.ivl,10) || that.config.interval;
                that.config.range = data.tr || that.config.range;
                that.config.rotation = data.rot || that.config.rotation;
            },
            error: function(data){
                console.debug("An error occurred saving charts config... ", data);
            },
            timeout: 2000
        });


        that.swapSheets(that.configSheet,that.contentSheet,
            function()
            {
                that.showing = 'content';
                if(that.needsResize)
                {
                    that.chartResize();
                }
                if(that.config.rotation == 'true')
                {
                    that.playCharts();
                }
            });
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
        hyperic.dashboard.widget.click_cancel_btn.apply(
            this,
            [
                e,
                function()
                {
                    that.showing = 'content';
                    if(that.needsResize)
                    {
                        that.chartResize();
                    }
                    if(that.config.rotation == 'true')
                    {
                        that.playCharts();
                    }
                }
            ]);
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
        // the last is to filter out the odd chartId's that get passed in in firefox when cycleChart is called from the playback timeout
        if(typeof chartId == 'undefined' || chartId < 0 || chartId >= that.chartselect.length)
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
            that.cycleId = setInterval(that.cycleCharts, parseInt(that.config.interval,10)*1000);

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
     * fetch the chart data from server for a given chart
     */
    that.fetchAndPlayCharts = function()
    {
        dojo11.xhrGet( {
            url: "/api.shtml?v=1.0&s_id=chart",
            handleAs: 'json',
            load: function(data){
                that.charts = data.sort(
                    function(a,b) { 
                        a = a.name.toLowerCase(); 
                        b = b.name.toLowerCase();
                        return a > b ? 1 : (a < b ? -1 : 0);
                    });
                that.populateChartSelect();
                that.playCharts();
            },
            error: function(data){
                console.debug("An error occurred fetching charts: ", data);
            },
            timeout: 2000
        });
    };

    that.fetchChartData = function(chart)
    {
        // console.log('fetching from url ' + "/api.shtml?v=1.0&s_id=chart&rid=" + that.charts[chart].rid + "&mtid=[" + that.charts[chart].mtid + "]");
        return dojo11.xhrGet( {
            url: "/api.shtml?v=1.0&s_id=chart&rid=" + that.charts[chart].rid + "&mtid=[" + that.charts[chart].mtid + "]",
            handleAs: 'json',
            load: function(data){
                // that.charts[chart].data = data;
                if(!data.error && data.length > 0)
                {
                    that.charts[chart].data = data[0].data;
                }
            },
            error: function(data){
                console.debug("An error occurred fetching charts config ", data);
            },
            timeout: 2000
        });
    };

    /**
     * fetch the stored config for the chart dashboard widget
     */
    that.fetchConfig = function()
    {
        // preset defaults
        that.config.interval = 60;
        that.config.range = '1h';
        that.config.rotation = 'true';

        dojo11.xhrGet( {
            url: "/api.shtml?v=1.0&s_id=chart&config=true",
            handleAs: 'json',
            load: function(data){
                that.config.interval = parseInt(data.ivl,10) || that.config.interval;
                that.config.range = data.tr || that.config.range;
                that.config.rotation = data.rot || that.config.rotation;
            },
            error: function(data){
                console.debug("An error occurred fetching charts config ", data);
            },
            timeout: 2000
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
        if(that.showing == 'content')
        {
            if (that.chart != null) {
                that.chart.cleanup();
            }
            dojo11.query('#chart_container',that.contentSheet)[0].style.width = that.contentSheet.offsetWidth - 150;
            that.chart = new hyperic.widget.Chart('chart_container', that.charts[that.currentChartId]);
            that.needsResize = false;
        }
        else
        {
            that.needsResize = true;
        }
    };

    if(that.chartsearch && that.chartselect)
    {
        // connect the onclick event of the whole widget to the clickHandler
        // function of this object, inherited from hyperic.dashboard.widget.
        dojo11.connect(node,'onclick',dojo11.hitch(that,'clickHandler'));

        // set up the event handlers for the live search box
        dojo11.connect(that.chartsearch,'onfocus', that.emptySearch);
        dojo11.connect(that.chartsearch,'onblur', that.resetSearch);
        dojo11.connect(that.chartsearch,'onkeyup',that.search);

        // set up the event handler for the select box
        dojo11.connect(that.chartselect.select,'onclick',that.select);

        // handle resizing of the window
        dojo11.connect(window,'onresize',dojo11.hitch(that, that.chartResize));

        // set the initial size of the chart (widget width -20px for left and right padding -10px for center spacing -120px selectbox)
        dojo11.query('#chart_container',that.contentSheet)[0].style.width = that.contentSheet.offsetWidth - 150;

        that.fetchConfig();
        that.fetchAndPlayCharts();

        // while(true)
        // {
        //     if(undefined !typeof this.charts[0].data)
        //     {
        //         this.playCharts();
        //         break;
        //     }
        // }

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
hyperic.dashboard.summaryWidget = function(node, portletName, portletLabel) {
    var that = this;

	that.max_alerts = 20;

    that.configSheet = dojo11.query('.config',node)[0];
    that.contentSheet = dojo11.query('.content',node)[0];
    
    that.alert_groups = {"data": {}, "count": 0};
    that.selected_alert_groups = [];
    that.alert_group_status = {};

    that.available_alert_groups = new hyperic.selectBox(dojo11.byId('available_alert_groups'));
    that.enabled_alert_groups = new hyperic.selectBox(dojo11.byId('enabled_alert_groups'));
    that.groupsearch = dojo11.byId('groupsearch');

	that.enable_alert_btn = dojo11.query('.enable_alert_btn',that.configSheet)[0];
	that.disable_alert_btn = dojo11.query('.disable_alert_btn',that.configSheet)[0];

    that.tables = {
        lcol: dojo11.query('.lcol table tbody',node)[0],
        rcol: dojo11.query('.rcol table tbody',node)[0]
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
		if(that.enabled_alert_groups.length < that.max_alerts)
		{
	        that.enabled_alert_groups.steal(that.available_alert_groups);

			if(that.enabled_alert_groups.length == that.max_alerts || that.available_alert_groups.length == 0)
			{
			    that.enable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_select_disabled.gif" alt="select">';
				that.enable_alert_btn.disabled = true;
			}
			if(that.disable_alert_btn.disabled === true)
    		{
    		    that.disable_alert_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_deselect.gif" alt="select">';
    			that.disable_alert_btn.disabled = false;
    		}
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
     * cancel button handler
     * reset the available/enabled alert group selectboxes
     * and swap the config layer and show content layer again
     */
    this.click_cancel_btn = function(e)
    {
        hyperic.dashboard.widget.click_cancel_btn.apply(
            this,
            [
                e,
                function()
                {
                    that.enabled_alert_groups.reset();
                    that.available_alert_groups.reset();
                    that.populateAlertGroups();
                }
            ]);
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

        dojo11.xhrGet( {
            url: "/api.shtml?v=1.0&s_id=alert_summary&config=true&rid=[" + that.selected_alert_groups + "]",
            handleAs: 'json',
            load: function(data){
                that.selected_alert_groups = data.rid || that.selected_alert_groups;
                that.alert_groups.data = data.avail || that.alert_groups.data;
                that.alert_groups.count = data.count || that.alert_groups.count;

                that.fetchAlertGroupStatus().addCallback(function(){
                    that.repaintAlertGroups();
                    that.swapSheets(that.configSheet,that.contentSheet);
                });
            },
            error: function(data){
                console.debug("An error occurred saving alerts config... " + data);
                that.swapSheets(that.configSheet,that.contentSheet);
            },
            timeout: 2000
        });
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
        var groups = that.selected_alert_groups.sort(
            function(a,b) {
                a = that.alert_groups.data[a].toLowerCase();
                b = that.alert_groups.data[b].toLowerCase();
                return a > b ? 1 : (a < b ? -1 : 0);
            });
        
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
            
            row.className = ((i < half ? i : i - half) % 2 == 0) ? 'even' : 'odd';
            row.style.display = '';
            row.id = 'alertGroup:' + groups[i];
            var data = that.alert_group_status[groups[i]] || ['gray','gray'];
            var name = that.alert_groups.data[groups[i]];
            if(that.alert_groups.data[groups[i]].length > 20)
            {
                name = '<abbr title="' + name + '">' + name.substring(0,20) + '&hellip;</abbr>';
            }
            row.childNodes[0].innerHTML = '<a href="/Resource.do?eid=5:' + groups[i] + '">' + name +'</a>';
            row.childNodes[1].innerHTML = '<img src="/images/4.0/icons/'+data[0]+'.gif" alt="'+ status[data[0]] +'">';
            row.childNodes[2].innerHTML = '<a href="/alerts/Alerts.do?mode=list&eid=5:' + groups[i] + '"><img src="/images/4.0/icons/'+data[1]+'.gif" alt="'+ status[data[1]]+'" border="0"></a>';
            table.appendChild(row);
            data = name = null;
        }
    };

    /**
     * fetch all available alert groups from server
     * the server should return a json object of the following form:
     * { 
     *     data: { 
     *         "id" : "name",
     *         ...
     *     },
     *     count: 10
     * }
     * the count element shall be the total number of alert groups available
     * (may be greater than number returned)
     *
     * @param {Number} page number
     * @param {String} string to search alerts for
     */
    // that.fetchAlertGroups = function(page, searchString)
    // {
    //     dojo11.xhrGet( {
    //         url: "/api.shtml?v=1.0&s_id=alert_summary&config=true",
    //         handleAs: 'json',
    //         load: function(data){
    //             that.alert_groups.data = data.avail || that.alert_groups.data;
    //             that.alert_groups.count = data.count || that.alert_groups.count;
    //             that.fetchAlertGroupStatus();
    //         },
    //         error: function(data){
    //             console.debug("An error occurred fetching alert groups... ", data);
    //         },
    //         timeout: 2000
    //     });
    // 
    //     // // offset = offset || 0;
    //     // that.alert_groups = { 
    //     //         data: { 
    //     //             1 : "Apache VHosts",
    //     //             2 : "HTTP Serivces",
    //     //             3 : "Linux Boxes",
    //     //             4 : "REST API",
    //     //             5 : "SF Data Center",
    //     //             6 : "Storage 1",
    //     //             7 : "WS API",
    //     //             8 : "Applications",
    //     //             9 : "CentOS Boxes",
    //     //             10 : "SuSE Boxes"
    //     //         },
    //     //         count: 10
    //     //     };
    // };

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
        return dojo11.xhrGet( {
            url: "/api.shtml?v=1.0&s_id=alert_summary",
            handleAs: 'json',
            load: function(data){
                that.alert_group_status = data;
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
                console.debug("An error occurred fetching alert groups status... ", data);
            },
            timeout: 20000
        });
    };

    /**
     * fetch the stored selected alert groups for the dashboard widget
     * the server should return a json array of the alert group id's
     * [ 
     *    "id",
     *    ...
     * ]
     */
    that.fetchConfig = function()
    {
        return dojo11.xhrGet( {
            url: "/api.shtml?v=1.0&s_id=alert_summary&config=true",
            handleAs: 'json',
            load: function(data){
                that.selected_alert_groups = data.rid || [];
                that.alert_groups.data = data.data || that.alert_groups.data;
                // that.selected_alert_groups = ['1','2','3','4','5','6','7'];
                // that.alert_groups = { 
                //         data: { 
                //             '1': "Apache VHosts",
                //             '2': "HTTP Serivces",
                //             '3': "Linux Boxes",
                //             '4': "REST API",
                //             '5': "SF Data Center",
                //             '6': "Storage 1",
                //             '7': "WS API",
                //             '8': "Applications",
                //             '9': "CentOS Boxes",
                //             '10' : "SuSE Boxes"
                //         },
                //         count: 10
                //     };
                // // that.alert_groups.count = data.count || that.alert_groups.count;
            },
            error: function(data){
                console.debug("An error occurred fetching alert group config... ", data);
            },
            timeout: 2000
        });
    };

    if(that.available_alert_groups && that.enabled_alert_groups)
    {
        // connect the onclick event of the whole widget to the clickHandler
        // function of this object, inherited from hyperic.dashboard.widget.
        dojo11.connect(node,'onclick',dojo11.hitch(that,'clickHandler'));

        // set up the event handlers for the live search box
        dojo11.connect(that.groupsearch,'onfocus', that.emptySearch);
        dojo11.connect(that.groupsearch,'onblur', that.resetSearch);
        dojo11.connect(that.groupsearch,'onkeyup',that.search);
        
        // preload 'disabled' view buttons
        var disabled_select = new Image();
        disabled_select.src = '/images/4.0/buttons/arrow_select_disabled.gif';
        var disabled_deselect = new Image();
        disabled_deselect.src = '/images/4.0/buttons/arrow_deselect_disabled.gif';
        
        that.fetchConfig().addCallback(
            function() {
                that.fetchAlertGroupStatus().addCallback(
                    function() {
                        that.populateAlertGroups();
                        that.paintAlertGroups();
                    });
            });
    }
};

// set the hyperic.dashboard.widget as the ancestor of the summaryWidget class.
hyperic.dashboard.summaryWidget.prototype = hyperic.dashboard.widget;

hyperic.maintenance_schedule = function(group_id, group_name) {
    var that = this;
    that.existing_schedule = {};
    that.group_id = group_id;
    that.group_name = group_name;
    that.title_name = that.group_name;
    that.dialog = null;
	that.buttons = {};
	that.inputs = {};
	that.canSchedule = true;
    that.selected_from_time = that.selected_to_time = new Date();
    that.server_time = new Date(); // default
    
    //dojo11.date.getTimezoneName(new Date())
    
    that.message_area = {
    	request_status : dojo11.byId('maintenance_status_' + that.group_id),
    	schedule_status : dojo11.byId('existing_downtime_' + that.group_id)
    };

	that.messages = {
		success : "Updated successfully.",
		serverError : "An error occurred processing your request.",
		currentSchedule : "The current downtime schedule for this group is:",
		noSchedule : "No downtime is currently scheduled for this group.",
		runningSchedule : "Downtime currently in progress..."
	};

    that.init = function() {
	    if(!that.dialog){
			if(that.title_name.length > 22) {
				that.title_name = that.title_name.substring(0,22) + "...";
			}
	    	var pane = dojo11.byId('maintenance' + that.group_id);
			pane.style.width = "450px";
			that.dialog = new dijit11.Dialog({
				id: "maintenance_schedule_dialog_" + that.group_id,
				refocus: true,
				autofocus: false,
				title: "Schedule Downtime - " + that.title_name
			},pane);
		}
        
        that.inputs.from_date = new dijit11.form.DateTextBox({
    			name: "from_date",
    			value: that.selected_from_time,
    			constraints: {
                    datePattern: 'MM/dd/y'},
    			lang: "en-us",
    			promptMessage: "mm/dd/yyyy",
    			rangeMessage: "The downtime start date cannot be in the past.",
    			invalidMessage: "Invalid date. Use mm/dd/yyyy format.",
    			required: true
    		}, "from_date");

        that.inputs.to_date = new dijit11.form.DateTextBox({
    			name: "to_date",
    			value: that.selected_to_time,
    			constraints: {
                    datePattern: 'MM/dd/y'},
    			lang: "en-us",
    			promptMessage: "mm/dd/yyyy",
    			rangeMessage: "The downtime end date cannot be earlier than the start date.",
    			invalidMessage: "Invalid date. Use mm/dd/yyyy format.",
    			required: true
    		}, "to_date");

        that.inputs.from_time = new dijit11.form.TimeTextBox({
    			name: "from_time",
    			value: that.selected_from_time,
    			lang: "en-us",
                rangeMessage: "The downtime start time must be later than the current time.",
    			invalidMessage: "The downtime start time is required.",
    			required: true
    		}, "from_time");

        that.inputs.to_time = new dijit11.form.TimeTextBox({
    			name: "to_time",
    			value: that.selected_to_time,
    			lang: "en-us",
                rangeMessage: "The downtime end time must be later than the start time.",
    			invalidMessage: "The downtime end time is required.",
    			required: true
    		}, "to_time");

		that.buttons.schedule_btn = new dijit11.form.Button({
			label: "Schedule",
			name: "schedule_btn",
			id: "schedule_btn",
			type: 'button'
		}, "schedule_btn");

        dojo11.connect(that.buttons.schedule_btn, 'onClick', that.schedule_action);

		that.buttons.cancel_btn = new dijit11.form.Button({
			label: "Cancel",
			name: "cancel_btn",
			id: "cancel_btn",
			type: 'cancel'
		}, "cancel_btn");
		dojo11.connect(that.buttons.cancel_btn, 'onClick', that.dialog.onCancel);

		that.buttons.clear_schedule_btn = new dijit11.form.Button({
			label: "Clear schedule",
			name: "clear_schedule_btn",
			id: "clear_schedule_btn",
			type: 'button'
		}, "clear_schedule_btn");
        dojo11.connect(that.buttons.clear_schedule_btn, 'onClick', that.clear_schedule_action);

    };

    that.schedule_action = function() {
		that.updateConstraints();
	
    	// validate with updated constraints
    	if(that.dialog.validate())
        {
            var args = that.dialog.getValues();

    	    // create unix epoch datetime in GMT timezone
            from_datetime = (args.from_date.getTime() + args.from_time.getTime() - args.from_time.getTimezoneOffset() * 60000);

            to_datetime = (args.to_date.getTime() + args.to_time.getTime() - args.to_time.getTimezoneOffset() * 60000);

            return dojo11.xhrPost( {
                url: "/api.shtml",
                content: {v: "1.0", s_id: "maint_win", groupId: that.group_id, sched: "true", startTime: from_datetime, endTime: to_datetime},
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
	
	                        that.redraw(false, that.messages.currentSchedule, that.messages.success);
                    	}
                    }
                    else
                    {
                    	that.displayError(that.messages.serverError);
                        console.debug(data.error);
                    }                    	
                },
                error: function(data){
                	that.displayError(that.messages.serverError);
                	console.debug("An error occurred setting maintenance schedule for group " + that.group_id, data);
                },
                timeout: 2000
            });
        }
    };
    
    that.clear_schedule_action = function() {
        return dojo11.xhrPost( {
            url: "/api.shtml",
            content: {v: "1.0", s_id: "maint_win", groupId: that.group_id, sched: "false"},
            handleAs: 'json',
            load: function(data){
                if(data && !data.error)
                {
            		that.server_time = new Date(data.serverTime);
                	that.resetSchedule();
					that.redraw(false, that.messages.noSchedule, that.messages.success);
				}
                else
                {
                	that.displayError(that.messages.serverError);                	
                }
            },
            error: function(data){
            	that.displayError(that.messages.serverError);
            	console.debug("An error occurred clearing maintenance schedule for group " + that.group_id, data);
            },
            timeout: 2000
        });
    };
    
    that.getSchedule = function() {
        return dojo11.xhrGet( {
            url: "/api.shtml",
            content: {v: "1.0", s_id: "maint_win", groupId: that.group_id},
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
                    	                    
                    	that.redraw(true, that.messages.currentSchedule);
                    	
                        if(data.state == 'running')
                    	{
                    		that.displayError(that.messages.runningSchedule);
                    	}
                	} 
        			else
        			{
                        that.resetSchedule();
                    	that.redraw(true, that.messages.noSchedule);        			
        			}
                }
                else
                {
                    that.resetSchedule();
                	that.redraw(true, "&nbsp;");        			
                	that.displayError(that.messages.serverError);
                }
            },
            error: function(data){
            	that.displayError(that.messages.serverError);
            	console.debug("An error occurred fetching maintenance schedule for group " + that.group_id, data);
            },
            timeout: 2000
        });
    };
    
    that.resetSchedule = function() {
		var curdatetime = new Date();
    	that.existing_schedule = {};
	    that.selected_from_time = new Date(curdatetime.getFullYear(), curdatetime.getMonth(), curdatetime.getDate(), curdatetime.getHours()+1);
	    that.selected_to_time = new Date(that.selected_from_time.getTime() + (60*60000));    	
	    that.deleteConstraints();    	
    }
    
    that.deleteConstraints = function() {
    	for(var inputName in that.inputs)
    	{
    		delete that.inputs[inputName].constraints.min;
    		delete that.inputs[inputName].constraints.max;
    	}
    }
    
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
    
    that.displayError = function(msg)
    {
		that.message_area.request_status.className = 'warningPanel';
		that.message_area.request_status.innerHTML = msg;
		that.message_area.request_status.style.display = '';   	
    }
    
    that.redraw = function(show, scheduleStatus, confirmationStatus)
    {    	
    	that.deleteConstraints();
    	
    	that.inputs.from_date.setValue(that.selected_from_time);
		that.inputs.from_time.setValue(that.selected_from_time);
		that.inputs.to_date.setValue(that.selected_to_time);
		that.inputs.to_time.setValue(that.selected_to_time);
				
        if(that.existing_schedule.from_time)
        {
        	that.buttons.clear_schedule_btn.domNode.style.display = '';
            that.buttons.schedule_btn.setLabel('Reschedule');
            
            if(that.selected_from_time.getTime() < that.server_time.getTime())
            {
            	that.inputs.from_date.setAttribute('disabled', 'disabled');
            	that.inputs.from_time.setAttribute('disabled', 'disabled');
            }
        }
        else
        {
        	that.buttons.clear_schedule_btn.domNode.style.display = 'none';
            that.buttons.schedule_btn.setLabel('Schedule');        	
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
        	setTimeout('maintenance_' + that.group_id + '.dialog.hide()', 2000);
        }
    };
    
	that.init();
};

hyperic.clone_resource_dialog = function(platform_id) {
    var that = this;
    that.dialog = null;
    that.data = {};
	that.buttons = {};
	that.platform_id = platform_id || null;

    that.available_clone_targets = new hyperic.selectBox(dojo11.byId('available_clone_targets'));
    that.selected_clone_targets = new hyperic.selectBox(dojo11.byId('selected_clone_targets'));
    
    that.searchbox = dojo11.byId('cln_search');

    that.init = function() {
	    if(!that.dialog){
			var pane = dojo11.byId('clone_resource_dialog');
			pane.style.width = "450px";
			that.dialog = new dijit11.Dialog({
				id: "clone_resource_dialog",
				refocus: true,
				autofocus: false,
				title: "Clone Server"
			},pane);
		}

		that.buttons.create_btn = new dijit11.form.Button({
			label: "Queue for Cloning",
			name: "clone_btn",
			id: "clone_btn",
			type: 'submit'
		}, "clone_btn");
        dojo11.connect(that.buttons.create_btn, 'onClick', that.clone_action);

		that.buttons.cancel_btn = new dijit11.form.Button({
			label: "Cancel",
			name: "create_cancel_btn",
			id: "create_cancel_btn",
			type: 'cancel'
		}, "clone_cancel_btn");
		dojo11.connect(that.buttons.cancel_btn, 'onClick', that.cancel_action);

        that.buttons.add_clone_btn = dojo11.byId('add_clone_btn');
        that.buttons.remove_clone_btn = dojo11.byId('remove_clone_btn');
		dojo11.connect(
		    that.buttons.add_clone_btn, 
		    'onclick', 
		    function(e) { 
		        that.selected_clone_targets.steal(that.available_clone_targets);

        		if(that.buttons.remove_clone_btn.disabled === true)
        		{
        		    that.buttons.remove_clone_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_deselect.gif" alt="select">';
        			that.buttons.remove_clone_btn.disabled = false;
        		}

        		if(that.available_clone_targets.length == 0)
        		{
        		    that.buttons.add_clone_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_select_disabled.gif" alt="select">';
        			that.buttons.add_clone_btn.disabled = true;
        		}
		    }
		);
		dojo11.connect(
		    dojo11.byId('remove_clone_btn'), 
		    'onclick', 
		    function(e) { 
		        that.available_clone_targets.steal(that.selected_clone_targets);

        		if(that.buttons.add_clone_btn.disabled === true)
        		{
        		    that.buttons.add_clone_btn.disabled.innerHTML = '<img src="/images/4.0/buttons/arrow_select.gif" alt="select">';
        			that.buttons.add_clone_btn.disabled.disabled = false;
        		}

        		if(that.selected_clone_targets.length == 0)
        		{
        		    that.buttons.remove_clone_btn.innerHTML = '<img src="/images/4.0/buttons/arrow_deselect_disabled.gif" alt="select">';
        			that.buttons.remove_clone_btn.disabled = true;
        		}
		    }
		);

        // search box connections
        dojo11.connect(that.searchbox,'onfocus', function(e) {if(e.target.value == '[ Resources ]') { e.target.value = ''; }});
        dojo11.connect(that.searchbox,'onblur', function(e) {if(e.target.value == '') { e.target.value = '[ Resources ]'; }});
        dojo11.connect(that.searchbox,'onkeyup',function(e) { that.available_clone_targets.search(e.target.value);});
        dojo11.connect(that.searchbox,'onkeyup',function(e) { that.selected_clone_targets.search(e.target.value);});

		that.fetchData();
    };

    that.fetchData = function() {
        dojo11.xhrGet( {
            url: "/api.shtml",
            content: {v: "1.0", s_id: "clone_platform", pid: that.platform_id},
            preventCache: true,
            handleAs: 'json',
            load: function(data){
                if(data && !data.error)
                {
                    that.data = data;
                    that.populateCloneTargets();
                }
            },
            error: function(data){
                console.debug("An error occurred fetching alert groups status... ", data);
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
    };

    that.cancel_action = function() {
        // hide the dialog
        that.dialog.onCancel();

        // reset the select boxes
        that.selected_clone_targets.reset();
        that.available_clone_targets.reset();
        that.populateCloneTargets();
    };

    that.clone_action = function() {
        
        var clone_target_ids = [];
        for(var i = 0, j = that.selected_clone_targets.length; i < j; i++)
        {
            clone_target_ids.push(that.selected_clone_targets.select.options[i].value);
        }
        if(clone_target_ids.length > 0)
        {
            dojo11.xhrPost( {
                url: "/api.shtml",
                content: {v: "1.0", s_id: "clone_platform", pid: that.platform_id, clone: "true", ctid: "[" + clone_target_ids.toString() + "]"},
                handleAs: 'json',
                load: function(data){
                },
                error: function(data){
                    console.debug("An error occurred queueing platforms for cloning " + that.platform_id, data);
                }
            });
			that.dialog.hide();
        }
        else
        {
            that.dialog.onCancel();
        }
    };

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
    var node = dojo.byId("rightPanel");
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

    format = format.replace("z",dojo11.date.getTimezoneName(date));

    var hours = date.getHours();       
    if (format.indexOf("t") > -1)
    {
       if (hours > 11)
       {
           format = format.replace("t","pm");
       }
       else
       {
           format = format.replace("t","am");
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

hyperic.MetricsUpdater = function(eid,ctype,messages) {
    that = this;
    that.attributes = ["min", "average", "max", "last", "avail"];
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
            dojo11.xhrGet( {
                url: url,
                handleAs: "json",
                timeout: 5000,
                load: function(data, ioArgs) {
                    console.log(data);
                    that.lastUpdate = now;
                    that.refreshTimeout = setTimeout( that.update, parseInt(that.refreshInterval,10)*1000 );
                    for (var i = 0; i < data.objects.length; i++) {
                        that.setValues(data.objects[i]);
                    }
                    // Update the time
                    if (dojo.byId('UpdatedTime') !== null) {
                        dojo.byId('UpdatedTime').innerHTML = messages.LastUpdated + now.toLocaleString();
                    }
                },
                error: function(data){
                    console.debug("An error occurred refreshing metrics:");
                    console.debug(data);
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
        var lastSpan = dojo11.byId(attribute + metric);
        console.log(lastSpan);
        console.log(attribute + metric);
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
                        dojo.byId('refresh' + i).innerHTML = that.refreshRates[i];
                    }
                    else
                    {
                        dojo.byId('refresh' + i).innerHTML = '<a href="javascript:" onclick="metricsUpdater.setRefresh('+ i +');">' + that.refreshRates[i] + '</a>';
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
