import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.hyperic.util.config.ConfigResponse
import org.json.JSONArray
import org.json.JSONObject

import javax.servlet.http.HttpServletResponse

class GemfireController extends BaseController {
    def s=0,g=0,c=0;
    def static Map cpu_cache=new HashMap()

    def tree(params){
        def liveData=viewedResource.getLiveData(user, "getMembers", new ConfigResponse())
        def members = []
        if(!liveData.hasError()){
            def _members = liveData.objectResult
            _members.each{i -> members.add(HtmlUtil.escapeHtml(i))}
            log.debug("[tree] members="+members)
        }else{
            invokeArgs.response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, liveData.errorMessage);
        }
        render(locals:[members:members])
    }

    def member(params){
        def mid=params.getOne("mid")
        //mid = (mid =~ /([^(]*).(\d*)..(\w*)..(\d*).(\d*)/).replaceAll("\$1(\$2)<\$3>:\$4/\$5")
        log.debug("mid="+mid)
        def members
        def member
        try {
            members = getMembersList(params)
            member=((Map)members).get(mid)
        }catch (Exception e){
            invokeArgs.response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.message);
        }
        if(member!=null){
            render(locals:[member:member])
        }else{
            log.error("member==null mid="+mid)
        }
    }

    def translate(members){
        s=0; g=0; c=0;
        members.each{k,it ->
            if(it.get("used_cpu")!=null){
                it.put("cpu",String.format("%.02f",(it.get("used_cpu")*100))+"%")
            }else{
                it.put("cpu","N/A")
            }
            if(it.get("used_memory")!=null){
                it.put("heap",String.format("%.02f",(float)(it.get("used_memory")*100))+"%")
            }else{
                it.put("heap","N/A")
            }
            it.put("id",HtmlUtil.escapeHtml(it.get("id")))
            it.put("name",HtmlUtil.escapeHtml(it.get("name")))
            it.get("isserver") ? ++s : void
            it.get("isgateway") ? ++g : void
        }

        // clients
        def clients=[:]
        members.each{k,it ->
            def cl=it.get("clients")
            cl.each{ client,i ->
                log.debug("client = "+client)
                clients.put(client,true)
            }
        }
        c=clients.size()

        //Uptime
        members.each{k,it ->
            long uptime=it.get("uptime")
            long minutes = (int)uptime / 60
            long hours = minutes / 60
            minutes = minutes - (hours*60)
            def sec = uptime - ((hours*60*60) + (minutes*60))
            it.put("uptime",String.format( "%d hours %d mins %d secs",hours,minutes,sec))
        }

        members.each{k,it -> log.debug(it.get("id")+" -> "+it.get("name")+" -> "+it.get("type")) }
    }

    def membersList(params) {
        def members
        def name
        try{
            members = getMembersList(params)
            name = getGMFSName()
        }catch (Exception e){
            invokeArgs.response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.message);
        }
        render(locals:[members:members,systemName:name, s:s, g:g, c:c])
    }

    def getGMFSName(){
        def liveData=viewedResource.getLiveData(user, "getSystemID", new ConfigResponse())
        def name = ":"
        if(!liveData.hasError()){
            name = liveData.objectResult
            log.debug("name="+name)
        }else{
            throw new RuntimeException(liveData.errorMessage)
        }
        name
    }

    def getMembersList(params) {
        def liveData=viewedResource.getLiveData(user, "getDetails", new ConfigResponse())
        def members = [:]
        if(!liveData.hasError()){
            members = liveData.objectResult
        }else{
            throw new RuntimeException(liveData.errorMessage)
        }

        translate(members)
        members
    }

    def index(params) {
        def eid=params.getOne("eid")
        render(locals:[eid:eid])
    }
}
