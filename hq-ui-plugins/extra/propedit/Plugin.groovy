import org.hyperic.hq.hqu.rendit.HQUPlugin

class Plugin extends HQUPlugin {
    Plugin() {
        addAdminView(true, '/prop/index.hqu', 'Property Editor')
    }
}
