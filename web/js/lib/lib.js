var init_lib = false;
var urlXtraVar = [];
var hyperic = {};
hyperic.URLS = {}; hyperic.widget = {}; hyperic.utils = {}; hyperic.html = {}; hyperic.data = {}, hyperic.i18n = {};

hyperic.init = function(){
    if(!init_lib)
        dojo.require("dojox.Grid");
}
hyperic.html = {
    show : function(/*String*/ node){
        dojo.style(node, 'display', '');
    },
    hide : function(/*String*/ node){
        dojo.style(node, 'display', 'none');
    }
}
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

hyperic.utils.addUrlXtraCallback = function(plugin_id, fn) {
    if(!urlXtraVar[plugin_id])
        urlXtraVar[plugin_id] = [];
    urlXtraVar[plugin_id].push(fn);
}

/**
 * Make a query string for HQU plugins XHR calls
 * Currently used for the dojo Grid
 * 
 * @param keywordArgs 
 * @param pageNumVar
 * @param sortOrderVar
 * @param sortFieldVar
 * @param urlXtraVar
 * @param id
 * 
 */
hyperic.utils.makeQueryString = function(kwArgs, pageNumVar, sortFieldVar, 
        sortOrderVar, urlXtraVar, id){
    var res = '?pageNum=' + pageNumVar;
    if (kwArgs && kwArgs.numRows)
        res += '&pageSize='+ kwArgs.numRows;
    else
        res += '&pageSize='+numRows;
    if(kwArgs && kwArgs.typeId)
        res += '&typeId='+ kwArgs.typeId;

    if (sortFieldVar)
        res += '&sortField=' + sortFieldVar;
    if (sortOrderVar != null)
        res += '&sortOrder=' + sortOrderVar;

    var callbacks = urlXtraVar;
    for (var i=0; i<callbacks.length; i++) {
        var cb = callbacks[i];

        var cbmap = cb(id);
        for (var v in cbmap) {
            if (v == 'extend') continue;
            res += '&' + v + '=' + cbmap[v];
        }
    }
    return res;
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
        if (password.length > 6) score++;

        //if password has both lower and uppercase characters give 1 point      
        if ( ( password.match(/[a-z]/) ) && ( password.match(/[A-Z]/) ) ) score++;

        //if password has at least one number give 1 point
        if (password.match(/\d+/)) score++;

        //if password has at least one special caracther give 1 point
        if ( password.match(/.[!,@,#,$,%,^,&,*,?,_,~,-,(,)]/) ) score++;

        //if password bigger than 12 give another 1 point
        if (password.length > 12) score++;
         document.getElementById("passwordDescription").innerHTML = desc[score];
         document.getElementById("passwordStrength").className = "strength" + score;
    }
};

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
};

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
};

/**
 * Hyperic Dojo Grid
 *
 * Some nomenclature
 *  Columns - Vertical groups of cells of the same type, continuity is not required
 *  Rows - horizontal contiguous groups of cells of the same type
 *  Cells - a single entity in the grid
 *  Views - a collection of cells (row groups) that form a logical row
 *  Layouts - a collection of views, side by side (sets of columns)
 *
 * Columns have the following schema variables
 *  name: The title of the column - ex "foo"
 *  width: the style width of the column - ex "150px"
 *  field: the index of the array of each row in the data array
 *  height: the style height of the colum cell
 *  formatter: a function that performs some display formatting and conversion - ex [0,1] -> [False,True]   
 *      can return any string including HTML
 * 
 * Example
 *  build a hyperic.widget.Grid.Model, hyperic.widget.Grid.View and a layout descriptor then
 *  var myGrid = new hyperic.widget.grid(node, "myGridNode", model, layout);
 *  var myDojoGrid = myGrid.dojoGrid; //the dojo grid
 * 
 * Example Layout Descriptor
 *  var subrow = [
 *      { name: '' },
 *      { name: '', formatter: formatPercentage }
 *  ];
 *
 *  var view = {
 *      rows: [
 *          subrow // 1..n
 *      ]
 *  };
 *
 *  var structure = [
 *      view // 1..n
 *  ];
 *
 * 
 * @param containerNode
 * @param tableId
 * @param model
 * @param layout
 * 
 */
hyperic.widget.Grid = function(/*DOMNode*/ containerNode, /*String*/ tableId, /*Object*/ model,
     /*Object*/ layout, /*Number*/ autoRefreshInterval) {
    this.dojoGrid = null;
    this.data   = null;
    this.store = null;
    this.model  = model;
    
    this._autoRefresh = false;
    this._autoRefreshInterval = autoRefreshInterval; //1 sec;
    this._intervalVar = -1;
    this._currentPage   = 0;
    this._isSortable    = false;
    this._sortIdx        = 0;
    this._sortOrder     = 0;
    
    /**
     * Turn off/on auto refresh on the grid.
     * @param a new interval to refresh on
     */
    this.toggleAutoRefresh = function(interval){
        if(interval && interval > 100){
             this._autoRefreshInterval = interval;
             clearInterval(this._intervalVar);
             this._initInterval();
        }else{
            if(this._autoRefresh){
                this._autoRefresh = false;
                clearInterval(this._intervalVar);
            }
            else{
                this._autoRefresh = true;
                this._initInterval();
            }
        }
    };
    this.nextPage = function(){
        if (this._currentPage != 0) {
            this._currentPage++;
            
            this.refreshTable();
        }
        
    };
    this.previousPage = function(){
        if (this._currentPage != 0) {
            this._currentPage--;
            this.refreshTable();
        }
    };
    this.refreshGrid = function(){
        this.dojoGrid.model.refresh();
         
    };
    this.highlightRow = function(){
    
    };
    this._setupHeader = function(){
    
    };
    this._setSortField = function(){
        if (!this._isSortable)
                return;
        var curSortIdx = this.dojoGrid.getSortAsc(this.dojoGrid.sortInfo);
        this.sortOrder = this.dojoGrid.getSortIndex(this.dojoGrid.sortInfo);
        if (curSortIdx == el.getAttribute('idx')) {
            sortOrder = ~sortOrder & 1;
        } else {
            sortOrder = 0;
        } 
        this.dojoGrid.setSortIndex(curSortIdx, this.sortOrder);
        this._sortIdx = curSortIdx;
        this._currentPage = 0;
        this.refreshGrid();
    };
    this._init = function(){
        //Build grid
        dojo.require("dojox.grid.Grid");
        this.dojoGrid = new dojox.grid.Grid({
            "id": tableId,
            "model": model,
            "structure": layout
        });
        //add the grid to the parent node
        if(typeof(containerNode) == "string")
            var node = dojo.byId(containerNode);
            node.appendChild(this.dojoGrid.domNode);    
        
        //do connects
            //connect header sort onclick
        //auto refresh
        this._initInterval();
        
    };
    this._initInterval = function(){
        if(this._autoRefresh){
            var that = this;
            this._intervalVar = setInterval( function(){that.refreshGrid();}, this._autoRefreshInterval);
        }
    };
    this._init();
};

/**
 * A datastore for a Grid
 * Don't really care which kind of datastore since they are being depricated quickly
 * just return the one that pages and writes or not
 * 
 * Example
 * var datastore = new hyperic.widget.Grid.Datastore(true, true, 'http://foo.org/tableData.html');
 * 
 * @param readOnlyGrid is this grid going to be editable
 * @param paging does this grid page data remotely
 * @param dataServiceUrl where can it get the data from
 * 
 */
hyperic.data.GridDatastore = function(/*boolean*/ readOnlyGrid, /*boolean*/ paging, /*String*/ dataServiceUrl){
    var store;
    //create the datastore
    if(readOnlyGrid && !paging){
        dojo.require("dojo.data.ItemFileReadStore");
        store = new dojo.data.ItemFileReadStore({url:dataServiceUrl});
    }else if(!paging){
        dojo.require("dojo.data.ItemFileWriteStore");
        store = new dojo.data.ItemFileWriteStore({url:dataServiceUrl});
    }else{
        dojo.require("dojox.data.QueryReadStore");
        store = new dojox.data.QueryReadStore({url:dataServiceUrl, requestMethod:"post", doClientPaging: false});
        //need to create the xhr to handle the optional writes to the server
    }
    return store;
};

/**
 * The Grid Model
 *  
 * Example
 * var model = new hyperic.widget.Grid.Model(500, myDatastoreObj, false);
 * 
 * add a comparator to the model
 *  ex -  model.fields.get(4).compare = function(a, b){ return (b > a ? 1 : (a == b ? 0 : -1)); }
 * 
 * @param tableHeight - the integer height of the table used to specify #rows for virtual scrolling
 * @param datastore - a dojo datastore or json object of the data
 * @param isClientDataSource is this data local or remote
 */
hyperic.data.GridModel = function(tableHeight, datastore, rows, clientSort, query, data){
    dojo.require("dojox.grid._data.model")
    //create the model    
    var model;
    if(datastore) {
        model = new dojox.grid.data.DojoData(null, datastore, {
            rowsPerPage: rows,
            clientSort: false,
            doClientPaging:false,
            /*clientSort: clientSort,*/
            getRowCount: function(){
                return 500;
            },
        });
    } else {
        model = new dojox.grid.data.Table(null, data);
    }
    return model;
};

hyperic.data.Comparators = {
    string : function(){
        
    }
    
}

/**
 * @eprecated used only for the struts header
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
