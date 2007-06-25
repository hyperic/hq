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
     *   schema:   ** TODO ** Document me
     */
    static String dojoTable(params) {
        def id          = "${params.id}"
	    def idVar       = "_hqu_${params.id}"
	    def tableVar    = "${idVar}_table" 
	    def selClassVar = "${idVar}_selClass"
	    def queryStrVar = "${idVar}_queryStr" 
	    def res      = new StringBuffer(""" 
	    <script type="text/javascript">
        
        var ${selClassVar};
        var ${queryStrVar};
	    dojo.addOnLoad(function() {
	        ${tableVar} = dojo.widget.createWidget("dojo:FilteringTable",
	                                               {alternateRows:false,
                                                    valueField: "id"},
	                                               dojo.byId("${id}"));

            dojo.io.bind({
	            url:      '${params.url}' + "?" + ${queryStrVar} + "=" + ${selClassVar},
	            method:   "get",
	            mimetype: "text/json-comment-filtered",
                load: function(type, data, evt) {
	                AjaxReturn = data;
                    ${tableVar}.store.setData(data.data);
                     ${idVar}_highlightFixed();
                }
            });
	    });    

        function ${idVar}_setColClass(el) {
            ${selClassVar} = '';
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
                    ${selClassVar} = el.className;
                } else if (classN == 'selectedUp') {
                    el.setAttribute((document.all ? 'className' : 'class'), "selectedDown");
                    ${selClassVar} = el.className;
                }
            } else {
                el.setAttribute((document.all ? 'className' : 'class'), "selectedUp");
                ${selClassVar} = el.className;
            }
            ${idVar}_refreshTable(el);
        }

        function ${idVar}_refreshTable(el) {
            var field = el.getAttribute('field')
            var queryStr = 'sortField=' + field;
            dojo.io.bind({
                url: '${params.url}' + "?" + queryStr,
                method: "get",
                mimetype: "text/json-comment-filtered",
                load: function(type, data, evt) {
                    AjaxReturn = data;
                    ${tableVar}.store.setData(data.data);
                    ${idVar}_highlightFixed();
                    var sortColHead = data.sortField;
                    var sortOrder   = data.sortOrder;
                    var strOrder    = sortOrder.toString();
                    var strColClass;
                    if (strOrder == 'false') {
                        strColClass = "selectedDown";
                    } else {
                        strColClass = "selectedUp";
                    }
                    if (sortColHead) {
                        var thead = dojo.byId("${id}").getElementsByTagName("thead")[0];
                        var ths = thead.getElementsByTagName('th')
                        for (j = 0; j < ths.length; j++) {
                            var setClass = ths[j].className;
                            var getColStr = ths[j].firstChild.nodeValue;
                            if (getColStr==sortColHead) {
                                setClass=strColClass;
                            }
                        }
                    }
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
	        res << " onclick='${idVar}_setColClass(this);'>"
	        res << "${label}</th>"
	    }
	    res << """
              </tr>
            </thead>
          </table>
        """
	    
		res.toString()	    
	}
     
    static JSONObject processTableRequest(schema, params) {
        def sortField = params.getOne("sortField")
        def sortOrder = params.getOne("sortOrder", 
                                      "${schema.defaultSortOrder}") != '1'
                                      
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
		 sortOrder : sortOrder,
		 pageNum   : pageNum,
		 lastPage  : lastPage] as JSONObject
    }
}
