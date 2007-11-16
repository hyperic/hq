import org.hyperic.hq.hqu.rendit.HQUPlugin

class Plugin extends HQUPlugin {
    Plugin() {
        addAdminView(true, '/health/index.hqu', 'HQ Health')
    }
}
