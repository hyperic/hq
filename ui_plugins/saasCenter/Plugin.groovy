import org.hyperic.hq.hqu.rendit.HQUPlugin

class Plugin extends HQUPlugin {
    
    Plugin() {
        addMastheadView(true, '/saascenter/index.hqu', 'SaaS Center', 'resource')    
    }
    
}    

