var tapestry={
    isIE: dojo.isIE,
    // setup firebug logging - useful for finding unimplemented methods
    log: function() {                   
        if (window.console) console.log.apply(this, arguments);
    },
    /**
     * Executes the passed function when the document has done loading
     */
    addOnLoad: function(){dojo.addOnLoad.apply(this,arguments);},
    /**
     * Returns the dom node with the given id
     */
    byId: dojo.byId,

    /**
     * Makes sure that the given namespace (passed as a string) exists
     * and is a valid javascript object.  
     */
    provide: dojo.provide,

    /**
     * Connects the event of the given target node to the given function of
     * the global namespace "tapestry".
     * Users of this method usually attach custom methods to the tapestry namespace
     * before calling this.
     *
     * Parameters: target, event, funcName
     */
    connect: function(target, event, funcName) {
        target = dojo.byId(target);
        tapestry['h_'+funcName] = dojo.connect(target, event, tapestry, funcName);        
    },
    
    /**
     * Function: connectBefore
     */
    connectBefore:function(target, event, funcName){
            target = dojo.byId(target);
            var original = target[event];
            target[event] = function() {
                tapestry[funcName].apply(this, arguments);
                original.apply(this, arguments);
            };
    },    

    /**
     * Disconnects the event of the given target node from the given function of
     * the global namespace "tapestry"
     *
     * Parameters: target, event, funcName
     */
    cleanConnect: function(target, event, funcName) {
        dojo.disconnect(tapestry['h_'+funcName]);
    },

	/**
	 * Perform an XHR.
	 * Implementation should set the mime-type to either "text/xml" or
	 * "text/json" and include the request headers described in the comments to the
	 * json parameter.
     * Implementations are also responsible for handling the responses.  
	 *
	 * Parameters:
	 * 	url     - The url to bind the request to.
	 * 	content - A properties map of optional extra content to send.
	 *  json    - (Optional) Boolean parameter specifying whether or not to create a
	 * 		    json request. If true, the request headers should include "json":true.
	 *          If false or unspecified, they should contain "dojo-ajax-request":true
	 */
    bind: function(url, content, json){
        tapestry.log('t.bind', arguments);
        
        var parms = {
            url:url,
            content:content,
            useCache:true,
            preventCache:true,
            encoding: tapestry.requestEncoding,
            error: (function(){tapestry.error.apply(this, arguments);})
        };

        // setup content type
        if (typeof json != "undefined" && json) {
            parms.handleAs = "json";
            parms.headers={"json":true};
            parms.load=(function(){tapestry.loadJson.apply(this, arguments);});
        } else {
            parms.handleAs = "xml";
            parms.headers={"dojo-ajax-request":true};
            parms.load=(function(){tapestry.load.apply(this, arguments);});
        }
        dojo.xhrGet(parms);        
    },
    
    error: function(){
        tapestry.log('error');
    },
    
    load: function(data, args){
        if (!data) {
                tapestry.log("No data received in response.");
                return;
        }

        var resp=data.getElementsByTagName("ajax-response");
        if (!resp || resp.length < 1 || !resp[0].childNodes) {
                tapestry.log("No ajax-response elements received.");
                return;
        }

        var elms=resp[0].childNodes;
        var bodyScripts=[];
        var initScripts=[];
        var rawData=[];
        for (var i=0; i<elms.length; i++) {
            var elmType=elms[i].getAttribute("type");
            var id=elms[i].getAttribute("id");

            if (elmType == "exception") {
                    dojo.log.err("Remote server exception received.");
                    tapestry.presentException(elms[i], kwArgs);
                    return;
            } else if (elmType == "page") {
                window.location=elms[i].getAttribute("url");
                return;
            } else if (elmType == "status") {
                dojo.publish(id, {message: tapestry.html.getContentAsString(elms[i])});
                continue;
            }

            // handle javascript evaluations
            if (elmType == "script") {

                    if (id == "initializationscript") {
                            initScripts.push(elms[i]);
                            continue;
                    } else if (id == "bodyscript") {
                            bodyScripts.push(elms[i]);
                            continue;
                    } else if (id == "includescript") {
                            // includes get processed immediately (synchronously)
                            var includes=elms[i].getElementsByTagName("include");
                            if (!includes){continue;}
                            for (var e=0; e<includes.length;e++) {
                                    tapestry.loadScriptFromUrl(includes[e].getAttribute("url"));
                            }
                            continue;
                    }
            } else {
                    rawData.push(elms[i]);
            }

            if (!id){
                    tapestry.log("No element id found in ajax-response node.");
                    continue;
            }

            var node=dojo.byId(id);
            if (!node) {
                    tapestry.log("No node could be found to update content in with id " + id);
                    continue;
            }

            tapestry.loadContent(id, node, elms[i]);
        }

        // load body scripts before initialization
        for (var i=0; i<bodyScripts.length; i++) {
                tapestry.loadScriptContent(bodyScripts[i], true);
        }

        for (var i=0; i<rawData.length; i++) {
                tapestry.loadScriptContent(rawData[i], true);
        }

        for (var i=0; i<initScripts.length; i++) {
                tapestry.loadScriptContent(initScripts[i], true);
        }        
    },
    
    loadJson: function(type, data, http, kwArgs){        
    },
    
    loadContent: function(id, node, element){
        if (typeof element.childNodes != "undefined" && element.childNodes.length > 0) {
            for (var i = 0; i < element.childNodes.length; i++) {
                if (element.childNodes[i].nodeType != 1) { continue; }

                var nodeId = element.childNodes[i].getAttribute("id");
                if (nodeId) {
                    element=element.childNodes[i];
                    break;
                }
            }
        }
        
        tapestry.log('dojo.event.browser.clean replacement???');
        //dojo.event.browser.clean(node); // prevent mem leaks in ie
    	
         var content=tapestry.html.getContentAsString(element); 
        // fix for IE - setting innerHTML does not work for SELECTs
        if (tapestry.isIE && node.outerHTML && node.nodeName == "SELECT") {
            node.outerHTML = node.outerHTML.replace(/(<SELECT[^<]*>).*(<\/SELECT>)/, '$1' + content + '$2');
            node=tapestry.byId(id);
        } else if (content && content.length > 0){
            node.innerHTML=content;
        }

        // copy attributes
		var atts=element.attributes;
		var attnode, i=0;
		while((attnode=atts[i++])){
			if(tapestry.isIE){
				if(!attnode){ continue; }
				if((typeof attnode == "object")&&
					(typeof attnode.nodeValue == 'undefined')||
					(attnode.nodeValue == null)||
					(attnode.nodeValue == '')){
					continue;
				}
			}

			var nn = attnode.nodeName;
			var nv = attnode.nodeValue;
			if (nn == "id" || nn == "type" || nn == "name"){continue;}

			/*if (nn == "style") {
				dojo.html.setStyleText(node, nv);
			} else */if (nn == "class") {
				dojo.addClass(node, nv);
			} else if (nn == "value") {
                node.value = nv;
            } else {
                node.setAttribute(nn, nv);
            }
        }

    	// apply disabled/not disabled
    	var disabled = element.getAttribute("disabled");
        if (!disabled && node["disabled"]) {
            node.disabled = false;
        } else if (disabled) {
            node.disabled = true;
        }
    },    
    
    loadScriptContent: function(element, async){
        tapestry.log('loadScriptContent', arguments);
        var text=tapestry.html.getContentAsString(element);
        
        tapestry.log(text);
    },
    
    loadScriptFromUrl: function(){
        tapestry.log('loadScriptFromUrl', arguments);
    },    

    /**
     * Helper that builds the content from eventName and (triggered) id and then forwards
     * execution to tapestry.bind
     * 
     * @param url
     * @param id
     * @param json
     * @param eventName
     */
    linkOnClick: function(url, id, isJson, eventName) {
        var content={beventname:(eventName || "onClick")};
        content["beventtarget.id"]=id;
        tapestry.bind(url, content, isJson);
        return false;        
    }
};

tapestry.form = {
    forms:{},
    currentFocus:null,
    /**
     * Submits the specified form.
     * Should check the value of form.submitmode to find out what type of
     * submission (cancel, refresh or normal) to do and whether to run client validation.
     *
     * Parameters:
     * form			-	The form or form id to submit.
     * submitName	- 	(Optional) Submit name to use when submitting. This is used
     * 					to associate a form submission with a particular component.
     *                  Implementations will typically just set form.submitname to this value.
     * parms		-	(Optional) Extra set of parameters. Implementations can just look for
     *                  the async key and if that's set to true, they should perform an async
     *                  submission.
     */
    submit:function(form, submitName, parms){
        form=dojo.byId(form);
        if (!form) {
                tapestry.log("Form not found with id " + form);
                return;
        }
        var id=form.getAttribute("id");
        if (submitName){
            form.submitname.value = submitName;
        }

        if (dojo.exists("value", form.submitmode)
                && (form.submitmode.value == "cancel" || form.submitmode.value == "refresh")
                && !parms) {
            form.submit();
            return;
        }

        if (!tapestry.form.validation.validateForm(form, this.forms[id]))
            return;

        if (parms && dojo.exists("async", parms) && parms.async) {
            tapestry.form.submitAsync(form, null, submitName, parms);
            return;
        } else if(dojo.exists(id, this.forms) && this.forms[id].async){
            tapestry.form.submitAsync(form);
            return;
        }
        
        form.submit();
    },

    /** Same as submit, but forces cancel submission */
    cancel: function(formId, submitName, parms) {tapestry.log('t.f.submit', arguments);},

    /** Same as submit, but forces refresh submission */
    refresh: function(formId, submitName, parms) {tapestry.log('t.f.submit', arguments);},

    /**
	 * Registers a form and allows definition of its properties.
	 * Implementation should keep track of such properties and
	 * use them later on, when the form is submitted.
	 *
	 * Parameters:
	 *	id		-	The form or form id to register.
	 *  async	-	Boolean, if true form submission should be asynchronous.
	 *  json	-	Boolean, if true form submission should be asyncrhronous json.
	 */
    registerForm: function(id, async, json) {
        var form=dojo.byId(id);
        if (!form) {
                dojo.raise("Form not found with id " + id);
                return;
        }

        // make sure id is correct just in case node passed in has only name
        id=form.getAttribute("id");

        // if previously connected, cleanup and reconnect
        if (this.forms[id]) {
            //dojo.disconnect(form, "onsubmit", this, "onFormSubmit");
            for(var i = 0; i < form.elements.length; i++) {
                var node = form.elements[i];
                if(node && node.type 
                    && (input.type.toLowerCase()=="submit" || input.type.toLowerCase()=="button")) {
                        //dojo.disconnect(node, "onclick", tapestry.form, "inputClicked");
                }
            }

            var inputs = form.getElementsByTagName("input");
            for(var i = 0; i < inputs.length; i++) {
                    var input = inputs[i];
                    if(input.type.toLowerCase() == "image" && input.form == form) {
                            //dojo.disconnect(input, "onclick", tapestry.form, "inputClicked");
                    }
            }

            //dojo.disconnect(form, "onsubmit", this, "overrideSubmit");
            delete this.forms[id];
        }

        this.forms[id]={};
        this.forms[id].validateForm=true;
        this.forms[id].profiles=[];
        this.forms[id].async=(typeof async != "undefined") ? async : false;
        this.forms[id].json=(typeof json != "undefined") ? json : false;

        if (!this.forms[id].async) {
            dojo.connect(form, "onsubmit", this, "onFormSubmit");
        } else {
            for(var i = 0; i < form.elements.length; i++) {
                var node = form.elements[i];
                if(node && node.type 
                    && (node.type.toLowerCase()=="submit" || node.type.toLowerCase()=="button")) {
                        dojo.connect(node, "onclick", tapestry.form, "inputClicked");
                }
            }

            var inputs = form.getElementsByTagName("input");
            for(var i = 0; i < inputs.length; i++) {
                var input = inputs[i];
                if(input.type.toLowerCase() == "image" && input.form == form) {
                        dojo.connect(input, "onclick", tapestry.form, "inputClicked");
                }
            }

            dojo.connect(form, "onsubmit", this, "overrideSubmit");
        }        
    },
    
    overrideSubmit: function(e){
        dojo.stopEvent(e);
        var elm = e.target;
        if (dojo.exists("form", elm)){
            elm = elm.form;
        }
        tapestry.form.submitAsync(elm);
    },
    
    inputClicked:function(e){
        var node = e.currentTarget;
        if(node.disabled || !dojo.exists("form", node)) { return; }
        this.forms[node.form.getAttribute("id")].clickedButton = node;
    },
    
    onFormSubmit:function(evt){
        if(!evt || !dojo.exists("target", evt)) {
            tapestry.log("No valid form event found with argument: " + evt);
            return;
        }

        var id=evt.target.getAttribute("id");
        if (!id) {
                tapestry.log("Form had no id attribute.");
                return;
        }
        var form = dojo.byId(id);

        if (dojo.exists("value", form.submitmode)
                && (form.submitmode.value == "cancel" || form.submitmode.value == "refresh")) {
                return;
        }

        if (!tapestry.form.validation.validateForm(form, this.forms[id])) {
                dojo.stopEvent(evt);
        }
    },
    
    /**
     * Function: setFormValidating
     * 
     * If a form registered with the specified formId
     * exists a local property will be set that causes
     * validation to be turned on/off depending on the argument.
     * 
     * Parameters:
     * 
     * formId - The id of the form to turn validation on/off for.
     * validate - Boolean for whether or not to validate form, if not specified assumes true.
     */
    setFormValidating:function(formId, validate){
        if (this.forms[formId]){
            this.forms[formId].validateForm = validate;
        }
    },
    
    submitAsync:function(form, content, submitName, parms){
        form=dojo.byId(form);
        if (!form) {
                tapestry.log("Form not found with id " + id);
                return;
        }
        var formId=form.getAttribute("id");

        if (!tapestry.form.validation.validateForm(form, this.forms[formId])) {
                tapestry.log("Form validation failed for form with id " + formId);
                return;
        }

        if (submitName){
            var previous = form.submitname.value;
            form.submitname.value=submitName;
            if(!content){ content={}; }
            if(form[submitName]){
                    content[submitName]=form[submitName].value;
            }
        }
		
        // handle submissions from input buttons
        if (dojo.exists("clickedButton", this.forms[formId])) {
            if (!content) { content={}; }
            content[this.forms[formId].clickedButton.getAttribute("name")]=this.forms[formId].clickedButton.getAttribute("value");
            delete this.forms[formId].clickedButton;
        }

        var kwArgs={
            form:form,
            content:content,
            useCache:true,
            preventCache:true,
            error: (function(){tapestry.error.apply(this, arguments);}),
            encoding: tapestry.requestEncoding
        };
		
        // check for override
        if (parms){
            if (dojo.exists("url", parms)) { kwArgs.url=parms.url; }
        }

        if (this.forms[formId].json || parms && parms.json) {
            kwArgs.headers = {"json":true};
            kwArgs.handleAs = "json";
            kwArgs.load = (function(){tapestry.loadJson.apply(this, arguments);});
        } else {
            kwArgs.headers = {"dojo-ajax-request":true};
            kwArgs.handleAs = "xml";
            kwArgs.load = (function(){tapestry.load.apply(this, arguments);});
        }

        dojo.xhrPost(kwArgs);

        if (submitName){
            form.submitname.value = previous;
        }
    },    

    /**
     * Registers a form validation/translation profile.
     * TODO: Describe profile structure.
     *
	 * Parameters:
	 *	formId		-	The form or form id to register profile with.
	 *	profile	    -	The object containing all of the validation/value constraints for the form.
     */    
    registerProfile: function(id, profile) {
        if (!this.forms[id]) return;
        this.forms[id].profiles.push(profile);
    },

	/**
	 * Clears any previously registered validation profiles on the specified form.
	 *
	 * Parameters:
	 *	formId      -   The form id to clear profiles for.
	 */
    clearProfiles: function(id) {
        if (!this.forms[id]) return;
		
        for (var i=0; i < this.forms[id].profiles.length; i++) {
                delete this.forms[id].profiles[i];
        }
        this.forms[id].profiles=[];
     },

    /**
     * Brings keyboard input focus to the specified field.
     */
    focusField: function(field) {
        tapestry.log('t.f.focusField', arguments);
        try{
            field = dojo.byId(field);
            if (field.disabled || field.clientWidth < 1)
			return;        
            if(dojo.exists("focus", field)){
                field.focus();
                return;
            }            
        } catch(e){}
    },

    // TODO: Describe validation methods
    datetime: {
        isValidDate: function(date) {tapestry.log('t.f.d.isValidDate', arguments);return true;}        
    },
    
    validation: {
        isReal: function() {tapestry.log('t.f.v.isReal', arguments);return true;},
        greaterThanOrEqual: function() {tapestry.log('t.f.v.greaterThanOrEqual', arguments);return true;},
        lessThanOrEqual: function() {tapestry.log('t.f.v.lessThanOrEqual', arguments);return true;},
        isText: function() {tapestry.log('t.f.v.isText', arguments);return true;},
        isEmailAddress: function() {tapestry.log('t.f.v.isEmailAddress', arguments);return true;},
        isValidPattern: function() {tapestry.log('t.f.v.isValidPattern', arguments);return true;},
        validateForm: function() {tapestry.log('t.f.v.validateForm', arguments);return true;}
    }
};

tapestry.event = {
    buildEventProperties:function(event, props, args){
       if (!props) props={};

       var isEvent = (typeof event != "undefined") && (event)
           && (typeof Event != "undefined") && (event.eventPhase);
       if (isEvent) {
           if(event["type"]) props.beventtype=event.type;
           if(event["keys"]) props.beventkeys=event.keys;
           if(event["charCode"]) props.beventcharCode=event.charCode;
           if(event["pageX"]) props.beventpageX=event.pageX;
           if(event["pageY"]) props.beventpageY=event.pageY;
           if(event["layerX"]) props.beventlayerX=event.layerX;
           if(event["layerY"]) props.beventlayerY=event.layerY;

           if (event["target"]) 
               this.buildTargetProperties(props, event.target);
       }

       props.methodArguments = dojo.toJson( args );

       return props;
   },
   
   buildTargetProperties:function(props, target){
       if(!target) { return; }

       var isNode = target.nodeType && target.cloneNode;
       if (isNode) {
           return this.buildNodeProperties(props, target);
       } else {
           dojo.raise("buildTargetProperties() Unknown target type:" + target);
       }
   }, 
   
   buildNodeProperties:function(props, node) {
       if (node.getAttribute("id")) {
           props["beventtarget.id"]=node.getAttribute("id");
       }
   },   
       
       
    stopEvent: dojo.stopEvent
};

tapestry.widget = {
    synchronizeWidgetState: function() {tapestry.log('t.w.synchronizeWidgetState', arguments);}
};

/**
 * package: tapestry.html
 * Provides functionality related to parsing and rendering dom nodes.
 */
tapestry.html={

    CompactElementRegexp:/<([a-zA-Z](?!nput)[a-zA-Z]*)([^>]*?)\/>/g, // regexp for compact html elements
    CompactElementReplacer:'<$1$2></$1>', // replace pattern for compact html elements

    /**
     * Function: getContentAsString
     *
     * Takes a dom node and returns its contents rendered in a string.
     *
     * The resulting string does NOT contain any markup (or attributes) of
     * the given node - only child nodes are rendered and returned.Content
     *
     * Implementation Note: This function tries to make use of browser
     * specific features (the xml attribute of nodes in IE and the XMLSerializer
     * object in Mozilla derivatives) - if those fails, a generic implementation
     * is used that is guaranteed to work in all platforms.
	 *
	 * Parameters:
	 *
	 *	node - The dom node.
	 * Returns:
	 *
	 * The string representation of the given node's contents.
	 */
	getContentAsString:function(node){
		if (typeof node.xml != "undefined") {
			return this._getContentAsStringIE(node);
		} else if (typeof XMLSerializer != "undefined" ) {
			return this._getContentAsStringMozilla(node);
		} else {
			return this._getContentAsStringGeneric(node);
		}
	},

        /**
         * Function: getElementAsString
         *
         * Takes a dom node and returns itself and its contents rendered in a string.
         *
         * Implementation Note: This function uses a generic implementation in order
         * to generate the returned string.
         *
         * Parameters:
         *
         *	node - The dom node.
         * Returns:
         *
         * The string representation of the given node.
         */
	getElementAsString:function(node){
		if (!node) { return ""; }

		var s='<' + node.nodeName;
		// add attributes
		if (node.attributes && node.attributes.length > 0) {
			for (var i=0; i < node.attributes.length; i++) {
				s += " " + node.attributes[i].name + "=\"" + node.attributes[i].value + "\"";
			}
		}
		// close start tag
		s += '>';
		// content of tag
		s += this._getContentAsStringGeneric(node);
		// end tag
		s += '</' + node.nodeName + '>';
		return s;
	},
        
    /**
     * Adds togglers and js effects to the exception page.
     */
    enhanceExceptionPage:function(){
        // attach toggles + hide content
        
        var elms=dojo.query('.toggle');
        
        if(elms && elms.length > 0){
            for(var i=0;i<elms.length;i++){

                dojo.connect(elms[i], "onclick", function(e) {
                    var thisLink = e.target;
                    //dojo.html.toggleShowing(dojo.byId(thisLink.id + 'Data'));
                    dojo.toggleClass(thisLink, "toggleSelected");

                    if (e.preventDefault)
                        tapestry.event.stopEvent(e);
                    return false;
                });
                //dojo.html.toggleShowing(elms[i].id+'Data');
            }
        }

        // but show last exception's content
        elms=dojo.query('.exception-link');
        if(elms && elms.length > 0){
            elms[elms.length-1].onclick({target:elms[elms.length-1]});
        }
    },

	_getContentAsStringIE:function(node){
		var s=" "; //blank works around an IE-bug
    	for (var i = 0; i < node.childNodes.length; i++){
        	s += node.childNodes[i].xml;
    	}
    	return s;
	},

	_getContentAsStringMozilla:function(node){
        if (!this.xmlSerializer){ this.xmlSerializer = new XMLSerializer();}

	    var s = "";
        for (var i = 0; i < node.childNodes.length; i++) {
	        s += this.xmlSerializer.serializeToString(node.childNodes[i]);
	        if (s == "undefined")
		        return this._getContentAsStringGeneric(node);
        }

        return this._processCompactElements(s);
	},

	_getContentAsStringGeneric:function(node){
		var s="";
		if (node == null) { return s; }
		for (var i = 0; i < node.childNodes.length; i++) {
			switch (node.childNodes[i].nodeType) {
				case 1: // ELEMENT_NODE
				case 5: // ENTITY_REFERENCE_NODE
					s += this.getElementAsString(node.childNodes[i]);
					break;
				case 3: // TEXT_NODE
				case 2: // ATTRIBUTE_NODE
				case 4: // CDATA_SECTION_NODE
					s += node.childNodes[i].nodeValue;
					break;
				default:
					break;
			}
		}
		return s;
	},

	_processCompactElements:function(htmlData)
 	{
		 return htmlData.replace(this.CompactElementRegexp, this.CompactElementReplacer);
 	}
}

dojo.provide("tapestry.core");
dojo.provide("tapestry.html");
dojo.provide("tapestry.event");
dojo.provide("tapestry.lang");
dojo.provide("tapestry.form");
dojo.provide("tapestry.form.datetime");