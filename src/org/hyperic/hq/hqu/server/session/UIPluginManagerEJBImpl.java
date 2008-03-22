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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.AppdefGroupManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.ViewDescriptor;
import org.hyperic.hq.hqu.rendit.RenditServer;
import org.hyperic.hq.hqu.shared.UIPluginManagerLocal;
import org.hyperic.hq.hqu.shared.UIPluginManagerUtil;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.hyperic.hq.hqu.server.session.View;
import org.hyperic.hq.hqu.server.session.ViewAdmin;
import org.hyperic.hq.hqu.server.session.ViewAdminCategory;
import org.hyperic.hq.hqu.server.session.ViewMasthead;
import org.hyperic.hq.hqu.server.session.ViewMastheadCategory;
import org.hyperic.hq.hqu.server.session.ViewResource;
import org.hyperic.hq.hqu.server.session.ViewResourceCategory;

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

    private UIPluginDAO           _pluginDAO;
    private ViewDAO               _viewDAO;
    private AttachmentDAO         _attachDAO;
    private AttachmentResourceDAO _attachRsrcDAO;
    
    public UIPluginManagerEJBImpl() {
        DAOFactory fact = DAOFactory.getDAOFactory();
        _pluginDAO     = new UIPluginDAO(fact);
        _viewDAO       = new ViewDAO(fact);
        _attachDAO     = new AttachmentDAO(fact);
        _attachRsrcDAO = new AttachmentResourceDAO(fact);
    }

    /**
     * @ejb:interface-method
     */
    public UIPlugin createPlugin(String name, String ver) {
        return _pluginDAO.create(name, ver);
    }
    
    /**
     * @ejb:interface-method
     */
    public UIPlugin createOrUpdate(String name, String version) {
        UIPlugin p = findPluginByName(name);
        
        if (p == null) {
            _log.info("Creating plugin [" + name + "]");
            p = _pluginDAO.create(name, version);
        } else {
            _log.info("Updating plugin [" + name + "]");
            updatePlugin(p, version);
        }
        return p;
    }
    
    /**
     * @ejb:interface-method
     */
    public View createAdminView(UIPlugin p, ViewDescriptor d) {
        View res = new ViewAdmin(p, d);
        p.addView(res);
        return res;
    }

    /**
     * @ejb:interface-method
     */
    public View createMastheadView(UIPlugin p, ViewDescriptor d) {
        View res = new ViewMasthead(p, d);
        p.addView(res);
        return res;
    }
    
    /**
     * @ejb:interface-method
     */
    public View createResourceView(UIPlugin p, ViewDescriptor d) {
        View res = new ViewResource(p, d);
        p.addView(res);
        return res;
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
    public void attachView(ViewMasthead view, ViewMastheadCategory cat) {
        if (!view.getAttachments().isEmpty()) {
            throw new IllegalArgumentException("View [" + view + "] already " + 
                                               "attached");
        }
        view.addAttachment(new AttachmentMasthead(view, cat));
        _log.info("Attaching [" + view + "] via [" + cat + "]");
    }

    /**
     * @ejb:interface-method
     */
    public void attachView(ViewAdmin view, ViewAdminCategory cat) {
        if (!view.getAttachments().isEmpty()) {
            throw new IllegalArgumentException("View [" + view + "] already " + 
                                               "attached");
        }
        view.addAttachment(new AttachmentAdmin(view, cat));
        _log.info("Attaching [" + view + "] via [" + cat + "]");
    }
    
    /**
     * @ejb:interface-method
     */
    public void attachView(ViewResource view, ViewResourceCategory cat,
                           Resource r) 
    {
        for (Iterator i=view.getAttachments().iterator(); i.hasNext(); ) {
            AttachmentResource a = (AttachmentResource)i.next();
            
            if (a.getCategory().equals(cat) && a.getResource().equals(r)) {
                throw new IllegalArgumentException("View [" + view + "] is " +
                            "already attached to [" + r + "] in [" + cat + "]");
            }
        }
        view.addAttachment(new AttachmentResource(view, cat, r));
        _log.info("Attaching [" + view + "] to [" + r + "] via [" + cat + "]");
    }

    /**
     * @ejb:interface-method
     */
    public void updatePlugin(UIPlugin p, String version) {
        if (!p.getPluginVersion().equals(version))
            p.setPluginVersion(version);

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
     * @return a collection of {@link AttachmentDescriptor}s
     * @ejb:interface-method
     */
    public Collection findAttachments(AttachType type, AuthzSubject user) {
        Resource root = ResourceManagerEJBImpl.getOne().findRootResource();

        return convertAttachmentsToDescriptors(_attachDAO.findFor(type), root,
                                               user);
    }

    private Integer appdefTypeToAuthzType(int appdefType) {
        switch(appdefType) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return AuthzConstants.authzPlatformProto;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            return AuthzConstants.authzServerProto;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            return AuthzConstants.authzServiceProto;
        }
        return null;
    }
    
    /**
     * @ejb:interface-method
     */
    public AttachmentDescriptor findAttachmentDescriptorById(Integer id,
                                                             AuthzSubject user) 
    {
        Attachment a = findAttachmentById(id);
        List c = new ArrayList(1);
        Collection res;
        Resource root = ResourceManagerEJBImpl.getOne().findRootResource();
        c.add(a);
        
        res = convertAttachmentsToDescriptors(c, root, user);
        if (res.isEmpty())
            return null;
        
        return (AttachmentDescriptor)res.iterator().next();
    }
    
    /**
     * Find attachments for a resource.
     * @return a collection of {@link AttachmentDescriptor}s
     * @ejb:interface-method
     */
    public Collection findAttachments(AppdefEntityID ent,
                                      ViewResourceCategory cat,
                                      AuthzSubject user) 
    {
        ResourceManagerLocal rman = ResourceManagerEJBImpl.getOne();
        Resource r;
        
        if (ent.isGroup()) {
            AuthzSubject overlord =
                AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo(); 
            AppdefGroupValue grp;
            
            try {
                grp = AppdefGroupManagerEJBImpl.getOne().findGroup(overlord, 
                                                                   ent);
            } catch(Exception e) {
                throw new SystemException("Unable to lookup attachments", e);
            }

            if (grp.isGroupAdhoc()) {
                r = rman.findRootResource();
            } else {
                Integer authzType = appdefTypeToAuthzType(grp.getGroupEntType());
                if (authzType == null)
                    return Collections.EMPTY_LIST;
                
                Integer entityType = new Integer(grp.getGroupEntResType());
                r = rman.findResourcePojoByInstanceId(authzType, entityType);
            }
        } else {
            r = rman.findResource(ent);
        } 
        
        Collection attachments = _attachRsrcDAO.findFor(r, cat);
        Resource viewedResource = 
            ResourceManagerEJBImpl.getOne().findResource(ent);
        return convertAttachmentsToDescriptors(attachments, viewedResource, 
                                               user);
    }
    
    private Collection convertAttachmentsToDescriptors(Collection attachments,
                                                       Resource viewedRsrc,
                                                       AuthzSubject user)
    {
        RenditServer rs = RenditServer.getInstance();
        
        Collection res = new ArrayList();
        for (Iterator i=attachments.iterator(); i.hasNext(); ) {
            Attachment a = (Attachment)i.next();
            String pluginName = a.getView().getPlugin().getName();
            AttachmentDescriptor d;
            
            try {
                d = (AttachmentDescriptor)
                    rs.getAttachmentDescriptor(pluginName, a, viewedRsrc, user); 
            } catch(Exception e) {
                _log.warn("Not returning attachment for [" + a + "], it " + 
                          "threw an exception", e);
                continue;
            }
            
            if (d != null) {
                res.add(d);
            } else {
                _log.debug("Not returning attachment for [" + a + "], the " +
                           "plugin says not to render it");
            }
        }
        return res; 
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
