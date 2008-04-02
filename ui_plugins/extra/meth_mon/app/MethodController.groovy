import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.application.HQApp

class MethodController 
	extends BaseController
{
    protected void init() {
        onlyAllowSuperUsers()
        setJSONMethods(['methodData'])
    }
    
    def index(params) {
    	render(locals:[methodSchema: getMethodSchema() ])
    }
    
    private getMethodSchema() {
        def nameCol  = new MethodColumn('name', 'Name',   true)
        def callsCol = new MethodColumn('calls', '# Calls', true)
        def totalCol = new MethodColumn('total', 'Total', true)
        def failCol  = new MethodColumn('failures', 'Failures', true)
        def minCol   = new MethodColumn('min', 'Min', true)
        def avgCol   = new MethodColumn('average', 'Average', true)
        def maxCol   = new MethodColumn('max', 'Max', true)
        
        def globalId = 0
        [
            getData: {pageInfo, params ->
                getMethodData(pageInfo)
            },
            defaultSort: nameCol,
            defaultSortOrder: 1,  // descending
            rowId: {globalId++},
            columns: [
                [field:  nameCol,
                 width:  '40%',
                 label:  {it.name}],
                [field:  callsCol,
                 width:  '10%',
                 label:  {"${it.calls}"}],
                [field:  totalCol,
                 width:  '10%',
                 label:  {"${it.total}"}],
                [field:  failCol,
                 width:  '10%',
                 label:  {"${it.failures}"}],
                [field:  minCol,
                 width:  '10%',
                 label:  {"${it.min}"}],
                [field:  avgCol,
                 width:  '10%',
                 label:  {"${it.average}"}],
                [field:  maxCol,
                 width:  '10%',
                 label:  {"${it.max}"}],
            ],
        ]
    }
    
	private getMethodData(pageInfo) {
	    log.info "Getting method data"
	    def app   = HQApp.instance
	    def res = app.getMethodStats()
	    def d = pageInfo.sort.description
	    res = res.sort {a, b ->
	        return a."${d}" <=> b."${d}"
	    }
	    if (!pageInfo.ascending) 
	        res = res.reverse()
	    
	    def startIdx = pageInfo.startRow
	    def endIdx   = startIdx + pageInfo.pageSize
	    if (endIdx >= res.size)
	        endIdx = -1
	    return res[startIdx..endIdx]
    }
	
    def methodData(params) {
        DojoUtil.processTableRequest(methodSchema, params)
    }
	
}
