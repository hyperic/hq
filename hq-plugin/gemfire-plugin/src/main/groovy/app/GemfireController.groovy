import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.hyperic.util.config.ConfigResponse
import org.json.JSONArray
import org.json.JSONObject

class GemfireController extends BaseController {
    def s=0,g=0,a=0;

    def tree(params){
        def members = getmembersList(params)
        render(locals:[members:members])
    }

    def member(params){
        def members = getmembersList(params)
        def mid=params.getOne("mid")
        mid = (mid =~ /(\w*).(\d*)..(\w*)..(\d*).(\d*)/).replaceAll("\$1(\$2)<\$3>:\$4/\$5")
        log.info("mid="+mid)
        def member=((Map)members).get(mid)
        def clients=member.get("clients").values()
        render(locals:[members:members, member:member, clients:clients])
    }

    def translate(members){
        s=0; g=0; a=0;
        members.each{k,it ->  it.get("isserver") && !it.get("isgateway")  ? it.put("_name","cache server "+ ++s) : void }
        members.each{k,it ->  it.get("isserver") &&  it.get("isgateway") ? it.put("_name","gateway hub "+ ++g) : void }
        members.each{k,it -> !it.get("isserver") && !it.get("isgateway") ? it.put("_name","application "+ ++a) : void }
        members.each{k,it -> it.put("_id",HtmlUtil.escapeHtml(it.get("id"))) }
        members.each{k,it -> it.put("id2",(it.get("id") =~ /(\w*).(\d*)..(\w*)..(\d*).(\d*)/).replaceAll("\$1-\$2--\$3--\$4-\$5") ) }


        // CPU Usage
        members.each{k,it ->
            it.put("cpu",0)
        }
        // Heap usage
        members.each{k,it ->
            if(it.get("stat.maxmemory")>0){
                it.put("heap",(it.get("stat.usedmemory") * 100) / it.get("stat.maxmemory"))
            }else{
                it.put("heap",0)
            }
        }
        members.each{k,it -> log.info(it.get("id")+" -> "+it.get("name")+" -> "+it.get("type")) }
    }

    def membersList(params) {
        def members = getmembersList(params)
        render(locals:[members:members, a:a, g:g, s:s])
    }

    def getmembersList(params) {
        def liveData=viewedResource.getLiveData(user, "getDetails", new ConfigResponse())
        def members = [:]
        if(!liveData.hasError()){
            members = liveData.objectResult
        }else{
            log.error(liveData.errorMessage);
        }

        translate(members)
        members
    }

    def index(params) {
        def eid=params.getOne("eid")
        render(locals:[eid:eid])
    }
}


//import org.hyperic.hq.context.Bootstrap;
//import org.hyperic.hq.hqu.shared.UIPluginManager;
//def pMan = Bootstrap.getBean(UIPluginManager.class)
//def plugin = pMan.findPluginByName("gemfire")
//pMan.deletePlugin(plugin)