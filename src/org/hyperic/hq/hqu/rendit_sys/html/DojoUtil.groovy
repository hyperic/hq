package org.hyperic.hq.hqu.rendit.html

import org.json.JSONArray
import org.json.JSONObject
import org.hyperic.hibernate.SortField
import org.hyperic.hibernate.PageInfo

class DojoUtil {
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
     *   id:       The HTML ID for the table
     *   url:      URL to contact to get data to populate the table
     *   numRows:  Number of rows to display
     *   schema:   ** TODO ** Document me
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
            ${idVar}_refreshTable();
	    });    

        function ${idVar}_makeQueryStr() {
            var res = '?pageNum=' + ${pageNumVar};

            res += '&pageSize=${params.numRows}';

            if (${sortFieldVar})
                res += '&sortField=' + ${sortFieldVar};
            if (${sortOrderVar})
                res += '&sortOrder=' + ${sortOrderVar};

            return res;
        }

        function ${idVar}_setSortField(el) {
            var selClass = '';
            var classN = el.className;
            var thead = dojo.byId("${id}").getElementsByTagName("thead")[0];
            var ths = thead.getElementsByTagName('th')
            for (i = 0; i < ths.length; i++) {
                var clrClass = ths[i].className;
                if (clrClass=='selectedUp' || clrClass=='selectedDown') {
                    ths[i].setAttribute((document.all ? 'className' : 'class'), " ");
                }
            }

            if (classN) {
                if (classN == '' || classN == 'selectedDown') {
                    el.setAttribute((document.all ? 'className' : 'class'), "selectedUp");
                    selClass  = el.className;
                    ${sortOrderVar} = 1; 
                } else if (classN == 'selectedUp') {
                    el.setAttribute((document.all ? 'className' : 'class'), "selectedDown");
                    selClass = el.className;
                    ${sortOrderVar} = -1;
                }
            } else {
                el.setAttribute((document.all ? 'className' : 'class'), "selectedUp");
                selClass = el.className;
                ${sortOrderVar} = 1;
            }

            ${sortFieldVar} = el.getAttribute('field')
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
                    ${tableVar}.store.setData(data.data);
                    ${idVar}_highlightFixed();
                    ${sortFieldVar} = data.sortField;
                    ${sortOrderVar} = data.sortOrder;
                    var strOrder    = ${sortOrderVar}.toString();
                    var strColClass;
                    if (strOrder == '1') {
                        strColClass = "selectedUp";
                    } else {
                        strColClass = "selectedDown";
                    }
                    if (${sortFieldVar}) {
                        var thead = dojo.byId("${id}").getElementsByTagName("thead")[0];
                        var ths = thead.getElementsByTagName('th')
                        for (j = 0; j < ths.length; j++) {
                            var setClass = ths[j].className;
                            var getColStr = ths[j].firstChild.nodeValue;
                            if (getColStr==${sortFieldVar}) {
                                setClass=strColClass;
                            }
                        }
                    }
                    ${pageNumVar}  = data.pageNum;
                    ${lastPageVar} = data.lastPage;
                    ${idVar}_setupPager();
                }
            });
        }

        function ${idVar}_highlightFixed() {
            var w = dojo.byId("${id}");
            var rowTDs = w.getElementsByTagName('td');

            for (k = 0; k < rowTDs.length; k++) {
                var fixedValue = rowTDs[k].firstChild.nodeValue;

                if (fixedValue == "No") {
                    var fixedSibs = rowTDs[k].parentNode.childNodes;
                    rowTDs[k].parentNode.style.backgroundColor = "#fa8672";
                }
            }
        }

        function ${idVar}_setupPager() {
            var leftClazz = "noprevious";
            
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
          <table id='${id}'>
            <thead>
              <tr>
        """
        
	    for (c in params.schema.columns) {
	        def field = c.field
	        def label = field.value
	        
	        field = field.description
	        
	        res << "<th field='${field}' dataType='String' align='left'"
	        res << " onclick='${idVar}_setSortField(this);'>"
	        res << "${label}</th>"
	    }
	    res << """
              </tr>
            </thead>
          </table>

         <div style="padding-bottom:5px;padding-top:5px;background:#eeeeee;border:0px solid #D5D8DE;width:100%;">
             <div style="float:right;padding-right:20px;">
                 <div id="${idVar}_pageLeft" class="previousLeft" onclick="${idVar}_previousPage();">&nbsp;</div>
                 <div id="pageNumbers">&nbsp;</div>
                 <div id="${idVar}_pageRight" class="nextRight" onclick="${idVar}_nextPage();">&nbsp;</div>
             </div>
             <div style="clear: both;"></div>
         </div>
        """
	    
		res.toString()	    
	}
     
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
		 sortOrder : sortOrder ? 1 : -1,
		 pageNum   : pageNum,
		 lastPage  : lastPage] as JSONObject
    }
}
