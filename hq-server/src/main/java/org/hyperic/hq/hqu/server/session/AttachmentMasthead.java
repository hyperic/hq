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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "EAM_UI_ATTACH_MAST")
@PrimaryKeyJoinColumn(name = "ATTACH_ID", referencedColumnName = "ID")
public class AttachmentMasthead
    extends Attachment {
    @Column(name = "CATEGORY", nullable = false, length = 255)
    private String category;

    protected AttachmentMasthead() {
    }

    AttachmentMasthead(View<AttachmentMasthead> view, ViewMastheadCategory c) {
        super(view);
        category = c.getDescription();
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj))
            return false;

        if (obj instanceof AttachmentResource == false)
            return false;

        AttachmentResource o = (AttachmentResource) obj;

        return o.getCategoryEnum().equals(getCategoryEnum());
    }

    public ViewMastheadCategory getCategory() {
        return ViewMastheadCategory.findByDescription(category);
    }

    protected String getCategoryEnum() {
        return category;
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + getCategoryEnum().hashCode();
        return result;
    }

    protected void setCategoryEnum(String cat) {
        category = cat;
    }

    public String toString() {
        return super.toString() + " (under " + getCategory().getValue() + ")";
    }
}
