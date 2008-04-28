package org.hyperic.hq.hqu.rendit.html

import org.json.JSONArray
import org.json.JSONObject
import org.hyperic.hibernate.SortField
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.hqu.rendit.i18n.BundleMapFacade

class DojoUtil {
    private static ResourceBundle RSRC_BUNDLE = 
        ResourceBundle.getBundle("org.hyperic.hq.hqu.rendit.Resources",
                                 Locale.getDefault(), 
                                 Thread.currentThread().contextClassLoader)
    private static BundleMapFacade BUNDLE = new BundleMapFacade(RSRC_BUNDLE) 

    // Var kept in the binding to keep track of tables on the page
    private static final TABLE_VAR = '_DOJOTABLES'
    private static final PANE_VAR  = '_DOJOPANES'
    
    /**
     * Output a header which sets up the inclusion of the DOJO javascript
     * framework.  This must be called before using any other DOJO methods.
     * 
     * Currently dojo init is handled by header.jsp
     */
	static String dojoInit() {
        '''
        '''
	}
	
    /**
     * Returns <script> tags which setup the appropriate DOJO libraries
     * to include.
     * 
     * @params libs:  An array of DOJO libraries to include.
     *
     * Example:  dojoInclude(["dijit.FilteringTable"])
     */
	static String dojoInclude(libs) {
	    StringBuffer b = new StringBuffer()
	    
	    for (l in libs) {
	        b << "dojo.require(\"${l}\");\n"
	    }
	    """
	        <script type="text/javascript">
                function getDojo() {
                    ${b}
                }
            </script>	
        """
	}
    
    /**
    * Returns the <script> block that controls the ajax updating accordion widget.
    * The accordion is built from a json map in the form of {[ ]}
    * 
    * Example: ajaxAccordionFilter(id:'SystemsDown',
                                refresh:60,
                                updateURL:urlFor(action:'actionName')"
                                filterTargetId:"updateTarget" )
    */
    static String ajaxAccordionFilter(params) {
        def id      = "${params.id}";
        def idVar   = "_hqu_${params.id}"
    	def res = new StringBuffer("""
            dojo.require("dojo.cookie");

            var selectedItem;
		    var currentCountFilter;
            var counter = 0;
		    var url = "";
		    var plugin={};plugin.accordion={};plugin.ajax={};
		    var updateKWArgs = {};

            function getUniqueId(){
                return "unique_"+counter;
            }
		    
		    plugin.ajax.getData = function(data, args) {
		        var unique = null;
		        if (data) {
                    if (data.length == 0) {
                        document.getElementById('resourceTree').innerHTML = '';
                        dojo.publish("XHRComplete", ["NO_DATA_RETURNED"]);
                    }else{
                        dojo.publish("XHRComplete", ["DATA_RETURNED"]);
			            var domTree = document.getElementById('resourceTree');
			            var tree = "";
			            for (var x = 0; x < data.length; x++) {
			                var parent = data[x]['parent'];
			                var children = data[x]['children'];
			                var innerChildren = "";
			                var markExpanded = false;
			                for (var i = 0; i < children.length; i++) {
			                    if(selectedItem){
			                        if(typeof(selectedItem) == 'string' && selectedItem == children[i]['id']){
			                            unique = getUniqueId();
			                            innerChildren += plugin.accordion.createChild(children[i]['name'], children[i]['id'], children[i]['count'], unique);
			                            markExpanded = true;
			                        }else if(typeof(selectedItem) == 'object' && selectedItem.getAttribute('nodeid') == children[i]['id']){
			                            unique = getUniqueId();
			                            innerChildren += plugin.accordion.createChild(children[i]['name'], children[i]['id'], children[i]['count'], unique);
			                            markExpanded = true;
			                        } else {
			                            innerChildren += plugin.accordion.createChild(children[i]['name'], children[i]['id'], children[i]['count']);
			                        }
			                    }else{
			                        innerChildren += plugin.accordion.createChild(children[i]['name'], children[i]['id'], children[i]['count']);
			                    }
			                }
			                if (selectedItem && typeof(selectedItem) == "string" && data[x]['id'] == selectedItem) {
			                     unique = getUniqueId();
			                     tree +=  plugin.accordion.createParent(data[x]['parent'], data[x]['id'], data[x]['count'], innerChildren, markExpanded, unique);
			                } else if (selectedItem && typeof(selectedItem) == "object" && data[x]['id'] == selectedItem.getAttribute('nodeid')) {
			                     unique = getUniqueId();
			                     tree +=  plugin.accordion.createParent(data[x]['parent'], data[x]['id'], data[x]['count'], innerChildren, markExpanded, unique);
			                } else {
			                     tree +=  plugin.accordion.createParent(data[x]['parent'], data[x]['id'], data[x]['count'], innerChildren, markExpanded);
			                }
			                
			            }
			            domTree.innerHTML = tree;
			            if(unique) {
			                plugin.accordion.setSelected(dojo.byId(unique));
			            }
                    }
		        }
		        if(unique != null)
		            selectedItem = dojo.byId(unique);
		    }
		
		    plugin.accordion.createParent = function(name, id, count, innerChildren, markExpanded, unique) {
		        var expandStyle = markExpanded?"collapse":"expand";
		        var ret;
		        if (unique) {
		        ret = '<div class="topCat" id="'+ unique + '" onclick="plugin.accordion.disableSelection(this);plugin.accordion.swapSelected(this);" nodeid="'
                          + id + '"><div class="' + expandStyle  + '" style="width:22px;height:18px;display:inline;" onclick="plugin.accordion' 
                          + '.swapVis(this);">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</div><div style="display:inline;position:relative;">'
                          + name + " ("+count+")</div></div>"  + '<div class="resourcetypelist"';
		        } else {
		        ret = '<div class="topCat" onclick="plugin.accordion.disableSelection(this);plugin.accordion.swapSelected(this);" nodeid="'
                          + id + '"><div class="' + expandStyle  + '" style="width:22px;height:18px;display:inline;" onclick="plugin.accordion' 
                          + '.swapVis(this);">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</div><div style="display:inline;position:relative;">'
                          + name + " ("+count+")</div></div>"  + '<div class="resourcetypelist"';
		        }
		        
		        if(markExpanded) {
		            ret += '>' ;
		        } else {
		            ret += 'style="display:none">' ;
		        }
		        ret += innerChildren;
		        ret += "</div>";
		        return ret;
		    };
		
		    plugin.accordion.createChild = function(name, id, count, unique) {
		        if(unique) {
		            return '<div class="listItem" onclick="plugin.accordion.swapSelected(this);plugin.accordion.itemClicked(this);" id="' + unique + '" nodeid="' + id + '">' + name + ' ('+count+')</div>';
		        } else
		            return '<div class="listItem" onclick="plugin.accordion.swapSelected(this);plugin.accordion.itemClicked(this);" nodeid="' + id + '">' + name + ' ('+count+')</div>';
		    };
		    
		    plugin.ajax.bindMixin = {
		       load: plugin.ajax.getData,
		       handleAs: "json-comment-filtered"
		    };
		
		    plugin.ajax.bind = function (url){
		       plugin.ajax.bindMixin.url = url;
		       dojo.xhrPost(plugin.ajax.bindMixin);
		    }
		    
    
            plugin.accordion.updateCallback = function(name){
                return updateKWArgs;
            }
            
            plugin.accordion.itemClicked = function(item) {
                updateKWArgs.typeId = item.getAttribute("nodeid");
            }
        
            plugin.accordion.swapVis = function(elem) {
                plugin.accordion.disableSelection(elem);
                var sib = elem.parentNode.nextSibling;
                if (dojo.style(sib,"display") == 'none') {
                    dojo.style(sib,"display", "block");
                    elem.className="collapse";
                } else {
                    sib.style.display = 'none';
                    elem.className="expand";
                }
                //plugin.accordion.update({typeId: elem.getAttribute('nodeid')});
            }
        
            plugin.accordion.swapSelected = function(elem) {
                plugin.accordion.disableSelection(elem);
                if (selectedItem && typeof(selectedItem) == 'object') {
                    selectedItem.style.padding = '3px 0px 3px 0px';
                    selectedItem.style.border = '';
                    selectedItem.style.background = '';
                }
                selectedItem = elem;
                dojo.cookie('selecteditemid', elem.getAttribute('nodeid'));
                plugin.accordion.setSelected(selectedItem);
                plugin.accordion.itemClicked(elem);
                plugin.accordion.update({typeId: elem.getAttribute('nodeid')});
            }
        
            plugin.accordion.setSelected = function(elem) {
                elem.style.padding = '3px 0px 3px 0px';
                elem.style.border = '1px solid #dddddd';
                elem.style.background = '#88BDEE none repeat scroll 0%';
            }
        
            plugin.accordion.disableSelection = function(element) {
                element.onselectstart = function() {
                    return false;
                };
                element.unselectable = "on";
                element.style.MozUserSelect = "none";
            }
        
            plugin.accordion.openAll = function() {
                var tree = document.getElementById('resourceTree');
                var x = tree.getElementsByTagName('div');
                for (var i = 0; i < x.length; i++) {
                    if (x[i].className == 'resourcetypelist') {
                        x[i].style.display = '';
                    } else if (x[i].className == 'expand') {
                        x[i].className = 'collapse';
                    }
                }
            }
        
            plugin.accordion.closeAll = function() {
                var tree = document.getElementById('resourceTree');
                var x = tree.getElementsByTagName('div');
                for (var i = 0; i < x.length; i++) {
                    if (x[i].className == 'resourcetypelist') {
                        x[i].style.display = 'none';
                    } else if (x[i].className == 'collapse') {
                        x[i].className = 'expand';
                    }
                }
            }
            
    		dojo.addOnLoad(function(){
                //Register update callback
                 hyperic.utils.addUrlXtraCallback("${params.filterTargetId}", plugin.accordion.updateCallback);
                
                if(dojo.cookie("selecteditemid")) {
                    selectedItem = dojo.cookie("selecteditemid")
                    updateKWArgs.typeId = selectedItem;
                }
                if(dojo.cookie("filtercount")) {
                    currentCountFilter = dojo.byId(dojo.cookie("filtercount"));
                    updateKWArgs.numRows = currentCountFilter.id;
                } else {
                    currentCountFilter = dojo.byId("50");
                    updateKWArgs.numRows = 50;
                }
                if(currentCountFilter)
                    updateFilterCount(currentCountFilter.id, currentCountFilter);
                plugin.ajax.bind("${params.updateURL}");
            });
            
            setInterval(function(){
                plugin.ajax.bind("${params.updateURL}");
            }, ${params.refresh}*1000);
        """)
        res.toString();
    }
     
     /**
     *  table - creates a table with the appropriate AJAXy interactions
     *
     *  Creates a datastore, then a model and a layout (schema) and then creates the table
     *
     *  TODO: enable cell editing
     * Params:
     *  id:       The HTML ID for the table.   
     *  url:      URL to contact to get data to populate the table
     *  title*:   A title to display above the table
     *  titleHtml*:  Additional HTML to place in the header of the table
     *  numRows:  Number of rows to display
     *  refresh*:  If specified, the table will refresh at the passed # of
     *              seconds.
     *  pageControls*:  If specified, provides a boolean value indicating
     *                   whether or not to display the page controls
     *  schema:   The schema is a map which contains information on how to
     *             retrieve data for the rows, how to format them, etc.  The
     *             following keys for the schema are used:
     *
     *  getData:  A closure which takes a PageInfo object, which should
     *                 return the rows which are used to fill out the table
     *  defaultSort:  A class implementing SortField, which is the 
     *                     default column to sort on
     *  defaultSortOrder: 0 to sort by descending, 1 by ascending
     *  rowId:  (optional) A closure which takes a single object (an 
     *               element as returned from the getData() call), and must 
     *               return an ID for the row.  This ID is used to map the row 
     *               to the object.  By default this calls the object.getId()
     *               
     *  styleClass (optional):  A closure which takes an element returned
     *                               from getData and returns a String to use
     *                               as the styleclass for that row.  If it 
     *                               returns null, no styleclass will be 
     *                               specified
     *  columns:  An array containing information about the columns.  Each
     *                 element of the array is a map, specifying:
     *
     *  field:  a class implementing SortField which will provide
     *                   information about sortability, column header,
     *  label:  a closure which takes an element returned by getData
     *                   and returns a string which will be the cell text
     *  header (optional):  If specified, overrides the header from
     *                               the column field.  If this is a closure,
     *                               the result of the closure will be used
     *  width (optional):  Specifies the width of the column.  
     *                              For instance: '10%' will ensure that the 
     *                              column is 10% of the table width
     */
    static String dojoTable(Binding scriptBinding, params) {
        def id           = "${params.id}"
        def containerId  = "${params.containerId}"
        def tableTitle   = params.get('title', '')
        def titleHtml    = params.get('titleHtml', '')
        def idVar        = "_hqu_${params.id}"
        
        def tableVar     = "${idVar}_table" 
        def sortFieldVar = "${idVar}_sortField"
        def pageNumVar   = "${idVar}_pageNum"
        def lastPageVar  = "${idVar}_lastPage"
        def sortOrderVar = "${idVar}_sortOrder"
        def urlXtraVar   = "${idVar}_urlXtra"
        def ajaxCountVar = "${idVar}_ajaxCountVar"
        def pageControlStyle = 'display:none'

        JSONArray innercells = new JSONArray()
        def colIdx = 0;
        for (c in params.schema.columns) {
            def field     = c.field
            def header    = c.header
            def label     = field.value
            def fieldName = field.description 

            if (label == null && field['getValue'] != null) {
                label = field.getValue()
            }
            if (header) {
                if (header in Closure)
                    label = header()
                else
                    label = header
            }
            def widthvar = ""
            if (c.width != null) {
                widthvar="${c.width}"   
            }
            /* build the view in the form of
             *   var layout = [
             *       // view n
             *       { cells: [[
             *           { field: "name", name: "Name", width: 'auto' },
             *           { field: "type", name: "Type", editor: iEditor, width: 'auto' },
             *       ]]}
             *   ];
            */
            JSONObject col_field = new JSONObject().put("field", fieldName).put("name", label).put("width", widthvar)
            innercells.put(col_field)
            colIdx++
        }
        
        JSONObject view = new JSONObject().put("cells", new JSONArray().put(innercells))
        
        def res = new StringBuffer( """
            <script type="text/javascript">
                var ${urlXtraVar} = [];
			    function ${id}_addUrlXtraCallback(fn) {
                    ${urlXtraVar}.push(fn);
			    }

                function ${id}_refreshTable(args){

                }
                var ${idVar}_layout = [ ${view.toString()} ];
               
                var ${idVar}_data = [[ ]];
                var ${idVar}_model = new dojox.grid.data.Table(null, ${idVar}_data);
            </script>
        """)
        if(params.headerHTML) {
            res << params.get('headerHTML');
        }
        res << """
            <div id="${id}_grid" class="dojogrid" dojoType="dojox.Grid" model="${idVar}_model" structure="${idVar}_layout">
            </div>
         """
         res.toString();   
    }
    
    /**
     * Processes an incoming web request from a DOJO table (as created via
     * dojoTable), which examines the query parameters, creates the correct 
     * paging objects, passes them to the schema's getData() method and
     * formulates the JSON result to pass back to DOJO.
     *
     * @param schema  A schema, as passed to dojoTable()
     * @param params  Query parameters from the web request
     *
     * @return a JSON result which can be written to the client via */
     //   render(inline:"/* ${json} */", contentType:'json-comment-filtered')
    static JSONObject processTableRequest(schema, params) {
        def sortField = params.getOne("sortField")
        def sortOrder = params.getOne("sortOrder", 
                                      "${schema.defaultSortOrder}") == '1'
        def sortColumn                                      
        for (c in schema.columns) {
            if (c.field.description == sortField) {
                sortColumn = c.field
                break
            }
        }
        
        if (sortColumn == null) {
            if (schema.defaultSort != null) {
            	sortColumn = schema.defaultSort
            } else {
                for (c in schema.columns) {
                    if (c.field.sortable) {
                        sortColumn = c.field
                        break
                    }
                }
            }
        }

        def pageNum  = new Integer(params.getOne("pageNum", "0"))
        def pageSize = new Integer(params.getOne("pageSize", "20"))

		/* To determine if we are at the last page, we modify the pageSize
		   when we query to get 1 additional row.  If that row exists, we know
		   we aren't on the last page */
		def pageInfo = PageInfo.create(pageNum, pageSize + 1, sortColumn, 
		                               sortOrder)
        def data     = schema.getData(pageInfo, params)
        def lastPage = (data.size() <= pageSize)

        if (data.size() == pageSize + 1)
            data = data[0..-2]
		
		JSONArray jsonData = new JSONArray()
        def rowId = schema.rowId
        if (rowId == null) {
            rowId = { it -> it.getId() }
        }
        for (d in data) {
            def val = [:]
            val.id = rowId(d)
            
            for (c in schema.columns) {
                def v = c.label(d)
                
                if (v != null && !(v in String))
                    v = v.toString()
                    
                if (v == null || v.trim() == '') {
                    v = '&nbsp;' // We need this to get the bottom border on <td>
                }
                
                if (!c.field) {
                    throw new IllegalArgumentException("Column with no field")
                }
                
                if (!c.field.description) {
                    throw new IllegalArgumentException("No column description")
                }
                val[c.field.description] = v
            }

            // Optionally define a styleClass attribute if the schema defines
            // the method, and it returns non-null
            if (schema.styleClass) {
                def sc = schema.styleClass(d)
                if (sc)
                	val.styleClass = sc
            }
            jsonData.put(val)
        }
                    
		[data      : jsonData, 
		 sortField : sortColumn?.description,
		 sortOrder : sortOrder ? 1 : 0,
		 pageNum   : pageNum,
		 lastPage  : lastPage] as JSONObject
    }

    /**
     * Create a tab pane within a container.
     *
     * Example:
     *
     *  <% dojoTabContainer(id:'myTabs', style:'width=100%;') { %>
     *    <% dojoTabPane(id:'feelMyPain', label:'MyPain') { %>
     *         Here is the pane content
     *    <% } %>
     *  <% } %>
     *
     */
    static dojoTabPane(Binding b, Map params, Closure yield) {
	    def idVar  = "_hqu_TabPane_${params.id}"
        def output = b.PAGE.getOutput()
        //output.write("<div id='${idVar}' style='display:none'>\n")
        // change to selected='true'
        output.write("  <div dojoType='dijit.layout.ContentPane' title='${params.label}'>\n ")
        yield()
        output.write('  </div>\n')
        //output.write('</div>\n')
        
        if (b.PAGE[PANE_VAR] == null)
            b.PAGE[PANE_VAR] = []

	    b.PAGE[PANE_VAR] << idVar
    }

     /**
      * Create a tab container.  Any additional attributes in 'params' 
      * will be used to generate the attributes for the HTML tags.
      *
      * Example:
      *
      *  <% dojoTabContainer(id:'myTabs', style:'width=100%;') { %>
      *    <% dojoTabPane(id:'feelMyPain', label:'MyPain') { %>
      *         Here is the pane content
      *    <% } %>
      *  <% } %>
      *
      */
    static dojoTabContainer(Binding b, Map params, Closure yield) {
	    def idVar  = "_hqu_TabContainer_${params.id}"
        def output = b.PAGE.getOutput()
        output.write('<div dojoType="dijit.layout.TabContainer" ' +
                     HtmlUtil.htmlOptions(params) + '>\n')
        yield()
        output.write('</div>\n')

        output.write('<script type="text/javascript">\n')
        if (b.PAGE[PANE_VAR] != null) {
            output.write("  ${idVar}_start = function() {\n")
            for (p in b.PAGE[PANE_VAR]) {
                output.write("  dojo.style('${p}','display','');\n");
            }
            output.write("  };\n")
            output.write("  dojo.connect(window, 'onload', '${idVar}_start');\n")
        }
	    
	    output.write('</script>\n')
    }
}
