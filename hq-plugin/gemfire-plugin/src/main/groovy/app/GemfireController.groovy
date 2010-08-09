import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.hyperic.util.config.ConfigResponse
import org.json.JSONArray
import org.json.JSONObject

class GemfireController extends BaseController {
    def s=0,g=0,a=0;

    def inventory(params){
        def result = new JSONObject()
        def nodes = new JSONArray()

        def members = viewedResource.getLiveData(user, "getDetails", new ConfigResponse()).objectResult
        translate(members)

        members.each{k,i ->
            JSONObject node = new JSONObject();
            def id = (i.get("id") =~ /(\w*).(\d*)..(\w*)..(\d*).(\d*)/).replaceAll("\$1-\$2--\$3--\$4-\$5")
            node.put("id",id)
            node.put("text",i.get("name"))
            node.put("classes","icon icon-hq")
            //node.put("children",new JSONArray())
            nodes.put(node)
        }

        result.put("payload", nodes)
        render(inline:"${result}", contentType:'text/json-comment-filtered')
    }

    def member(params){
        def members = viewedResource.getLiveData(user, "getDetails", new ConfigResponse()).objectResult
        translate(members)
        def mid=params.getOne("mid")
        mid = (mid =~ /(\w*).(\d*)..(\w*)..(\d*).(\d*)/).replaceAll("\$1(\$2)<\$3>:\$4/\$5")
        log.info("mid="+mid)
        def member=((Map)members).get(mid)
        def clients=member.get("clients").values()
        render(locals:[members:members, member:member, clients:clients])
    }

    def translate(members){
        s=0; g=0; a=0;
        members.each{k,it -> it.get("isserver") ? it.put("name","cache server "+ ++s) : void }
        members.each{k,it -> it.get("isgateway") ? it.put("name","gateway hub "+ ++g) : void }
        members.each{k,it -> !it.get("isserver") && !it.get("isgateway") ? it.put("name","application "+ ++a) : void }
        members.each{k,it -> it.put("_id",HtmlUtil.escapeHtml(it.get("id"))) }


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
        def members = viewedResource.getLiveData(user, "getDetails", new ConfigResponse()).objectResult
        translate(members)
        render(locals:[members:members, a:a, g:g, s:s])
    }

    def index(params) {
        def eid=params.getOne("eid")
        render(locals:[eid:eid])
    }
}