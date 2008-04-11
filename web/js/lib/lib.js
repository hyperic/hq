var hyperic = {};
hyperic.URLS = {}; hyperic.widget = {}; hyperic.utils = {};

hyperic.form = {
    fieldFocus : function(/*DOMNode*/elem) {
        if (!elem.getAttribute('readonly')) {
            if (elem.parentNode.className == "fieldRow hint")
                elem.parentNode.className = "fieldRow hint active";
            else
                elem.parentNode.className = "fieldRow active";
        }
    },
    fieldBlur : function(elem) {
        if (elem.parentNode.className == "fieldRow hint active")
            elem.parentNode.className = "fieldRow hint";
        else
            elem.parentNode.className = "fieldRow";
    }
};

/**
 * @deprecated
 */
hyperic.utils.key = {
    enterKeyHandler : function(evt) {
        if (evt){}
        else
            evt = window.event;
        if (window.event) {
            evt.cancelBubble = true;
            evt.returnValue = false;
        } else {
            evt.preventDefault();
            evt.stopPropagation();            
        }
        if (evt.keyCode == 13) {
            dojo.publish('enter', [evt]);
        }
    },
    registerListener : function(/*DOMNode*/node, /*fp*/handler){
        if (handler && node) {
            if (dojo.isIE) {
                node.attachEvent("onkeyup", handler);
            } else {
                node.addEventListener("keyup", handler, false);
            }
        }
    }
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
        if(e && e.keyCode == this.keyComb.keyCode && !this.canceled){
            if(this.keyComb.ctrl || this.keyComb.alt || this.keyComb.shift){
                if (e.ctrlKey || e.altKey || e.shiftKey){
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
        dojo.publish(this.topic, [e]);
    };
    this.cancel = function(){
        this.canceled = true;
        dojo.disconnect(node, "onkeyup", this, this.keyListener);
        delete this;
    };
    dojo.connect(node, "onkeyup", this, this.keyListener);
    return this;
}

hyperic.widget.search = function(/*Object*/ urls, /*number*/ minStrLenth, /*Object*/ keyCode){
    this.opened     = false;
    this.minStrLen  = minStrLenth; 
    this.resourceURL= urls.resource;
    this.searchURL  = urls.search;
    this.keyCode    = keyCode;
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
        new hyperic.utils.addKeyListener(window, this.keyCode, 'search');
        new hyperic.utils.addKeyListener(this.searchContainer, {keyCode: 13}, 'enter');
        new hyperic.utils.addKeyListener(dojo.byId('header'), {keyCode: 27}, 'escape');
    };
    this.search = function(e){
        var string = e.target.value;
        if(this.searchBox.value.length >= this.minStrLen){
            this.searchStarted();
            dojo.xhrGet( {
                url: this.searchURL+'?q='+string, 
                handleAs: "json",
                timeout: 5000, 
                load: loadSearchData,
                error: this.error
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
            dojo.fx.wipeOut({node:this.searchContainer, duration: 400}).play();
            this.opened = false;
            this.searchEnded();
            this.searchBox.value = '';
        }
        else {
            window.scrollTo(0,0);
            dojo.fx.wipeIn({node:this.searchContainer, duration: 400}).play();
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
}

function loadSearchData(response, ioArgs) {
    if(response){
        var resURL = resourceURL+"?eid=";
        var template = "<li><a href='link' class='type'>text<\/a><\/li>";
        var count = 0;
        var res = "";
        var relink = new RegExp("link", "g");
        var retext = new RegExp("text", "g");
        var retype = new RegExp("type", "g");
        for(var i in response) {
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

hyperic.widget.menu = {
    onclick: function (node){   
        var widget = dijit.byId(node.id+'_1');
        if(!widget.isShowingNow){
            var x,y;
            x=node.offsetLeft;
            y=node.clientHeight+node.offsetTop-3;
            var self=widget;
            var savedFocus = dijit.getFocus(widget);
            function closeAndRestoreFocus(){
                // user has clicked on a menu or popup
                dijit.focus(savedFocus);
                dijit.popup.close(widget);
            }
            dijit.popup.open({
                popup: widget,
                x: x,
                y: y,
                onExecute: closeAndRestoreFocus,
                onCancel: closeAndRestoreFocus,
                orient: 'L'
            });
            widget.focus();
            widget._onBlur = function(){
                // Usually the parent closes the child widget but if this is a context
                // menu then there is no parent
                dijit.popup.close(widget);
                // don't try to restore focus; user has clicked another part of the screen
                // and set focus there
            }
        }
    }
}

/**
 * @deprecated used only for the struts header
 */
function activateHeaderTab(){
    var l = document.location;
    l = l+""; // force string cast
    if(l.indexOf("Dash")!=-1)
        dojo.byId("dashTab").className = "active";
    if(l.indexOf("Resou")!=-1)
        dojo.byId("resTab").className = "active";
    if(l.indexOf("rep")!=-1 || l.indexOf("Rep")!=-1 || l.indexOf("masth")!=-1)
        dojo.byId("analyzeTab").className = "active";
    if(l.indexOf("admin")!=-1 || l.indexOf("Adm")!=-1)
        dojo.byId("adminTab").className = "active";
}
