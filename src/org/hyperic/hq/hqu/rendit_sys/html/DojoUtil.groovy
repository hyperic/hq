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
	    def idVar   = "_hqu_${params.id}"
	    def res     = """
	    <script type="text/javascript">

	    dojo.addOnLoad(function() {
	        ${idVar}_table = dojo.widget.createWidget("dojo:FilteringTable",
	                                                  {valueField: "id"},
	                                                  dojo.byId("${params.id}"));

            dojo.io.bind({
	            url: '${params.url}',
	            method: "get",
	            mimetype: "text/json-comment-filtered",
                load: function(type, data, evt) {
	                AjaxReturn = data;
                    var cols = data.columns
                    for (var x = 0; x<cols.length; x++) {
                        ${idVar}_table.columns.push(${idVar}_table.createMetaData(cols[x]));
                    }
                    ${idVar}_table.store.setData(data.data);
                }
	        });
	    });    
	    </script>
	    <table id="${params.id}"></table>
	    """
		res	    
	}
}
