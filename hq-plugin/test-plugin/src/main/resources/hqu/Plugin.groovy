import org.hyperic.hq.hqu.rendit.HQUPlugin

class Plugin extends HQUPlugin {
    Plugin() {
        addAdminView(true, '/test/index.hqu', 'HQU Test')
    }
}
