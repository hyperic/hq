package org.hyperic.hq.hqu.rendit

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.hqu.server.session.Attachment
import org.hyperic.hq.hqu.server.session.UIPlugin
import org.hyperic.hq.hqu.AttachmentDescriptor
import org.hyperic.hq.hqu.SimpleAttachmentDescriptor
import org.hyperic.hq.hqu.ViewDescriptor
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl as uiManImpl
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
        def uiMan = uiManImpl.one
        ViewAdmin view = findViewByPath(me.views, parms.path)
        
        if (view == null) {
            AttachType atype = AttachType.ADMIN
            ViewDescriptor vd = new ViewDescriptor(parms.path, 
                                                   parms.description, atype)
            view = uiMan.createAdminView(me, vd)
        }
                
        if (view.attachments.empty) {
            ViewAdminCategory cat = 
                ViewAdminCategory.findByDescription(parms.category)
            uiMan.attachView(view, cat)
        }
    }
    
    private void createAndAttachMasthead(UIPlugin me, String name, Map parms) {
        def uiMan = uiManImpl.one
        ViewMasthead view = findViewByPath(me.views, parms.path)
        
        if (view == null) {
            AttachType atype = AttachType.MASTHEAD
            ViewDescriptor vd = new ViewDescriptor(parms.path, 
                                                   parms.description, atype)
            view = uiMan.createMastheadView(me, vd)
        }
                
        if (view.attachments.empty) {
            ViewMastheadCategory cat = 
                ViewMastheadCategory.findByDescription(parms.category)
            uiMan.attachView(view, cat)
        }
    }
    
    Properties getDescriptor() {
        return this.descriptor
    }
    
    String getName() {
        descriptor.getProperty('plugin.name')
    }

    AttachmentDescriptor getAttachmentDescriptor(Attachment a, Resource r,
                                                 AuthzSubject u) 
    { 
        new SimpleAttachmentDescriptor(a, 
                                       descriptor.getProperty('plugin.helpTag'), 
                                       description)
    }
    
    String getDescription() {
        def loader = this.class.classLoader
        def subloader = new URLClassLoader([new File(pluginDir, 'etc').toURL()] as URL[],
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
