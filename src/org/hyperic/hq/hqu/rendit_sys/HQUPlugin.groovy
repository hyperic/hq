package org.hyperic.hq.hqu.rendit


import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as AuthzMan
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl as ResourceMan
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl as PlatMan
import org.hyperic.hq.hqu.rendit.helpers.ResourceHelper
import org.hyperic.hq.hqu.server.session.Attachment
import org.hyperic.hq.hqu.server.session.UIPlugin
import org.hyperic.hq.hqu.AttachmentDescriptor
import org.hyperic.hq.hqu.SimpleAttachmentDescriptor
import org.hyperic.hq.hqu.ViewDescriptor
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl as PluginMan 
import org.hyperic.hq.hqu.server.session.AttachType
import org.hyperic.hq.hqu.server.session.View
import org.hyperic.hq.hqu.server.session.ViewAdmin
import org.hyperic.hq.hqu.server.session.ViewResource
import org.hyperic.hq.hqu.server.session.ViewMasthead
import org.hyperic.hq.hqu.server.session.ViewAdminCategory
import org.hyperic.hq.hqu.server.session.ViewResourceCategory
import org.hyperic.hq.hqu.server.session.ViewMastheadCategory


/**
 * The plugin's deployer is a class which deals with requests from the 
 * hosting application (HQ).  
 *
 * It is able to answer many questions, such as:
 *   - What is your name?
 *   - Describe yourself
 *   - Do you work with version X of HQ?
 *
 * And also able to process HQ actions, such as:
 *   - You have been deployed, what do you want to do?
 *   - You are being undeployed.
 */
class HQUPlugin implements IHQUPlugin {
    private Map views = [:]
    private Properties descriptor
    Log        log
    File       pluginDir
    
    HQUPlugin() {
    }
    
    void initialize(File pluginDir) {
        this.pluginDir = pluginDir
        this.descriptor = new Properties()
        new File(pluginDir, 'plugin.properties').withInputStream { s ->
            this.descriptor.load(s)
        }
        this.log = LogFactory.getLog("hqu.plugin.${name}")
    }
    
    /**
     * Add a view to be linked & viewed within HQ.
     *
     * The target URL which will be executed by HQ to render the view is
     * typically:  localhost:7080/hqu/pluginName/foo/myAction.hqu
     *                (given a controller of FooController)
     *
     * description:  The brief name of the view (e.g.: "Fast Executor")
     * attachType:   one of ['masthead', 'admin']
     * controller:   The controller to invoke when the view is to be generated
     * action:       The method within 'controller' to invoke
     * category:     (optional)  If set, specifies either 'tracker' or 'resource' menu
     */
    protected void addView(Map p) {
        def autoAttach  = p.get('autoAttach', true)
        def type        = p['attachType']
        def controller  = p['controller']
        def action      = p['action']
        def description = p['description']
        def category    = p['category']
        
        assert controller.name.endsWith('Controller'), 'Your controller must'+
               ' be end with Controller (e.g. FooController)'
        def controllerName = controller.name[0..-11].toLowerCase()
        def path = "/${controllerName}/${action}.hqu"
        p['path'] = path
        if (type == 'masthead') {
            addMastheadView(autoAttach, path, description, category) 
        } else if (type == 'admin') {
            addAdminView(autoAttach, path, description)
        } else if (type == 'resource') {
            views[description] = [type: 'resource'] + p
        } else {
            throw new IllegalArgumentException("type: must be ['masthead', " + 
                                               "'admin']")
        }
    }
    
    protected void addMastheadView(boolean autoAttach, String path,
                                   String description, String category)
    {
        views[description] = [type: 'masthead', autoAttach: autoAttach, 
                              path: path, description: description, 
                              category: category]
    }
    
    protected void addAdminView(boolean autoAttach, String path,
                                String description)
    {
        views[description] = [type: 'admin', autoAttach: autoAttach, 
                              path: path, description: description,
                              category: 'plugins']
    }
    
    void deploy(UIPlugin me) {
        for (e in views) {
            def name  = e.key
            def parms = e.value
            
            if (!parms.autoAttach)
                continue
                
            if (me.views.empty) {
                if (parms.type == 'masthead')
                    createAndAttachMasthead(me, name, parms)
                else if (parms.type == 'admin') 
                    createAndAttachAdmin(me, name, parms)
                else if (parms.type == 'resource')
                    createAndAttachResource(me, name, parms)
                else
                    log.error("Unknown view type ${parms.type}")
            }
        }
    }
    
    private findViewByPath(views, path) {
        for (v in views) {
            if (v.path == path)
                return v
        }
        return null
    }
    
    private void createAndAttachAdmin(UIPlugin me, String name, Map parms) {
        def pMan = PluginMan.one
        ViewAdmin view = findViewByPath(me.views, parms.path)
        
        if (view == null) {
            AttachType atype = AttachType.ADMIN
            ViewDescriptor vd = new ViewDescriptor(parms.path, 
                                                   parms.description, atype)
            view = pMan.createAdminView(me, vd)
        }
                
        if (view.attachments.empty) {
            ViewAdminCategory cat = 
                ViewAdminCategory.findByDescription(parms.category)
            pMan.attachView(view, cat)
        }
    }
    
    private void createAndAttachMasthead(UIPlugin me, String name, Map parms) {
        def pMan= PluginMan.one
        ViewMasthead view = findViewByPath(me.views, parms.path)
        
        if (view == null) {
            AttachType atype = AttachType.MASTHEAD
            ViewDescriptor vd = new ViewDescriptor(parms.path, 
                                                   parms.description, atype)
            view = pMan.createMastheadView(me, vd)
        }
                
        if (view.attachments.empty) {
            ViewMastheadCategory cat = 
                ViewMastheadCategory.findByDescription(parms.category)
            pMan.attachView(view, cat)
        }
    }
    
    /**
     * Create and attach a Resource view
     *
     * e.g.:
         addView(description:  'LiveExec',
                 attachType:   'resource',
                 resourceType: ['Linux', 'FileServer File', 'Nagios Plugin'],
                 controller:   LiveController,
                 action:       'index')
                 
     *
     * resourceType:  Defines a list a of resource types to attach type
     */
    private void createAndAttachResource(UIPlugin me, String name, Map p) {
        def pMan = PluginMan.one

        ViewResource view
        if (me.views.empty) {
            def vd = new ViewDescriptor(p.path, p.description,
                                        AttachType.RESOURCE)
            view = pMan.createResourceView(me, vd)
        } else {
            view = me.views.iterator().next()
        }

        if (p.platforms == 'all') {
            def platMan   = PlatMan.one
            for (pt in platMan.findAllPlatformTypes()) {
                if (p.byPlugin && pt.plugin != p.byPlugin)
                    continue
            
                def r = platMan.findResource(pt)
                if (resourceAttached(view, r))
                    continue

                pMan.attachView(view, ViewResourceCategory.VIEWS, r)
            }
        } 

        def rHelper = new ResourceHelper(getOverlord())
        if (p.resourceType) {
            for (typeName in p.resourceType) {
                def type = rHelper.findResourcePrototype(typeName)
                if (type == null) {
                    log.error("Unable to find resource prototype [" + 
                              "${typeName}]")
                    continue
                }
                if (!resourceAttached(view, type)) {
                    pMan.attachView(view, ViewResourceCategory.VIEWS, type)
                }
            }
        }
        
        if (p.toRoot) {
            def root = ResourceMan.one.findRootResource()
            if (!resourceAttached(view, root))
                pMan.attachView(view, ViewResourceCategory.VIEWS, root)
        }
    }
    
    private boolean resourceAttached(view, resource) {
        for (a in view.attachments) {
            if (a.resource == resource)
                return true
        }
        return false
    }
    
    
    Properties getDescriptor() {
        return this.descriptor
    }
    
    String getName() {
        descriptor.getProperty('plugin.name')
    }
    
    protected AuthzSubject getOverlord() {
        AuthzMan.one.getOverlordPojo() 
    }
    
    AttachmentDescriptor getAttachmentDescriptor(Attachment a, Resource r,
                                                 AuthzSubject u) 
    { 
        // If the view was setup via addView* from above, this should be
        // non-null
        def view = views[a.view?.description]
        
        if (view.type == 'resource') {
            if (view.showAttachmentIf in Closure) {
                if (!view.showAttachmentIf(a, r, u))
                    return null
            }
        }
                        
        new SimpleAttachmentDescriptor(a, 
                                       descriptor.getProperty('plugin.helpTag'), 
                                       description)
    }
    
    String getDescription() {
        def loader = this.class.classLoader
        def subloader = new URLClassLoader([new File((File)pluginDir, 'etc').toURL()] as URL[],
                                           loader)
        
        def file = "${name}_i18n"
        try {
            def b = ResourceBundle.getBundle(file, Locale.getDefault(), 
                                             subloader)
            return b.getString("${name}.description")
        } catch(MissingResourceException e) {
            log.warn("Unable to find resource bundle at " + 
                     "${pluginDir}/etc/${file}.properties or unable " +
                     "to find ${name}.description property inside it", e)
        }
        return "Error getting description"
    }
}
