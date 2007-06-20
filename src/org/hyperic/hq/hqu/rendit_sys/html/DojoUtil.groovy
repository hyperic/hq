package org.hyperic.hq.hqu.rendit.html

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
     *   columns:  An array of Strings, containing the column names
     *   url:      URL to contact to get data to populate the table
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
	                                               {valueField: "id"},
	                                               dojo.byId("${id}"));

            dojo.io.bind({
	            url:      '${params.url}' + "?" + ${queryStrVar} + "=" + ${selClassVar},
	            method:   "get",
	            mimetype: "text/json-comment-filtered",
                load: function(type, data, evt) {
	                AjaxReturn = data;
                    ${tableVar}.store.setData(data.data);
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
            var colHead = el.firstChild.nodeValue;
            queryStr = colHead + '=' + ${selClassVar};

            var requestData = dojo.io.bind({
                url: '${params.url}' + "?" + queryStr,
                method: "get",
                mimetype: "text/json-comment-filtered",
                load: function(type, data, evt) {
                    AjaxReturn = data;
                    ${tableVar}.store.update(data.data);
                }
            });
        }
	    </script>
        """)
	    
	    res << """
          <table id='${id}'>
            <thead>
              <tr>
        """
	    for (c in params.columns) {
	        res << "<th field='${c}' dataType='String' align='left'"
	        res << " noSort='true' onclick='${idVar}_setColClass(this);'>${c}</th>"
	    }
	    res << """
              </tr>
            </thead>
          </table>
        """
	    
		res.toString()	    
	}
}
