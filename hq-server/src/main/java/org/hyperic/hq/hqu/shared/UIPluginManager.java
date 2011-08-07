/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.hqu.shared;

import java.util.Collection;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.ViewDescriptor;
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
 * Local interface for UIPluginManager.
 */
public interface UIPluginManager {

	public UIPlugin createPlugin(String name, String ver);

	public UIPlugin createOrUpdate(String name, String version);

	public View createAdminView(UIPlugin p, ViewDescriptor d);

	public View createMastheadView(UIPlugin p, ViewDescriptor d);

	public View createResourceView(UIPlugin p, ViewDescriptor d);

	public UIPlugin findPluginByName(String name);

	public UIPlugin findPluginById(Integer id);

	public View findViewById(Integer id);

	public Attachment findAttachmentById(Integer id);

	/**
	 * Remove a plugin, all its views, and attach points from the system.
	 */
	public void deletePlugin(UIPlugin p);

	public void detach(Attachment a);

	public void attachView(ViewMasthead view, ViewMastheadCategory cat);

	public void attachView(ViewAdmin view, ViewAdminCategory cat);

	public void attachView(ViewResource view, ViewResourceCategory cat,
			Resource r);

	public void updatePlugin(UIPlugin p, String version);

	/**
	 * Finds all {@link UIPlugin}s
	 */
	public Collection<UIPlugin> findAll();

	/**
	 * Find all the views attached via a specific attach type
	 * 
	 * @return a collection of {@link AttachType}s
	 */
	public Collection<View> findViews(AttachType type);

	/**
	 * Find all attachments for a specific type
	 * 
	 * @return a collection of {@link AttachmentDescriptor}s
	 */
	public Collection<AttachmentDescriptor> findAttachments(AttachType type,
			AuthzSubject user);

	public AttachmentDescriptor findAttachmentDescriptorById(Integer id,
			AuthzSubject user);

	/**
	 * Find attachments for a resource.
	 * 
	 * @return a collection of {@link AttachmentDescriptor}s
	 */
	public Collection<AttachmentDescriptor> findAttachments(AppdefEntityID ent,
			ViewResourceCategory cat, AuthzSubject user);
}
