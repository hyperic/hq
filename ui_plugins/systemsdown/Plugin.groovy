import org.hyperic.hq.hqu.rendit.HQUPlugin

class Plugin extends HQUPlugin {
    Plugin() {
        addMastheadView(true, '/systemsdown/index.hqu', 'Currently Down Resources', 'resource')    
    }
}
