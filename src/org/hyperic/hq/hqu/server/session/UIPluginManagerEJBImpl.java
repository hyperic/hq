/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.hqu.server.session;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.UIPluginDescriptor;
import org.hyperic.hq.hqu.ViewDescriptor;
import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.View;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.shared.UIPluginManagerLocal;
import org.hyperic.hq.hqu.shared.UIPluginManagerUtil;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;

/**
 * @ejb:bean name="UIPluginManager"
 *      jndi-name="ejb/hqu/UIPluginManager"
 *      local-jndi-name="LocalUIPluginManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class UIPluginManagerEJBImpl 
    implements SessionBean 
{
    private final Log _log = LogFactory.getLog(UIPluginManagerEJBImpl.class);

    private UIPluginDAO   _pluginDAO;
    private ViewDAO       _viewDAO;
    private AttachmentDAO _attachDAO;
    
    public UIPluginManagerEJBImpl() {
        DAOFactory fact = DAOFactory.getDAOFactory();
        _pluginDAO = new UIPluginDAO(fact);
        _viewDAO   = new ViewDAO(fact);
        _attachDAO = new AttachmentDAO(fact);
    }

    /**
     * @ejb:interface-method
     */
    public UIPlugin createPlugin(UIPluginDescriptor pInfo) {
        return _pluginDAO.create(pInfo);
    }
    
    /**
     * @ejb:interface-method
     */
    public UIPlugin createOrUpdate(UIPluginDescriptor pInfo) {
        UIPlugin p = findPluginByName(pInfo.getName());
        
        if (p == null) {
            _log.info("Creating plugin [" + pInfo.getName() + "]");
            p = _pluginDAO.create(pInfo);
        } else {
            _log.info("Updating plugin [" + pInfo.getName() + "]");
            updatePlugin(p, pInfo);
        }
        
        autoAttach(p, pInfo);
        return p;
    }

    private void autoAttach(UIPlugin p, UIPluginDescriptor pInfo) {
        for (Iterator i=p.getViews().iterator(); i.hasNext(); ) {
            View v = (View)i.next();
            
            for (Iterator j=pInfo.getViews().iterator(); j.hasNext(); ) {
                ViewDescriptor vd = (ViewDescriptor)j.next();
                
                if (!vd.getPath().equals(v.getPath()))
                    continue;
                
                AttachmentDescriptor protoDesc = v.getPrototype();
                
                if (vd.getAutoAttacher() != null) {
                    _log.info("Auto attaching [" + v + "]");
                    vd.getAutoAttacher().attach(v);
                }
            }
        }
    }
    
    /**
     * @ejb:interface-method
     */
    public UIPlugin findPluginByName(String name) {
        return _pluginDAO.findByName(name);
    }
    
    /**
     * @ejb:interface-method
     */
    public UIPlugin findPluginById(Integer id) {
        return _pluginDAO.findById(id);
    }
    
    /**
     * @ejb:interface-method
     */
    public View findViewById(Integer id) {
        return _viewDAO.findById(id);
    }

    /**
     * @ejb:interface-method
     */
    public Attachment findAttachmentById(Integer id) {
        return _attachDAO.findById(id);
    }

    /**
     * Remove a plugin, all its views, and attach points from the system.
     * @ejb:interface-method
     */
    public void deletePlugin(UIPlugin p) {
        _log.info("Deleting plugin " + p);
        _pluginDAO.remove(p);
    }

    /**
     * @ejb:interface-method
     */
    public void detach(Attachment a) {
        _log.info("Detaching " + a);
        a.getView().removeAttachment(a);
    }

    /**
     * @ejb:interface-method
     */
    public void attachView(View view, AttachmentDescriptor d) {
        AttachType viewType = view.getAttachType();
        
        if (!viewType.equals(d.getAttachType())) {
            throw new IllegalArgumentException("Attachment descriptor is " + 
                                               "incompatable with view");
        }

        if (!view.isAttachable(d)) {
            throw new IllegalArgumentException("View [" + view + "] is not " +
                                               "attachable");
        }
        
        _log.info("Attaching " + view + " [" + d + "]");
        viewType.attach(view, d);
    }
    
    /**
     * @ejb:interface-method
     */
    public void updatePlugin(UIPlugin p, UIPluginDescriptor pInfo) {
        if (!p.getDescription().equals(pInfo.getDescription()))
            p.setDescription(pInfo.getDescription());
        if (!p.getPluginVersion().equals(pInfo.getVersion()))
            p.setPluginVersion(pInfo.getVersion());

        // TODO:  What do we do here if the views for a particular plugin
        //        have changed?  Work it out.
    }

    /**
     * Finds all {@link UIPlugin}s
     * @ejb:interface-method
     */
    public Collection findAll() {
        return _pluginDAO.findAll();
    }
    
    /**
     * Find all the views attached via a specific attach type
     * 
     * @return a collection of {@link AttachType}s
     * @ejb:interface-method
     */
    public Collection findViews(AttachType type) {
        return _viewDAO.findFor(type);
    }
    
    /**
     * Find all attachments for a specific type
     * 
     * @return a collection of {@link Attachment}s
     * @ejb:interface-method
     */
    public Collection findAttachments(AttachType type) {
        return _attachDAO.findFor(type);
    }

    public static UIPluginManagerLocal getOne() {
        try {
            return UIPluginManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
