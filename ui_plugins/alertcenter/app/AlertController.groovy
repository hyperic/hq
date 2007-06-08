import org.hyperic.hq.hqu.rendit.BaseController

class AlertController 
	extends BaseController
{
    def AlertController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index = { params ->
    	render(locals:[ pluginInfo : pluginInfo ])
    }
    
    def data(params) {
        def testData = '''
        /*[{ myId:0, "Date": "5/10/07", "Time": "5:10AM", "Alert": "123", "Resource": "Apache 2", "State": "Fixed", "Severity": "high", "Group": "none" },
        { myId:1, "Date": "5/10/07", "Time": "5:10PM", "Alert": "zsdzdv", "Resource": "Linux box", "State": "Fixed", "Severity": "medium", "Group": "Example 3" },
        { myId:2, "Date": "5/10/07", "Time": "5:10PM", "Alert": ",jk,.", "Resource": "another", "State": "Fixed", "Severity": "medium", "Group": "none"  },
        { myId:3, "Date": "5/10/07", "Time": "5:10PM", "Alert": "k,", "Resource": "something", "State": "Fixed", "Severity": "low", "Group": "none" },
        { myId:4, "Date": "5/10/07", "Time": "5:10PM", "Alert": "ewrw3r", "Resource": "Tomcat", "State": "Fixed", "Severity": "medium", "Group": "none" },
        { myId:5, "Date": "5/10/07", "Time": "5:10PM", "Alert": "cv", "Resource": "sql server", "State": "Fixed", "Severity": "medium", "Group": "Example 1" },
        { myId:6, "Date": "5/10/07", "Time": "5:10PM", "Alert": "SDF3wr", "Resource": "Oracle server", "State": "Fixed", "Severity": "medium", "Group": "none" }]*/
        '''
		render(inline:testData, contentType:'text/json-comment-filtered')
    }
}
