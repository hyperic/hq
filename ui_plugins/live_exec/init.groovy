import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl as PMI
import org.hyperic.hq.hqu.server.session.AttachmentDescriptorResource as Descriptor
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl as UIPM
import org.hyperic.hq.hqu.server.session.View
import org.hyperic.hq.hqu.server.session.ViewResourceCategory
import org.hyperic.hq.hqu.ViewAttacher

plugin.name        = "live_exec"
plugin.description = "LiveData Execution View"
plugin.version     = '0.1'
plugin.apiMajor    = 0
plugin.apiMinor    = 1

class MyAttach implements ViewAttacher {
    public void attach(View view) {
        def pluginMan = UIPM.one
        def platMan   = PMI.one
        for (pt in platMan.findAllPlatformTypes()) {
            if (pt.plugin != 'system')
                continue
                
            def r = platMan.findResource(pt)
            def found = false
            for (a in view.attachments) {
                if (a.resource == r){
                    found = true
                    break
                }
            }
            if (found)
                continue
            
            def desc = new Descriptor(ViewResourceCategory.VIEWS, r)

            pluginMan.attachView(view, desc)
        }
    }
}

plugin.addView('/live/index.hqu', 'LiveExec', "resource", new MyAttach())
