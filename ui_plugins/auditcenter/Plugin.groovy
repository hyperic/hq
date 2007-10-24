import org.hyperic.hq.hqu.rendit.HQUPlugin

class Plugin extends HQUPlugin {
    Plugin() {
        addMastheadView(false, '/audit/index.hqu', 'Audit Center', 'tracker')    
    }
}
