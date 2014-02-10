/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

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
 
	static String dojoInit() {
		""""""
	}
	
    /**
     * Returns <script> tags which setup the appropriate DOJO libraries
     * to include.
     * 
     * @params libs:  An array of DOJO libraries to include.
     *
     * Example:  dojoInclude(["dojo.widget.FilteringTable"])
     */
    static String dojoInclude(libs) {
        """"""
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
            var selectedItem;
            var currentCountFilter;
            var url = "";
            var plugin={};plugin.accordion={};plugin.ajax={};
            var updateKWArgs = {};
            
            plugin.ajax.getData = function(response, args) {
                var unique = null;
                if (response) {
                    if (response.length == 0) {
                        document.getElementById('resourceTree').innerHTML = '';
                        hqDojo.publish("XHRComplete", [ "NO_DATA_RETURNED" ]);
                    }else{
                        hqDojo.publish("XHRComplete", [ "DATA_RETURNED" ]);
                        var domTree = document.getElementById('resourceTree');
                        var tree = "";
                        for (var x = 0; x < response.length; x++) {
                            var parent = response[x]['parent'];
                            var children = response[x]['children'];
                            var innerChildren = "";
                            var markExpanded = false;
                            for (var i = 0; i < children.length; i++) {
                                if(selectedItem){
                                    if(typeof(selectedItem) == 'string' && selectedItem == children[i]['id']){
                                        unique = hqDijit.getUniqueId("unique");
                                        innerChildren += plugin.accordion.createChild(children[i]['name'], children[i]['id'], children[i]['count'], unique);
                                        markExpanded = true;
                                    }else if(typeof(selectedItem) == 'object' && selectedItem.getAttribute('nodeid') == children[i]['id']){
                                        unique = hqDijit.getUniqueId("unique");
                                        innerChildren += plugin.accordion.createChild(children[i]['name'], children[i]['id'], children[i]['count'], unique);
                                        markExpanded = true;
                                    } else {
                                        innerChildren += plugin.accordion.createChild(children[i]['name'], children[i]['id'], children[i]['count']);
                                    }
                                }else{
                                    innerChildren += plugin.accordion.createChild(children[i]['name'], children[i]['id'], children[i]['count']);
                                }
                            }
                            if (selectedItem && typeof(selectedItem) == "string" && response[x]['id'] == selectedItem) {
                                 unique = hqDijit.getUniqueId("unique");
                                 tree +=  plugin.accordion.createParent(response[x]['parent'], response[x]['id'], response[x]['count'], innerChildren, markExpanded, unique);
                            } else if (selectedItem && typeof(selectedItem) == "object" && response[x]['id'] == selectedItem.getAttribute('nodeid')) {
                                 unique = hqDijit.getUniqueId("unique");
                                 tree +=  plugin.accordion.createParent(response[x]['parent'], response[x]['id'], response[x]['count'], innerChildren, markExpanded, unique);
                            } else {
                                 tree +=  plugin.accordion.createParent(response[x]['parent'], response[x]['id'], response[x]['count'], innerChildren, markExpanded);
                            }
                            
                        }
                        domTree.innerHTML = tree;
                        if(unique) {
                            plugin.accordion.setSelected(hqDojo.byId(unique));
                        }
                    }
                }
                if(unique != null)
                    selectedItem = hqDojo.byId(unique);
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
                
                if (markExpanded) {
                    ret += '>' ;
                } else {
                    ret += 'style="display:none">' ;
                }
                
                ret += innerChildren;
                ret += "</div>";
                
                return ret;
            };
        
            plugin.accordion.createChild = function(name, id, count, unique) {
                if (unique) {
                    return '<div class="listItem" onclick="plugin.accordion.swapSelected(this);plugin.accordion.itemClicked(this);" id="' + unique + '" nodeid="' + id + '">' + name + ' ('+count+')</div>';
                } else {
                    return '<div class="listItem" onclick="plugin.accordion.swapSelected(this);plugin.accordion.itemClicked(this);" nodeid="' + id + '">' + name + ' ('+count+')</div>';
                }
            };
            
            plugin.ajax.bindMixin = {
               load: plugin.ajax.getData,
               handleAs: "json-comment-filtered"
            };
        
            plugin.ajax.bind = function (url){
               plugin.ajax.bindMixin.url = url;
               hqDojo.xhrGet(plugin.ajax.bindMixin);
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
                if (hqDojo.style(sib, 'display') == 'none') {
                    sib.style.display = 'block';
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
                hqDojo.cookie('selecteditemid', elem.getAttribute('nodeid'));
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
            
            hqDojo.require("dojo.cookie");
            hqDojo.ready(function(){
                //Register update callback
                 ${params.filterTargetId}_addUrlXtraCallback(plugin.accordion.updateCallback);
                
                if(hqDojo.cookie("selecteditemid")) {
                    selectedItem = hqDojo.cookie("selecteditemid")
                    updateKWArgs.typeId = selectedItem;
                }
                if(hqDojo.cookie("filtercount")) {
                    currentCountFilter = hqDojo.byId(hqDojo.cookie("filtercount"));
                    updateKWArgs.pageSize = currentCountFilter.id;
                } else {
                    currentCountFilter = hqDojo.byId("50");
                    updateKWArgs.pageSize = 50;
                }
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
     * Spit out a table, and appropriate <script> tags to enable AJAXification.
     *
     * 'params' is a map of key/vals for configuration, which include:
     *          (* denotes an optional parameter)
     *    
     *   id:       The HTML ID for the table.   
     *   url:      URL to contact to get data to populate the table
     *   title*:   A title to display above the table
     *   titleHtml*:  Additional HTML to place in the header of the table
     *   hidden*: Set to true if this table should start hidden
     *   numRows:  Number of rows to display
     *   pageSize: Number of items to retrieve per page
     *   refresh*:  If specified, the table will refresh at the passed # of
     *              seconds.
     *   pageControls*:  If specified, provides a boolean value indicating
     *                   whether or not to display the page controls
     *   schema:   The schema is a map which contains information on how to
     *             retrieve data for the rows, how to format them, etc.  The
     *             following keys for the schema are used:
     *
     *       getData:  A closure which takes a PageInfo object, which should
     *                 return the rows which are used to fill out the table
     *       defaultSort:  A class implementing SortField, which is the 
     *                     default column to sort on
     *       defaultSortOrder: 0 to sort by descending, 1 by ascending
     *       rowId:  (optional) A closure which takes a single object (an 
     *               element as returned from the getData() call), and must 
     *               return an ID for the row.  This ID is used to map the row 
     *               to the object.  By default this calls the object.getId()
     *               
     *       styleClass (optional):  A closure which takes an element returned
     *                               from getData and returns a String to use
     *                               as the styleclass for that row.  If it 
     *                               returns null, no styleclass will be 
     *                               specified
     *       columns:  An array containing information about the columns.  Each
     *                 element of the array is a map, specifying:
     *
     *           field:  a class implementing SortField which will provide
     *                   information about sortability, column header,
     *           label:  a closure which takes an element returned by getData
     *                   and returns a string which will be the cell text
     *           header (optional):  If specified, overrides the header from
     *                               the column field.  If this is a closure,
     *                               the result of the closure will be used
     *           width (optional):  Specifies the width of the column.  
     *                              For instance: '10%' will ensure that the 
     *                              column is 10% of the table width
     *           nowrap (optional): If set to false, the nowrap attribute will
     *                              not be set.  Defaults to true.
     */
    static String dojoTable(Binding scriptBinding, params) {
        def id           = "${params.id}"
        def tableTitle   = params.get('title', '')
        def titleHtml    = params.get('titleHtml', '')
        def hidden       = params.get('hidden')
        def idVar        = "_hqu_${params.id}"
        def tableVar     = "${idVar}_table" 
        def sortFieldVar = "${idVar}_sortField"
        def pageNumVar   = "${idVar}_pageNum"
        def pageSizeVar  = "${idVar}_pageSize"
        def lastPageVar  = "${idVar}_lastPage"
        def sortOrderVar = "${idVar}_sortOrder"
        def urlXtraVar   = "${idVar}_urlXtra"
        def postRefreshVar = "${idVar}_postRefresh"
        def ajaxCountVar = "${idVar}_ajaxCountVar"
        def refreshTimeoutVar = "${idVar}_refreshTimeout"
        def pageControlStyle = 'display:none'

        def scriptMap = scriptBinding.variables['PAGE']
        if (!scriptMap[TABLE_VAR])
            scriptMap[TABLE_VAR] = []
        scriptMap[TABLE_VAR] << id
        
        if (params.get('pageControls', true)) {
            pageControlStyle = 'display:block'
        }
        
		def pageSizeValue = new Integer(params.get("pageSize", params.get("numRows", "20")))
		
        def res = new StringBuffer(""" 
        <script type="text/javascript">
        hqDojo.require("dojox.grid.DataGrid");
        hqDojo.require("dojo.data.ItemFileReadStore");
        
        var ${sortFieldVar};
		var ${urlXtraVar} = [];
        var	${postRefreshVar} = [];
        var	${ajaxCountVar} = 0;
        var	${lastPageVar} = false;
        var	${pageSizeVar} = ${pageSizeValue};
        var	${pageNumVar}  = 0; 
		var ${sortOrderVar}; 
        var ${refreshTimeoutVar};
		var ${tableVar};
		var ${tableVar}_canSortList;
		
		
        hqDojo.ready(function() {
        	var ${tableVar}_layout = [""")
		def canSortList = new StringBuffer(""" ${tableVar}_canSortList = new Array(null,""")  
        //the column index starts from index 1

            for (c in params.schema.columns) {
				def field     = c.field
			    def header    = c.header
			    def label     = field.value
			    def fieldName = field.description
                def canSort   = field.sortable
			
			    if (label == null && field['getValue'] != null) {
					label = field.getValue()
			    }
			    if (canSort == null && field['isSortable']!=null){
                    canSort = field.isSortable()                    
                }        
                canSortList << canSort <<""","""

			    if (header) {
					if (header in Closure) {
			            label = header()
					} else {
			            label = header
					}
			    }
				
				res << """{ field: '${fieldName}', name: '${label}'"""
				
				if (c.width != null) {
					res << """, width: '${c.width}'"""
				}
				
				res << """ },"""
			}
	
            res << """
            {}];
            
            ${tableVar}_layout.pop();
            
            ${tableVar} = new hqDojox.grid.DataGrid({
            	structure: ${tableVar}_layout,
            	autoHeight: ${params.get('numRows', true)},
            	escapeHTMLInData: false,
            	selectionMode: "none"
            }, hqDojo.byId("${id}"));
            """
            canSortList.deleteCharAt(canSortList.length()-1) 
            canSortList << """);
            """
            
            res.append(canSortList)
            
            res << """
			${tableVar}.canSort = function canSort(col){ return ${tableVar}_canSortList[Math.abs(col)]; }
			
			${id}_refreshTable();           	
            
            ${tableVar}.startup();
            
            hqDojo.connect(${tableVar}, 'onStyleRow' , this, function(row) {
				var it=${tableVar}.getItem(row.index);
				if(it!=null){
					row.customClasses += " "+it.styleClass;
				}
			});
        });    

        // Allows the caller to specify a callback which will return 
        // additional query parameters (in the form of a map)
        function ${id}_addUrlXtraCallback(fn) {
            ${urlXtraVar}.push(fn);
        }

        // Allows the caller to register callbacks to operate on the data
        // returned by the table refresh
        function ${id}_addRefreshCallback(fn) {
            ${postRefreshVar}.push(fn);
        }

        function ${idVar}_makeQueryStr(kwArgs) {
            var res = '?pageNum=' + ${pageNumVar};
            if (kwArgs && kwArgs.pageSize)
                res += '&pageSize='+ kwArgs.pageSize;
            else
                res += '&pageSize=' + ${pageSizeVar};
            if(kwArgs && kwArgs.typeId)
                res += '&typeId='+ kwArgs.typeId;

            if (${sortFieldVar})
                res += '&sortField=' + ${sortFieldVar};
            if (${sortOrderVar} != null)
                res += '&sortOrder=' + ${sortOrderVar};

            var callbacks = ${urlXtraVar};
            for (var i=0; i<callbacks.length; i++) {
                var cb = callbacks[i];

                var cbmap = cb("${id}");
                for (var v in cbmap) {
                    if (v == 'extend') continue;
                    res += '&' + v + '=' + encodeURIComponent(cbmap[v]);
                }
            }
            res += '&preventCache=' + new Date().getTime();
            return res;
        }

        function ${id}_show() {
            hqDojo.style("${id}_tableWrapper", 'display', 'block');
        }

        function ${id}_hide() {
            hqDojo.style("${id}_tableWrapper", 'display', 'none');
        }

        function ${id}_clear() {
            ${tableVar}.store.close();
        }

        function ${id}_refreshTable(kwArgs) {
            // Don't refresh data for this table if it's hidden.
            var tableWrapper = hqDojo.byId("${id}_tableWrapper");
            if (hqDojo.style(tableWrapper, 'display') == 'none') {
                return;
            }
            var queryStr = ${idVar}_makeQueryStr(kwArgs);
            ${ajaxCountVar}++;
            if (${ajaxCountVar} > 0) {
                hqDojo.style("${idVar}_loadMsg", 'visibility', 'visible');
            }
            hqDojo.xhrGet({
                url: '${params.url}' + queryStr,
                handleAs: "json-comment-filtered",
				headers: { "Content-Type": "application/x-www-form-urlencoded; charset=utf-8"},
                load: function(response, args) {
                    AjaxReturn = response;
                    ${sortFieldVar} = response.sortField;
                    ${sortOrderVar} = response.sortOrder;
                    var sortColIdx = 0;
                    var thead = hqDojo.query("thead", hqDojo.byId("${id}"))[0];
                    var ths = hqDojo.query('th', thead);
                    for (j = 0; j < ths.length; j++) {
                        if (hqDojo.attr(ths[j], 'field') == ${sortFieldVar}) {
                            sortColIdx = j;
                            break;
                        }
                    }

					response.items = response.data;
					response.identifier = "id";
					
					var dataStore = new hqDojo.data.ItemFileReadStore({
						clearOnClose: true,
						data: response 
					});

					${tableVar}.setStore(dataStore);
                    ${pageNumVar}  = response.pageNum;
                    ${lastPageVar} = response.lastPage;
                    ${idVar}_setupPager();
                    ${idVar}_highlightRow(response.data);
                    ${ajaxCountVar}--;

                    // Handle any registered refresh callbacks
                    var callbacks = ${postRefreshVar};
                    for (var i=0; i<callbacks.length; i++) {
                        var cb = callbacks[i];
                        cb(response);
                    }

                    if (${ajaxCountVar} == 0) {
                        hqDojo.style("${idVar}_loadMsg", "visibility", 'hidden');
                    }
                    if (response.data == '') {
                        hqDojo.publish("XHRComplete", [ "NO_DATA_RETURNED" ]);                        
                    } else {
                        hqDojo.publish("XHRComplete", [ "DATA_RETURNED" ]);
                    }
                }
            });
        }

        function ${idVar}_highlightRow(el) {
            for (i = 0; i < el.length; i++) {
                var id = el[i].id;
                var body = document.getElementById("${id}");
                var trs = body.getElementsByTagName('tr');
                var styleClassVal = el[i].styleClass;
                if (typeof(id)!='undefined' && id != null && (styleClassVal && styleClassVal != '')) {
                    for (b = 0; b < trs.length; b++) {
                        var vals = trs[b].getAttribute("value");
                        if (id == vals) {
                            var rowTDs = trs[b].getElementsByTagName('td');
                            for (k = 0; k < rowTDs.length; k++) {
                            	hqDojo.attr(rowTDs[k], "class", styleClassVal);
                            }
                        }
                    }
                }
            }
        }
         
        function ${idVar}_setupPager() {
            var leftClazz = "noprevious";
            var pageNumDisplay = hqDojo.byId("${idVar}_pageNumbers");
            pageNumDisplay.innerHTML = "${BUNDLE['dojoutil.PageNum']} " + 
                                       (${pageNumVar} + 1); 

            if (${pageNumVar} != 0) {
                leftClazz = 'previousLeft';
            }
            hqDojo.attr("${idVar}_pageLeft", "class", leftClazz);

            var rightClazz = "nonext";
            if (${lastPageVar} == false) {
                rightClazz = "nextRight";
            }
            hqDojo.attr("${idVar}_pageRight", "class", rightClazz);
        }

        function ${idVar}_nextPage() {
            if (${lastPageVar} == false)  {
                ${pageNumVar}++;
                ${id}_refreshTable();
            }
        }

        function ${idVar}_previousPage() {
            if (${pageNumVar} != 0) {
                ${pageNumVar}--;
                ${id}_refreshTable();
            }
        }"""

        if (params.refresh) {
            res << """
            function ${idVar}_autoRefresh() {
                ${refreshTimeoutVar} = setTimeout("${idVar}_autoRefresh()", ${params.refresh * 1000});
                ${id}_refreshTable();
            }

            ${refreshTimeoutVar} = setTimeout("${idVar}_autoRefresh()", ${params.refresh * 1000});"""   
        }
		res << "</script>"
		    
	    def tableWrapperStyle = "display:block"
	    
		if (hidden) {
	        tableWrapperStyle = "display:none"
	    }
	
		res << """
	    <div id="${id}_tableWrapper" style="${tableWrapperStyle}">
			<div id="${id}_pageCont" class="pageCont">
				<div id="${id}_tableTitleWrapper" class="tableTitleWrapper">
	            	<div id="${id}_tableTitle" style="display:inline;width:75px;">${tableTitle}</div>
					<span id="${id}_titleHtml">${titleHtml}</span>
				</div>
				<div id="${idVar}_noValues" style="float: left; padding-top:3px;padding-right:15px;font-weight:bold;font-size:12px;display:none;">There isn't any information currently</div>
				<div style="${pageControlStyle}">
	            	<div class="boldText" style="position:relative;display:inline;float: right;padding-left:5px;padding-right:10px;padding-top:5px;">${BUNDLE['dojoutil.Next']}</div>
					<div class="pageButtonCont">
						<div id="${idVar}_pageLeft" style="float:left;width:19px;height:20px;" class="previousLeft" onclick="${idVar}_previousPage();">&nbsp;</div>
						<div id="${idVar}_pageNumbers" style="position: relative;display:inline;padding-left: 5px;padding-right: 5px;padding-top: 5px;float: left;">&nbsp;</div>
						<div id="${idVar}_pageRight" style="position: relative;display:inline;width: 19px;height:20px;float: left;" class="nextRight" onclick="${idVar}_nextPage();">&nbsp;</div>
					</div>
					<div class="boldText" style="position: relative;float: right;padding-right:5px;padding-top:5px;">${BUNDLE['dojoutil.Previous']}</div>
				</div>
				<div class='refreshButton'><img src='/hqu/public/images/arrow_refresh.gif' width='16' height='16' title="${BUNDLE['dojoutil.Refresh']}" onclick='${id}_refreshTable();'/></div>
				<div class="acLoader" id="${idVar}_loadMsg"></div>"""
	        
	        	if(params.headerHTML) {
					res << params.get('headerHTML');
				}
	        
				res << """
				<div style="clear: both;"></div>
			</div>
	        <div id='${id}'></div>
		</div>"""
	        
		res.toString()      
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
     //   render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
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
        def pageSize = new Integer(params.getOne("pageSize", params.getOne("numRows", "20")))
		
        // TODO Verify the last page when:
        // ALL data size %  Page Size == 0
        def pageInfo = PageInfo.create(pageNum, pageSize, sortColumn, 
                                       sortOrder)
        def data     = schema.getData(pageInfo, params)
        def lastPage = (data.size() < pageSize)
        
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
        def cntVar = "_hqu_ContentPane_${params.id}"
        
        def output = b.PAGE.getOutput()
        output.write("<div id='${idVar}' style='display:none'>\n")
        output.write("  <div id='${cntVar}' hqDojoType='dijit.layout.ContentPane' title='${params.label}'>\n")
        yield()
        output.write('  </div>\n')
        output.write('</div>\n')
        
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
        output.write('<div hqDojoType="dijit.layout.TabContainer" ' +
                     HtmlUtil.htmlOptions(params) + '>\n')
        yield()
        output.write('</div>\n')
        output.write('<script type="text/javascript">\n')
		output.write('hqDojo.require("dijit.dijit");\n')
		output.write('hqDojo.require("dijit.layout.TabContainer");\n')
		output.write('hqDojo.require("dijit.layout.ContentPane");\n')
		
        if (b.PAGE[PANE_VAR] != null) {
            output.write("  ${idVar}_start = function() {\n")
            for (p in b.PAGE[PANE_VAR]) {
                output.write("  hqDojo.style('${p}', 'display', '');\n");
            }
			output.write("     hqDijit.byId('${params.id}').resize();\n")
			output.write("  };\n")
            output.write("  hqDojo.ready(${idVar}_start);\n")
        }
        
        output.write('</script>\n')
    }
}
