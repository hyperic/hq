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
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.EntityNotFoundException;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.RenditServer;
import org.hyperic.hq.hqu.ViewDescriptor;
import org.hyperic.hq.hqu.data.AttachmentRepository;
import org.hyperic.hq.hqu.data.AttachmentResourceRepository;
import org.hyperic.hq.hqu.data.UIPluginRepository;
import org.hyperic.hq.hqu.data.ViewRepository;
import org.hyperic.hq.hqu.shared.UIPluginManager;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UIPluginManagerImpl implements UIPluginManager {
    private final static Log log = LogFactory.getLog(UIPluginManagerImpl.class.getName());

    private UIPluginRepository uiPluginRepository;
    private ViewRepository viewRepository;
    private AttachmentRepository attachmentRepository;
    private AttachmentResourceRepository attachmentResourceRepository;
    private RenditServer renditServer;
    private ResourceManager resourceManager;
    private ResourceGroupManager resourceGroupManager;

    @Autowired
    public UIPluginManagerImpl(AttachmentRepository attachmentRepository,
                               AttachmentResourceRepository attachmentResourceRepository, ViewRepository viewRepository,
                               UIPluginRepository uiPluginRepository, RenditServer renditServer,
                               ResourceManager resourceManager,
                               ResourceGroupManager resourceGroupManager) {
        this.attachmentRepository = attachmentRepository;
        this.attachmentResourceRepository = attachmentResourceRepository;
        this.viewRepository = viewRepository;
        this.uiPluginRepository = uiPluginRepository;
        this.renditServer = renditServer;
        this.resourceManager = resourceManager;
        this.resourceGroupManager = resourceGroupManager;
    }

    public UIPlugin createPlugin(String name, String ver) {
        UIPlugin uiPlugin = new UIPlugin(name,ver);
        return uiPluginRepository.save(uiPlugin);
    }

    public UIPlugin createOrUpdate(String name, String version) {
        UIPlugin p = findPluginByName(name);

        if (p == null) {
            log.info("Creating plugin [" + name + "]");
            UIPlugin uiPlugin = new UIPlugin(name,version);
            p = uiPluginRepository.save(uiPlugin);
        } else {
            log.info("Updating plugin [" + name + "]");
            updatePlugin(p, version);
        }

        return p;
    }

    public View<AttachmentAdmin> createAdminView(UIPlugin p, ViewDescriptor d) {
        View<AttachmentAdmin> res = new ViewAdmin(p, d);

        p.addView(res);

        return res;
    }

    public View<AttachmentMasthead> createMastheadView(UIPlugin p, ViewDescriptor d) {
        View<AttachmentMasthead> res = new ViewMasthead(p, d);

        p.addView(res);

        return res;
    }

    public View<AttachmentResource> createResourceView(UIPlugin p, ViewDescriptor d) {
        View<AttachmentResource> res = new ViewResource(p, d);

        p.addView(res);

        return res;
    }

    @Transactional(readOnly = true)
    public UIPlugin findPluginByName(String name) {
        return uiPluginRepository.findByName(name);
    }

    

    @Transactional(readOnly = true)
    public View findViewById(Integer id) {
        View view = viewRepository.findById(id);
        if(view == null) {
            throw new EntityNotFoundException("View with ID: " + id + " was not found");
        }
        return view;
    }

    @Transactional(readOnly = true)
    public Attachment findAttachmentById(Integer id) {
        Attachment attachment = attachmentRepository.findById(id);
        if (attachment == null) {
            throw new EntityNotFoundException("Attachment with ID: " + id + " was not found");
        }
        return attachment;
    }

    /**
     * Remove a plugin, all its views, and attach points from the system.
     */
    public void deletePlugin(UIPlugin p) {
        log.info("Deleting plugin " + p);

        uiPluginRepository.delete(p);
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

    public void attachView(ViewResource view, ViewResourceCategory cat, Resource r) {
        for (Iterator<AttachmentResource> i = view.getAttachments().iterator(); i.hasNext();) {
            AttachmentResource a = i.next();

            if (a.getCategory().equals(cat) && a.getResource().equals(r)) {
                throw new IllegalArgumentException("View [" + view + "] is " +
                                                   "already attached to [" + r + "] in [" + cat +
                                                   "]");
            }
        }

        view.addAttachment(new AttachmentResource(view, cat, r));

        log.info("Attaching [" + view + "] to [" + r + "] via [" + cat + "]");
    }

    public void updatePlugin(UIPlugin p, String version) {
        if (!p.getPluginVersion().equals(version)) {
            p.setPluginVersion(version);
        }

        // TODO: What do we do here if the views for a particular plugin
        // have changed? Work it out.
    }

    /**
     * Finds all {@link UIPlugin}s
     */
    @Transactional(readOnly = true)
    public Collection<UIPlugin> findAll() {
        return uiPluginRepository.findAll();
    }

    /**
     * Find all attachments for a specific type
     * 
     * @return a collection of {@link AttachmentDescriptor}s
     */
    @Transactional(readOnly = true)
    public Collection<AttachmentDescriptor> findAttachments(AttachType type, AuthzSubject user) {
        Resource root = resourceManager.findRootResource();

        return convertAttachmentsToDescriptors(attachmentRepository.findFor(type.getCode()), root, user);
    }

    @Transactional(readOnly = true)
    public AttachmentDescriptor findAttachmentDescriptorById(Integer id, AuthzSubject user) {
        Attachment attachment = findAttachmentById(id);
        List<Attachment> attachments = new ArrayList<Attachment>(1);

        attachments.add(attachment);

        Resource root = resourceManager.findRootResource();
        Collection<AttachmentDescriptor> attachmentDescriptors = convertAttachmentsToDescriptors(
            attachments, root, user);

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
    @Transactional(readOnly = true)
    public Collection<AttachmentDescriptor> findAttachments(AppdefEntityID ent,
                                                            ViewResourceCategory cat,
                                                            AuthzSubject user) {
        Collection<Attachment> attachments;

        if (ent.isGroup()) {
            ResourceGroup group = resourceGroupManager.findResourceGroupById(ent.getId());

            attachments = attachmentResourceRepository.findFor(resourceManager.findRootResource(), cat.getDescription());

            if (!resourceGroupManager.getGroupConvert(user, group).isMixed()) {
                // For compatible groups add in attachments specific to that
                // resource type.
                Collection<Attachment> compatAttachments = attachmentResourceRepository
                    .findFor(group, cat.getDescription());

                attachments.addAll(compatAttachments);
            }
        } else {
            attachments = attachmentResourceRepository.findFor(resourceManager.findResource(ent), cat.getDescription());
        }

        Resource viewedResource = resourceManager.findResource(ent);

        return convertAttachmentsToDescriptors(attachments, viewedResource, user);
    }

    private Collection<AttachmentDescriptor> convertAttachmentsToDescriptors(Collection<Attachment> attachments,
                                                                             Resource viewedRsrc,
                                                                             AuthzSubject user) {
        Collection<AttachmentDescriptor> attachmentDescriptors = new ArrayList<AttachmentDescriptor>();

        for (Iterator<Attachment> i = attachments.iterator(); i.hasNext();) {
            Attachment attachment = i.next();
            String pluginName = attachment.getView().getPlugin().getName();
            AttachmentDescriptor attachmentDescriptor;

            try {
                attachmentDescriptor = (AttachmentDescriptor) renditServer.getAttachmentDescriptor(
                    pluginName, attachment, viewedRsrc, user);
            } catch (Exception e) {
                log.warn("Not returning attachment for [" + attachment + "], it " +
                         "threw an exception", e);
                continue;
            }

            if (attachmentDescriptor != null) {
                attachmentDescriptors.add(attachmentDescriptor);
            } else {
                log.debug("Not returning attachment for [" + attachment + "], the " +
                          "plugin says not to render it");
            }
        }

        return attachmentDescriptors;
    }

}
