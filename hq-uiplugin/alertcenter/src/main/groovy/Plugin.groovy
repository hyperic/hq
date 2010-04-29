import org.hyperic.hq.hqu.rendit.HQUPlugin

class Plugin extends HQUPlugin {
    Plugin() {
        addMastheadView(true, '/alert/index.hqu', 'Alert Center', 'tracker')    
    }
}
