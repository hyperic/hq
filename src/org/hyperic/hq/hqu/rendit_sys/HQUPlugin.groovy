package org.hyperic.hq.hqu.rendit

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.hyperic.hq.hqu.server.session.UIPlugin

import org.hyperic.hq.hqu.ViewDescriptor
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl as uiManImpl
import org.hyperic.hq.hqu.server.session.AttachType
import org.hyperic.hq.hqu.server.session.View
import org.hyperic.hq.hqu.server.session.ViewResource
import org.hyperic.hq.hqu.server.session.ViewMasthead
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
    Log  log
    File pluginDir
    
    HQUPlugin() {
    }
    
    void initialize(File pluginDir) {
        this.pluginDir = pluginDir
        this.log       = LogFactory.getLog("hqu.plugin.${name}")
    }
    
    protected void addMastheadView(boolean autoAttach, String path,
                                   String description, String category)
    {
        views[description] = [type: 'masthead', autoAttach: autoAttach, 
                              path: path, description: description, 
                              category: category]
    }
    
    void deploy(UIPlugin me) {
        for (e in views) {
            def name  = e.key
            def parms = e.value
            
            if (!parms.autoAttach)
                continue
                
            if (me.views.empty) {
                createAndAttachMasthead(me, name, parms)
            }
        }
    }
    
    private void createAndAttachMasthead(UIPlugin me, String name, Map parms) {
        def uiMan = uiManImpl.one
        ViewMasthead view

        boolean foundView = false
        for (v in me.views) {
            if (v.path == parms.path) {
                foundView = true
                view = v
                break
            }
        }
        
        if (!foundView) {
            AttachType atype = AttachType.findByDescription(parms.type)
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
        Properties res = new Properties()
        new File(pluginDir, 'plugin.properties').withInputStream { s ->
            res.load(s)
        }
    	res
    }
    
    String getName() {
        descriptor.getProperty('plugin.name')
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
