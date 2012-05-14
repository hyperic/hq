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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.RenditServer;
import org.hyperic.hq.hqu.ViewDescriptor;
import org.hyperic.hq.hqu.shared.UIPluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UIPluginManagerImpl implements UIPluginManager {
    private final static Log log = LogFactory.getLog(UIPluginManagerImpl.class.getName());

    private UIPluginDAO uiPluginDAO;
    private ViewDAO viewDAO;
    private AttachmentDAO attachmentDAO;
    private AttachmentResourceDAO attachmentResourceDAO;
    private RenditServer renditServer;
    private ResourceManager resourceManager;
    private ResourceGroupManager resourceGroupManager;

    @Autowired
    public UIPluginManagerImpl(AttachmentDAO attachmentDAO, AttachmentResourceDAO attachmentResourceDAO,
                               ViewDAO viewDAO, UIPluginDAO uiPluginDAO, RenditServer renditServer,
                               ResourceManager resourceManager, ResourceGroupManager resourceGroupManager) {
        this.attachmentDAO = attachmentDAO;
        this.attachmentResourceDAO = attachmentResourceDAO;
        this.viewDAO = viewDAO;
        this.uiPluginDAO = uiPluginDAO;
        this.renditServer = renditServer;
        this.resourceManager = resourceManager;
        this.resourceGroupManager = resourceGroupManager;
    }

    public UIPlugin createPlugin(String name, String version) {
        return uiPluginDAO.create(name, version);
    }

    public UIPlugin createOrUpdate(String name, String version) {
        UIPlugin p = findPluginByName(name);

        if (p == null) {
            log.info("Creating plugin [" + name + "]");
            p = createPlugin(name, version);
        } else {
            log.info("Updating plugin [" + name + "]");
            updatePlugin(p, version);
        }

        return p;
    }

    public View createAdminView(UIPlugin p, ViewDescriptor d) {
        View res = new ViewAdmin(p, d);

        p.addView(res);

        return res;
    }

    public View createMastheadView(UIPlugin p, ViewDescriptor d) {
        View res = new ViewMasthead(p, d);

        p.addView(res);

        return res;
    }

    public View createResourceView(UIPlugin p, ViewDescriptor d) {
        View res = new ViewResource(p, d);

        p.addView(res);

        return res;
    }

    @Transactional(readOnly=true)
    public UIPlugin findPluginByName(String name) {
        return uiPluginDAO.findByName(name);
    }

    @Transactional(readOnly=true)
    public UIPlugin findPluginById(Integer id) {
        return uiPluginDAO.findById(id);
    }

    @Transactional(readOnly=true)
    public View findViewById(Integer id) {
        return viewDAO.findById(id);
    }

    @Transactional(readOnly=true)
    public Attachment findAttachmentById(Integer id) {
        return attachmentDAO.findById(id);
    }

    /**
     * Remove a plugin, all its views, and attach points from the system.
     */
    public void deletePlugin(UIPlugin p) {
        log.info("Deleting plugin " + p);
        uiPluginDAO.remove(p);
    }

    public void detach(Attachment a) {
        log.info("Detaching " + a);

        a.getView().removeAttachment(a);
    }

    public void attachView(ViewMasthead view, ViewMastheadCategory cat) {
        if (!view.getAttachments().isEmpty()) {
            throw new IllegalArgumentException("View [" + view + "] already " + "attached");
        }

        view.addAttachment(new AttachmentMasthead(view, cat));

        log.info("Attaching [" + view + "] via [" + cat + "]");
    }

    public void attachView(ViewAdmin view, ViewAdminCategory cat) {
        if (!view.getAttachments().isEmpty()) {
            throw new IllegalArgumentException("View [" + view + "] already " + "attached");
        }

        view.addAttachment(new AttachmentAdmin(view, cat));

        log.info("Attaching [" + view + "] via [" + cat + "]");
    }

    @SuppressWarnings("unchecked")
    public void attachView(ViewResource view, ViewResourceCategory cat, Resource r) {
        for (Iterator<AttachmentResource> i = view.getAttachments().iterator(); i.hasNext();) {
            AttachmentResource a = i.next();
            if (a.getCategory().equals(cat) && a.getResource().equals(r)) {
                throw new IllegalArgumentException("View [" + view + "] is " + "already attached to [" + r + "] in [" +
                                                   cat + "]");
            }
        }
        view.addAttachment(new AttachmentResource(view, cat, r));
        log.info("Attaching [" + view + "] to [" + r + "] via [" + cat + "]");
    }

    public void updatePlugin(UIPlugin p, String version) {
        // On update, we need to clean the previous views out and recreate.
        // Recreation of views will be done when deploy is called on the plugin.
        Collection views = p.getViewsBag();
        if (!views.isEmpty()){
            for (Object viewObject : views){
                ((View)viewObject).getAttachmentsBag().clear();
            }
            views.clear();
            // need to flush the session to allow for adding the views back during deploy
            // within the same transaction.
            uiPluginDAO.flushSession();
        }
        if (!p.getPluginVersion().equals(version)) {
            log.info("Updating plugin version to " + version);
            p.setPluginVersion(version);
        }
    }

    /**
     * Finds all {@link UIPlugin}s
     */
    @Transactional(readOnly=true)
    public Collection<UIPlugin> findAll() {
        return uiPluginDAO.findAll();
    }

    /**
     * Find all the views attached via a specific attach type
     * TODO This does not appear to be used anywhere. Delete?
     * @return a collection of {@link AttachType}s
     */
    @Transactional(readOnly=true)
    public Collection<View> findViews(AttachType type) {
        return viewDAO.findFor(type);
    }

    /**
     * Find all attachments for a specific type
     * 
     * @return a collection of {@link AttachmentDescriptor}s
     */
    @Transactional(readOnly=true)
    public Collection<AttachmentDescriptor> findAttachments(AttachType type, AuthzSubject user) {
        Resource root = resourceManager.findRootResource();
        return convertAttachmentsToDescriptors(attachmentDAO.findFor(type), root, user);
    }

    @Transactional(readOnly=true)
    public AttachmentDescriptor findAttachmentDescriptorById(Integer id, AuthzSubject user) {
        Attachment attachment = findAttachmentById(id);
        List<Attachment> attachments = new ArrayList<Attachment>(1);

        attachments.add(attachment);

        Resource root = resourceManager.findRootResource();
        Collection<AttachmentDescriptor> attachmentDescriptors = convertAttachmentsToDescriptors(attachments, root,
            user);

        if (attachmentDescriptors.isEmpty()) {
            return null;
        }

        return attachmentDescriptors.iterator().next();
    }

    /**
     * Find attachments for a resource.
     * 
     * @return a collection of {@link AttachmentDescriptor}s
     */
    @Transactional(readOnly=true)
    public Collection<AttachmentDescriptor> findAttachments(AppdefEntityID ent, ViewResourceCategory cat,
                                                            AuthzSubject user) {
        Collection<Attachment> attachments;

        if (ent.isGroup()) {
            ResourceGroup group = resourceGroupManager.findResourceGroupById(ent.getId());

            attachments = attachmentResourceDAO.findFor(resourceManager.findRootResource(), cat);

            if (!group.isMixed()) {
                // For compatible groups add in attachments specific to that
                // resource type.
                Collection<Attachment> compatAttachments = attachmentResourceDAO.findFor(group.getResourcePrototype(),
                    cat);

                attachments.addAll(compatAttachments);
            }
        } else {
            attachments = attachmentResourceDAO.findFor(resourceManager.findResource(ent), cat);
        }

        Resource viewedResource = resourceManager.findResource(ent);

        return convertAttachmentsToDescriptors(attachments, viewedResource, user);
    }

    private Collection<AttachmentDescriptor> convertAttachmentsToDescriptors(Collection<Attachment> attachments,
                                                                             Resource viewedRsrc, AuthzSubject user) {
        Collection<AttachmentDescriptor> attachmentDescriptors = new ArrayList<AttachmentDescriptor>();

        for (Iterator<Attachment> i = attachments.iterator(); i.hasNext();) {
            Attachment attachment = i.next();
            String pluginName = attachment.getView().getPlugin().getName();
            AttachmentDescriptor attachmentDescriptor;

            try {
                attachmentDescriptor = (AttachmentDescriptor) renditServer.getAttachmentDescriptor(pluginName,
                    attachment, viewedRsrc, user);
            } catch (Exception e) {
                log.warn("Not returning attachment for [" + attachment + "], it " + "threw an exception", e);
                continue;
            }

            if (attachmentDescriptor != null) {
                attachmentDescriptors.add(attachmentDescriptor);
            } else {
                log.debug("Not returning attachment for [" + attachment + "], the " + "plugin says not to render it");
            }
        }

        return attachmentDescriptors;
    }

}
