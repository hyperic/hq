import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.hyperic.util.config.ConfigResponse
import org.json.JSONArray
import org.json.JSONObject

class GemfireController extends BaseController {
    def s=0,g=0,c=0;
    def static Map cpu_cache=new HashMap()

    def tree(params){
        def liveData=viewedResource.getLiveData(user, "getMembers", new ConfigResponse())
        def members = []
        if(!liveData.hasError()){
            def _members = liveData.objectResult
            _members.each{i -> members.add(HtmlUtil.escapeHtml(i))}
            log.info("[tree] members="+members)
        }else{
            log.error(liveData.errorMessage);
        }
        render(locals:[members:members])
    }

    def member(params){
        def mid=params.getOne("mid")
        //mid = (mid =~ /([^(]*).(\d*)..(\w*)..(\d*).(\d*)/).replaceAll("\$1(\$2)<\$3>:\$4/\$5")
        log.info("mid="+mid)
        def members = getMembersList(params)
        def member=((Map)members).get(mid)
        if(member!=null){
            def clients=member.get("clients").values()
            def clientsDetails=[]
            clients.each{it->
                def cid=it.get("gemfire.client.id.string")
                log.info("mid="+mid+" cid="+cid)
                clientsDetails.push(((Map)members).get(cid))
            }
            render(locals:[members:members, member:member, clients:clientsDetails])
        }else{
            log.error("member==null mid="+mid)
        }
    }

    def translate(members){
        s=0; g=0; c=0;
        members.each{k,it ->
            it.put("cpu",String.format("%.02f",(it.get("used_cpu")*100)))
            it.put("heap",String.format("%.02f",(it.get("used_memory")*100)))
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
                log.info("client = "+client)
                clients.put(client,true)
            }
        }
        c=clients.size()

        // CPU Usage
        //        members.each{k,it ->
        //            long last_cpu=it.get("stat.processcputime")
        //            long last_time=System.currentTimeMillis()
        //
        //            def c_entry=cpu_cache.get(it.get("id"));
        //            if(c_entry!=null) {
        //                long prev_cpu=c_entry.cpuTime
        //                long prev_time=c_entry.time
        //                last_cpu=last_cpu/1000000
        //                if ((prev_cpu!=null) && (last_cpu>=prev_cpu)){
        //                    def cpu=((last_cpu-prev_cpu)*100)/((last_time-prev_time)*it.get("vmStats.cpus"))
        //                    it.put("cpu",String.format("%.02f",cpu))
        //                }else{
        //                    it.put("cpu",0)
        //                    last_cpu=0
        //                }
        //            }else{
        //                it.put("cpu",0)
        //            }
        //            cpu_cache.put(it.get("id"),[cpuTime:last_cpu, time:last_time])
        //        }

        // Heap usage
        //        members.each{k,it ->
        //            if(it.get("stat.maxmemory")>0){
        //                def men=(it.get("stat.usedmemory") * 100) / it.get("stat.maxmemory")
        //                it.put("heap",String.format("%.02f",men))
        //            }else{
        //                it.put("heap",0)
        //            }
        //        }

        //Uptime
        members.each{k,it ->
            long uptime=it.get("uptime")
            long minutes = (int)uptime / 60
            long hours = minutes / 60
            minutes = minutes - (hours*60)
            def sec = uptime - ((hours*60*60) + (minutes*60))
            it.put("uptime",String.format( "%d hours %d mins %d secs",hours,minutes,sec))
        }

        members.each{k,it -> log.info(it.get("id")+" -> "+it.get("name")+" -> "+it.get("type")) }
    }

    def membersList(params) {
        def members = getMembersList(params)
        def name = getGMFSName()
        render(locals:[members:members,systemName:name, s:s, g:g, c:c])
    }

    def getGMFSName(){
        def liveData=viewedResource.getLiveData(user, "connectToSystem", new ConfigResponse())
        def name = ":"
        if(!liveData.hasError()){
            name = liveData.objectResult
            log.info("name="+name)
        }else{
            log.error(liveData.errorMessage);
        }
        name
    }

    def getMembersList(params) {
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