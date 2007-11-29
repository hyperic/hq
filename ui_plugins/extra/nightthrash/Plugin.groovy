import org.hyperic.hq.hqu.rendit.HQUPlugin

class Plugin extends HQUPlugin {
    Plugin() {
        addAdminView(true, '/thrash/index.hqu', 'Night Thrasher')
    }
}
