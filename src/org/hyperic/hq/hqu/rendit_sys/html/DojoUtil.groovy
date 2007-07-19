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
    private static BundleMapFacade BUNDLE = 
        new BundleMapFacade(RSRC_BUNDLE)

    /**
     * Output a header which sets up the inclusion of the DOJO javascript
     * framework.  This must be called before using any other DOJO methods.
     */
	static String dojoInit() {
        '''
	        <script type="text/javascript">
                var djConfig = {
                    isDebug: true
                };
            </script>

            <script src="/js/dojo/dojo.js" type="text/javascript">
            </script>
        '''
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
	    StringBuffer b = new StringBuffer()
	    
	    for (l in libs) {
	        b << "dojo.require(\"${l}\");\n"
	    }
	    
	    b << "dojo.hostenv.writeIncludes();\n"
	    """
            <script type="text/javascript">
                ${b}
            </script>
            <script type="text/javascript">
                function getDojo() {
                    ${b}
                }
            </script>	
        """
	}
     
    /**
     * Spit out a table, and appropriate <script> tags to enable AJAXification.
     *
     * 'params' is a map of key/vals for configuration, which include:
     *    
     *   id:       The HTML ID for the table.   
     *   url:      URL to contact to get data to populate the table
     *   numRows:  Number of rows to display
     *   schema:   The schema is a map which contains information on how to
     *             retrieve data for the rows, how to format them, etc.  The
     *             following keys for the schema are used:
     *
     *       getData:  A closure which takes a PageInfo object, which should
     *                 return the rows which are used to fill out the table
     *       defaultSort:  A class implementing SortField, which is the 
     *                     default column to sort on
     *       defaultSortOrder: 0 to sort by descending, 1 by ascending
     *       rowId:  A closure which takes a single object (an element as
     *               returned from the getData() call), and must return an
     *               ID for the row.  This ID is used to map the row to the
     *               object.
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
     */
    static String dojoTable(params) {
        def id           = "${params.id}"
	    def idVar        = "_hqu_${params.id}"
	    def tableVar     = "${idVar}_table" 
	    def sortFieldVar = "${idVar}_sortField"
	    def pageNumVar   = "${idVar}_pageNum"
	    def lastPageVar  = "${idVar}_lastPage"
	    def sortOrderVar = "${idVar}_sortOrder"
	    def res      = new StringBuffer(""" 
	    <script type="text/javascript">
        
        var ${sortFieldVar};
        var ${pageNumVar}  = 0;
        var ${lastPageVar} = false;
        var ${sortOrderVar};

	    dojo.addOnLoad(function() {
	        ${tableVar} = dojo.widget.createWidget("dojo:FilteringTable",
	                                               {alternateRows:false,
                                                    valueField: "id"},
	                                               dojo.byId("${id}"));
            ${tableVar}.createSorter = function(a) { return null; };
            ${idVar}_refreshTable();
	    });    

        function ${idVar}_makeQueryStr() {
            var res = '?pageNum=' + ${pageNumVar};

            res += '&pageSize=${params.numRows}';

            if (${sortFieldVar})
                res += '&sortField=' + ${sortFieldVar};
            if (${sortOrderVar} != null)
                res += '&sortOrder=' + ${sortOrderVar};

            return res;
        }

        function ${idVar}_setSortField(el) {
            if (el.getAttribute('sortable') == 'false')
                return;

            var curSortIdx  = ${tableVar}.sortInformation[0].index;
            ${sortOrderVar} = ${tableVar}.sortInformation[0].direction;

            if (curSortIdx == el.getAttribute('colIdx')) {
                ${sortOrderVar} = ~${sortOrderVar} & 1;
            } else {
                ${sortOrderVar} = 0;
            } 
            ${sortFieldVar} = el.getAttribute('field');
            ${tableVar}.sortInformation[0] = {index:el.getAttribute('colidx'),
                                              direction:${sortOrderVar}};
            ${pageNumVar}   = 0;
            ${idVar}_refreshTable();
        }

        function ${idVar}_refreshTable() {
            var queryStr = ${idVar}_makeQueryStr();
            dojo.io.bind({
                url: '${params.url}' + queryStr,
                method: "get",
                mimetype: "text/json-comment-filtered",
                load: function(type, data, evt) {
                    AjaxReturn = data;
                    ${sortFieldVar} = data.sortField;
                    ${sortOrderVar} = data.sortOrder;
                    var sortColIdx = 0;
                    var thead = dojo.byId("${id}").getElementsByTagName("thead")[0];
                    var ths = thead.getElementsByTagName('th')
                    for (j = 0; j < ths.length; j++) {
                        if (ths[j].getAttribute('field') == ${sortFieldVar}) {
                            sortColIdx = j;
                            break;
                        }
                    }

                    ${tableVar}.sortInformation = [{index:sortColIdx, 
                                                    direction:${sortOrderVar}}]
                    ${tableVar}.store.setData(data.data);
                    ${pageNumVar}  = data.pageNum;
                    ${lastPageVar} = data.lastPage;
                    ${idVar}_setupPager();
                    ${idVar}_highlightRow(data.data);
                }
            });
        }

        function ${idVar}_highlightRow(el) {
            for (i = 0; i < el.length; i++) {
                var id = el[i].id;
                var body = document.getElementById("${id}");
                var trs = body.getElementsByTagName('tr');
                var styleClassVal = el[i].styleClass;
                if (id && (styleClassVal && styleClassVal != '')) {
                    for (b = 0; b < trs.length; b++) {
                        var vals = trs[b].getAttribute("value");
                        if (id == vals) {
                            var rowTDs = trs[b].getElementsByTagName('td');
                            for (k = 0; k < rowTDs.length; k++) {
                                rowTDs[k].setAttribute((document.all ? 'className' : 'class'), styleClassVal);
                            }
                        }
                    }
                }
            }
        }
         
        function ${idVar}_setupPager() {
            var leftClazz = "noprevious";
            var pageNumDisplay = dojo.byId("${idVar}_pageNumbers");
            pageNumDisplay.innerHTML = "${BUNDLE['dojoutil.PageNum']} " + 
                                       (${pageNumVar} + 1); 

            if (${pageNumVar} != 0) {
                leftClazz = 'previousLeft';
            }
            dojo.byId("${idVar}_pageLeft").setAttribute((document.all ? 'className' : 'class'), leftClazz);

            var rightClazz = "nonext";
            if (${lastPageVar} == false) {
                rightClazz = "nextRight";
            }
            dojo.byId("${idVar}_pageRight").setAttribute((document.all ? 'className' : 'class'), rightClazz);
        }

        function ${idVar}_nextPage() {
            if (${lastPageVar} == false)  {
                ${pageNumVar}++;
                ${idVar}_refreshTable();
            }
        }

        function ${idVar}_previousPage() {
            if (${pageNumVar} != 0) {
                ${pageNumVar}--;
                ${idVar}_refreshTable();
            }
        }
	    </script>
        """)
	    
	    res << """
	    <div class="pageCont">
	    <div style="position:absolute;padding-left:10px;font-size:13px;padding-top:2px;font-weight:bold;">${params.id}</div>
	        <div class="boldText" style="position:relative;float: right;padding-left:5px;padding-right:10px;padding-top:5px;">${BUNDLE['dojoutil.Next']}</div>
	         <div class="pageButtonCont">
                 <div id="${idVar}_pageLeft" style="float:left;width:19px;height:20px;"
                      class="previousLeft" onclick="${idVar}_previousPage();">&nbsp;</div>
                 <div id="${idVar}_pageNumbers" style="position: relative;display:inline;padding-left: 5px;padding-right: 5px;padding-top: 5px;float: left;">&nbsp;</div>
                 <div id="${idVar}_pageRight" style="position: relative;display:inline;width: 19px;height:20px;float: left;"
                      class="nextRight" onclick="${idVar}_nextPage();">&nbsp;</div>

             </div>

             <div class="boldText" style="position: relative;float: right;padding-right:5px;padding-top:5px;">${BUNDLE['dojoutil.Previous']}</div>

             <div style="clear: both;"></div>
         </div>
        
          <table id='${id}'>
            <thead>
              <tr>
        """
        
        def colIdx = 0;
	    for (c in params.schema.columns) {
	        def field     = c.field
	        def label     = field.value
	        def fieldName = field.description 

	        res << """<th field='${fieldName}' align='left' nosort='true'  nowrap='true'
	                      onclick='${idVar}_setSortField(this);'
                          colidx="${colIdx}" """
            if (!field.sortable) {
                res << " sortable='false'"
            }
            res << """>
	                    ${label}
                      </th>"""
            colIdx++
	    }
	    res << """
              </tr>
            </thead>
          </table>

        """
	    
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
            sortColumn = schema.defaultSort
        }
            
        def pageNum  = new Integer(params.getOne("pageNum", "0"))
        def pageSize = new Integer(params.getOne("pageSize", "20"))

		/* To determine if we are at the last page, we modify the pageSize
		   when we query to get 1 additional row.  If that row exists, we know
		   we aren't on the last page */
		def pageInfo = PageInfo.create(pageNum, pageSize + 1, sortColumn, 
		                               sortOrder)
        def data     = schema.getData(pageInfo)
        def lastPage = (data.size() <= pageSize)

        if (data.size() == pageSize + 1)
            data = data[0..-2]
		
		JSONArray jsonData = new JSONArray()
        for (d in data) {
            def val = [:]
            val.id = schema.rowId(d)
            for (c in schema.columns) {
                val[c.field.description] = c.label(d)
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
		 sortField : sortColumn.description,
		 sortOrder : sortOrder ? 1 : 0,
		 pageNum   : pageNum,
		 lastPage  : lastPage] as JSONObject
    }
}
